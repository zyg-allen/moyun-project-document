package com.moyun.agent.service.impl;

import com.moyun.agent.service.ResultFusionService;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 结果融合服务实现类
 *
 * <p>使用RRF(Reciprocal Rank Fusion)算法融合多路召回结果，
 * 将向量检索和BM25检索的结果进行加权融合，提升检索准确性</p>
 *
 * @author laomao
 */
@Slf4j
@Service
public class ResultFusionServiceImpl implements ResultFusionService {

    /**
     * RRF常数k，通常取60
     */
    private static final int RRF_K = 60;

    @Override
    public List<EmbeddingMatch<TextSegment>> fuseResults(
            List<EmbeddingMatch<TextSegment>> vectorResults,
            List<EmbeddingMatch<TextSegment>> bm25Results,
            int topK) {

        log.info("开始RRF融合 - 向量结果: {}, BM25结果: {}", vectorResults.size(), bm25Results.size());

        // 使用文档ID作为key，RRF分数作为value
        Map<String, Double> rrfScores = new HashMap<>();
        Map<String, EmbeddingMatch<TextSegment>> docMap = new HashMap<>();

        // 1. 计算向量检索的RRF分数
        for (int rank = 0; rank < vectorResults.size(); rank++) {
            EmbeddingMatch<TextSegment> match = vectorResults.get(rank);
            String docId = getDocId(match);
            double rrfScore = 1.0 / (RRF_K + rank + 1);

            rrfScores.merge(docId, rrfScore, Double::sum);
            docMap.putIfAbsent(docId, match);

            log.debug("向量结果[{}] - docId: {}, RRF分数: {}", rank, docId, rrfScore);
        }

        // 2. 计算BM25检索的RRF分数
        for (int rank = 0; rank < bm25Results.size(); rank++) {
            EmbeddingMatch<TextSegment> match = bm25Results.get(rank);
            String docId = getDocId(match);
            double rrfScore = 1.0 / (RRF_K + rank + 1);

            rrfScores.merge(docId, rrfScore, Double::sum);
            docMap.putIfAbsent(docId, match);

            log.debug("BM25结果[{}] - docId: {}, RRF分数: {}", rank, docId, rrfScore);
        }

        // 3. 按RRF分数排序
        List<Map.Entry<String, Double>> sortedEntries = rrfScores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topK)
                .collect(Collectors.toList());

        // 4. 构建最终结果
        List<EmbeddingMatch<TextSegment>> fusedResults = new ArrayList<>();
        for (Map.Entry<String, Double> entry : sortedEntries) {
            String docId = entry.getKey();
            double rrfScore = entry.getValue();

            EmbeddingMatch<TextSegment> originalMatch = docMap.get(docId);

            // 创建新的Match，使用RRF分数
            EmbeddingMatch<TextSegment> fusedMatch = new EmbeddingMatch<>(
                    rrfScore,
                    originalMatch.embeddingId(),
                    originalMatch.embedding(),
                    originalMatch.embedded()
            );

            fusedResults.add(fusedMatch);
        }

        log.info("RRF融合完成 - 融合前总数: {}, 融合后Top-{}: {}",
                rrfScores.size(), topK, fusedResults.size());

        return fusedResults;
    }

    @Override
    public List<EmbeddingMatch<TextSegment>> fuseWithWeights(
            List<EmbeddingMatch<TextSegment>> vectorResults,
            List<EmbeddingMatch<TextSegment>> bm25Results,
            double vectorWeight,
            double bm25Weight,
            int topK) {

        log.info("开始加权融合 - 向量权重: {}, BM25权重: {}", vectorWeight, bm25Weight);

        Map<String, Double> weightedScores = new HashMap<>();
        Map<String, EmbeddingMatch<TextSegment>> docMap = new HashMap<>();

        // 1. 向量检索结果加权
        for (EmbeddingMatch<TextSegment> match : vectorResults) {
            String docId = getDocId(match);
            double score = match.score() * vectorWeight;
            weightedScores.merge(docId, score, Double::sum);
            docMap.putIfAbsent(docId, match);
        }

        // 2. BM25检索结果加权
        for (EmbeddingMatch<TextSegment> match : bm25Results) {
            String docId = getDocId(match);
            double score = match.score() * bm25Weight;
            weightedScores.merge(docId, score, Double::sum);
            docMap.putIfAbsent(docId, match);
        }

        // 3. 排序并返回Top-K
        return weightedScores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topK)
                .map(entry -> {
                    String docId = entry.getKey();
                    double score = entry.getValue();
                    EmbeddingMatch<TextSegment> originalMatch = docMap.get(docId);

                    return new EmbeddingMatch<>(
                            score,
                            originalMatch.embeddingId(),
                            originalMatch.embedding(),
                            originalMatch.embedded()
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取文档ID（用于去重）
     * 优先使用embeddingId，如果没有则使用文本内容的hash
     */
    private String getDocId(EmbeddingMatch<TextSegment> match) {
        if (match.embeddingId() != null && !match.embeddingId().isEmpty()) {
            return match.embeddingId();
        }

        // 使用文本内容的hash作为ID
        String text = match.embedded().text();
        return String.valueOf(text.hashCode());
    }
}
