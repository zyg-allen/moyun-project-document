package com.moyun.ext.ai.store;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import dev.langchain4j.store.embedding.filter.comparison.IsIn;
import dev.langchain4j.store.embedding.filter.logical.And;
import dev.langchain4j.store.embedding.filter.logical.Or;
import io.github.jbellis.jvector.graph.GraphIndexBuilder;
import io.github.jbellis.jvector.graph.GraphSearcher;
import io.github.jbellis.jvector.graph.ImmutableGraphIndex;
import io.github.jbellis.jvector.graph.ListRandomAccessVectorValues;
import io.github.jbellis.jvector.graph.RandomAccessVectorValues;
import io.github.jbellis.jvector.graph.SearchResult;
import io.github.jbellis.jvector.graph.similarity.BuildScoreProvider;
import io.github.jbellis.jvector.graph.similarity.DefaultSearchScoreProvider;
import io.github.jbellis.jvector.graph.similarity.SearchScoreProvider;
import io.github.jbellis.jvector.util.Bits;
import io.github.jbellis.jvector.vector.VectorSimilarityFunction;
import io.github.jbellis.jvector.vector.VectorizationProvider;
import io.github.jbellis.jvector.vector.types.VectorFloat;
import io.github.jbellis.jvector.vector.types.VectorTypeSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * JVector 向量存储实现（嵌入式，替代 Elasticsearch）
 *
 * <p>功能特性：</p>
 * <ul>
 *   <li>基于 JVector HNSW 图索引的余弦相似度向量检索（纯 Java，无 JNI / 无外部服务）</li>
 *   <li>支持 langchain4j {@link EmbeddingStore} 标准能力（add / search / 过滤检索）</li>
 *   <li>支持 {@link VectorStoreExtension} 扩展能力（BM25 全文检索、按知识库删除、按 ID 删除）</li>
 *   <li>元数据过滤通过 JVector {@link Bits} 预过滤实现，检索结果只落在允许的分片集合内</li>
 *   <li>本地磁盘持久化（DataOutputStream 二进制快照，脏数据定时落盘 + 停机落盘）</li>
 *   <li>索引懒重建：写入 / 删除只使快照失效，下一次检索时一次性重建（适合中小规模知识库）</li>
 * </ul>
 *
 * <p>持久化文件格式（{@code ${jvector.data-path}/moyun_ai_vectors.bin}）：</p>
 * <pre>
 * magic(int) + version(int) + dimension(int) + count(int)
 * 每条记录: id(UTF) + text(UTF) + metadataCount(int) + (key(UTF)+value(UTF))* + vectorLen(int) + float[vectorLen]
 * </pre>
 *
 * <p>维度策略：与 ES 实现一致，首次写入时根据实际 Embedding 维度锁定；检索时维度不一致返回空结果。</p>
 *
 * @author laomao
 * @since 2026-09-09
 */
@Slf4j
@Component("embeddingStore")
public class JVectorEmbeddingStore implements VectorStoreExtension, DisposableBean {

    /** 持久化文件魔数 "MJV1" */
    private static final int MAGIC = 0x4D4A5631;
    /** 持久化文件版本 */
    private static final int VERSION = 1;
    /** HNSW 图最大度数（M） */
    private static final int MAX_DEGREE = 16;
    /** 图构建 / 检索束宽 */
    private static final int BEAM_WIDTH = 100;
    /** 构建参数：邻居溢出因子 */
    private static final float NEIGHBOR_OVERFLOW = 1.2f;
    /** 构建参数：多样性因子 */
    private static final float ALPHA = 1.2f;
    /** BM25 参数 */
    private static final double BM25_K1 = 1.2;
    private static final double BM25_B = 0.75;
    /** BM25 分数归一化除数（与原 ES 实现保持一致） */
    private static final double BM25_SCORE_CAP = 20.0;

    private final VectorTypeSupport vectorTypeSupport = VectorizationProvider.getInstance().getVectorTypeSupport();
    private final VectorSimilarityFunction similarityFunction = VectorSimilarityFunction.COSINE;

    private final Path persistenceFile;

    /** 所有写操作使用写锁；检索基于不可变快照，无锁 */
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /** 全量数据：id → 条目（插入有序，重建快照时按此顺序分配 ordinal） */
    private final LinkedHashMap<String, StoredEntry> entries = new LinkedHashMap<>();

    /** 向量维度（首个写入的 Embedding 决定；-1 表示尚未初始化） */
    private volatile int dimension = -1;

    /** 不可变检索快照（数据或索引变更后置空，下次检索重建） */
    private volatile Snapshot snapshot;

    /** 是否存在未落盘的变更 */
    private volatile boolean dirty = false;

    /**
     * 存储条目
     */
    private static class StoredEntry {
        final String id;
        final float[] vector;
        final String text;
        final Map<String, String> metadata;

        StoredEntry(String id, float[] vector, String text, Map<String, String> metadata) {
            this.id = id;
            this.vector = vector;
            this.text = text;
            this.metadata = metadata;
        }
    }

    /**
     * 不可变检索快照：图索引 + 向量列表 + ordinal→条目 映射
     */
    private static class Snapshot {
        final ImmutableGraphIndex index;
        final List<VectorFloat<?>> vectors;
        final StoredEntry[] byOrdinal;

        Snapshot(ImmutableGraphIndex index, List<VectorFloat<?>> vectors, StoredEntry[] byOrdinal) {
            this.index = index;
            this.vectors = vectors;
            this.byOrdinal = byOrdinal;
        }
    }

    public JVectorEmbeddingStore(
            @Value("${jvector.data-path:./data/jvector}") String dataPath) {
        this.persistenceFile = Path.of(dataPath).resolve("moyun_ai_vectors.bin");
        loadFromDisk();
        log.info("JVectorEmbeddingStore初始化完成，持久化文件: {}, 已加载向量数: {}",
                persistenceFile.toAbsolutePath(), entries.size());
    }

    // ==================== EmbeddingStore 标准能力 ====================

    @Override
    public String add(Embedding embedding) {
        String id = java.util.UUID.randomUUID().toString();
        add(id, embedding);
        return id;
    }

    @Override
    public void add(String id, Embedding embedding) {
        addInternal(id, embedding, null);
    }

    @Override
    public String add(Embedding embedding, TextSegment textSegment) {
        String id = java.util.UUID.randomUUID().toString();
        addInternal(id, embedding, textSegment);
        return id;
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings) {
        List<String> ids = new ArrayList<>(embeddings.size());
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
        List<String> ids = new ArrayList<>(embeddings.size());
        for (int i = 0; i < embeddings.size(); i++) {
            ids.add(add(embeddings.get(i), textSegments.get(i)));
        }
        return ids;
    }

    @Override
    public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
        if (request.queryEmbedding() == null || request.queryEmbedding().vector() == null) {
            return new EmbeddingSearchResult<>(Collections.emptyList());
        }

        Snapshot snap = ensureSnapshot();
        if (snap == null || snap.byOrdinal.length == 0) {
            return new EmbeddingSearchResult<>(Collections.emptyList());
        }

        // 维度校验：与写入维度不一致直接返回空（与 ES 实现行为一致）
        if (request.queryEmbedding().dimension() != dimension) {
            log.warn("查询向量维度({})与存储维度({})不一致，返回空结果",
                    request.queryEmbedding().dimension(), dimension);
            return new EmbeddingSearchResult<>(Collections.emptyList());
        }

        // 元数据过滤 → Bits 预过滤
        Bits bits = Bits.ALL;
        if (request.filter() != null) {
            bits = buildBits(snap, request.filter());
            if (bits == null) {
                // 过滤条件无匹配项
                return new EmbeddingSearchResult<>(Collections.emptyList());
            }
        }

        // 带过滤时适度超采，缓解 HNSW 稀疏过滤下的召回不足
        int searchK = request.filter() == null
                ? request.maxResults()
                : (int) Math.min((long) snap.byOrdinal.length, Math.max(request.maxResults() * 4L, 64L));

        try {
            VectorFloat<?> query = toVectorFloat(request.queryEmbedding().vector());
            RandomAccessVectorValues vectorValues = new ListRandomAccessVectorValues(snap.vectors, dimension);
            SearchScoreProvider scoreProvider =
                    DefaultSearchScoreProvider.exact(query, similarityFunction, vectorValues);
            SearchResult result = new GraphSearcher(snap.index).search(scoreProvider, searchK, bits);

            List<EmbeddingMatch<TextSegment>> matches = new ArrayList<>();
            for (SearchResult.NodeScore nodeScore : result.getNodes()) {
                if (matches.size() >= request.maxResults()) {
                    break;
                }
                // 余弦相似度 [-1,1] → [0,1]
                double score = (1.0 + nodeScore.score) / 2.0;
                if (score < request.minScore()) {
                    continue;
                }
                StoredEntry entry = snap.byOrdinal[nodeScore.node];
                if (entry != null) {
                    matches.add(toMatch(entry, score));
                }
            }
            log.debug("JVector检索完成 - 候选: {}, 返回: {}", result.getNodes().length, matches.size());
            return new EmbeddingSearchResult<>(matches);
        } catch (Exception e) {
            log.error("JVector向量检索失败: {}", e.getMessage(), e);
            return new EmbeddingSearchResult<>(Collections.emptyList());
        }
    }

    // ==================== VectorStoreExtension 扩展能力 ====================

    @Override
    public List<EmbeddingMatch<TextSegment>> bm25Search(String queryText, String knowledgeBaseId, int maxResults) {
        log.info("BM25检索 - query: {}, knowledgeBaseId: {}, maxResults: {}", queryText, knowledgeBaseId, maxResults);

        List<StoredEntry> candidates = new ArrayList<>();
        lock.readLock().lock();
        try {
            for (StoredEntry entry : entries.values()) {
                if (knowledgeBaseId == null
                        || knowledgeBaseId.equals(entry.metadata.get("knowledgeBaseId"))) {
                    candidates.add(entry);
                }
            }
        } finally {
            lock.readLock().unlock();
        }

        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        // 分词（CJK 二元组 + 拉丁词）并计算 BM25
        List<String> queryTokens = tokenize(queryText);
        if (queryTokens.isEmpty()) {
            return Collections.emptyList();
        }

        double avgdl = 0;
        Map<StoredEntry, List<String>> docTokens = new LinkedHashMap<>();
        Map<String, Integer> docFreq = new HashMap<>();
        for (StoredEntry entry : candidates) {
            List<String> tokens = tokenize(entry.text);
            docTokens.put(entry, tokens);
            avgdl += tokens.size();
        }
        avgdl /= candidates.size();

        for (List<String> tokens : docTokens.values()) {
            for (String term : new java.util.HashSet<>(tokens)) {
                docFreq.merge(term, 1, Integer::sum);
            }
        }

        int n = candidates.size();
        List<EmbeddingMatch<TextSegment>> scored = new ArrayList<>();
        for (Map.Entry<StoredEntry, List<String>> e : docTokens.entrySet()) {
            Map<String, Integer> tf = new HashMap<>();
            for (String token : e.getValue()) {
                tf.merge(token, 1, Integer::sum);
            }

            double score = 0;
            for (String term : queryTokens) {
                Integer f = tf.get(term);
                if (f == null || f == 0) {
                    continue;
                }
                int df = docFreq.getOrDefault(term, 0);
                double idf = Math.log(1.0 + (n - df + 0.5) / (df + 0.5));
                double norm = f * (BM25_K1 + 1) / (f + BM25_K1 * (1 - BM25_B + BM25_B * e.getValue().size() / avgdl));
                score += idf * norm;
            }
            if (score > 0) {
                // 归一化到 [0,1]，与原 ES 实现保持一致
                scored.add(toMatch(e.getKey(), Math.min(score / BM25_SCORE_CAP, 1.0)));
            }
        }

        scored.sort((a, b) -> Double.compare(b.score(), a.score()));
        List<EmbeddingMatch<TextSegment>> result =
                scored.size() > maxResults ? new ArrayList<>(scored.subList(0, maxResults)) : scored;
        log.info("BM25检索完成 - 候选: {}, 命中: {}", candidates.size(), result.size());
        return result;
    }

    @Override
    public int deleteByKnowledgeBaseId(String knowledgeBaseId) {
        lock.writeLock().lock();
        try {
            List<String> toRemove = new ArrayList<>();
            for (StoredEntry entry : entries.values()) {
                if (knowledgeBaseId.equals(entry.metadata.get("knowledgeBaseId"))) {
                    toRemove.add(entry.id);
                }
            }
            toRemove.forEach(entries::remove);
            if (!toRemove.isEmpty()) {
                snapshot = null;
                dirty = true;
            }
            log.info("删除知识库向量完成: knowledgeBaseId={}, 删除数量={}", knowledgeBaseId, toRemove.size());
            return toRemove.size();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean deleteById(String documentId) {
        lock.writeLock().lock();
        try {
            boolean removed = entries.remove(documentId) != null;
            if (removed) {
                snapshot = null;
                dirty = true;
            }
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ==================== 内部实现 ====================

    private void addInternal(String id, Embedding embedding, TextSegment textSegment) {
        if (id == null || embedding == null || embedding.vector() == null) {
            throw new IllegalArgumentException("id 和 embedding 不能为空");
        }

        lock.writeLock().lock();
        try {
            // 首次写入锁定维度
            if (dimension < 0) {
                dimension = embedding.dimension();
                log.info("JVector 向量维度锁定为: {}", dimension);
            }
            if (embedding.dimension() != dimension) {
                throw new IllegalArgumentException(String.format(
                        "向量维度(%d)与存储维度(%d)不一致", embedding.dimension(), dimension));
            }

            String text = textSegment != null ? textSegment.text() : "";
            Map<String, String> metadata = new HashMap<>();
            if (textSegment != null && textSegment.metadata() != null) {
                textSegment.metadata().toMap().forEach((k, v) ->
                        metadata.put(k, v != null ? v.toString() : null));
            }

            entries.put(id, new StoredEntry(id, embedding.vector().clone(), text, metadata));
            snapshot = null;
            dirty = true;
            log.debug("向量已添加, id={}, 当前总数: {}", id, entries.size());
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 获取可用快照；数据变更后懒重建（含 HNSW 索引构建）
     */
    private Snapshot ensureSnapshot() {
        Snapshot snap = snapshot;
        if (snap != null) {
            return snap;
        }
        lock.writeLock().lock();
        try {
            snap = snapshot;
            if (snap != null) {
                return snap;
            }
            if (entries.isEmpty() || dimension <= 0) {
                return null;
            }
            long start = System.currentTimeMillis();
            List<VectorFloat<?>> vectors = new ArrayList<>(entries.size());
            StoredEntry[] byOrdinal = new StoredEntry[entries.size()];
            int i = 0;
            for (StoredEntry entry : entries.values()) {
                byOrdinal[i++] = entry;
                vectors.add(toVectorFloat(entry.vector));
            }

            RandomAccessVectorValues vectorValues = new ListRandomAccessVectorValues(vectors, dimension);
            BuildScoreProvider scoreProvider =
                    BuildScoreProvider.randomAccessScoreProvider(vectorValues, similarityFunction);
            GraphIndexBuilder builder = null;
            try {
                builder = new GraphIndexBuilder(
                        scoreProvider, dimension, MAX_DEGREE, BEAM_WIDTH, NEIGHBOR_OVERFLOW, ALPHA, false);
                ImmutableGraphIndex index = builder.build(vectorValues);
                snap = new Snapshot(index, vectors, byOrdinal);
                snapshot = snap;
                log.info("JVector 索引重建完成 - 向量数: {}, 耗时: {}ms", vectors.size(),
                        System.currentTimeMillis() - start);
            } finally {
                if (builder != null) {
                    try {
                        builder.close();
                    } catch (IOException e) {
                        log.warn("GraphIndexBuilder 关闭失败: {}", e.getMessage());
                    }
                }
            }
            return snap;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 根据 langchain4j Filter 构建 JVector Bits 预过滤位图
     *
     * @return Bits；过滤结果为空集时返回 null
     */
    private Bits buildBits(Snapshot snap, Filter filter) {
        java.util.BitSet allowed = new java.util.BitSet();
        boolean anyMatch = false;
        for (int i = 0; i < snap.byOrdinal.length; i++) {
            StoredEntry entry = snap.byOrdinal[i];
            if (entry != null && matches(entry.metadata, filter)) {
                allowed.set(i);
                anyMatch = true;
            }
        }
        if (!anyMatch) {
            return null;
        }
        return allowed::get;
    }

    /**
     * 元数据过滤求值（支持 IsEqualTo / IsIn / And / Or，其余类型退化为全通过）
     */
    private boolean matches(Map<String, String> metadata, Filter filter) {
        if (filter == null) {
            return true;
        }
        if (filter instanceof IsEqualTo isEqualTo) {
            return Objects.equals(
                    metadata.get(isEqualTo.key()),
                    isEqualTo.comparisonValue() != null ? isEqualTo.comparisonValue().toString() : null);
        }
        if (filter instanceof IsIn isIn) {
            for (Object value : isIn.comparisonValues()) {
                if (Objects.equals(metadata.get(isIn.key()), value != null ? value.toString() : null)) {
                    return true;
                }
            }
            return false;
        }
        if (filter instanceof And and) {
            return matches(metadata, and.left()) && matches(metadata, and.right());
        }
        if (filter instanceof Or or) {
            return matches(metadata, or.left()) || matches(metadata, or.right());
        }
        log.warn("不支持的过滤器类型: {}，默认通过", filter.getClass().getSimpleName());
        return true;
    }

    private EmbeddingMatch<TextSegment> toMatch(StoredEntry entry, double score) {
        Metadata metadata = new Metadata();
        entry.metadata.forEach((k, v) -> {
            if (v != null) {
                metadata.put(k, v);
            }
        });
        TextSegment segment = TextSegment.from(entry.text, metadata);
        Embedding emb = Embedding.from(entry.vector);
        return new EmbeddingMatch<>(score, entry.id, emb, segment);
    }

    private VectorFloat<?> toVectorFloat(float[] vector) {
        return vectorTypeSupport.createFloatVector(vector);
    }

    /**
     * 轻量分词：拉丁/数字按词，CJK 按二元组（尾部单字兜底）
     */
    private List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        String lower = text.toLowerCase(Locale.ROOT);
        List<String> tokens = new ArrayList<>();
        StringBuilder latin = new StringBuilder();
        StringBuilder cjk = new StringBuilder();

        Runnable flushLatin = () -> {
            if (latin.length() > 0) {
                tokens.add(latin.toString());
                latin.setLength(0);
            }
        };
        Runnable flushCjk = () -> {
            if (cjk.length() > 0) {
                String run = cjk.toString();
                for (int i = 0; i < run.length() - 1; i++) {
                    tokens.add(run.substring(i, i + 2));
                }
                if (run.length() == 1) {
                    tokens.add(run);
                }
                cjk.setLength(0);
            }
        };

        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (isCjk(c)) {
                flushLatin.run();
                cjk.append(c);
            } else if (Character.isLetterOrDigit(c)) {
                flushCjk.run();
                latin.append(c);
            } else {
                flushLatin.run();
                flushCjk.run();
            }
        }
        flushLatin.run();
        flushCjk.run();
        return tokens;
    }

    private boolean isCjk(char c) {
        return (c >= 0x4E00 && c <= 0x9FFF)      // CJK 统一表意
                || (c >= 0x3400 && c <= 0x4DBF)  // 扩展 A
                || (c >= 0xF900 && c <= 0xFAFF); // 兼容表意
    }

    // ==================== 持久化 ====================

    /**
     * 定时落盘（每 60 秒，存在脏数据时触发）
     */
    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void saveIfDirty() {
        if (dirty) {
            try {
                saveToDisk();
            } catch (Exception e) {
                log.error("JVector 定时落盘失败: {}", e.getMessage(), e);
            }
        }
    }

    @Override
    public void destroy() {
        try {
            saveToDisk();
        } catch (Exception e) {
            log.error("JVector 停机落盘失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 全量快照落盘（写临时文件后原子替换）
     */
    public synchronized void saveToDisk() throws IOException {
        Files.createDirectories(persistenceFile.getParent());
        Path tmp = persistenceFile.resolveSibling(persistenceFile.getFileName() + ".tmp");

        lock.readLock().lock();
        try (DataOutputStream out = new DataOutputStream(
                new java.io.BufferedOutputStream(Files.newOutputStream(tmp)))) {
            out.writeInt(MAGIC);
            out.writeInt(VERSION);
            out.writeInt(dimension);
            out.writeInt(entries.size());
            for (StoredEntry entry : entries.values()) {
                out.writeUTF(entry.id);
                out.writeUTF(entry.text != null ? entry.text : "");
                out.writeInt(entry.metadata.size());
                for (Map.Entry<String, String> m : entry.metadata.entrySet()) {
                    out.writeUTF(m.getKey() != null ? m.getKey() : "");
                    out.writeUTF(m.getValue() != null ? m.getValue() : "");
                }
                out.writeInt(entry.vector.length);
                for (float v : entry.vector) {
                    out.writeFloat(v);
                }
            }
        } finally {
            lock.readLock().unlock();
        }

        Files.move(tmp, persistenceFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        dirty = false;
        log.info("JVector 向量快照已落盘: {}, 条目数: {}", persistenceFile.toAbsolutePath(), entries.size());
    }

    /**
     * 启动时从磁盘加载
     */
    private void loadFromDisk() {
        if (!Files.exists(persistenceFile)) {
            return;
        }
        try (DataInputStream in = new DataInputStream(
                new java.io.BufferedInputStream(Files.newInputStream(persistenceFile)))) {
            int magic = in.readInt();
            if (magic != MAGIC) {
                log.error("JVector 持久化文件魔数不匹配: {}", persistenceFile);
                return;
            }
            int version = in.readInt();
            if (version != VERSION) {
                log.error("JVector 持久化文件版本不兼容: {}", version);
                return;
            }
            int fileDimension = in.readInt();
            int count = in.readInt();

            for (int i = 0; i < count; i++) {
                String id = in.readUTF();
                String text = in.readUTF();
                int metaCount = in.readInt();
                Map<String, String> metadata = new HashMap<>(metaCount * 2);
                for (int j = 0; j < metaCount; j++) {
                    metadata.put(in.readUTF(), in.readUTF());
                }
                int len = in.readInt();
                float[] vector = new float[len];
                for (int j = 0; j < len; j++) {
                    vector[j] = in.readFloat();
                }
                entries.put(id, new StoredEntry(id, vector, text, metadata));
            }
            if (!entries.isEmpty() && fileDimension > 0) {
                dimension = fileDimension;
            }
            log.info("JVector 从磁盘加载完成 - 条目数: {}, 维度: {}", entries.size(), dimension);
        } catch (EOFException e) {
            log.error("JVector 持久化文件不完整，从空索引启动: {}", persistenceFile, e);
            entries.clear();
            dimension = -1;
        } catch (Exception e) {
            log.error("JVector 持久化文件加载失败，从空索引启动: {}", e.getMessage(), e);
            entries.clear();
            dimension = -1;
        }
    }

    /**
     * 当前条目数（供迁移 / 监控使用）
     */
    public int size() {
        lock.readLock().lock();
        try {
            return entries.size();
        } finally {
            lock.readLock().unlock();
        }
    }
}
