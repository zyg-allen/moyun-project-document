package com.moyun.ext.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 知识库配置模板响应DTO
 *
 * @author laomao
 * @time 2025/11/23
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeTemplateResponse {

    /**
     * 模板列表
     */
    private List<Template> templates;

    /**
     * 模板类
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Template {
        /**
         * 模板ID
         */
        private String id;

        /**
         * 模板名称
         */
        private String templateName;

        /**
         * 模板描述
         */
        private String templateDesc;

        /**
         * 是否为推荐模板
         */
        private Boolean isRecommended;

        /**
         * 适用的文件类型
         */
        private List<String> fileTypes;

        /**
         * 配置参数
         */
        private Map<String, Object> config;
    }
}
