package com.moyun.ext.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识库处理状态响应DTO
 *
 * @author laomao
 * @time 2025/11/23
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeStatusResponse {

    /**
     * 知识库ID
     */
    private Long knowledgeId;

    /**
     * 处理状态
     */
    private String processingStatus;

    /**
     * 状态文本
     */
    private String statusText;

    /**
     * 进度百分比（0-100）
     */
    private Integer progress;

    /**
     * 已处理的分片数
     */
    private Integer processedSegments;

    /**
     * 总分片数
     */
    private Integer totalSegments;

    /**
     * 错误信息
     */
    private String errorMessage;
}
