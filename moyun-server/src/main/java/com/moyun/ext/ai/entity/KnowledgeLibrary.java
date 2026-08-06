package com.moyun.ext.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识库主表实体类
 * 
 * <p>一个知识库可以包含多个文档，支持按主题组织和管理知识</p>
 *
 * @author laomao
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("knowledge_library")
public class KnowledgeLibrary {
    
    @TableId(type = IdType.AUTO)
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
     * 知识库图标（emoji或图标类名）
     */
    private String icon;
    
    // ========== 统计信息 ==========
    
    /**
     * 文档数量
     */
    private Integer documentCount;
    
    /**
     * 总分段数
     */
    private Integer totalSegments;
    
    /**
     * 总文件大小（字节）
     */
    private Long totalSize;
    
    /**
     * 使用次数（被检索次数）
     */
    private Integer usageCount;
    
    /**
     * 命中次数
     */
    private Integer hitCount;
    
    /**
     * 最后使用时间
     */
    private LocalDateTime lastUsedTime;
    
    // ========== 状态 ==========
    
    /**
     * 状态：active(正常), disabled(禁用), archived(归档)
     */
    private String status;
    
    /**
     * 是否公开（预留多租户）
     */
    private Boolean isPublic;
    
    // ========== 时间戳 ==========
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
