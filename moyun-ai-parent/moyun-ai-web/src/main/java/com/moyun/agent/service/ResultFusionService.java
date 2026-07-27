package com.moyun.agent.service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;

import java.util.List;

/**
 * 结果融合服务接口
 *
 * <p>使用RRF(Reciprocal Rank Fusion)算法融合多路召回结果，
 * 将向量检索和BM25检索的结果进行加权融合，提升检索准确性</p>
 *
 * @author laomao
 */
public interface ResultFusionService {

    /**
     * 使用RRF算法融合多路检索结果
     *
     * @param vectorResults 向量检索结果
     * @param bm25Results BM25检索结果
     * @param topK 返回Top-K结果
     * @return 融合后的结果
     */
    List<EmbeddingMatch<TextSegment>> fuseResults(
            List<EmbeddingMatch<TextSegment>> vectorResults,
            List<EmbeddingMatch<TextSegment>> bm25Results,
            int topK);

    /**
     * 加权融合算法（备选方案）
     *
     * @param vectorResults 向量检索结果
     * @param bm25Results BM25检索结果
     * @param vectorWeight 向量检索权重
     * @param bm25Weight BM25检索权重
     * @param topK 返回Top-K结果
     * @return 融合后的结果
     */
    List<EmbeddingMatch<TextSegment>> fuseWithWeights(
            List<EmbeddingMatch<TextSegment>> vectorResults,
            List<EmbeddingMatch<TextSegment>> bm25Results,
            double vectorWeight,
            double bm25Weight,
            int topK);
}
