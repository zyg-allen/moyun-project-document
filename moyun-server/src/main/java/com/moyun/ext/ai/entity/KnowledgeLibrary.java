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
 * <p>继承 {@link AiBaseEntity}，复用 createTime / updateTime / deleted 字段（P3-2 Phase 2）。
 * 原有的 {@code created_at / updated_at / deleted} 列经 117 脚本重命名为
 * {@code create_time / update_time}，deleted 列保持不变。
 * 时间字段的 {@code @TableField(fill=...)} 自动填充由 AiBaseEntity 统一提供，
 * strictInsertFill 仅填 null，不覆盖 Service 层显式赋值（P0-1 兜底兼容）。</p>
 *
 * @author laomao
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("ai_knowledge_library")
public class KnowledgeLibrary extends AiBaseEntity {

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

    // ========== 时间戳 / 软删除 ==========

    // createTime / updateTime / deleted 字段继承自 AiBaseEntity（P3-2 Phase 2），
    // 对应列：create_time（原 created_at，117 脚本重命名）/
    //         update_time（原 updated_at，117 脚本重命名）/ deleted
}
