#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
=============================================================================
墨韵 AI 向量存储迁移脚本：Elasticsearch → Redis 8.0+ RediSearch
-----------------------------------------------------------------------------
配套：
  - 源端：com.moyun.ext.ai.store.ElasticsearchEmbeddingStore
  - 目标：com.moyun.ext.ai.store.RedisEmbeddingStore

数据结构映射：
  ES 文档                          →  Redis Hash (key = ai_vectors:{id})
  ─────────────────────────────────────────────────────────────────
  id                               →  id
  text                             →  text            (TEXT / BM25)
  embedding: [0.1, 0.2, ...]      →  embedding        (FLOAT32 小端序 bytes)
  metadata.knowledgeBaseId         →  knowledgeBaseId (TAG)
  metadata.fileName                →  fileName         (TEXT)
  metadata.pageNumber              →  pageNumber       (NUMERIC)
  metadata.segmentIndex            →  segmentIndex     (NUMERIC)
  metadata.type                    →  type             (TAG)
  metadata.fileType                →  fileType         (TAG)
  metadata.* (其余字段)            →  同名字段平铺写入

依赖：
  pip install elasticsearch>=8.0,<9.0 redis>=5.0

用法：
  python3 migrate-es-to-redis.py --dry-run          # 仅统计，不写入
  python3 migrate-es-to-redis.py                    # 执行迁移
  python3 migrate-es-to-redis.py --recreate-index   # 迁移前重建 Redis 索引

环境变量（或命令行参数覆盖）：
  ES_HOST          默认 localhost
  ES_PORT          默认 9200
  ES_INDEX         默认 moyun_ai_vectors
  ES_USERNAME      默认空
  ES_PASSWORD      默认空

  REDIS_HOST       默认 127.0.0.1
  REDIS_PORT       默认 6379
  REDIS_DB         默认 0
  REDIS_PASSWORD   默认空
  REDIS_INDEX      默认 moyun_ai_vectors
  REDIS_PREFIX     默认 ai_vectors:
=============================================================================
"""

import argparse
import os
import struct
import sys
import time

try:
    from elasticsearch import Elasticsearch
except ImportError:
    sys.exit("❌ 缺少依赖 elasticsearch，请执行: pip install 'elasticsearch>=8.0,<9.0'")

try:
    import redis
except ImportError:
    sys.exit("❌ 缺少依赖 redis，请执行: pip install 'redis>=5.0'")

# ─────────────────────── 配置 ───────────────────────

ES_HOST     = os.getenv("ES_HOST", "localhost")
ES_PORT     = int(os.getenv("ES_PORT", "9200"))
ES_INDEX    = os.getenv("ES_INDEX", "moyun_ai_vectors")
ES_USERNAME = os.getenv("ES_USERNAME", "")
ES_PASSWORD = os.getenv("ES_PASSWORD", "")

REDIS_HOST   = os.getenv("REDIS_HOST", "127.0.0.1")
REDIS_PORT   = int(os.getenv("REDIS_PORT", "6379"))
REDIS_DB     = int(os.getenv("REDIS_DB", "0"))
REDIS_PASSWORD = os.getenv("REDIS_PASSWORD", "")
REDIS_INDEX  = os.getenv("REDIS_INDEX", "moyun_ai_vectors")
REDIS_PREFIX = os.getenv("REDIS_PREFIX", "ai_vectors:")

SCROLL_SIZE  = 500        # ES scroll 批量
PIPELINE_SIZE = 500        # Redis pipeline 批量

# 索引 schema 字段定义（与 RedisEmbeddingStore.createIndex 一致）
# name → (type, 是否从 metadata 提取)
SCHEMA_FIELDS = [
    ("text",            "TEXT",    False),
    ("embedding",       "VECTOR",  False),
    ("knowledgeBaseId", "TAG",     True),
    ("fileName",        "TEXT",    True),
    ("pageNumber",      "NUMERIC", True),
    ("segmentIndex",    "NUMERIC", True),
    ("type",            "TAG",     True),
    ("fileType",        "TAG",     True),
]


def log(msg):
    ts = time.strftime("%H:%M:%S")
    print(f"[{ts}] {msg}", flush=True)


# ─────────────────────── ES 读取 ───────────────────────

def connect_es():
    hosts = [{"host": ES_HOST, "port": ES_PORT, "scheme": "https" if ES_PORT == 443 else "http"}]
    kwargs = {"hosts": hosts}
    if ES_USERNAME or ES_PASSWORD:
        kwargs["basic_auth"] = (ES_USERNAME, ES_PASSWORD)
        kwargs["verify_certs"] = False
        kwargs["ssl_show_warn"] = False
    es = Elasticsearch(**kwargs)
    if not es.ping():
        sys.exit(f"❌ 无法连接 Elasticsearch: {ES_HOST}:{ES_PORT}")
    log(f"✅ ES 连接成功: {ES_HOST}:{ES_PORT}, 索引: {ES_INDEX}")
    return es


def scan_es(es):
    """使用 scroll API 全量扫描 ES 索引，逐条 yield 文档"""
    resp = es.search(
        index=ES_INDEX,
        body={"query": {"match_all": {}}, "size": SCROLL_SIZE},
        scroll="2m",
    )
    scroll_id = resp["_scroll_id"]
    total = resp["hits"]["total"]["value"]
    log(f"📊 ES 文档总数: {total}")

    count = 0
    while True:
        hits = resp["hits"]["hits"]
        if not hits:
            break
        for hit in hits:
            yield hit
            count += 1
            if count % 1000 == 0:
                log(f"   已扫描 {count}/{total} ...")
        resp = es.scroll(scroll_id=scroll_id, scroll="2m")

    es.clear_scroll(scroll_id=scroll_id)
    log(f"✅ 扫描完成，共 {count} 条")


# ─────────────────────── Redis 写入 ───────────────────────

def connect_redis():
    r = redis.Redis(
        host=REDIS_HOST, port=REDIS_PORT, db=REDIS_DB,
        password=REDIS_PASSWORD or None, decode_responses=False,
    )
    r.ping()
    log(f"✅ Redis 连接成功: {REDIS_HOST}:{REDIS_PORT} db={REDIS_DB}")
    return r


def ensure_redis_index(r, dim, recreate=False):
    """创建 Redis RediSearch 索引（与 RedisEmbeddingStore.createIndex 一致）"""
    try:
        r.execute_command("FT.INFO", REDIS_INDEX)
        if recreate:
            log(f"🗑️ 删除已有索引: {REDIS_INDEX}")
            r.execute_command("FT.DROPINDEX", REDIS_INDEX)
        else:
            log(f"✅ Redis 索引已存在: {REDIS_INDEX}")
            return
    except redis.ResponseError as e:
        if "Unknown Index" not in str(e) and "no such" not in str(e).lower():
            raise

    # 构造 FT.CREATE 命令
    cmd = [
        "FT.CREATE", REDIS_INDEX,
        "ON", "HASH",
        "PREFIX", "1", REDIS_PREFIX,
        "SCHEMA",
        "text", "TEXT",
        "embedding", "VECTOR", "HNSW", "6",
        "TYPE", "FLOAT32",
        "DIM", str(dim),
        "DISTANCE_METRIC", "COSINE",
        "knowledgeBaseId", "TAG",
        "fileName", "TEXT",
        "pageNumber", "NUMERIC",
        "segmentIndex", "NUMERIC",
        "type", "TAG",
        "fileType", "TAG",
    ]
    r.execute_command(*cmd)
    log(f"✅ Redis 索引创建成功: {REDIS_INDEX} (dim={dim})")


def floats_to_bytes(floats):
    """float 数组 → FLOAT32 小端序 bytes（RediSearch VECTOR 要求）"""
    return struct.pack(f"<{len(floats)}f", *floats)


def build_hash_fields(doc_id, source):
    """ES 文档 → Redis Hash fields dict（bytes 编码）"""
    fields = {}

    # 顶层字段
    text = source.get("text") or ""
    fields[b"text"] = text.encode("utf-8")

    emb_list = source.get("embedding")
    if emb_list is None:
        return None, 0  # 无向量，跳过
    fields[b"embedding"] = floats_to_bytes(emb_list)
    dim = len(emb_list)

    # id
    fields[b"id"] = (source.get("id") or doc_id).encode("utf-8")

    # metadata 扁平化
    metadata = source.get("metadata") or {}
    for k, v in metadata.items():
        if v is None:
            continue
        fields[k.encode("utf-8")] = str(v).encode("utf-8")

    return fields, dim


# ─────────────────────── 迁移主流程 ───────────────────────

def detect_dimension(es):
    """从 ES 取第一条文档，检测向量维度"""
    resp = es.search(index=ES_INDEX, body={"query": {"match_all": {}}, "size": 1})
    hits = resp["hits"]["hits"]
    if not hits:
        sys.exit("❌ ES 索引中无文档，无需迁移")
    emb = hits[0]["_source"].get("embedding")
    if not emb:
        sys.exit("❌ 首条文档无 embedding 字段")
    dim = len(emb)
    log(f"📐 检测到向量维度: {dim}")
    return dim


def migrate(es, r, dry_run=False):
    total = 0
    skipped = 0
    dim_detected = 0
    pipe = r.pipeline(transaction=False)
    pipe_count = 0

    for hit in scan_es(es):
        doc_id = hit["_id"]
        source = hit["_source"]

        fields, dim = build_hash_fields(doc_id, source)
        if fields is None:
            skipped += 1
            continue

        # 检测/校验维度
        if dim_detected == 0:
            dim_detected = dim
            log(f"📐 检测到向量维度: {dim}")
        elif dim != dim_detected:
            log(f"⚠️ 文档 {doc_id} 维度 {dim} 与已检测 {dim_detected} 不一致，跳过")
            skipped += 1
            continue

        if dry_run:
            total += 1
            continue

        # HSET 写入 pipeline
        key = (REDIS_PREFIX + (source.get("id") or doc_id)).encode("utf-8")
        pipe.hset(key, mapping=fields)
        pipe_count += 1
        total += 1

        if pipe_count >= PIPELINE_SIZE:
            pipe.execute()
            pipe.reset()
            pipe_count = 0

    # 刷入剩余
    if pipe_count > 0 and not dry_run:
        pipe.execute()

    log(f"{'📊 [dry-run] ' if dry_run else '✅ '}迁移完成: 成功 {total} 条, 跳过 {skipped} 条, 维度 {dim_detected}")
    return dim_detected


def main():
    global ES_HOST, ES_PORT, ES_INDEX, ES_USERNAME, ES_PASSWORD
    global REDIS_HOST, REDIS_PORT, REDIS_DB, REDIS_PASSWORD, REDIS_INDEX, REDIS_PREFIX

    p = argparse.ArgumentParser(description="ES → Redis 8.0+ 向量迁移")
    p.add_argument("--dry-run", action="store_true", help="仅统计不写入")
    p.add_argument("--recreate-index", action="store_true", help="迁移前重建 Redis 索引")
    p.add_argument("--es-host", default=ES_HOST)
    p.add_argument("--es-port", type=int, default=ES_PORT)
    p.add_argument("--es-index", default=ES_INDEX)
    p.add_argument("--es-user", default=ES_USERNAME)
    p.add_argument("--es-pass", default=ES_PASSWORD)
    p.add_argument("--redis-host", default=REDIS_HOST)
    p.add_argument("--redis-port", type=int, default=REDIS_PORT)
    p.add_argument("--redis-db", type=int, default=REDIS_DB)
    p.add_argument("--redis-pass", default=REDIS_PASSWORD)
    p.add_argument("--redis-index", default=REDIS_INDEX)
    p.add_argument("--redis-prefix", default=REDIS_PREFIX)
    args = p.parse_args()

    ES_HOST, ES_PORT, ES_INDEX = args.es_host, args.es_port, args.es_index
    ES_USERNAME, ES_PASSWORD = args.es_user, args.es_pass
    REDIS_HOST, REDIS_PORT, REDIS_DB = args.redis_host, args.redis_port, args.redis_db
    REDIS_PASSWORD, REDIS_INDEX, REDIS_PREFIX = args.redis_pass, args.redis_index, args.redis_prefix

    log("=" * 60)
    log("墨韵 AI 向量迁移: Elasticsearch → Redis 8.0+")
    log(f"  ES   : {ES_HOST}:{ES_PORT} / {ES_INDEX}")
    log(f"  Redis: {REDIS_HOST}:{REDIS_PORT} db={REDIS_DB} / {REDIS_INDEX}")
    log(f"  模式 : {'dry-run（仅统计）' if args.dry_run else '迁移写入'}")
    log("=" * 60)

    es = connect_es()
    r = connect_redis()

    # 1. 先检测真实向量维度（从 ES 取首条文档）
    dim = detect_dimension(es)

    # 2. dry-run 模式仅统计，不创建索引也不写入
    if args.dry_run:
        migrate(es, r, dry_run=True)
        log("🎉 dry-run 完成")
        return

    # 3. 创建/重建 Redis 索引（使用检测到的真实维度）
    ensure_redis_index(r, dim=dim, recreate=args.recreate_index)

    # 4. 全量迁移写入
    migrate(es, r, dry_run=False)

    log("🎉 全部完成")


if __name__ == "__main__":
    main()
