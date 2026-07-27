package com.moyun.agent.dto;

import lombok.Data;

/**
 * 知识库元数据更新请求
 *
 * @author laomao
 */
@Data
public class KnowledgeMetadataRequest {
    
    /**
     * 知识库分组
     */
    private String category;
    
    /**
     * 知识库标签（JSON数组字符串）
     */
    private String tags;
    
    /**
     * 知识库描述
     */
    private String description;
}
