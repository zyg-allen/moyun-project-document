package com.moyun.ext.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.ext.ai.config.KnowledgeDefaults;
import com.moyun.ext.ai.dto.KnowledgeConfigRequest;
import com.moyun.ext.ai.exception.BusinessException;
import com.moyun.ext.ai.exception.ErrorCode;
import com.moyun.ext.ai.entity.KnowledgeConfig;
import com.moyun.ext.ai.entity.KnowledgeConfigTemplate;
import com.moyun.ext.ai.entity.KnowledgeLibraryConfig;
import com.moyun.ext.ai.mapper.KnowledgeConfigMapper;
import com.moyun.ext.ai.mapper.KnowledgeConfigTemplateMapper;
import com.moyun.ext.ai.service.KnowledgeConfigService;
import com.moyun.ext.ai.util.DocumentCleanerUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    /**
     * 知识库默认参数（P2-2 阶段 3）：从硬编码迁到 {@link KnowledgeDefaults} 配置类，
     * 与 {@code KnowledgeBaseServiceImpl.getKnowledgeConfig} 和
     * {@code KnowledgeLibraryServiceImpl.createLibrary} 共用同一套默认值，消除分歧。
     */
    @Autowired
    private KnowledgeDefaults knowledgeDefaults;

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

        // 兜底设置时间戳（strictInsertFill 对 Jackson 反序列化对象可能不生效）
        LocalDateTime now = LocalDateTime.now();
        if (config.getCreatedAt() == null) {
            config.setCreatedAt(now);
        }
        if (config.getUpdatedAt() == null) {
            config.setUpdatedAt(now);
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

        // 兜底设置时间戳
        LocalDateTime now = LocalDateTime.now();
        config.setCreatedAt(now);
        config.setUpdatedAt(now);

        configMapper.insert(config);
        return config;
    }

    /**
     * 构建默认配置对象（P2-2 阶段 3 重构）
     *
     * <p>原硬编码默认值已迁至 {@link KnowledgeDefaults} 配置类，本方法仅作转发，
     * 保证 {@code resolveEffectiveConfig} / {@code createDefaultConfig} / {@code parseTemplateConfig}
     * 三处调用点共用同一套默认值，与 {@code KnowledgeBaseServiceImpl.getKnowledgeConfig} 和
     * {@code KnowledgeLibraryServiceImpl.createLibrary} 完全一致。
     *
     * <p>统一后默认值：segmentMaxLength=800 / segmentOverlapLength=100 /
     * retrievalMode=hybrid / retrievalTopK=10（与新建库级默认对齐）
     */
    private KnowledgeConfig createDefaultConfigObject() {
        return knowledgeDefaults.toKnowledgeConfig();
    }

    @Override
    @Transactional
    public KnowledgeConfig updateConfig(KnowledgeConfigRequest request) {
        return applyConfiguration(request);
    }

    @Override
    public KnowledgeConfig resolveEffectiveConfig(Long knowledgeId, KnowledgeLibraryConfig libraryConfig) {
        log.info("解析有效配置: knowledgeId={}, hasLibraryConfig={}", knowledgeId, libraryConfig != null);

        // 1. 优先读取文档级配置（实例级覆盖）
        KnowledgeConfig docConfig = getConfigByKnowledgeId(knowledgeId);

        // 2. 以"硬编码默认"为兜底基线，保证返回值永不为 null
        KnowledgeConfig effective = createDefaultConfigObject();
        effective.setKnowledgeId(knowledgeId);

        // 3. 库级默认合并到 effective（低优先级：仅当文档级未设时才生效）
        //    合并顺序：硬编码默认 ← 库级默认 ← 文档级覆盖
        if (libraryConfig != null) {
            mergeLibraryConfig(effective, libraryConfig);
        }

        // 4. 文档级覆盖合并到 effective（最高优先级）
        if (docConfig != null) {
            mergeDocConfig(effective, docConfig);
        }

        // 5. 时间戳兜底
        LocalDateTime now = LocalDateTime.now();
        if (effective.getCreatedAt() == null) {
            effective.setCreatedAt(now);
        }
        if (effective.getUpdatedAt() == null) {
            effective.setUpdatedAt(now);
        }

        return effective;
    }

    /**
     * 将库级配置合并到 effective（仅填充 effective 中仍为 null 或默认值的字段）。
     * 库级配置的 13 个冗余字段全部复制（含旧版有损转换遗漏的 embeddingModel / rerankModel）。
     */
    private void mergeLibraryConfig(KnowledgeConfig effective, KnowledgeLibraryConfig lib) {
        // 分段配置
        if (lib.getSegmentMode() != null) {
            effective.setSegmentMode(lib.getSegmentMode());
        }
        if (lib.getSegmentSeparator() != null) {
            effective.setSegmentSeparator(lib.getSegmentSeparator());
        }
        if (lib.getSegmentMaxLength() != null) {
            effective.setSegmentMaxLength(lib.getSegmentMaxLength());
        }
        if (lib.getSegmentOverlapLength() != null) {
            effective.setSegmentOverlapLength(lib.getSegmentOverlapLength());
        }
        // 预处理配置
        if (lib.getPreprocessReplaceSpaces() != null) {
            effective.setPreprocessReplaceSpaces(lib.getPreprocessReplaceSpaces());
        }
        if (lib.getPreprocessRemoveUrls() != null) {
            effective.setPreprocessRemoveUrls(lib.getPreprocessRemoveUrls());
        }
        if (lib.getPreprocessRemoveExtraNewlines() != null) {
            effective.setPreprocessRemoveExtraNewlines(lib.getPreprocessRemoveExtraNewlines());
        }
        // 索引配置
        if (lib.getIndexMode() != null) {
            effective.setIndexMode(lib.getIndexMode());
        }
        if (lib.getEmbeddingModel() != null) {
            effective.setEmbeddingModel(lib.getEmbeddingModel());
        }
        // 检索配置（注：当前为死字段，检索侧实际读 agent 表 + RagConfig，保留以兼容前端表单）
        if (lib.getRetrievalMode() != null) {
            effective.setRetrievalMode(lib.getRetrievalMode());
        }
        if (lib.getRetrievalTopK() != null) {
            effective.setRetrievalTopK(lib.getRetrievalTopK());
        }
        if (lib.getRerankEnabled() != null) {
            effective.setRerankEnabled(lib.getRerankEnabled());
        }
        if (lib.getRerankModel() != null) {
            effective.setRerankModel(lib.getRerankModel());
        }
    }

    /**
     * 将文档级配置合并到 effective（文档级优先级最高，非 null 字段全部覆盖）。
     * 包含文档级独有字段（chunkingStrategy / documentType / qaMode 等），这些字段库级没有。
     */
    private void mergeDocConfig(KnowledgeConfig effective, KnowledgeConfig doc) {
        // 分段配置
        if (doc.getSegmentMode() != null) {
            effective.setSegmentMode(doc.getSegmentMode());
        }
        if (doc.getSegmentSeparator() != null) {
            effective.setSegmentSeparator(doc.getSegmentSeparator());
        }
        if (doc.getSegmentMaxLength() != null) {
            effective.setSegmentMaxLength(doc.getSegmentMaxLength());
        }
        if (doc.getSegmentOverlapLength() != null) {
            effective.setSegmentOverlapLength(doc.getSegmentOverlapLength());
        }
        // 分片策略（文档级独有）
        if (doc.getChunkingStrategy() != null) {
            effective.setChunkingStrategy(doc.getChunkingStrategy());
        }
        if (doc.getDocumentType() != null) {
            effective.setDocumentType(doc.getDocumentType());
        }
        if (doc.getFaqChunkSize() != null) {
            effective.setFaqChunkSize(doc.getFaqChunkSize());
        }
        if (doc.getTableChunkStrategy() != null) {
            effective.setTableChunkStrategy(doc.getTableChunkStrategy());
        }
        if (doc.getCodeChunkStrategy() != null) {
            effective.setCodeChunkStrategy(doc.getCodeChunkStrategy());
        }
        if (doc.getTechnicalChunkSize() != null) {
            effective.setTechnicalChunkSize(doc.getTechnicalChunkSize());
        }
        if (doc.getEnableSmartBoundary() != null) {
            effective.setEnableSmartBoundary(doc.getEnableSmartBoundary());
        }
        // 预处理配置
        if (doc.getPreprocessReplaceSpaces() != null) {
            effective.setPreprocessReplaceSpaces(doc.getPreprocessReplaceSpaces());
        }
        if (doc.getPreprocessRemoveUrls() != null) {
            effective.setPreprocessRemoveUrls(doc.getPreprocessRemoveUrls());
        }
        if (doc.getPreprocessRemoveExtraNewlines() != null) {
            effective.setPreprocessRemoveExtraNewlines(doc.getPreprocessRemoveExtraNewlines());
        }
        if (doc.getPreprocessRemoveSpecialChars() != null) {
            effective.setPreprocessRemoveSpecialChars(doc.getPreprocessRemoveSpecialChars());
        }
        if (doc.getPreprocessRemoveTableDesc() != null) {
            effective.setPreprocessRemoveTableDesc(doc.getPreprocessRemoveTableDesc());
        }
        if (doc.getPreprocessRemoveHeaderFooter() != null) {
            effective.setPreprocessRemoveHeaderFooter(doc.getPreprocessRemoveHeaderFooter());
        }
        // 索引配置
        if (doc.getIndexMode() != null) {
            effective.setIndexMode(doc.getIndexMode());
        }
        if (doc.getEmbeddingModel() != null) {
            effective.setEmbeddingModel(doc.getEmbeddingModel());
        }
        // 检索配置
        if (doc.getRetrievalMode() != null) {
            effective.setRetrievalMode(doc.getRetrievalMode());
        }
        if (doc.getRetrievalTopK() != null) {
            effective.setRetrievalTopK(doc.getRetrievalTopK());
        }
        if (doc.getRerankEnabled() != null) {
            effective.setRerankEnabled(doc.getRerankEnabled());
        }
        if (doc.getRerankModel() != null) {
            effective.setRerankModel(doc.getRerankModel());
        }
        // Q&A 模式
        if (doc.getQaMode() != null) {
            effective.setQaMode(doc.getQaMode());
        }
        if (doc.getQaExtractionPrompt() != null) {
            effective.setQaExtractionPrompt(doc.getQaExtractionPrompt());
        }
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
