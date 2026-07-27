package com.moyun.agent.model;

import java.util.List;

/**
 * 多模态 Reranker 模型接口
 * 
 * <p>支持文本、图片、视频等多种模态的重排序，提供更精准的相关性评分。
 * 适用于多模态检索场景的精排阶段。</p>
 * 
 * <p><b>注意：</b>此接口为预留接口，等待 DashScope API 支持 Qwen3-VL-Reranker 后实现。
 * 当前可以通过本地部署 vLLM 服务来使用。</p>
 * 
 * @author laomao
 * @since 2025-01-22
 * @see <a href="https://github.com/QwenLM/Qwen3-VL-Embedding">Qwen3-VL-Reranker</a>
 */
public interface MultimodalRerankModel {
    
    /**
     * 对多模态文档列表进行重排序
     * 
     * <p>支持以下场景：</p>
     * <ul>
     *   <li>文本查询 → 文本文档</li>
     *   <li>文本查询 → 图片文档</li>
     *   <li>图片查询 → 文本文档</li>
     *   <li>图片查询 → 图片文档</li>
     *   <li>混合查询 → 混合文档</li>
     * </ul>
     * 
     * @param query 查询内容（可以是文本、图片或混合）
     * @param documents 待排序的文档列表（可以是文本、图片或混合）
     * @param topK 返回前K个结果
     * @return 重排序后的结果列表，按相关性降序排列
     */
    List<MultimodalRerankResult> rerank(MultimodalInput query, List<MultimodalInput> documents, int topK);
    
    /**
     * 对多模态文档列表进行重排序（返回所有结果）
     * 
     * @param query 查询内容
     * @param documents 待排序的文档列表
     * @return 重排序后的结果列表，按相关性降序排列
     */
    default List<MultimodalRerankResult> rerank(MultimodalInput query, List<MultimodalInput> documents) {
        return rerank(query, documents, documents.size());
    }
    
    /**
     * 对文本文档进行重排序（兼容纯文本场景）
     * 
     * @param query 文本查询
     * @param documents 文本文档列表
     * @param topK 返回前K个结果
     * @return 重排序后的结果列表
     */
    default List<RerankResult> rerankText(String query, List<String> documents, int topK) {
        // 转换为 MultimodalInput
        MultimodalInput queryInput = MultimodalInput.text(query);
        List<MultimodalInput> docInputs = documents.stream()
            .map(MultimodalInput::text)
            .toList();
        
        // 调用多模态重排
        List<MultimodalRerankResult> results = rerank(queryInput, docInputs, topK);
        
        // 转换回 RerankResult
        return results.stream()
            .map(r -> new RerankResult(r.getIndex(), r.getDocument().getText(), r.getRelevanceScore()))
            .toList();
    }
}
