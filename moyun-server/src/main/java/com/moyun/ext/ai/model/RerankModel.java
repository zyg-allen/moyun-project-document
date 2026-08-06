package com.moyun.ext.ai.model;

import java.util.List;

/**
 * Rerank 模型接口
 * 
 * <p>用于对检索结果进行重排序，提升相关性排序准确度。
 * 类似于 LangChain4j 的 ChatLanguageModel 和 EmbeddingModel 接口。</p>
 * 
 * @author laomao
 * @since 2025-01-22
 */
public interface RerankModel {
    
    /**
     * 对文档列表进行重排序
     * 
     * @param query 查询文本
     * @param documents 待排序的文档列表
     * @param topK 返回前K个结果
     * @return 重排序后的结果列表，按相关性降序排列
     */
    List<RerankResult> rerank(String query, List<String> documents, int topK);
    
    /**
     * 对文档列表进行重排序（返回所有结果）
     * 
     * @param query 查询文本
     * @param documents 待排序的文档列表
     * @return 重排序后的结果列表，按相关性降序排列
     */
    default List<RerankResult> rerank(String query, List<String> documents) {
        return rerank(query, documents, documents.size());
    }
}
