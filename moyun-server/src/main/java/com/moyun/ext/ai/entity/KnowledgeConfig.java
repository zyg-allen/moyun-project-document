package com.moyun.ext.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 知识库配置实体
 *
 * <p>对应数据库表 knowledge_config，存储知识库的处理配置（分片策略、预处理规则等）</p>
 *
 * @author laomao
 */
@Data
@TableName("knowledge_config")
public class KnowledgeConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long knowledgeId;

    // ========== 分段设置（必填参数，有默认值）==========
    /**
     * 分段模式：general(通用), parent_child(父子分段)
     * 默认：general
     */
    private String segmentMode;

    /**
     * 分段标识符（如 \n\n）
     */
    private String segmentSeparator;

    /**
     * 分段最大长度（字符数）
     * 默认：400，推荐400-800
     */
    private Integer segmentMaxLength;

    /**
     * 分段重叠长度（字符数）
     * 默认：50，推荐50-100
     */
    private Integer segmentOverlapLength;

    // ========== 分片策略配置（新增）==========
    /**
     * 分片策略：fixed(固定大小), adaptive(自适应), document_type(按文档类型)
     * 默认：fixed
     */
    private String chunkingStrategy;

    /**
     * 文档类型：general(通用), faq(问答), table(表格), code(代码), technical(技术文档)
     * 默认：general
     */
    private String documentType;

    /**
     * FAQ分片大小（字符数）
     * 默认：400，推荐300-500
     */
    private Integer faqChunkSize;

    /**
     * 表格分片策略：by_row(按行), by_table(整表), by_cell(按单元格)
     * 默认：by_row
     */
    private String tableChunkStrategy;

    /**
     * 代码分片策略：by_function(按函数), by_class(按类), by_file(按文件)
     * 默认：by_function
     */
    private String codeChunkStrategy;

    /**
     * 技术文档分片大小（字符数）
     * 默认：1200，推荐1200-1500
     */
    private Integer technicalChunkSize;

    /**
     * 启用智能边界检测（避免切断句子）
     * 默认：true
     */
    private Boolean enableSmartBoundary;

    // ========== 文本预处理规则 ==========
    /**
     * 替换连续空格、换行、制表符
     */
    private Boolean preprocessReplaceSpaces;

    /**
     * 删除URL和邮箱地址
     */
    private Boolean preprocessRemoveUrls;

    /**
     * 删除多余换行
     */
    private Boolean preprocessRemoveExtraNewlines;

    // ========== 索引方式（必填参数，有默认值）==========
    /**
     * 索引方式：high_quality(高质量), economy(经济)
     * 默认：high_quality
     */
    private String indexMode;

    /**
     * 嵌入模型名称
     */
    private String embeddingModel;

    // ========== 检索设置（必填参数，有默认值）==========
    /**
     * 检索模式：vector(向量), keyword(关键词), hybrid(混合)
     * 默认：vector
     */
    private String retrievalMode;

    /**
     * 检索Top K数量
     * 默认：10，推荐5-10
     */
    private Integer retrievalTopK;

    /**
     * 是否启用重排序
     * 默认：false
     */
    private Boolean rerankEnabled;

    /**
     * 重排序模型
     */
    private String rerankModel;

    // ========== Q&A模式 ==========
    /**
     * 是否启用Q&A模式
     * 默认：false
     */
    private Boolean qaMode;
    
    /**
     * Q&A提取提示词
     */
    private String qaExtractionPrompt;

    // ========== 文档清洗增强 ==========
    /**
     * 删除特殊字符
     */
    private Boolean preprocessRemoveSpecialChars;
    
    /**
     * 删除表格描述
     */
    private Boolean preprocessRemoveTableDesc;
    
    /**
     * 删除页眉页脚
     */
    private Boolean preprocessRemoveHeaderFooter;

    // ========== 元数据 ==========
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
