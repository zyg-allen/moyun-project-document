package com.moyun.ext.ai.store;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;

import java.util.List;

/**
 * 向量存储扩展接口
 *
 * <p>在 langchain4j {@link EmbeddingStore} 标准能力（add/search）之外，补充本项目
 * RAG / 知识库管理所需的扩展能力：BM25 全文检索、按知识库批量删除、按 ID 删除。</p>
 *
 * <p>所有向量存储实现（ES / Redis / 其他）均应实现此接口，业务层面向此接口编程，
 * 便于底层向量库切换而业务零改动。</p>
 *
 * @author laomao
 * @since 2025-12-15
 */
public interface VectorStoreExtension extends EmbeddingStore<TextSegment> {

    /**
     * BM25 关键词检索（全文匹配）
     *
     * @param queryText       查询文本
     * @param knowledgeBaseId 知识库 ID（用于过滤）
     * @param maxResults      最大返回数
     * @return 匹配结果列表（score 已归一化到 [0,1]）
     */
    List<EmbeddingMatch<TextSegment>> bm25Search(String queryText, String knowledgeBaseId, int maxResults);

    /**
     * 按知识库 ID 删除该知识库下所有向量
     *
     * @param knowledgeBaseId 知识库 ID
     * @return 删除的文档数量（无法精确统计时返回 0 不抛异常）
     */
    int deleteByKnowledgeBaseId(String knowledgeBaseId);

    /**
     * 按文档 ID 删除单条向量
     *
     * @param documentId 文档 ID
     * @return 是否删除成功
     */
    boolean deleteById(String documentId);
}
