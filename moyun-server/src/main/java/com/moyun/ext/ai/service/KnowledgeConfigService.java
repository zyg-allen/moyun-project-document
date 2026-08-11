package com.moyun.ext.ai.service;

import com.moyun.ext.ai.dto.KnowledgeConfigRequest;
import com.moyun.ext.ai.entity.KnowledgeConfig;
import com.moyun.ext.ai.entity.KnowledgeConfigTemplate;
import com.moyun.ext.ai.entity.KnowledgeLibraryConfig;

import java.util.List;

/**
 * 知识库配置服务接口
 *
 * <p>管理知识库处理配置，包括分片策略、预处理规则、配置模板等</p>
 *
 * <p><b>双轨配置说明（P2-2）：</b>
 * 项目存在两套配置表：<code>knowledge_library_config</code>（库级默认）和 <code>knowledge_config</code>（文档级实例）。
 * 文档级未设字段应继承库级默认，通过 {@link #resolveEffectiveConfig} 实现合并。
 * 检索参数（retrievalMode/topK/rerank*）目前是死字段（检索侧实际读 agent 表 + 全局 RagConfig），后续会清理。
 * </p>
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

    /**
     * 解析文档的"有效配置"（双轨合并）。
     *
     * <p>双轨合并规则：
     * <ol>
     *   <li>读取文档级配置 {@code knowledge_config}（通过 knowledgeId）；</li>
     *   <li>读取库级配置 {@code knowledge_library_config}（通过 libraryConfig 参数）；</li>
     *   <li>文档级非 null 字段优先，null 字段回退库级默认；</li>
     *   <li>库级也未设的字段，回退到 {@link KnowledgeConfigServiceImpl#createDefaultConfigObject} 的硬编码默认；</li>
     *   <li>返回合并后的 KnowledgeConfig，保证调用方拿到的配置是完整可用的。</li>
     * </ol>
     *
     * <p>注意：本方法不持久化合并结果，调用方需要时自行 insert/update。
     *
     * @param knowledgeId   文档ID（必填，写入返回对象的 knowledgeId 字段）
     * @param libraryConfig 库级默认配置（可为 null，表示无库级默认，全部回退到硬编码默认）
     * @return 合并后的有效配置（永不为 null）
     */
    KnowledgeConfig resolveEffectiveConfig(Long knowledgeId, KnowledgeLibraryConfig libraryConfig);
}
