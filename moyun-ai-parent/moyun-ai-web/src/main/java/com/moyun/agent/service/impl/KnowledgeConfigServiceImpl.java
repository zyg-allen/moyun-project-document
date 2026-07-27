package com.moyun.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.agent.dto.KnowledgeConfigRequest;
import com.moyun.agent.exception.BusinessException;
import com.moyun.agent.exception.ErrorCode;
import com.moyun.agent.entity.KnowledgeConfig;
import com.moyun.agent.entity.KnowledgeConfigTemplate;
import com.moyun.agent.mapper.KnowledgeConfigMapper;
import com.moyun.agent.mapper.KnowledgeConfigTemplateMapper;
import com.moyun.agent.service.KnowledgeConfigService;
import com.moyun.agent.util.DocumentCleanerUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 知识库配置服务实现
 */
@Slf4j
@Service
public class KnowledgeConfigServiceImpl implements KnowledgeConfigService {

    @Autowired
    private KnowledgeConfigMapper configMapper;

    @Autowired
    private KnowledgeConfigTemplateMapper templateMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<KnowledgeConfigTemplate> getAllTemplates() {
        return templateMapper.selectList(
            new LambdaQueryWrapper<KnowledgeConfigTemplate>()
                .orderByDesc(KnowledgeConfigTemplate::getUseCount)
        );
    }

    @Override
    public List<KnowledgeConfigTemplate> getRecommendedTemplates(String fileType) {
        // 根据文件类型推荐模板
        String recommendedType = getRecommendedTemplateType(fileType);

        return templateMapper.selectList(
            new LambdaQueryWrapper<KnowledgeConfigTemplate>()
                .eq(KnowledgeConfigTemplate::getTemplateType, recommendedType)
                .or()
                .eq(KnowledgeConfigTemplate::getTemplateType, "general")
                .orderByDesc(KnowledgeConfigTemplate::getUseCount)
                .last("LIMIT 3")
        );
    }

    private String getRecommendedTemplateType(String fileType) {
        if (fileType == null) {
            return "general";
        }

        String lowerType = fileType.toLowerCase();
        if (lowerType.matches(".*\\.(java|py|js|cpp|c|h|cs|go|rs|kt)")) {
            return "technical";
        }
        return "general";
    }

    @Override
    public KnowledgeConfigTemplate getTemplateById(Long templateId) {
        return templateMapper.selectById(templateId);
    }

    @Override
    @Transactional
    public KnowledgeConfig applyConfiguration(KnowledgeConfigRequest request) {
        log.info("应用知识库配置，knowledgeId={}", request.getKnowledgeId());

        KnowledgeConfig config;

        // 检查是否已有配置
        KnowledgeConfig existingConfig = getConfigByKnowledgeId(request.getKnowledgeId());

        if (request.getTemplateId() != null) {
            // 使用模板
            log.info("使用模板ID={}", request.getTemplateId());
            KnowledgeConfigTemplate template = getTemplateById(request.getTemplateId());
            if (template == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "模板不存在: " + request.getTemplateId());
            }

            // 从模板JSON创建配置
            config = parseTemplateConfig(template.getConfigJson());
            config.setKnowledgeId(request.getKnowledgeId());

            // 更新模板使用次数
            template.setUseCount(template.getUseCount() + 1);
            templateMapper.updateById(template);

        } else {
            // 自定义配置
            log.info("使用自定义配置");
            config = new KnowledgeConfig();
            BeanUtils.copyProperties(request, config);
        }

        // 保存或更新配置
        if (existingConfig != null) {
            config.setId(existingConfig.getId());
            configMapper.updateById(config);
            log.info("更新配置成功");
        } else {
            configMapper.insert(config);
            log.info("创建配置成功");
        }

        return config;
    }

    private KnowledgeConfig parseTemplateConfig(String configJson) {
        try {
            return objectMapper.readValue(configJson, KnowledgeConfig.class);
        } catch (Exception e) {
            log.error("解析模板配置失败", e);
            return createDefaultConfigObject();
        }
    }

    @Override
    public KnowledgeConfig getConfigByKnowledgeId(Long knowledgeId) {
        return configMapper.selectOne(
            new LambdaQueryWrapper<KnowledgeConfig>()
                .eq(KnowledgeConfig::getKnowledgeId, knowledgeId)
        );
    }

    @Override
    @Transactional
    public KnowledgeConfig createDefaultConfig(Long knowledgeId) {
        log.info("创建默认配置，knowledgeId={}", knowledgeId);

        KnowledgeConfig config = createDefaultConfigObject();
        config.setKnowledgeId(knowledgeId);

        configMapper.insert(config);
        return config;
    }

    private KnowledgeConfig createDefaultConfigObject() {
        KnowledgeConfig config = new KnowledgeConfig();
        config.setSegmentMode("general");
        config.setSegmentSeparator("\n\n");
        config.setSegmentMaxLength(1024);
        config.setSegmentOverlapLength(50);
        config.setPreprocessReplaceSpaces(true);
        config.setPreprocessRemoveUrls(true);
        config.setPreprocessRemoveExtraNewlines(true);
        config.setIndexMode("high_quality");
        config.setRetrievalMode("vector");
        config.setRetrievalTopK(3);
        config.setRerankEnabled(false);
        return config;
    }

    @Override
    @Transactional
    public KnowledgeConfig updateConfig(KnowledgeConfigRequest request) {
        return applyConfiguration(request);
    }

    @Override
    public String preprocessText(String text, KnowledgeConfig config) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String processed = text;

        // 1. 替换连续空格、换行、制表符
        if (Boolean.TRUE.equals(config.getPreprocessReplaceSpaces())) {
            processed = processed.replaceAll("[ \\t]+", " ");
        }

        // 2. 删除URL和邮箱
        if (Boolean.TRUE.equals(config.getPreprocessRemoveUrls())) {
            // 删除URL
            processed = processed.replaceAll(
                "https?://[\\w\\-]+(\\.[\\w\\-]+)+([\\w\\-.,@?^=%&:/~+#]*[\\w\\-@?^=%&/~+#])?",
                ""
            );
            // 删除邮箱
            processed = processed.replaceAll(
                "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}",
                ""
            );
        }

        // 3. 删除多余换行
        if (Boolean.TRUE.equals(config.getPreprocessRemoveExtraNewlines())) {
            processed = processed.replaceAll("\\n{3,}", "\n\n");
        }

        // 4. 增强文档清洗（使用DocumentCleanerUtil）
        processed = DocumentCleanerUtil.cleanText(
            processed,
            Boolean.TRUE.equals(config.getPreprocessRemoveSpecialChars()),
            Boolean.TRUE.equals(config.getPreprocessRemoveTableDesc()),
            Boolean.TRUE.equals(config.getPreprocessRemoveHeaderFooter())
        );

        log.debug("文本预处理完成，原长度={}, 处理后长度={}", text.length(), processed.length());
        return processed.trim();
    }
}
