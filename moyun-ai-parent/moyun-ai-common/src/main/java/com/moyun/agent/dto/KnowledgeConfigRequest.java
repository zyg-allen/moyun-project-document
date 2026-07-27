package com.moyun.agent.dto;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 知识库配置请求（参考Dify）
 */
@Data
@Schema(description = "知识库配置请求")
public class KnowledgeConfigRequest {

    @Schema(description = "知识库ID", required = true)
    private Long knowledgeId;

    // ========== 快捷方式：使用模板 ==========
    @Schema(description = "配置模板ID（可选，使用模板快速配置）")
    private Long templateId;

    // ========== 分段设置 ==========
    @Schema(description = "分段模式：general(通用), parent_child(父子分段)", example = "general")
    private String segmentMode;

    @Schema(description = "分段标识符", example = "\\n\\n")
    private String segmentSeparator;

    @Schema(description = "分段最大长度（字符数）", example = "1024")
    private Integer segmentMaxLength;

    @Schema(description = "分段重叠长度（字符数）", example = "50")
    private Integer segmentOverlapLength;

    // ========== 文本预处理规则 ==========
    @Schema(description = "替换连续空格、换行、制表符", example = "true")
    private Boolean preprocessReplaceSpaces;

    @Schema(description = "删除URL和邮箱地址", example = "true")
    private Boolean preprocessRemoveUrls;

    @Schema(description = "删除多余换行", example = "true")
    private Boolean preprocessRemoveExtraNewlines;

    @Schema(description = "删除特殊字符", example = "false")
    private Boolean preprocessRemoveSpecialChars;

    @Schema(description = "删除表格描述", example = "false")
    private Boolean preprocessRemoveTableDesc;

    @Schema(description = "删除页眉页脚", example = "false")
    private Boolean preprocessRemoveHeaderFooter;

    // ========== 索引方式 ==========
    @Schema(description = "索引方式：high_quality(高质量), economy(经济)", example = "high_quality")
    private String indexMode;

    @Schema(description = "嵌入模型名称（可选）")
    private String embeddingModel;

    // ========== 检索设置 ==========
    @Schema(description = "检索模式：vector(向量), keyword(关键词), hybrid(混合)", example = "vector")
    private String retrievalMode;

    @Schema(description = "检索Top K数量", example = "3")
    private Integer retrievalTopK;

    @Schema(description = "是否启用重排序", example = "false")
    private Boolean rerankEnabled;

    @Schema(description = "重排序模型（可选）")
    private String rerankModel;

    // ========== Q&A模式 ==========
    @Schema(description = "是否启用Q&A模式", example = "false")
    private Boolean qaMode;

    @Schema(description = "Q&A提取提示词（可选）")
    private String qaExtractionPrompt;

    // ========== 立即处理 ==========
    @Schema(description = "配置后是否立即开始处理", example = "true")
    private Boolean startProcessing;
}
