package com.moyun.ext.ai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 多模态 Rerank 结果
 * 
 * <p>封装重排序后的多模态文档及其相关性分数</p>
 * 
 * @author laomao
 * @since 2025-01-22
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MultimodalRerankResult {
    
    /**
     * 原始文档在输入列表中的索引
     */
    private int index;
    
    /**
     * 文档内容（多模态）
     */
    private MultimodalInput document;
    
    /**
     * 相关性分数（0-1之间，越高越相关）
     */
    private double relevanceScore;
}
