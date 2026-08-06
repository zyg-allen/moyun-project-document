package com.moyun.ext.ai.dto;

import lombok.Data;

import java.util.List;

/**
 * 知识库创建/更新DTO
 *
 * @author laomao
 */
@Data
public class KnowledgeLibraryDTO {
    
    /**
     * 知识库ID（更新时使用）
     */
    private Long id;
    
    /**
     * 知识库名称
     */
    private String name;
    
    /**
     * 知识库描述
     */
    private String description;
    
    /**
     * 知识库图标
     */
    private String icon;
    
    // ========== 配置参数 ==========
    
    /**
     * 分段模式
     */
    private String segmentMode;
    
    /**
     * 分段最大长度
     */
    private Integer segmentMaxLength;
    
    /**
     * 分段重叠长度
     */
    private Integer segmentOverlapLength;
    
    /**
     * 索引模式
     */
    private String indexMode;
    
    /**
     * 检索模式
     */
    private String retrievalMode;
    
    /**
     * 检索返回数量
     */
    private Integer retrievalTopK;
    
    /**
     * 是否启用Rerank
     */
    private Boolean rerankEnabled;
    
    /**
     * 预处理：替换连续空格
     */
    private Boolean preprocessReplaceSpaces;
    
    /**
     * 预处理：删除URL
     */
    private Boolean preprocessRemoveUrls;
    
    /**
     * 预处理：删除多余换行
     */
    private Boolean preprocessRemoveExtraNewlines;
    
    /**
     * 配置模板ID（使用模板时）
     */
    private Long templateId;
}
