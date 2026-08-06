package com.moyun.ext.ai.dto;

import lombok.Data;

/**
 * 检索测试请求
 *
 * @author laomao
 */
@Data
public class RetrievalTestRequest {
    
    /**
     * 查询文本
     */
    private String query;
    
    /**
     * 检索模式：vector(向量), keyword(关键词), hybrid(混合)
     */
    private String retrievalMode;
    
    /**
     * Top K数量
     */
    private Integer topK;
}
