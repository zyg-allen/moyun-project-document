package com.moyun.agent.dto;

import com.moyun.agent.entity.KnowledgeConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 配置知识库响应DTO
 *
 * @author laomao
 * @time 2025/11/23
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfigureKnowledgeResponse {

    /**
     * 知识库ID
     */
    private Long knowledgeId;

    /**
     * 配置对象
     */
    private KnowledgeConfig config;

    /**
     * 处理状态
     */
    private String processingStatus;
}
