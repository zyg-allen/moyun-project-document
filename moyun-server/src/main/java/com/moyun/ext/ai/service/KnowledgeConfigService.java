package com.moyun.ext.ai.service;

import com.moyun.ext.ai.dto.KnowledgeConfigRequest;
import com.moyun.ext.ai.entity.KnowledgeConfig;
import com.moyun.ext.ai.entity.KnowledgeConfigTemplate;

import java.util.List;

/**
 * 知识库配置服务接口
 *
 * <p>管理知识库处理配置，包括分片策略、预处理规则、配置模板等</p>
 *
 * @author laomao
 */
public interface KnowledgeConfigService {

    /**
     * 获取所有配置模板
     */
    List<KnowledgeConfigTemplate> getAllTemplates();

    /**
     * 根据文件类型推荐模板
     */
    List<KnowledgeConfigTemplate> getRecommendedTemplates(String fileType);

    /**
     * 根据模板ID获取模板
     */
    KnowledgeConfigTemplate getTemplateById(Long templateId);

    /**
     * 为知识库应用配置（使用模板或自定义）
     */
    KnowledgeConfig applyConfiguration(KnowledgeConfigRequest request);

    /**
     * 获取知识库的配置
     */
    KnowledgeConfig getConfigByKnowledgeId(Long knowledgeId);

    /**
     * 创建默认配置
     */
    KnowledgeConfig createDefaultConfig(Long knowledgeId);

    /**
     * 更新配置
     */
    KnowledgeConfig updateConfig(KnowledgeConfigRequest request);

    /**
     * 根据配置处理文本（应用预处理规则）
     */
    String preprocessText(String text, KnowledgeConfig config);
}
