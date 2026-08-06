package com.moyun.ext.ai.dto;

import lombok.Data;

/**
 * 检索测试结果
 *
 * @author laomao
 */
@Data
public class RetrievalTestResult {
    
    /**
     * 分片索引
     */
    private Integer segmentIndex;
    
    /**
     * 分片内容
     */
    private String content;
    
    /**
     * 相似度评分（0-1）
     */
    private Double score;
    
    /**
     * 元数据（如页码等）
     */
    private String metadata;
    
    /**
     * 文件名（多文档检索时使用）
     */
    private String fileName;
}
