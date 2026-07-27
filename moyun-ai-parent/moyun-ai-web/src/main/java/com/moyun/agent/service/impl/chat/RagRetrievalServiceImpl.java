package com.moyun.agent.service.impl.chat;

import com.moyun.agent.config.RagConfig;
import com.moyun.agent.entity.Agent;
import com.moyun.agent.entity.ModelConfig;
import com.moyun.agent.model.RerankModel;
import com.moyun.agent.model.RerankResult;
import com.moyun.agent.service.ModelConfigService;
import com.moyun.agent.service.QueryExpansionService;
import com.moyun.agent.service.TokenUsageService;
import com.moyun.agent.service.chat.ContentScoringService;
import com.moyun.agent.service.chat.RagRetrievalService;
import com.moyun.agent.service.chat.QueryRewritingService;
import com.moyun.agent.store.ElasticsearchEmbeddingStore;
import com.moyun.agent.util.TextProcessingUtils;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.comparison.IsIn;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * RAG检索服务
 *
 * <p>负责知识库检索相关功能，包括：
 * <ul>
 *   <li>混合检索（向量 + BM25）</li>
 *   <li>重排（Rerank）</li>
 *   <li>相邻分片合并</li>
 *   <li>性能统计</li>
 * </ul>
 * </p>
 *
 * @author laomao
 * @since 2025-12-11
 */
@Slf4j
@Service
public class RagRetrievalServiceImpl implements RagRetrievalService {

    @Autowired
    @Qualifier("embeddingStore")
    private EmbeddingStore<TextSegment> embeddingStore;

    // 不使用 @Autowired，通过 @PostConstruct 动态初始化
    private EmbeddingModel embeddingModel;

    // Reranker 模型（可选，如果配置了则使用，否则降级到规则匹配）
    private RerankModel rerankModel;

    @Autowired
    private RagConfig ragConfig;

    @Autowired
    private ModelConfigService modelConfigService;

    @Autowired
    private QueryExpansionService queryExpansionService;

    @Autowired
    private ElasticsearchEmbeddingStore elasticsearchEmbeddingStore;

    @Autowired
    private TokenUsageService tokenUsageService;

    @Autowired
    private ContentScoringService contentScoringService;

    @Autowired
    private QueryRewritingService queryRewritingService;

    @Autowired(required = false)
    private com.moyun.agent.service.QueryTypeClassifier queryTypeClassifier;

    /**
     * 存储Content和其重排分数的映射
     *
     * <p>
     * 使用ThreadLocal确保线程安全，每个请求有独立的映射。
     * 使用IdentityHashMap确保对象identity比较。
     * </p>
     */
    private final ThreadLocal<Map<Content, Double>> contentRerankScoresHolder = ThreadLocal
            .withInitial(java.util.IdentityHashMap::new);

    /**
     * 初始化 Embedding 模型
     *
     * <p>
     * 应用启动时自动执行，从配置中加载默认的Embedding模型。
     * 如果初始化失败，会记录错误日志但不会阻止应用启动。
     * </p>
     */
    @PostConstruct
    public void initEmbeddingModel() {
        try {
            ModelConfig config = modelConfigService.getDefaultEmbeddingConfig();
            if (config != null) {
                this.embeddingModel = modelConfigService.createEmbeddingModel(config.getId());
                log.info("✅ RagRetrievalService - 使用动态配置的 Embedding 模型: {} ({})", config.getName(),
                        config.getModelName());
            } else {
                log.warn("⚠️ RagRetrievalService - 未找到默认 Embedding 模型配置");
            }
        } catch (Exception e) {
            log.error("❌ RagRetrievalService - 初始化 Embedding 模型失败: {}", e.getMessage(), e);
        }
        
        // 初始化 Reranker 模型（可选）
        try {
            ModelConfig rerankConfig = modelConfigService.getDefaultRerankConfig();
            if (rerankConfig != null) {
                this.rerankModel = modelConfigService.createRerankModel(rerankConfig.getId());
                log.info("✅ RagRetrievalService - 使用动态配置的 Reranker 模型: {} ({})", 
                    rerankConfig.getName(), rerankConfig.getModelName());
            } else {
                log.info("ℹ️ RagRetrievalService - 未配置 Reranker 模型，将使用规则匹配进行重排");
            }
        } catch (Exception e) {
            log.warn("⚠️ RagRetrievalService - 初始化 Reranker 模型失败，将使用规则匹配: {}", e.getMessage());
        }
    }

    /**
     * 获取当前线程的重排分数映射
     *
     * @return 重排分数映射
     */
    @Override
    public Map<Content, Double> getContentRerankScores() {
        return contentRerankScoresHolder.get();
    }

    /**
     * 清空当前线程的重排分数映射
     */
    @Override
    public void clearContentRerankScores() {
        contentRerankScoresHolder.get().clear();
    }
    
    /**
     * 解析知识库权重配置
     * 
     * @param weightsJson JSON格式的权重配置，如 {"1": 1.0, "2": 0.8}
     * @return 知识库ID到权重的映射
     */
    private Map<String, Double> parseKnowledgeBaseWeights(String weightsJson) {
        Map<String, Double> weights = new HashMap<>();
        
        if (weightsJson == null || weightsJson.trim().isEmpty()) {
            return weights;
        }
        
        try {
            // 使用 Jackson 解析 JSON
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.Map<String, Object> map = mapper.readValue(weightsJson, 
                new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {});
            
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                try {
                    String kbId = entry.getKey();
                    Double weight = Double.valueOf(entry.getValue().toString());
                    
                    // 权重范围检查 (0.0-1.0)，支持0表示禁用
                    if (weight < 0.0) {
                        weight = 0.0;
                    } else if (weight > 1.0) {
                        weight = 1.0;
                    }
                    
                    weights.put(kbId, weight);
                } catch (Exception e) {
                    log.warn("解析知识库权重失败: key={}, value={}", entry.getKey(), entry.getValue());
                }
            }
            
            log.debug("解析知识库权重成功: {}", weights);
        } catch (Exception e) {
            log.warn("解析知识库权重配置失败: {}", e.getMessage());
        }
        
        return weights;
    }
    
    /**
     * 按知识库权重分别检索
     * 
     * <p>根据知识库权重分配召回数量，高权重知识库召回更多结果</p>
     * 
     * @param query 查询文本
     * @param knowledgeBaseIds 知识库ID列表
     * @param knowledgeBaseWeights 知识库权重配置
     * @param totalCandidateCount 总召回数量
     * @param minScore 最低相似度
     * @param isVectorSearch 是否为向量检索（true）还是BM25检索（false）
     * @return 检索结果列表
     */
    private List<Content> retrieveByKnowledgeBaseWeights(
            String query,
            List<Long> knowledgeBaseIds,
            Map<String, Double> knowledgeBaseWeights,
            int totalCandidateCount,
            double minScore,
            boolean isVectorSearch) {
        
        // 1. 计算总权重
        double totalWeight = 0.0;
        Map<String, Double> effectiveWeights = new HashMap<>();
        
        for (Long kbId : knowledgeBaseIds) {
            String kbIdStr = String.valueOf(kbId);
            Double weight = knowledgeBaseWeights.getOrDefault(kbIdStr, 1.0);
            
            // 权重为0表示禁用该知识库
            if (weight > 0.0) {
                effectiveWeights.put(kbIdStr, weight);
                totalWeight += weight;
            } else {
                log.info("  - 知识库ID={} 权重为0，跳过检索", kbId);
            }
        }
        
        if (effectiveWeights.isEmpty()) {
            log.warn("所有知识库权重为0，返回空结果");
            return new ArrayList<>();
        }
        
        // 2. 按权重分配召回数量
        List<Content> allContents = new ArrayList<>();
        
        for (Map.Entry<String, Double> entry : effectiveWeights.entrySet()) {
            String kbIdStr = entry.getKey();
            Double weight = entry.getValue();
            
            // 计算该知识库应召回的数量
            int kbCandidateCount = (int) Math.ceil(totalCandidateCount * (weight / totalWeight));
            
            // 至少召回1条（如果权重>0）
            kbCandidateCount = Math.max(kbCandidateCount, 1);
            
            log.info("  - 知识库ID={}, 权重={}, 分配召回数={}", 
                    kbIdStr, String.format("%.2f", weight), kbCandidateCount);
            
            // 3. 为该知识库单独检索
            // 使用IsEqualTo过滤器
            Filter kbFilter = new dev.langchain4j.store.embedding.filter.comparison.IsEqualTo(
                    "knowledgeBaseId", Long.parseLong(kbIdStr));
            
            List<Content> kbContents;
            if (isVectorSearch) {
                // 向量检索
                EmbeddingStoreContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
                        .embeddingModel(embeddingModel)
                        .embeddingStore(embeddingStore)
                        .maxResults(kbCandidateCount)
                        .minScore(minScore)
                        .filter(kbFilter)
                        .build();
                
                kbContents = retriever.retrieve(new Query(query));
            } else {
                // BM25检索
                List<EmbeddingMatch<TextSegment>> bm25Matches = elasticsearchEmbeddingStore.bm25Search(
                        query, kbIdStr, kbCandidateCount);
                
                // 转换为Content列表
                kbContents = bm25Matches.stream()
                        .filter(match -> match.score() >= minScore)
                        .map(match -> {
                            TextSegment segment = match.embedded();
                            return Content.from(segment);
                        })
                        .collect(java.util.stream.Collectors.toList());
            }
            
            log.info("    ✓ 实际召回: {} 条", kbContents.size());
            allContents.addAll(kbContents);
        }
        
        log.info("  📊 总召回数: {} 条（来自 {} 个知识库）", 
                allContents.size(), effectiveWeights.size());
        
        return allContents;
    }
    
    /**
     * 应用知识库权重到检索结果
     * 
     * <p>根据知识库权重调整每个结果的分数，实现知识库优先级控制</p>
     * 
     * @param contents 检索结果列表
     * @param knowledgeBaseWeights 知识库权重配置
     * @return 应用权重后的结果列表（已按分数排序）
     */
    private List<Content> applyKnowledgeBaseWeights(List<Content> contents, 
                                                      Map<String, Double> knowledgeBaseWeights) {
        if (knowledgeBaseWeights.isEmpty()) {
            log.debug("未配置知识库权重，跳过权重应用");
            return contents;
        }
        
        log.info("\n>>> 开始应用知识库权重");
        
        // 为每个内容计算加权分数
        List<WeightedContent> weightedContents = new ArrayList<>();
        
        for (Content content : contents) {
            var metadata = content.textSegment().metadata();
            if (metadata == null) {
                weightedContents.add(new WeightedContent(content, 1.0, 1.0));
                continue;
            }
            
            String kbId = metadata.getString("knowledgeBaseId");
            if (kbId == null) {
                weightedContents.add(new WeightedContent(content, 1.0, 1.0));
                continue;
            }
            
            // 获取知识库权重（默认1.0）
            Double weight = knowledgeBaseWeights.getOrDefault(kbId, 1.0);
            
            // 获取原始分数（如果有）
            Double originalScore = 1.0;
            Object scoreObj = metadata.toMap().get("score");
            if (scoreObj != null) {
                try {
                    originalScore = Double.parseDouble(scoreObj.toString());
                } catch (Exception e) {
                    // 忽略解析错误
                }
            }
            
            // 计算加权分数
            double weightedScore = originalScore * weight;
            
            weightedContents.add(new WeightedContent(content, originalScore, weightedScore));
            
            log.debug("知识库ID={}, 原始分数={}, 权重={}, 加权分数={}", 
                     kbId, String.format("%.4f", originalScore), 
                     String.format("%.2f", weight), String.format("%.4f", weightedScore));
        }
        
        // 按加权分数降序排序
        weightedContents.sort((a, b) -> Double.compare(b.weightedScore, a.weightedScore));
        
        // 提取排序后的内容
        List<Content> result = weightedContents.stream()
            .map(wc -> wc.content)
            .collect(java.util.stream.Collectors.toList());
        
        log.info("✅ 知识库权重应用完成，结果已重新排序");
        
        return result;
    }
    
    /**
     * 加权内容包装类
     */
    private static class WeightedContent {
        Content content;
        double originalScore;
        double weightedScore;
        
        WeightedContent(Content content, double originalScore, double weightedScore) {
            this.content = content;
            this.originalScore = originalScore;
            this.weightedScore = weightedScore;
        }
    }

    /**
     * 获取检索内容列表
     *
     * <p>
     * 使用混合检索策略：向量检索 + BM25检索 + RRF融合。
     * 检索流程：
     * <ol>
     * <li>查询扩展（可配置）</li>
     * <li>向量检索（语义匹配）</li>
     * <li>BM25检索（关键词匹配，可配置）</li>
     * <li>RRF融合排序</li>
     * <li>应用知识库权重</li>
     * <li>重排（Rerank）</li>
     * <li>相邻分片合并</li>
     * </ol>
     * </p>
     *
     * @param query            用户查询
     * @param knowledgeBaseIds 知识库ID列表
     * @param agent            智能体配置（包含检索参数和知识库权重）
     * @return 检索到的内容列表，按相关性排序
     */
    @Override
    public List<Content> retrieveContents(String query, List<Long> knowledgeBaseIds, Agent agent) {
        log.info("\n" + "=".repeat(80));
        log.info(">>> 开始混合检索（向量 + BM25 + RRF融合 + 知识库权重）");
        log.info("知识库IDs: {}", knowledgeBaseIds);
        log.info("用户查询: {}", query);
        
        // 解析知识库权重配置
        Map<String, Double> knowledgeBaseWeights = parseKnowledgeBaseWeights(agent.getKnowledgeBaseWeights());
        if (!knowledgeBaseWeights.isEmpty()) {
            log.info("知识库权重配置: {}", knowledgeBaseWeights);
        }

        try {
            // 0. 查询改写（LLM优化查询）
            String rewrittenQuery = query;
            boolean enableQueryRewriting = ragConfig.isEnableQueryRewriting();
            
            if (enableQueryRewriting && queryRewritingService.shouldRewrite(query)) {
                try {
                    rewrittenQuery = queryRewritingService.rewriteQuery(query, agent);
                    if (!rewrittenQuery.equals(query)) {
                        log.info("🔄 查询改写: \"{}\" → \"{}\"", query, rewrittenQuery);
                    }
                } catch (Exception e) {
                    log.warn("⚠️ 查询改写失败，使用原始查询: {}", e.getMessage());
                    rewrittenQuery = query;
                }
            }
            
            // 1. 查询扩展（优先使用Agent配置，null时使用全局兜底配置）
            boolean enableQueryExpansion = agent.getRagEnableQueryExpansion() != null
                    ? agent.getRagEnableQueryExpansion()
                    : ragConfig.isEnableQueryExpansion();

            log.info("混合检索配置（来源：{}）:",
                    agent.getRagEnableQueryExpansion() != null ? "智能体" : "全局默认");
            log.info("  - 查询改写: {}", enableQueryRewriting ? "启用" : "禁用");
            log.info("  - 查询扩展: {}", enableQueryExpansion ? "启用" : "禁用");

            String expandedQuery = rewrittenQuery;
            if (enableQueryExpansion) {
                // 使用智能体专属词典进行查询扩展（基于改写后的查询）
                expandedQuery = queryExpansionService.getExpandedQueryString(rewrittenQuery, "agent", agent.getId());
                log.info("✅ 查询扩展（智能体ID={}）: {} → {}", agent.getId(), rewrittenQuery, expandedQuery);
            } else {
                log.info("⚠️ 查询扩展已禁用，使用改写后的查询");
            }

            // 将Long类型的ID转换为String类型
            List<String> idStrings = knowledgeBaseIds.stream()
                    .map(String::valueOf)
                    .toList();
            log.info("转换为字符串的IDs: {}", idStrings);

            Filter filter = new IsIn("knowledgeBaseId", idStrings);

            // 从智能体配置中获取检索参数
            double minScore = agent.getRagMinScore();
            int maxResults = agent.getRagMaxResults();
            double recallMultiplier = agent.getRagRecallMultiplier();

            // 召回数量
            int candidateCount = Math.max((int) (maxResults * recallMultiplier), ragConfig.getMinRecallCount());
            double initialMinScore = Math.max(minScore - 0.15, 0.3); // 降低阈值，增加召回

            log.info("检索参数 - 目标结果数: {}, 最低相似度: {}, 召回倍数: {}x",
                    maxResults, minScore, String.format("%.1f", recallMultiplier));

            long startTime = System.currentTimeMillis();

            // 2. 向量检索（按知识库权重分配召回数量）
            log.info("\n[路径1] 向量检索...");
            long vectorStartTime = System.currentTimeMillis();

            List<Content> vectorContents;
            
            if (!knowledgeBaseWeights.isEmpty() && knowledgeBaseIds.size() > 1) {
                // 多知识库且配置了权重：按权重分别检索
                log.info("📊 按知识库权重分配召回数量");
                vectorContents = retrieveByKnowledgeBaseWeights(
                    query, knowledgeBaseIds, knowledgeBaseWeights, 
                    candidateCount, initialMinScore, true);
            } else {
                // 单知识库或未配置权重：统一检索
                log.info("混合检索 - 每路召回: {} 条 (阈值: {}), 融合后返回: {} 条",
                        candidateCount, initialMinScore, maxResults);
                
                EmbeddingStoreContentRetriever vectorRetriever = EmbeddingStoreContentRetriever.builder()
                        .embeddingModel(embeddingModel)
                        .embeddingStore(embeddingStore)
                        .maxResults(candidateCount)
                        .minScore(initialMinScore)
                        .filter(filter)
                        .build();

                vectorContents = vectorRetriever.retrieve(new Query(query));
            }
            
            long vectorDuration = System.currentTimeMillis() - vectorStartTime;
            log.info("✅ 向量检索完成 - 结果数: {}, 耗时: {}ms", vectorContents.size(), vectorDuration);

            // 📊 记录Embedding Token使用（查询向量化）
            try {
                ModelConfig embeddingConfig = modelConfigService.getDefaultEmbeddingConfig();
                if (embeddingConfig != null) {
                    tokenUsageService.recordEmbeddingUsageAsync(
                            agent.getId(),
                            embeddingConfig.getModelName(),
                            embeddingConfig.getProvider(),
                            query,
                            "embedding_query");
                }
            } catch (Exception e) {
                log.warn("记录Embedding Token使用失败: {}", e.getMessage());
            }

            // 3. BM25检索（使用扩展查询，可配置开关）
            boolean enableHybridSearch = agent.getRagEnableHybridSearch() != null ? agent.getRagEnableHybridSearch()
                    : ragConfig.isEnableHybridSearch();

            log.info("  - 混合检索: {}", enableHybridSearch ? "启用" : "禁用");

            // 动态权重调整：根据查询类型自动调整向量/BM25权重
            double bm25Weight;
            double vectorWeight;
            
            if (enableHybridSearch && queryTypeClassifier != null) {
                // 1. 识别查询类型
                String queryType = queryTypeClassifier.classifyQueryType(query);
                log.info("  - 查询类型: {}", queryType);
                
                // 2. 获取推荐权重
                double[] recommendedWeights = queryTypeClassifier.getRecommendedWeights(queryType);
                double recommendedVectorWeight = recommendedWeights[0];
                double recommendedBm25Weight = recommendedWeights[1];
                
                // 3. 如果智能体配置了自定义权重，使用自定义权重；否则使用推荐权重
                if (agent.getRagBm25Weight() != null && agent.getRagVectorWeight() != null) {
                    // 使用智能体配置的权重
                    bm25Weight = agent.getRagBm25Weight();
                    vectorWeight = agent.getRagVectorWeight();
                    log.info("  - 权重来源: 智能体配置");
                } else {
                    // 使用动态推荐权重
                    bm25Weight = recommendedBm25Weight;
                    vectorWeight = recommendedVectorWeight;
                    log.info("  - 权重来源: 动态推荐（基于查询类型）");
                }
                
                log.info("  - 权重配置: 向量{}% : BM25{}%",
                        (int) (vectorWeight * 100), (int) (bm25Weight * 100));
            } else if (enableHybridSearch) {
                // 降级：使用配置的固定权重
                bm25Weight = agent.getRagBm25Weight() != null ? agent.getRagBm25Weight()
                        : ragConfig.getBm25Weight();
                vectorWeight = agent.getRagVectorWeight() != null ? agent.getRagVectorWeight()
                        : ragConfig.getVectorWeight();
                log.info("  - 权重来源: 固定配置");
                log.info("  - 权重配置: 向量{}% : BM25{}%",
                        (int) (vectorWeight * 100), (int) (bm25Weight * 100));
            } else {
                // 混合检索未启用，权重无意义
                bm25Weight = 0.0;
                vectorWeight = 1.0;
            }

            List<Content> bm25Contents = new ArrayList<>();

            if (enableHybridSearch) {
                log.info("\n[路径2] BM25检索...");
                long bm25StartTime = System.currentTimeMillis();

                // 对每个知识库执行BM25检索
                for (String kbId : idStrings) {
                    List<dev.langchain4j.store.embedding.EmbeddingMatch<dev.langchain4j.data.segment.TextSegment>> bm25Results = elasticsearchEmbeddingStore
                            .bm25Search(expandedQuery, kbId, candidateCount);

                    // 转换为Content对象
                    for (var match : bm25Results) {
                        Content content = Content.from(match.embedded());
                        bm25Contents.add(content);
                    }
                }

                long bm25Duration = System.currentTimeMillis() - bm25StartTime;
                log.info("✅ BM25检索完成 - 结果数: {}, 耗时: {}ms", bm25Contents.size(), bm25Duration);
            } else {
                log.info("\n⚠️ 混合检索已禁用，跳过BM25检索");
            }

            // 4. 合并去重（简单合并，后续RRF会处理）
            Set<String> seen = new HashSet<>();
            List<Content> allContents = new ArrayList<>();

            for (Content content : vectorContents) {
                String key = content.textSegment().text();
                if (seen.add(key)) {
                    allContents.add(content);
                }
            }

            for (Content content : bm25Contents) {
                String key = content.textSegment().text();
                if (seen.add(key)) {
                    allContents.add(content);
                }
            }

            long retrieveDuration = System.currentTimeMillis() - startTime;
            log.info("\n" + "=".repeat(80));
            log.info("✅ 混合检索完成!");
            log.info("统计信息:");
            log.info("  - 向量检索: {} 条 ({}ms)", vectorContents.size(), vectorDuration);
            log.info("  - BM25检索: {} 条 ({}ms)", bm25Contents.size(),
                    enableHybridSearch ? (System.currentTimeMillis() - startTime - vectorDuration) : 0);
            log.info("  - 合并去重: {} 条", allContents.size());
            log.info("  - 总耗时: {}ms", retrieveDuration);
            log.info("=".repeat(80));
            
            // 4.5. 应用知识库权重（在重排之前）
            if (!allContents.isEmpty() && !knowledgeBaseWeights.isEmpty()) {
                allContents = applyKnowledgeBaseWeights(allContents, knowledgeBaseWeights);
            }

            // 5. 重排（Rerank）- 基于关键词匹配和语义相关性
            if (!allContents.isEmpty()) {
                log.info("\n>>> 开始重排（Rerank）");
                allContents = rerankContents(allContents, query, maxResults);
                log.info("✅ 重排完成，返回Top {} 结果\n", allContents.size());

                // 6. 合并相邻分片（暂时禁用，因为合并会导致页码不准确）
                // 合并后使用的是主分片的页码，但合并的内容可能跨越多页
                // TODO: 如需启用，需要改进合并逻辑，保留每个分片的原始页码
                // if (enableHybridSearch) {
                // allContents = mergeAdjacentSegments(allContents);
                // log.info("✅ 相邻分片合并完成，最终结果: {} 条\n", allContents.size());
                // }
                log.info("ℹ️ 分片合并已禁用（保证页码准确性），最终结果: {} 条\n", allContents.size());
            }

            log.info("最终结果数量: {}", allContents.size());

            if (allContents != null && !allContents.isEmpty()) {
                int imageCount = 0;
                int textCount = 0;

                log.info("最终返回的内容详情:");
                for (int i = 0; i < allContents.size(); i++) {
                    Content content = allContents.get(i);
                    String text = content.textSegment().text();
                    String preview = text.substring(0, Math.min(150, text.length()));

                    var metadata = content.textSegment().metadata();
                    String type = metadata != null ? metadata.getString("type") : "text";

                    if ("image".equals(type)) {
                        imageCount++;
                    } else {
                        textCount++;
                    }

                    // 获取相似度分数（如果有）
                    Double score = null;
                    if (metadata != null) {
                        Object scoreObj = metadata.toMap().get("score");
                        if (scoreObj != null) {
                            score = Double.parseDouble(scoreObj.toString());
                        }
                    }

                    String contentType = "image".equals(type) ? "🖼️ 图片" : "📄 文本";
                    log.info("  [{}] {} | 相似度: {} | 长度: {} 字符",
                            i + 1,
                            contentType,
                            score != null ? String.format("%.4f", score) : "N/A",
                            text.length());
                    log.info("      预览: {}...", preview);

                    // 如果是图片，打印图片路径
                    if ("image".equals(type) && metadata != null) {
                        String imagePath = metadata.getString("imagePath");
                        String fileName = metadata.getString("fileName");
                        log.info("      📸 图片路径: {}", imagePath);
                        log.info("      📁 文件名: {}", fileName);
                    }

                    // 打印元数据以验证（包括knowledgeBaseId和定位信息）
                    if (metadata != null) {
                        String kbId = metadata.getString("knowledgeBaseId");
                        String pageNum = metadata.getString("pageNumber");
                        String segIdx = metadata.getString("segmentIndex");
                        String fileName = metadata.getString("fileName");

                        log.info("      🔑 knowledgeBaseId: {}", kbId != null ? kbId : "❌ NULL");
                        log.info("      📄 fileName: {}", fileName != null ? fileName : "❌ NULL");
                        log.info("      📍 pageNumber: {}", pageNum != null ? pageNum : "⚠️ NULL");
                        log.info("      🔢 segmentIndex: {}", segIdx != null ? segIdx : "⚠️ NULL");

                        // 判断是否可定位
                        boolean isLocatable = kbId != null && !kbId.isEmpty() &&
                                (pageNum != null || segIdx != null);
                        log.info("      {} 定位状态: {}",
                                isLocatable ? "✅" : "❌",
                                isLocatable ? "可定位" : "无法定位（缺少必要信息）");

                        log.debug("      📋 完整元数据: {}", metadata.toMap());
                    } else {
                        log.warn("      ❌ metadata 为 NULL");
                    }
                }

                log.info("\n📊 检索结果统计: 文本={}, 图片={}, 总计={}", textCount, imageCount, allContents.size());
            }

            if (allContents == null || allContents.isEmpty()) {
                log.warn("\n⚠️⚠️⚠️ 未检索到任何内容! ⚠️⚠️⚠️");
                log.warn("可能的原因:");
                log.warn("  1. 向量库中没有对应知识库ID的向量数据");
                log.warn("  2. 元数据字段名不匹配(检查是否为'knowledgeBaseId')");
                log.warn("  3. 相似度阈值太高");
                log.warn("  4. 查询内容与知识库内容相关性太低");
                log.warn("=".repeat(80));
                return new ArrayList<>();
            }

            log.info("\n✅ 混合检索成功! 返回内容数量: {}", allContents.size());
            log.info("=".repeat(80) + "\n");

            // 性能统计
            logPerformanceStats(vectorDuration,
                    enableHybridSearch ? (System.currentTimeMillis() - startTime - vectorDuration) : 0,
                    retrieveDuration, allContents.size());

            return allContents;

        } catch (Exception e) {
            log.error("\n❌ 知识库检索异常!", e);
            log.error("异常类型: {}", e.getClass().getName());
            log.error("异常消息: {}", e.getMessage());
            log.error("-".repeat(80) + "\n");
            return null;
        }
    }

    /**
     * 重排（Rerank）检索结果
     *
     * <p>
     * 优先使用 Reranker 模型（如 Qwen3-Rerank）进行深度语义重排，
     * 如果未配置 Reranker，则降级到基于规则的关键词匹配重排。
     * </p>
     *
     * @param contents 原始检索结果
     * @param query    用户查询
     * @param topK     返回的最大结果数
     * @return 重排后的结果列表
     */
    @Override
    public List<Content> rerankContents(List<Content> contents, String query, int topK) {
        log.info("开始重排 {} 个候选结果", contents.size());
        
        // 🔥 如果配置了 Reranker 模型，使用 API 进行深度语义重排
        if (rerankModel != null) {
            return rerankByModel(contents, query, topK);
        }
        
        // 否则使用原有的规则匹配（向后兼容）
        log.info("使用规则匹配进行重排");
        return rerankByRules(contents, query, topK);
    }
    
    /**
     * 使用 Reranker 模型进行重排（深度语义理解）
     */
    private List<Content> rerankByModel(List<Content> contents, String query, int topK) {
        log.info("🤖 使用 Reranker 模型进行深度语义重排");
        
        try {
            // 提取文档文本
            List<String> documents = contents.stream()
                .map(c -> c.textSegment().text())
                .toList();
            
            // 调用 Reranker API
            List<RerankResult> results = rerankModel.rerank(query, documents, topK);
            
            // 根据结果重新排序 Content
            List<Content> reranked = new ArrayList<>();
            Map<Content, Double> contentRerankScores = contentRerankScoresHolder.get();
            
            for (RerankResult result : results) {
                Content content = contents.get(result.getIndex());
                reranked.add(content);
                
                // 存储重排分数（供前端显示）
                contentRerankScores.put(content, result.getRelevanceScore());
                
                String preview = result.getDocument().substring(0, Math.min(100, result.getDocument().length()));
                log.info("  [{}] 分数: {} | 预览: {}...", 
                    reranked.size(), 
                    String.format("%.4f", result.getRelevanceScore()),
                    preview);
            }
            
            log.info("✅ Reranker 模型重排完成，返回 {} 条结果", reranked.size());
            return reranked;
            
        } catch (Exception e) {
            log.error("❌ Reranker 模型调用失败，降级到规则匹配: {}", e.getMessage());
            // 降级到规则匹配
            return rerankByRules(contents, query, topK);
        }
    }
    
    /**
     * 使用规则匹配进行重排（关键词匹配）
     */
    private List<Content> rerankByRules(List<Content> contents, String query, int topK) {
        log.info("开始重排 {} 个候选结果", contents.size());

        // 获取当前线程的重排分数映射
        Map<Content, Double> contentRerankScores = contentRerankScoresHolder.get();

        // 提取查询关键词（去除标点和停用词）
        String normalizedQuery = TextProcessingUtils.PUNCTUATION_PATTERN.matcher(query.toLowerCase()).replaceAll(" ")
                .trim();
        String[] keywords = contentScoringService.extractKeywords(query);

        log.info("提取的关键词: {}", String.join(", ", keywords));

        // 为每个内容计算相关性分数，并将分数写入metadata
        List<ContentScoringService.ScoredContent> scoredContents = new ArrayList<>();
        for (Content content : contents) {
            String text = content.textSegment().text().toLowerCase();
            String originalText = content.textSegment().text();
            double score = contentScoringService.calculateRelevanceScore(text, keywords, normalizedQuery);

            // 将重排分数存储到Map中，供后续buildReferencesButtons使用
            contentRerankScores.put(content, score);

            scoredContents.add(new ContentScoringService.ScoredContent(content, score));

            // 打印前100字符和分数用于调试
            String preview = originalText.substring(0, Math.min(100, originalText.length()));
            log.info("📝 候选[{}]: \"{}...\" | 重排分数: {}",
                    scoredContents.size(), preview, String.format("%.2f", score));
        }

        // 按分数降序排序
        scoredContents.sort((a, b) -> Double.compare(b.score, a.score));

        if (scoredContents.isEmpty()) {
            log.warn("⚠️ 重排后无内容");
            return new ArrayList<>();
        }

        double topScore = scoredContents.get(0).score;
        log.info("📊 重排完成 - 最高分: {}, 共{}个候选", String.format("%.1f", topScore), scoredContents.size());

        // 🔥 如果最高分 <= 0，说明完全不相关，返回空列表
        // 这样AI会收到友好的"无知识库内容"提示，可以自然地打招呼或介绍自己
        if (topScore <= 0.0) {
            log.warn("⚠️ 重排后最高分为0，内容与查询完全不相关");
            log.warn("💡 返回空列表，由AI根据系统提示词友好回应");
            return new ArrayList<>();
        }

        // 🎯 智能过滤策略：
        // - 文本：最高分的30%作为阈值，过滤低质量文本
        // - 图片：使用独立评分体系，阈值设为 4.5（图片描述因0.3降权分数较低）
        //   该阈值可过滤弱相关图片（如本例4.08分的智能音箱），保留强相关图片
        double textMinScore = Math.max(2.0, topScore * 0.3);
        double imageMinScore = 4.5;  // 图片独立阈值，高于4.08可过滤弱相关图片
        List<Content> reranked = new ArrayList<>();
        
        int filteredTextCount = 0;
        int filteredImageCount = 0;
        for (int i = 0; i < Math.min(topK, scoredContents.size()); i++) {
            ContentScoringService.ScoredContent sc = scoredContents.get(i);
            var metadata = sc.content.textSegment().metadata();
            String type = metadata != null ? metadata.getString("type") : null;
            
            // 🖼️ 图片过滤：使用独立阈值 4.5（只保留强相关图片）
            if ("image".equals(type)) {
                if (sc.score < imageMinScore) {
                    log.info("🚫 过滤弱相关图片 - 页码: {}, 分数: {} (阈值: {})",
                            metadata.getString("pageNumber"),
                            String.format("%.2f", sc.score),
                            String.format("%.2f", imageMinScore));
                    filteredImageCount++;
                    continue;
                } else {
                    log.info("✅ 保留强相关图片 - 页码: {}, 分数: {} (阈值: {})",
                            metadata.getString("pageNumber"),
                            String.format("%.2f", sc.score),
                            String.format("%.2f", imageMinScore));
                }
            }
            // 📄 文本过滤：使用30%阈值
            else {
                if (sc.score < textMinScore) {
                    log.debug("⏭️ 跳过低分文本 - 分数: {} (阈值: {})",
                            String.format("%.2f", sc.score),
                            String.format("%.2f", textMinScore));
                    filteredTextCount++;
                    continue;
                }
            }
            
            reranked.add(sc.content);
        }

        if (filteredTextCount > 0 || filteredImageCount > 0) {
            log.info("🔥 过滤统计 - 文本: {} 个（分数 < {}），图片: {} 个（分数 < {}）",
                    filteredTextCount, String.format("%.2f", textMinScore), 
                    filteredImageCount, String.format("%.2f", imageMinScore));
        }
        log.info("✅ 重排完成，返回Top {} 结果（过滤后）", reranked.size());

        return reranked;
    }

    /**
     * 合并相邻分片
     *
     * <p>
     * 智能合并来自同一文件的相邻分片，避免内容断层。
     * 合并策略：
     * <ul>
     * <li>按文件分组分析所有分片</li>
     * <li>查找segmentIndex差距<=3的相邻分片</li>
     * <li>合并文本并保留主分片的页码信息</li>
     * </ul>
     * </p>
     *
     * @param contents 原始内容列表
     * @return 合并后的内容列表
     */
    @Override
    public List<Content> mergeAdjacentSegments(List<Content> contents) {
        if (contents.size() <= 1) {
            return contents;
        }

        log.info("开始智能合并相邻分片（主动补充关联内容）...");

        // 第一步：分析所有分片，按文件分组
        Map<String, List<SegmentInfo>> fileSegments = new HashMap<>();

        for (int i = 0; i < contents.size(); i++) {
            Content content = contents.get(i);
            var metadata = content.textSegment().metadata();
            if (metadata == null) {
                continue;
            }

            String fileName = metadata.getString("fileName");
            String knowledgeBaseId = metadata.getString("knowledgeBaseId");
            if (fileName == null || knowledgeBaseId == null) {
                continue;
            }

            String fileKey = knowledgeBaseId + ":" + fileName;

            SegmentInfo info = new SegmentInfo();
            info.content = content;
            info.index = i;
            info.pageNumber = parsePageNumber(metadata.getString("pageNumber"));
            info.segmentIndex = parseSegmentIndex(metadata.getString("segmentIndex"));

            // 使用computeIfAbsent避免重复查找
            fileSegments.computeIfAbsent(fileKey, k -> new ArrayList<>()).add(info);
        }

        // 第二步：对每个文件的分片进行分析和补充
        Set<Integer> processedIndices = new HashSet<>();
        List<Content> merged = new ArrayList<>();

        for (int i = 0; i < contents.size(); i++) {
            if (processedIndices.contains(i)) {
                continue; // 已经被合并过
            }

            Content content = contents.get(i);
            var metadata = content.textSegment().metadata();

            if (metadata == null) {
                merged.add(content);
                processedIndices.add(i);
                continue;
            }

            String fileName = metadata.getString("fileName");
            String knowledgeBaseId = metadata.getString("knowledgeBaseId");

            if (fileName == null || knowledgeBaseId == null) {
                merged.add(content);
                processedIndices.add(i);
                continue;
            }

            String fileKey = knowledgeBaseId + ":" + fileName;
            List<SegmentInfo> segments = fileSegments.get(fileKey);

            if (segments == null || segments.size() <= 1) {
                merged.add(content);
                processedIndices.add(i);
                continue;
            }

            // 找到当前分片在列表中的位置
            SegmentInfo currentInfo = null;
            for (SegmentInfo info : segments) {
                if (info.index == i) {
                    currentInfo = info;
                    break;
                }
            }

            if (currentInfo == null) {
                merged.add(content);
                processedIndices.add(i);
                continue;
            }

            // 查找需要合并的相邻分片（前后各查找2个分片索引范围）
            List<SegmentInfo> toMerge = new ArrayList<>();
            toMerge.add(currentInfo);

            // 向前查找
            for (SegmentInfo info : segments) {
                if (info.index < i && !processedIndices.contains(info.index)) {
                    // 检查是否相邻（segmentIndex差距<=3）
                    if (Math.abs(info.segmentIndex - currentInfo.segmentIndex) <= 3) {
                        toMerge.add(info);
                    }
                }
            }

            // 向后查找
            for (SegmentInfo info : segments) {
                if (info.index > i && !processedIndices.contains(info.index)) {
                    // 检查是否相邻（segmentIndex差距<=3）
                    if (Math.abs(info.segmentIndex - currentInfo.segmentIndex) <= 3) {
                        toMerge.add(info);
                    }
                }
            }

            // 按segmentIndex排序
            toMerge.sort((a, b) -> Integer.compare(a.segmentIndex, b.segmentIndex));

            if (toMerge.size() > 1) {
                // 合并文本
                StringBuilder mergedText = new StringBuilder();
                // 找出所有涉及的页码
                Set<Integer> involvedPages = new TreeSet<>();
                for (SegmentInfo info : toMerge) {
                    if (mergedText.length() > 0) {
                        mergedText.append("\n");
                    }
                    mergedText.append(info.content.textSegment().text());
                    processedIndices.add(info.index);
                    if (info.pageNumber > 0) {
                        involvedPages.add(info.pageNumber);
                    }
                }

                // 使用"主分片"（currentInfo，即被检索命中的分片）的页码
                // 因为这是与用户查询最相关的内容所在的页
                // 如果主分片页码无效，则使用涉及页码的中间值
                int bestPageNumber = currentInfo.pageNumber;
                if (bestPageNumber <= 0 && !involvedPages.isEmpty()) {
                    List<Integer> pageList = new ArrayList<>(involvedPages);
                    bestPageNumber = pageList.get(pageList.size() / 2); // 取中间页码
                }

                // 创建合并后的Content，使用正确的页码
                var originalMetadata = currentInfo.content.textSegment().metadata();
                // 安全获取元数据值，避免null
                String kbId = originalMetadata.getString("knowledgeBaseId");
                String fName = originalMetadata.getString("fileName");
                String segIdx = originalMetadata.getString("segmentIndex");
                String fType = originalMetadata.getString("fileType");
                String cType = originalMetadata.getString("type");

                dev.langchain4j.data.segment.TextSegment mergedSegment = dev.langchain4j.data.segment.TextSegment.from(
                        mergedText.toString(),
                        Metadata.from("knowledgeBaseId", kbId != null ? kbId : "")
                                .put("fileName", fName != null ? fName : "")
                                .put("segmentIndex", segIdx != null ? segIdx : "0")
                                .put("pageNumber", String.valueOf(bestPageNumber))
                                .put("fileType", fType != null ? fType : "")
                                .put("type", cType != null ? cType : "text"));
                Content mergedContent = Content.from(mergedSegment);

                merged.add(mergedContent);

                log.info("  ✓ 合并 {} 个分片: {} (索引 {} - {}, 页码范围: {}, 使用页码: {})",
                        toMerge.size(), fileName,
                        toMerge.get(0).segmentIndex,
                        toMerge.get(toMerge.size() - 1).segmentIndex,
                        involvedPages,
                        bestPageNumber);
            } else {
                merged.add(content);
                processedIndices.add(i);
            }
        }

        int mergedCount = contents.size() - merged.size();
        if (mergedCount > 0) {
            log.info("✅ 智能合并了 {} 个分片，从 {} 条减少到 {} 条", mergedCount, contents.size(), merged.size());
        } else {
            log.info("ℹ️ 没有可合并的分片");
        }

        return merged;
    }

    /**
     * 分片信息辅助类
     *
     * <p>
     * 用于合并相邻分片时存储分片的元信息
     * </p>
     */
    private static class SegmentInfo {
        /** 分片内容 */
        Content content;
        /** 在原始列表中的索引 */
        int index;
        /** 页码 */
        int pageNumber;
        /** 分片索引（用于判断相邻性） */
        int segmentIndex;
    }

    /**
     * 解析页码字符串
     *
     * @param pageNumberStr 页码字符串
     * @return 页码数值，解析失败返回-1
     */
    private int parsePageNumber(String pageNumberStr) {
        if (pageNumberStr == null)
            return -1;
        try {
            return Integer.parseInt(pageNumberStr);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * 解析分片索引字符串
     *
     * @param segmentIndexStr 分片索引字符串
     * @return 分片索引数值，解析失败返回-1
     */
    private int parseSegmentIndex(String segmentIndexStr) {
        if (segmentIndexStr == null)
            return -1;
        try {
            return Integer.parseInt(segmentIndexStr);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * 记录性能统计信息
     *
     * <p>
     * 输出检索各阶段的耗时统计，用于性能分析和优化
     * </p>
     *
     * @param vectorDuration 向量检索耗时（毫秒）
     * @param bm25Duration   BM25检索耗时（毫秒）
     * @param totalDuration  总耗时（毫秒）
     * @param resultCount    结果数量
     */
    private void logPerformanceStats(long vectorDuration, long bm25Duration, long totalDuration, int resultCount) {
        log.info("\n" + "=".repeat(80));
        log.info("📊 性能统计报告");
        log.info("=".repeat(80));
        log.info("向量检索耗时: {}ms (占比 {}%)", vectorDuration,
                String.format("%.1f", vectorDuration * 100.0 / totalDuration));

        if (bm25Duration > 0) {
            log.info("BM25检索耗时: {}ms (占比 {}%)", bm25Duration,
                    String.format("%.1f", bm25Duration * 100.0 / totalDuration));
        }

        log.info("总检索耗时: {}ms", totalDuration);
        log.info("结果数量: {} 条", resultCount);
        log.info("平均每条耗时: {}ms", resultCount > 0 ? totalDuration / resultCount : 0);
        log.info("=".repeat(80) + "\n");
    }
}
