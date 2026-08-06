#!/usr/bin/env bash
# =============================================================================
# Redis 8.0+ 向量索引初始化 / 运维脚本
# -----------------------------------------------------------------------------
# 配套：com.moyun.ext.ai.store.RedisEmbeddingStore
# 作用：手动预创建 / 验证 / 重建 Redis RediSearch 向量索引
#
# 要求：Redis 8.0+（内置 RediSearch / Query Engine）
#
# 用法：
#   ./redis-vector-init.sh create [dim]   # 创建索引（dim 默认 1024，千问 v3）
#   ./redis-vector-init.sh info          # 查看索引信息
#   ./redis-vector-init.sh count         # 统计索引文档数
#   ./redis-vector-init.sh drop           # 删除索引（保留数据）
#   ./redis-vector-init.sh dropall        # 删除索引 + 清空向量数据
#   ./redis-vector-init.sh recreate [dim] # 重建索引（先 drop 再 create）
#
# 环境变量（可在 .env 或命令前注入）：
#   REDIS_HOST          默认 127.0.0.1
#   REDIS_PORT          默认 6379
#   REDIS_PASSWORD      默认空
#   REDIS_DB            默认 0
#   INDEX_NAME          默认 moyun_ai_vectors
#   KEY_PREFIX          默认 ai_vectors:
# =============================================================================
set -euo pipefail

# ---------- 配置 ----------
REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
REDIS_PORT="${REDIS_PORT:-6379}"
REDIS_PASSWORD="${REDIS_PASSWORD:-}"
REDIS_DB="${REDIS_DB:-0}"
INDEX_NAME="${INDEX_NAME:-moyun_ai_vectors}"
KEY_PREFIX="${KEY_PREFIX:-ai_vectors:}"
DEFAULT_DIM="${EMBEDDING_DIM:-1024}"

# 颜色输出
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
info()  { echo -e "${CYAN}[INFO]${NC}  $*"; }
ok()    { echo -e "${GREEN}[OK]${NC}    $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
err()   { echo -e "${RED}[ERROR]${NC} $*" >&2; }

# ---------- redis-cli 封装 ----------
redis_cmd() {
    local args=(-h "$REDIS_HOST" -p "$REDIS_PORT" -n "$REDIS_DB")
    if [[ -n "$REDIS_PASSWORD" ]]; then
        args+=(-a "$REDIS_PASSWORD")
    fi
    # shellcheck disable=SC2086
    redis-cli "${args[@]}" --no-auth-warning "$@"
}

# 检查 redis-cli 是否可用
check_redis_cli() {
    if ! command -v redis-cli &>/dev/null; then
        err "未找到 redis-cli，请先安装 Redis 客户端工具"
        exit 1
    fi
}

# 检查 Redis 连接
check_connection() {
    if ! redis_cmd PING &>/dev/null; then
        err "无法连接 Redis: ${REDIS_HOST}:${REDIS_PORT} db=${REDIS_DB}"
        exit 1
    fi
    ok "Redis 连接正常: ${REDIS_HOST}:${REDIS_PORT} db=${REDIS_DB}"
}

# 检查 RediSearch 模块（Redis 8.0+ 内置）
check_module() {
    local modules
    modules=$(redis_cmd MODULE LIST 2>/dev/null || echo "")
    if echo "$modules" | grep -qi "search\|query"; then
        ok "RediSearch / Query Engine 模块已加载"
    else
        warn "未检测到 RediSearch 模块（Redis 需 ≥ 8.0，旧版 Redis Stack 需手动加载）"
        warn "继续执行可能失败，请确认 Redis 版本"
    fi
}

# 索引是否存在
index_exists() {
    redis_cmd FT.INFO "$INDEX_NAME" &>/dev/null
}

# ---------- 命令实现 ----------

cmd_create() {
    local dim="${1:-$DEFAULT_DIM}"
    info "创建索引: ${INDEX_NAME}，向量维度: ${dim}"

    if index_exists; then
        warn "索引 ${INDEX_NAME} 已存在，如需重建请使用 recreate"
        return 0
    fi

    # schema 必须与 RedisEmbeddingStore.createIndex 完全一致
    redis_cmd FT.CREATE "$INDEX_NAME" \
        ON HASH \
        PREFIX 1 "$KEY_PREFIX" \
        SCHEMA \
        text TEXT \
        embedding VECTOR HNSW 6 \
            TYPE FLOAT32 \
            DIM "$dim" \
            DISTANCE_METRIC COSINE \
        knowledgeBaseId TAG \
        fileName TEXT \
        pageNumber NUMERIC \
        segmentIndex NUMERIC \
        type TAG \
        fileType TAG

    if [[ $? -eq 0 ]]; then
        ok "索引 ${INDEX_NAME} 创建成功（dim=${dim}）"
    else
        err "索引创建失败"
        exit 1
    fi
}

cmd_info() {
    info "索引信息: ${INDEX_NAME}"
    if ! index_exists; then
        warn "索引 ${INDEX_NAME} 不存在"
        return 0
    fi
    redis_cmd FT.INFO "$INDEX_NAME"
}

cmd_count() {
    if ! index_exists; then
        warn "索引 ${INDEX_NAME} 不存在"
        return 0
    fi
    local total
    total=$(redis_cmd FT.SEARCH "$INDEX_NAME" '*' LIMIT 0 0 2>/dev/null | head -1)
    ok "索引 ${INDEX_NAME} 文档数: ${total:-0}"
}

cmd_drop() {
    if ! index_exists; then
        warn "索引 ${INDEX_NAME} 不存在，无需删除"
        return 0
    fi
    info "删除索引: ${INDEX_NAME}（保留 Hash 数据）"
    redis_cmd FT.DROPINDEX "$INDEX_NAME"
    ok "索引 ${INDEX_NAME} 已删除"
}

cmd_dropall() {
    info "删除索引 + 清空向量数据: ${INDEX_NAME}"

    # 1. 先查所有 key
    local key_count=0
    if index_exists; then
        key_count=$(redis_cmd FT.SEARCH "$INDEX_NAME" '*' NOCONTENT LIMIT 0 0 2>/dev/null | head -1 || echo 0)
        info "当前向量文档数: ${key_count}"
        # 删除索引
        redis_cmd FT.DROPINDEX "$INDEX_NAME" 2>/dev/null || true
    fi

    # 2. 扫描删除所有 key（兜底，确保 Hash 数据清除）
    info "扫描并删除 ${KEY_PREFIX}* 数据..."
    local cursor="0"
    local deleted=0
    while true; do
        # shellcheck disable=SC2207
        local resp=($(redis_cmd SCAN "$cursor" MATCH "${KEY_PREFIX}*" COUNT 500))
        cursor="${resp[0]}"
        local keys=("${resp[@]:1}")
        if [[ ${#keys[@]} -gt 0 ]]; then
            redis_cmd DEL "${keys[@]}" >/dev/null
            deleted=$((deleted + ${#keys[@]}))
        fi
        [[ "$cursor" == "0" ]] && break
    done
    ok "已删除 ${deleted} 个 Hash key，索引已清除"
}

cmd_recreate() {
    local dim="${1:-$DEFAULT_DIM}"
    info "重建索引: ${INDEX_NAME}（dim=${dim}）"
    cmd_drop
    sleep 1
    cmd_create "$dim"
}

# ---------- 主流程 ----------

main() {
    local action="${1:-}"
    shift || true

    case "$action" in
        create)   check_redis_cli; check_connection; check_module; cmd_create "$@" ;;
        info)     check_redis_cli; check_connection; cmd_info ;;
        count)    check_redis_cli; check_connection; cmd_count ;;
        drop)     check_redis_cli; check_connection; cmd_drop ;;
        dropall)  check_redis_cli; check_connection; cmd_dropall ;;
        recreate) check_redis_cli; check_connection; cmd_recreate "$@" ;;
        *)
            echo "用法: $0 {create [dim]|info|count|drop|dropall|recreate [dim]}"
            echo ""
            echo "命令说明："
            echo "  create [dim]   创建索引，dim 为向量维度（默认 ${DEFAULT_DIM}）"
            echo "  info           查看索引详情（FT.INFO）"
            echo "  count          统计索引文档数"
            echo "  drop           删除索引（保留 Hash 数据，可重新 create 恢复）"
            echo "  dropall        删除索引 + 清空所有向量 Hash 数据"
            echo "  recreate [dim] 重建索引（drop + create）"
            echo ""
            echo "环境变量："
            echo "  REDIS_HOST=${REDIS_HOST}  REDIS_PORT=${REDIS_PORT}  REDIS_DB=${REDIS_DB}"
            echo "  INDEX_NAME=${INDEX_NAME}  KEY_PREFIX=${KEY_PREFIX}"
            echo "  EMBEDDING_DIM=${DEFAULT_DIM}（千问 text-embedding-v3=1024, OpenAI text-embedding-3-small=1536）"
            exit 1
            ;;
    esac
}

main "$@"
