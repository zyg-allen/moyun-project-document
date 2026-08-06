package com.moyun.ext.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识库配置实体类
 * 
 * <p>存储知识库级别的配置，包括分段、索引、检索等参数</p>
 *
 * @author laomao
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("knowledge_library_config")
public class KnowledgeLibraryConfig {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 知识库ID
     */
    private Long libraryId;
    
    // ========== 分段配置 ==========
    
    /**
     * 分段模式：general(通用), qa(问答), code(代码)
     */
    private String segmentMode;
    
    /**
     * 分段标识符
     */
    private String segmentSeparator;
    
    /**
     * 分段最大长度（字符数）
     */
    private Integer segmentMaxLength;
    
    /**
     * 分段重叠长度（字符数）
     */
    private Integer segmentOverlapLength;
    
    // ========== 预处理配置 ==========
    
    /**
     * 替换连续空格
     */
    private Boolean preprocessReplaceSpaces;
    
    /**
     * 删除URL
     */
    private Boolean preprocessRemoveUrls;
    
    /**
     * 删除多余换行
     */
    private Boolean preprocessRemoveExtraNewlines;
    
    // ========== 索引配置 ==========
    
    /**
     * 索引模式：high_quality(高质量), economy(经济)
     */
    private String indexMode;
    
    /**
     * Embedding模型
     */
    private String embeddingModel;
    
    // ========== 检索配置 ==========
    
    /**
     * 检索模式：vector(向量), keyword(关键词), hybrid(混合)
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
     * Rerank模型
     */
    private String rerankModel;
    
    // ========== 时间戳 ==========
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
