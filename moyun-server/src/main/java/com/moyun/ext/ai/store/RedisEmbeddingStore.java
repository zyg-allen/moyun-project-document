package com.moyun.ext.ai.store;

import com.moyun.ext.ai.exception.BusinessException;
import com.moyun.ext.ai.exception.ErrorCode;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.output.ArrayOutput;
import io.lettuce.core.output.CommandOutput;
import io.lettuce.core.output.StatusOutput;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.lettuce.LettuceConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Redis 向量存储实现（基于 Redis 8.0+ 内置 RediSearch / Redis Query Engine）
 *
 * <p>功能特性：</p>
 * <ul>
 *   <li>使用 Redis Hash 存储文档（key = ai_vectors:{id}）</li>
 *   <li>RediSearch 索引：VECTOR HNSW FLOAT32 + TEXT（BM25）+ TAG 过滤</li>
 *   <li>向量检索（KNN + 余弦相似度）+ BM25 全文检索 + 混合检索</li>
 *   <li>按知识库 ID / 文档 ID 删除</li>
 *   <li>零额外依赖：复用 Spring Boot Data Redis（Lettuce），通过 LettuceConnection.execute 原生命令</li>
 * </ul>
 *
 * <p>Hash 结构：</p>
 * <pre>
 *   key: ai_vectors:{uuid}
 *   fields:
 *     id          - 文档ID
 *     text        - 文本内容（TEXT，建立BM25索引）
 *     embedding   - 向量二进制（FLOAT32 小端序，VECTOR HNSW）
 *     knowledgeBaseId - 知识库ID（TAG，过滤用）
 *     fileName / pageNumber / segmentIndex / type / fileType / score ...
 * </pre>
 *
 * <p>索引 schema（首次写入时按实际向量维度创建）：</p>
 * <pre>
 *   FT.CREATE moyun_ai_vectors ON HASH PREFIX 1 ai_vectors: SCHEMA
 *     text TEXT
 *     embedding VECTOR HNSW 6 TYPE FLOAT32 DIM {N} DISTANCE_METRIC COSINE
 *     knowledgeBaseId TAG
 *     fileName TEXT
 *     pageNumber NUMERIC
 *     segmentIndex NUMERIC
 *     type TAG
 *     fileType TAG
 * </pre>
 *
 * <p>要求：Redis 8.0+ 开源版（AGPLv3/RSALv2/SSPLv1 三选一），RediSearch 已并入主线。</p>
 *
 * @author laomao
 * @since 2025-12-15
 */
@Slf4j
@Component("embeddingStore")
@ConditionalOnProperty(name = "app.embedding-store.type", havingValue = "redis", matchIfMissing = true)
public class RedisEmbeddingStore implements VectorStoreExtension {

    private final StringRedisTemplate redisTemplate;
    private final String indexName;
    private final String keyPrefix;

    private volatile boolean indexInitialized = false;
    private volatile int indexDimension = 0;
    private final Object indexLock = new Object();

    public RedisEmbeddingStore(
            StringRedisTemplate redisTemplate,
            @Value("${app.embedding-store.index-name:moyun_ai_vectors}") String indexName,
            @Value("${app.embedding-store.key-prefix:ai_vectors:}") String keyPrefix) {
        this.redisTemplate = redisTemplate;
        this.indexName = indexName;
        this.keyPrefix = keyPrefix;
        log.info("RedisEmbeddingStore 初始化，索引名: {}, key前缀: {}", indexName, keyPrefix);
    }

    // ==================== 工具方法 ====================

    private static byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private static String str(Object o) {
        if (o == null) return null;
        if (o instanceof byte[]) return new String((byte[]) o, StandardCharsets.UTF_8);
        if (o instanceof Long) return String.valueOf(o);
        return o.toString();
    }

    /** float[] → byte[]（FLOAT32 小端，RediSearch VECTOR 要求） */
    private static byte[] floatArrayToBytes(float[] floats) {
        ByteBuffer buffer = ByteBuffer.allocate(floats.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float f : floats) {
            buffer.putFloat(f);
        }
        return buffer.array();
    }

    private static float[] bytesToFloatArray(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return new float[0];
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        float[] floats = new float[bytes.length / 4];
        for (int i = 0; i < floats.length; i++) {
            floats[i] = buffer.getFloat();
        }
        return floats;
    }

    /**
     * 发送 FT.* 原生命令，指定返回值类型 hint
     *
     * <p>必须用 {@link LettuceConnection#execute(String, CommandOutput, byte[]...)} 重载，
     * 默认 {@code execute(String, byte[]...)} 对 integer 返回值会抛
     * {@code ByteArrayOutput does not support set(long)}。</p>
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private Object dispatchFt(String command, CommandOutput outputHint, byte[]... args) {
        return redisTemplate.execute((RedisCallback<Object>) connection -> {
            if (!(connection instanceof LettuceConnection)) {
                throw new BusinessException(ErrorCode.ES_QUERY_FAILED,
                        "RedisEmbeddingStore 仅支持 Lettuce 连接，当前: " + connection.getClass().getName());
            }
            LettuceConnection lettuce = (LettuceConnection) connection;
            return lettuce.execute(command, outputHint, args);
        });
    }

    // ==================== 索引管理 ====================

    private void ensureIndexExists(int dimension) {
        if (indexInitialized && indexDimension == dimension) {
            return;
        }
        synchronized (indexLock) {
            if (indexInitialized && indexDimension == dimension) {
                return;
            }
            try {
                if (!indexExists()) {
                    log.info("索引 {} 不存在，使用维度 {} 创建...", indexName, dimension);
                    createIndex(dimension);
                    log.info("✅ 索引 {} 创建成功，向量维度: {}", indexName, dimension);
                } else {
                    log.info("✅ 索引 {} 已存在", indexName);
                }
                indexInitialized = true;
                indexDimension = dimension;
            } catch (Exception e) {
                log.error("❌ 初始化 Redis 向量索引失败: {}", e.getMessage(), e);
                throw new BusinessException(ErrorCode.ES_QUERY_FAILED,
                        "初始化 Redis 索引失败: " + e.getMessage(), e);
            }
        }
    }

    private boolean indexExists() {
        try {
            Object result = dispatchFt("FT.INFO",
                    new ArrayOutput<>(ByteArrayCodec.INSTANCE), utf8(indexName));
            return result != null;
        } catch (Exception e) {
            // 索引不存在时 FT.INFO 抛 "Unknown Index name"
            return false;
        }
    }

    private void createIndex(int dimension) {
        List<byte[]> args = new ArrayList<>();
        Collections.addAll(args,
                utf8(indexName),
                utf8("ON"), utf8("HASH"),
                utf8("PREFIX"), utf8("1"), utf8(keyPrefix),
                utf8("SCHEMA"),
                utf8("text"), utf8("TEXT"),
                // HNSW {n}：n 为属性 token 总数（TYPE+FLOAT32+DIM+{dim}+DISTANCE_METRIC+COSINE = 6）
                utf8("embedding"), utf8("VECTOR"), utf8("HNSW"), utf8("6"),
                utf8("TYPE"), utf8("FLOAT32"),
                utf8("DIM"), utf8(String.valueOf(dimension)),
                utf8("DISTANCE_METRIC"), utf8("COSINE"),
                utf8("knowledgeBaseId"), utf8("TAG"),
                utf8("fileName"), utf8("TEXT"),
                utf8("pageNumber"), utf8("NUMERIC"),
                utf8("segmentIndex"), utf8("NUMERIC"),
                utf8("type"), utf8("TAG"),
                utf8("fileType"), utf8("TAG")
        );
        Object result = dispatchFt("FT.CREATE",
                new StatusOutput<>(ByteArrayCodec.INSTANCE),
                args.toArray(new byte[0][]));
        log.debug("FT.CREATE 结果: {}", result);
    }

    // ==================== EmbeddingStore 标准接口 ====================

    @Override
    public String add(Embedding embedding) {
        String id = UUID.randomUUID().toString();
        add(id, embedding);
        return id;
    }

    @Override
    public void add(String id, Embedding embedding) {
        addInternal(id, embedding, null);
    }

    @Override
    public String add(Embedding embedding, TextSegment textSegment) {
        String id = UUID.randomUUID().toString();
        addInternal(id, embedding, textSegment);
        return id;
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings) {
        List<String> ids = new ArrayList<>();
        for (Embedding e : embeddings) ids.add(add(e));
        return ids;
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings, List<TextSegment> textSegments) {
        if (embeddings.size() != textSegments.size()) {
            throw new IllegalArgumentException("嵌入和文本段的数量必须相同");
        }
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < embeddings.size(); i++) {
            ids.add(add(embeddings.get(i), textSegments.get(i)));
        }
        return ids;
    }

    private void addInternal(String id, Embedding embedding, TextSegment textSegment) {
        try {
            int dimension = embedding.dimension();
            ensureIndexExists(dimension);

            String key = keyPrefix + id;
            Map<byte[], byte[]> fields = new HashMap<>();
            fields.put(utf8("id"), utf8(id));
            fields.put(utf8("embedding"), floatArrayToBytes(embedding.vector()));

            if (textSegment != null) {
                fields.put(utf8("text"), utf8(textSegment.text()));
                if (textSegment.metadata() != null) {
                    Map<String, Object> metaMap = textSegment.metadata().toMap();
                    metaMap.forEach((k, v) -> {
                        if (v != null) {
                            fields.put(utf8(k), utf8(v.toString()));
                        }
                    });
                }
            }

            redisTemplate.execute((RedisCallback<Object>) connection -> {
                connection.hashCommands().hMSet(utf8(key), fields);
                return null;
            });

            log.debug("向量文档已添加到 Redis, id={}", id);
        } catch (Exception e) {
            log.error("添加向量到 Redis 失败, id={}, error={}", id, e.getMessage(), e);
            throw new BusinessException(ErrorCode.ES_QUERY_FAILED, "添加向量到 Redis 失败", e);
        }
    }

    // ==================== 向量检索（KNN） ====================

    @Override
    public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
        Embedding queryEmb = request.queryEmbedding();
        int maxResults = request.maxResults();
        double minScore = request.minScore();
        dev.langchain4j.store.embedding.filter.Filter filter = request.filter();

        byte[] queryVec = floatArrayToBytes(queryEmb.vector());

        // KNN query: "(* | @filter)=>[KNN $K $VEC AS score]"
        String filterPart = filter != null ? buildFilterQuery(filter) : "*";
        String query = filterPart + "=>[KNN $K $VEC AS score]";

        List<byte[]> args = new ArrayList<>();
        Collections.addAll(args,
                utf8(indexName),
                utf8(query),
                utf8("WITHSCORES"),
                utf8("LIMIT"), utf8("0"), utf8(String.valueOf(maxResults)),
                utf8("PARAMS"), utf8("4"),
                utf8("K"), utf8(String.valueOf(maxResults)),
                utf8("VEC"), queryVec,
                utf8("DIALECT"), utf8("2")
        );

        Object result;
        try {
            result = dispatchFt("FT.SEARCH",
                    new ArrayOutput<>(ByteArrayCodec.INSTANCE),
                    args.toArray(new byte[0][]));
        } catch (Exception e) {
            log.error("Redis 向量检索失败: {}", e.getMessage(), e);
            return new EmbeddingSearchResult<>(Collections.emptyList());
        }

        List<EmbeddingMatch<TextSegment>> matches = parseSearchResult(result, minScore, ScoreMode.KNN);
        log.debug("Redis KNN 检索到 {} 个匹配, minScore={}", matches.size(), minScore);
        return new EmbeddingSearchResult<>(matches);
    }

    /**
     * 构造 langchain4j Filter → RediSearch query 表达式
     */
    private String buildFilterQuery(dev.langchain4j.store.embedding.filter.Filter filter) {
        if (filter instanceof dev.langchain4j.store.embedding.filter.comparison.IsEqualTo) {
            dev.langchain4j.store.embedding.filter.comparison.IsEqualTo eq =
                    (dev.langchain4j.store.embedding.filter.comparison.IsEqualTo) filter;
            return "@" + eq.key() + ":{" + eq.comparisonValue() + "}";
        }
        if (filter instanceof dev.langchain4j.store.embedding.filter.comparison.IsIn) {
            dev.langchain4j.store.embedding.filter.comparison.IsIn in =
                    (dev.langchain4j.store.embedding.filter.comparison.IsIn) filter;
            Collection<?> values = in.comparisonValues();
            String joined = values.stream().map(Object::toString)
                    .collect(java.util.stream.Collectors.joining("|"));
            return "@" + in.key() + ":{" + joined + "}";
        }
        log.warn("⚠️ 不支持的过滤器类型: {}, 使用 match all", filter.getClass().getSimpleName());
        return "*";
    }

    // ==================== VectorStoreExtension 扩展接口 ====================

    @Override
    public List<EmbeddingMatch<TextSegment>> bm25Search(String queryText, String knowledgeBaseId, int maxResults) {
        try {
            log.info("BM25检索(Redis) - query: {}, knowledgeBaseId: {}, maxResults: {}",
                    queryText, knowledgeBaseId, maxResults);

            // 转义 RediSearch 查询语法的特殊字符，保留空格作为分词分隔
            String escaped = queryText.replaceAll("[\"\\*\\(\\)\\[\\]\\|:{}\\\\]", " ").trim();
            if (escaped.isEmpty()) {
                return Collections.emptyList();
            }

            // @text:(word1 word2) @knowledgeBaseId:{kbId}
            // 注意：空格在 RediSearch 中默认是 AND，多词想 OR 用 | 分隔
            // 这里用空格（AND 语义），符合"全部命中"的检索预期
            String query = "@text:(" + escaped + ") @knowledgeBaseId:{" + knowledgeBaseId + "}";

            List<byte[]> args = new ArrayList<>();
            Collections.addAll(args,
                    utf8(indexName),
                    utf8(query),
                    utf8("WITHSCORES"),
                    utf8("LIMIT"), utf8("0"), utf8(String.valueOf(maxResults)),
                    utf8("DIALECT"), utf8("1")
            );

            Object result = dispatchFt("FT.SEARCH",
                    new ArrayOutput<>(ByteArrayCodec.INSTANCE),
                    args.toArray(new byte[0][]));

            List<EmbeddingMatch<TextSegment>> matches = parseSearchResult(result, 0.0, ScoreMode.BM25);
            log.info("✅ BM25检索完成(Redis) - 结果数: {}", matches.size());
            return matches;
        } catch (Exception e) {
            log.error("❌ BM25检索失败(Redis)", e);
            return Collections.emptyList();
        }
    }

    @Override
    public int deleteByKnowledgeBaseId(String knowledgeBaseId) {
        try {
            log.info("🗑️ 删除知识库向量(Redis): knowledgeBaseId={}", knowledgeBaseId);

            // 1. 查出该知识库下所有 docId（NOCONTENT 提升性能）
            List<byte[]> searchArgs = new ArrayList<>();
            Collections.addAll(searchArgs,
                    utf8(indexName),
                    utf8("@knowledgeBaseId:{" + knowledgeBaseId + "}"),
                    utf8("NOCONTENT"),
                    utf8("LIMIT"), utf8("0"), utf8("100000")
            );
            Object result = dispatchFt("FT.SEARCH",
                    new ArrayOutput<>(ByteArrayCodec.INSTANCE),
                    searchArgs.toArray(new byte[0][]));

            if (!(result instanceof List)) return 0;
            List<?> list = (List<?>) result;
            if (list.size() < 2) return 0;

            // 2. 收集所有 docId（从 index 1 开始，跳过 total）
            List<String> docIds = new ArrayList<>();
            for (int i = 1; i < list.size(); i++) {
                String docId = str(list.get(i));
                if (docId != null && !docId.isEmpty()) {
                    docIds.add(docId);
                }
            }

            // 3. 批量 DEL
            int deleted = 0;
            for (String docId : docIds) {
                Boolean ok = redisTemplate.delete(keyPrefix + docId);
                if (Boolean.TRUE.equals(ok)) deleted++;
            }

            log.info("✅ 删除知识库向量完成(Redis): knowledgeBaseId={}, 删除数量={}", knowledgeBaseId, deleted);
            return deleted;
        } catch (Exception e) {
            log.error("❌ 删除知识库向量失败(Redis): knowledgeBaseId={}, error={}",
                    knowledgeBaseId, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public boolean deleteById(String documentId) {
        try {
            Boolean ok = redisTemplate.delete(keyPrefix + documentId);
            log.debug("✅ 删除向量(Redis): id={}, result={}", documentId, ok);
            return Boolean.TRUE.equals(ok);
        } catch (Exception e) {
            log.error("❌ 删除向量失败(Redis): id={}, error={}", documentId, e.getMessage());
            return false;
        }
    }

    // ==================== 结果解析 ====================

    /** 分数转换模式 */
    private enum ScoreMode { KNN, BM25, RAW }

    /**
     * 解析 FT.SEARCH WITHSCORES 返回结果
     *
     * <p>格式：[total(Long), docId1, score1, fields1(List), docId2, score2, fields2, ...]</p>
     *
     * @param result   FT.SEARCH 返回的 List
     * @param minScore 最低相似度阈值（过滤）
     * @param mode     分数转换模式
     */
    @SuppressWarnings("unchecked")
    private List<EmbeddingMatch<TextSegment>> parseSearchResult(Object result, double minScore, ScoreMode mode) {
        List<EmbeddingMatch<TextSegment>> matches = new ArrayList<>();
        if (!(result instanceof List)) return matches;
        List<Object> list = (List<Object>) result;
        if (list.size() < 4) return matches; // 至少 total + 1 个 doc(含 score+fields)

        // 跳过 index 0 (total)，每个文档占 3 位：docId, score, fields
        int i = 1;
        while (i + 2 < list.size()) {
            Object docIdObj = list.get(i);
            Object scoreObj = list.get(i + 1);
            Object fieldsObj = list.get(i + 2);
            i += 3;

            if (!(fieldsObj instanceof List)) continue;

            // 解析 fields
            List<Object> fieldsList = (List<Object>) fieldsObj;
            Map<String, byte[]> fieldMap = new HashMap<>();
            byte[] embeddingBytes = null;
            for (int j = 0; j + 1 < fieldsList.size(); j += 2) {
                String fName = str(fieldsList.get(j));
                Object fVal = fieldsList.get(j + 1);
                if (fName == null) continue;
                byte[] valBytes = fVal instanceof byte[] ? (byte[]) fVal : null;
                fieldMap.put(fName, valBytes);
                if ("embedding".equals(fName)) {
                    embeddingBytes = valBytes;
                }
            }

            // 分数转换
            double score = convertScore(scoreObj, mode);
            if (score < minScore) continue;

            // 构建 TextSegment
            String docId = str(docIdObj);
            byte[] textBytes = fieldMap.get("text");
            String text = textBytes != null ? new String(textBytes, StandardCharsets.UTF_8) : "";

            Metadata metadata = new Metadata();
            fieldMap.forEach((k, v) -> {
                if (!"text".equals(k) && !"embedding".equals(k) && v != null) {
                    metadata.put(k, new String(v, StandardCharsets.UTF_8));
                }
            });

            TextSegment segment = TextSegment.from(text, metadata);
            Embedding emb = embeddingBytes != null ? Embedding.from(bytesToFloatArray(embeddingBytes)) : null;

            matches.add(new EmbeddingMatch<>(score, docId, emb, segment));
        }
        return matches;
    }

    /**
     * 分数转换：
     * <ul>
     *   <li>KNN: RediSearch HNSW cosine 返回 distance ∈ [0,2]，相似度 = 1 - distance/2 ∈ [0,1]</li>
     *   <li>BM25: 分数通常 0-10+，归一化 min(score/20, 1)</li>
     *   <li>RAW: 直接解析</li>
     * </ul>
     */
    private double convertScore(Object scoreObj, ScoreMode mode) {
        if (scoreObj == null) return 0.0;
        double raw;
        try {
            if (scoreObj instanceof byte[]) {
                raw = Double.parseDouble(new String((byte[]) scoreObj, StandardCharsets.UTF_8));
            } else {
                raw = Double.parseDouble(scoreObj.toString());
            }
        } catch (NumberFormatException e) {
            return 0.0;
        }
        switch (mode) {
            case KNN: return Math.max(0.0, Math.min(1.0, 1.0 - raw / 2.0));
            case BM25: return Math.min(raw / 20.0, 1.0);
            default: return raw;
        }
    }
}
