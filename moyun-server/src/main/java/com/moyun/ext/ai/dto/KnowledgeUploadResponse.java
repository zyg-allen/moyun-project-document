package com.moyun.ext.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 知识库上传响应DTO
 *
 * <p>两阶段处理：先上传文件，再配置处理参数</p>
 *
 * @author laomao
 * @time 2025/11/23
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeUploadResponse {

    /**
     * 知识库ID
     */
    private Long knowledgeId;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 处理状态：pending(待配置), configured(已配置), processing(处理中), completed(已完成)
     */
    private String processingStatus;

    /**
     * 是否需要配置（新上传的文件为true）
     */
    private Boolean needsConfiguration;

    /**
     * 推荐的配置模板列表
     */
    private List<ConfigTemplateInfo> recommendedTemplates;

    /**
     * 下一步操作提示
     */
    private String nextStep;

    @Data
    public static class ConfigTemplateInfo {
        private Long templateId;
        private String templateName;
        private String templateDesc;
        private String templateType;
        private Boolean isRecommended;
    }
}
