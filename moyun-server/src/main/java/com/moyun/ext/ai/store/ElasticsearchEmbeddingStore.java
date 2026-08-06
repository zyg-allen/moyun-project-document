package com.moyun.ext.ai.store;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.moyun.ext.ai.exception.BusinessException;
import com.moyun.ext.ai.exception.ErrorCode;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Elasticsearch向量存储实现
 *
 * <p>功能特性：</p>
 * <ul>
 *   <li>使用ES 8.14.3的dense_vector类型存储向量</li>
 *   <li>采用文档对象方式，灵活可扩展</li>
 *   <li>支持余弦相似度向量搜索</li>
 *   <li>自动创建和管理索引</li>
 * </ul>
 *
 * <p>文档结构：</p>
 * <pre>
 * {
 *   "id": "文档ID",
 *   "text": "文本内容",
 *   "embedding": [向量数据],
 *   "metadata": {元数据键值对}
 * }
 * </pre>
 *
 * @author: laomao
 * @time: 2025/11/23
 */
@Slf4j
@Component("embeddingStore")
@ConditionalOnProperty(name = "app.embedding-store.type", havingValue = "es")
public class ElasticsearchEmbeddingStore implements VectorStoreExtension {

    private final ElasticsearchClient client;
    private final String indexName;

    /**
     * ES中存储的文档结构
     */
    public static class VectorDocument {
        private String id;
        private List<Float> embedding;
        private String text;
        private Map<String, String> metadata;

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public List<Float> getEmbedding() { return embedding; }
        public void setEmbedding(List<Float> embedding) { this.embedding = embedding; }

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }

        public Map<String, String> getMetadata() { return metadata; }
        public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
    }

    private volatile boolean indexInitialized = false;
    private final Object indexLock = new Object();

    public ElasticsearchEmbeddingStore(
            ElasticsearchClient client,
            @Value("${app.embedding-store.index-name:${elasticsearch.index.vector:moyun_ai_vectors}}") String indexName) {
        this.client = client;
        this.indexName = indexName;
        // 不在构造函数中创建索引，等到第一次添加数据时根据实际维度创建
        log.info("ElasticsearchEmbeddingStore初始化，索引名: {}", indexName);
    }

    /**
     * 确保索引存在，如果不存在则根据实际向量维度创建
     */
    private void ensureIndexExists(int dimension) {
        if (indexInitialized) {
            return;
        }

        synchronized (indexLock) {
            if (indexInitialized) {
                return;
            }

            try {
                // 检查索引是否存在
                boolean exists = client.indices().exists(
                    ExistsRequest.of(e -> e.index(indexName))
                ).value();

                if (!exists) {
                    log.info("索引 {} 不存在，使用维度 {} 创建索引...", indexName, dimension);

                    // 创建索引，使用实际的向量维度
                    client.indices().create(c -> c
                        .index(indexName)
                        .mappings(m -> m
                            .properties("id", p -> p.keyword(k -> k))
                            .properties("text", p -> p.text(t -> t))
                            .properties("embedding", p -> p.denseVector(d -> d
                                .dims(dimension) // 使用实际维度
                                .index(true)
                                .similarity("cosine")
                            ))
                            .properties("metadata", p -> p.object(o -> o.enabled(true)))
                        )
                    );

                    log.info("✅ 索引 {} 创建成功，向量维度: {}", indexName, dimension);
                } else {
                    log.info("✅ 索引 {} 已存在", indexName);
                }

                indexInitialized = true;

            } catch (Exception e) {
                log.error("❌ 初始化Elasticsearch索引失败: {}", e.getMessage(), e);
                throw new BusinessException(ErrorCode.ES_QUERY_FAILED, "初始化ES索引失败: " + e.getMessage(), e);
            }
        }
    }

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
        for (Embedding embedding : embeddings) {
            ids.add(add(embedding));
        }
        return ids;
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings, List<TextSegment> textSegments) {
        if (embeddings.size() != textSegments.size()) {
            throw new IllegalArgumentException("嵌入和文本段的数量必须相同");
        }

        List<String> ids = new ArrayList<>();
        for (int i = 0; i < embeddings.size(); i++) {
            String id = UUID.randomUUID().toString();
            addInternal(id, embeddings.get(i), textSegments.get(i));
            ids.add(id);
        }
        return ids;
    }

    /**
     * 内部添加方法
     */
    private void addInternal(String id, Embedding embedding, TextSegment textSegment) {
        try {
            // 确保索引存在（使用实际向量维度）
            int dimension = embedding.dimension();
            ensureIndexExists(dimension);

            VectorDocument doc = new VectorDocument();
            doc.setId(id);

            // 转换向量为Float列表
            doc.setEmbedding(toFloatList(embedding.vector()));

            // 设置文本和元数据
            if (textSegment != null) {
                doc.setText(textSegment.text());
                if (textSegment.metadata() != null) {
                    Map<String, String> metadata = new HashMap<>();
                    // 使用toMap()方法获取所有元数据
                    Map<String, Object> metadataMap = textSegment.metadata().toMap();
                    metadataMap.forEach((k, v) ->
                        metadata.put(k, v != null ? v.toString() : null)
                    );
                    doc.setMetadata(metadata);
                }
            }

            // 索引文档
            IndexResponse response = client.index(i -> i
                .index(indexName)
                .id(id)
                .document(doc)
            );

            log.debug("向量文档已添加到ES, id={}, result={}", id, response.result());

        } catch (Exception e) {
            log.error("添加向量到ES失败, id={}, error={}", id, e.getMessage(), e);
            throw new BusinessException(ErrorCode.ES_QUERY_FAILED, "添加向量到ES失败", e);
        }
    }

    /**
     * 将float数组转换为Float列表
     */
    private List<Float> toFloatList(float[] array) {
        List<Float> list = new ArrayList<>(array.length);
        for (float v : array) {
            list.add(v);
        }
        return list;
    }

    /**
     * 将Float列表转换为float数组
     */
    private float[] toFloatArray(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    /**
     * 查找相关向量（辅助方法）
     */
    private List<EmbeddingMatch<TextSegment>> findRelevant(Embedding referenceEmbedding, int maxResults, double minScore) {
        try {
            // 转换向量
            List<Float> queryVector = toFloatList(referenceEmbedding.vector());

            // 使用script_score查询进行向量搜索
            SearchResponse<VectorDocument> response = client.search(s -> s
                .index(indexName)
                .size(maxResults)
                .query(q -> q
                    .scriptScore(ss -> ss
                        .query(qq -> qq.matchAll(m -> m))
                        .script(sc -> sc
                            .inline(i -> i
                                .source("cosineSimilarity(params.queryVector, 'embedding') + 1.0")
                                .params("queryVector", JsonData.of(queryVector))
                            )
                        )
                    )
                )
                .minScore(minScore),
                VectorDocument.class
            );

            // 转换结果
            List<EmbeddingMatch<TextSegment>> matches = new ArrayList<>();
            for (Hit<VectorDocument> hit : response.hits().hits()) {
                VectorDocument doc = hit.source();
                if (doc != null) {
                    // 重建TextSegment
                    Metadata metadata = new Metadata();
                    if (doc.getMetadata() != null) {
                        doc.getMetadata().forEach(metadata::put);
                    }

                    TextSegment segment = TextSegment.from(doc.getText(), metadata);

                    // 将向量转回Embedding
                    Embedding emb = Embedding.from(toFloatArray(doc.getEmbedding()));

                    // 创建匹配结果
                    EmbeddingMatch<TextSegment> match = new EmbeddingMatch<>(
                        hit.score(), // 相似度分数
                        doc.getId(),
                        emb,
                        segment
                    );

                    matches.add(match);
                }
            }

            log.debug("从ES检索到 {} 个相关向量, minScore={}", matches.size(), minScore);
            return matches;

        } catch (Exception e) {
            log.error("从ES检索向量失败: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 查找相关向量（辅助方法，使用默认minScore）
     */
    private List<EmbeddingMatch<TextSegment>> findRelevant(Embedding referenceEmbedding, int maxResults) {
        return findRelevant(referenceEmbedding, maxResults, 0.0);
    }

    @Override
    public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
        // 从请求中提取参数
        Embedding queryEmbedding = request.queryEmbedding();
        int maxResults = request.maxResults();
        double minScore = request.minScore();
        dev.langchain4j.store.embedding.filter.Filter filter = request.filter();

        // 如果有过滤器，使用带过滤的查询
        if (filter != null) {
            return searchWithFilter(queryEmbedding, maxResults, minScore, filter);
        }

        // 否则使用现有的findRelevant方法
        List<EmbeddingMatch<TextSegment>> matches = findRelevant(queryEmbedding, maxResults, minScore);

        // 返回搜索结果
        return new EmbeddingSearchResult<>(matches);
    }

    /**
     * 带过滤器的搜索
     */
    private EmbeddingSearchResult<TextSegment> searchWithFilter(
            Embedding queryEmbedding,
            int maxResults,
            double minScore,
            dev.langchain4j.store.embedding.filter.Filter filter) {
        try {
            List<Float> queryVector = toFloatList(queryEmbedding.vector());

            // 构建过滤查询
            Query filterQuery = buildFilterQuery(filter);

            SearchResponse<VectorDocument> response = client.search(s -> s
                .index(indexName)
                .size(maxResults)
                .query(q -> q
                    .scriptScore(ss -> ss
                        .query(filterQuery)
                        .script(sc -> sc
                            .inline(i -> i
                                .source("cosineSimilarity(params.queryVector, 'embedding') + 1.0")
                                .params("queryVector", JsonData.of(queryVector))
                            )
                        )
                    )
                )
                .minScore(minScore),
                VectorDocument.class
            );

            // 转换结果
            List<EmbeddingMatch<TextSegment>> matches = new ArrayList<>();
            for (Hit<VectorDocument> hit : response.hits().hits()) {
                VectorDocument doc = hit.source();
                if (doc != null) {
                    Metadata metadata = new Metadata();
                    if (doc.getMetadata() != null) {
                        doc.getMetadata().forEach(metadata::put);
                    }

                    TextSegment segment = TextSegment.from(doc.getText(), metadata);
                    Embedding emb = Embedding.from(toFloatArray(doc.getEmbedding()));

                    EmbeddingMatch<TextSegment> match = new EmbeddingMatch<>(
                        hit.score(),
                        doc.getId(),
                        emb,
                        segment
                    );

                    matches.add(match);
                }
            }

            log.info("✅ 带过滤器检索完成 - 结果数: {}, 过滤器: {}", matches.size(), filter);
            return new EmbeddingSearchResult<>(matches);

        } catch (Exception e) {
            log.error("❌ 带过滤器检索失败", e);
            return new EmbeddingSearchResult<>(Collections.emptyList());
        }
    }

    /**
     * 构建过滤查询
     */
    private Query buildFilterQuery(dev.langchain4j.store.embedding.filter.Filter filter) {

        // 支持 IsEqualTo 过滤器
        if (filter instanceof dev.langchain4j.store.embedding.filter.comparison.IsEqualTo) {
            dev.langchain4j.store.embedding.filter.comparison.IsEqualTo isEqualToFilter =
                (dev.langchain4j.store.embedding.filter.comparison.IsEqualTo) filter;

            String key = isEqualToFilter.key();
            Object value = isEqualToFilter.comparisonValue();
            String stringValue = value.toString();

            log.info("构建IsEqualTo过滤器 - 字段: metadata.{}, 值: {}", key, stringValue);

            return Query.of(q -> q
                .term(t -> t
                    .field("metadata." + key)
                    .value(stringValue)
                )
            );
        }

        // 支持 IsIn 过滤器
        if (filter instanceof dev.langchain4j.store.embedding.filter.comparison.IsIn) {
            dev.langchain4j.store.embedding.filter.comparison.IsIn isInFilter =
                (dev.langchain4j.store.embedding.filter.comparison.IsIn) filter;

            String key = isInFilter.key();
            Collection<?> values = isInFilter.comparisonValues();

            // 转换为字符串列表
            List<String> stringValues = values.stream()
                .map(Object::toString)
                .collect(java.util.stream.Collectors.toList());

            log.info("构建IsIn过滤器 - 字段: metadata.{}, 值: {}", key, stringValues);

            return Query.of(q -> q
                .terms(t -> t
                    .field("metadata." + key)
                    .terms(tt -> tt.value(stringValues.stream()
                        .map(co.elastic.clients.elasticsearch._types.FieldValue::of)
                        .collect(java.util.stream.Collectors.toList())))
                )
            );
        }

        // 默认返回match_all
        log.warn("⚠️ 不支持的过滤器类型: {}, 使用match_all", filter.getClass().getSimpleName());
        return Query.of(q -> q.matchAll(m -> m));
    }

    /**
     * BM25关键词检索
     * @param queryText 查询文本
     * @param knowledgeBaseId 知识库ID
     * @param maxResults 最大结果数
     * @return 检索结果
     */
    public List<EmbeddingMatch<TextSegment>> bm25Search(String queryText, String knowledgeBaseId, int maxResults) {
        try {
            log.info("BM25检索 - query: {}, knowledgeBaseId: {}, maxResults: {}", queryText, knowledgeBaseId, maxResults);

            SearchResponse<VectorDocument> response = client.search(s -> s
                .index(indexName)
                .size(maxResults)
                .query(q -> q
                    .bool(b -> b
                        .must(m -> m
                            .match(mt -> mt
                                .field("text")
                                .query(queryText)
                            )
                        )
                        .filter(f -> f
                            .term(t -> t
                                .field("metadata.knowledgeBaseId")
                                .value(knowledgeBaseId)
                            )
                        )
                    )
                ),
                VectorDocument.class
            );

            // 转换结果
            List<EmbeddingMatch<TextSegment>> matches = new ArrayList<>();
            for (Hit<VectorDocument> hit : response.hits().hits()) {
                VectorDocument doc = hit.source();
                if (doc != null) {
                    Metadata metadata = new Metadata();
                    if (doc.getMetadata() != null) {
                        doc.getMetadata().forEach(metadata::put);
                    }

                    TextSegment segment = TextSegment.from(doc.getText(), metadata);

                    // BM25分数需要归一化（ES返回的分数通常在0-20之间）
                    double normalizedScore = Math.min(hit.score() / 20.0, 1.0);

                    // 创建一个空的Embedding（BM25不需要向量）
                    Embedding emb = doc.getEmbedding() != null ?
                        Embedding.from(toFloatArray(doc.getEmbedding())) :
                        null;

                    EmbeddingMatch<TextSegment> match = new EmbeddingMatch<>(
                        normalizedScore,
                        doc.getId(),
                        emb,
                        segment
                    );

                    matches.add(match);
                }
            }

            log.info("✅ BM25检索完成 - 结果数: {}", matches.size());
            return matches;

        } catch (Exception e) {
            log.error("❌ BM25检索失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 按知识库ID删除向量数据
     *
     * @param knowledgeBaseId 知识库ID
     * @return 删除的文档数量
     */
    public int deleteByKnowledgeBaseId(String knowledgeBaseId) {
        try {
            log.info("🗑️ 开始删除知识库向量: knowledgeBaseId={}", knowledgeBaseId);
            
            // 使用delete_by_query API
            var response = client.deleteByQuery(d -> d
                .index(indexName)
                .query(q -> q
                    .term(t -> t
                        .field("metadata.knowledgeBaseId")
                        .value(knowledgeBaseId)
                    )
                )
            );
            
            long deleted = response.deleted();
            log.info("✅ 删除知识库向量完成: knowledgeBaseId={}, 删除数量={}", knowledgeBaseId, deleted);
            return (int) deleted;
            
        } catch (Exception e) {
            log.error("❌ 删除知识库向量失败: knowledgeBaseId={}, error={}", knowledgeBaseId, e.getMessage());
            return 0;
        }
    }

    /**
     * 按文档ID删除单个向量
     *
     * @param documentId 文档ID
     * @return 是否删除成功
     */
    public boolean deleteById(String documentId) {
        try {
            client.delete(d -> d
                .index(indexName)
                .id(documentId)
            );
            log.debug("✅ 删除向量: id={}", documentId);
            return true;
        } catch (Exception e) {
            log.error("❌ 删除向量失败: id={}, error={}", documentId, e.getMessage());
            return false;
        }
    }
}
