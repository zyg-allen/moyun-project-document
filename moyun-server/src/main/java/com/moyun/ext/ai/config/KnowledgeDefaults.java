package com.moyun.ext.ai.config;

import com.moyun.ext.ai.entity.KnowledgeConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 知识库默认参数配置（P2-2 阶段 3）
 *
 * <p>统一承载知识库分片 / 预处理 / 索引 / 检索 / 文档级类型化策略的默认参数，
 * 消除散落在以下位置的硬编码默认值分歧：
 * <ul>
 *   <li>{@code KnowledgeConfigServiceImpl.createDefaultConfigObject} — resolveEffectiveConfig 兜底基线（原 1024/50/vector/3）</li>
 *   <li>{@code KnowledgeBaseServiceImpl.getKnowledgeConfig} — legacy Excel/PPT 路径默认值（原 800/150/fixed）</li>
 *   <li>{@code KnowledgeLibraryServiceImpl.createLibrary / applyTemplateConfig} — 新建库级默认配置（原 800/100/hybrid/10）</li>
 * </ul>
 *
 * <p>统一后取多数派值（库级默认路径最常用），baseline 与新建库级行为对齐：
 * segmentMaxLength=800 / segmentOverlapLength=100 / retrievalMode=hybrid / retrievalTopK=10。
 *
 * <p>层级合并优先级（由低到高）：本配置（兜底基线）← 库级默认 ← 文档级覆盖
 *
 * <p>YAML 配置示例：
 * <pre>
 * moyun-ai:
 *   knowledge-defaults:
 *     segment-mode: general
 *     segment-max-length: 800
 *     segment-overlap-length: 100
 *     retrieval-mode: hybrid
 *     retrieval-top-k: 10
 * </pre>
 *
 * @author laomao
 */
@Data
@Component
@ConfigurationProperties(prefix = "moyun-ai.knowledge-defaults")
public class KnowledgeDefaults {

    // ===== 分片配置 =====

    /** 分片模式：general(通用) / parent_child(父子分段) */
    private String segmentMode = "general";

    /** 分片分隔符 */
    private String segmentSeparator = "\n\n";

    /** 分片最大长度（字符数），推荐 400-800 */
    private Integer segmentMaxLength = 800;

    /** 分片重叠长度（字符数），推荐 50-100 */
    private Integer segmentOverlapLength = 100;

    // ===== 文档级类型化策略（仅文档级有效，库级无此字段）=====

    /** 分片策略：fixed(固定大小) / adaptive(自适应) / document_type(按文档类型) */
    private String chunkingStrategy = "fixed";

    /** 文档类型：general / faq / table / code / technical */
    private String documentType = "general";

    /** FAQ 分片大小（字符数） */
    private Integer faqChunkSize = 400;

    /** 技术文档分片大小（字符数） */
    private Integer technicalChunkSize = 1200;

    /** 是否启用智能边界检测（避免切断句子） */
    private Boolean enableSmartBoundary = true;

    // ===== 预处理配置 =====

    /** 替换连续空格、换行、制表符 */
    private Boolean preprocessReplaceSpaces = true;

    /** 删除 URL 和邮箱 */
    private Boolean preprocessRemoveUrls = true;

    /** 删除多余换行 */
    private Boolean preprocessRemoveExtraNewlines = true;

    // ===== 索引配置 =====

    /** 索引模式：high_quality(高质量) / economy(经济) */
    private String indexMode = "high_quality";

    // ===== 检索配置（注：检索侧实际读 agent 表 + RagConfig，本字段保留以兼容前端表单）=====

    /** 检索模式：vector(向量) / keyword(关键词) / hybrid(混合) */
    private String retrievalMode = "hybrid";

    /** 检索 Top K 数量 */
    private Integer retrievalTopK = 10;

    /** 是否启用重排序 */
    private Boolean rerankEnabled = false;

    /**
     * 转换为 {@link KnowledgeConfig} 实例，作为 resolveEffectiveConfig 的兜底基线。
     *
     * <p>仅复制本配置承载的默认字段；embeddingModel / rerankModel / qaMode /
     * preprocessRemoveSpecialChars 等非默认字段保持 null，由上层（库级默认或文档级覆盖）填充。
     *
     * @return 新建的 KnowledgeConfig 实例，所有默认字段已填充
     */
    public KnowledgeConfig toKnowledgeConfig() {
        KnowledgeConfig config = new KnowledgeConfig();
        config.setSegmentMode(segmentMode);
        config.setSegmentSeparator(segmentSeparator);
        config.setSegmentMaxLength(segmentMaxLength);
        config.setSegmentOverlapLength(segmentOverlapLength);
        config.setChunkingStrategy(chunkingStrategy);
        config.setDocumentType(documentType);
        config.setFaqChunkSize(faqChunkSize);
        config.setTechnicalChunkSize(technicalChunkSize);
        config.setEnableSmartBoundary(enableSmartBoundary);
        config.setPreprocessReplaceSpaces(preprocessReplaceSpaces);
        config.setPreprocessRemoveUrls(preprocessRemoveUrls);
        config.setPreprocessRemoveExtraNewlines(preprocessRemoveExtraNewlines);
        config.setIndexMode(indexMode);
        config.setRetrievalMode(retrievalMode);
        config.setRetrievalTopK(retrievalTopK);
        config.setRerankEnabled(rerankEnabled);
        return config;
    }
}
