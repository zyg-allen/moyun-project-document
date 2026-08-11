package com.moyun.ext.ai.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 模块实体基类
 *
 * <p>统一 AI 核心实体的时间字段与软删字段命名，消除 7 个核心实体（Agent / ModelConfig /
 * Workflow / DomainDictionary / AgentTool / Conversation / DataSourceConfig）的字段重复声明。
 *
 * <p><b>字段映射</b>：
 * <ul>
 *   <li>{@code createTime} (LocalDateTime) → {@code create_time} 列</li>
 *   <li>{@code updateTime} (LocalDateTime) → {@code update_time} 列</li>
 *   <li>{@code deleted} (Boolean, {@code @TableLogic}) → {@code deleted} 列（0=存在, 1=删除）</li>
 * </ul>
 *
 * <p><b>自动填充</b>：createTime / updateTime 带 {@code @TableField(fill=...)}，
 * 由 {@link com.moyun.core.config.MyMetaObjectHandler} 在 INSERT / UPDATE 时自动填充。
 * {@code strictInsertFill} 仅在字段为 null 时填充，不会覆盖 Service 层显式设置的值（P0-1 兜底赋值）。
 *
 * <p><b>不继承 BaseEntity 的原因</b>：AI 表无 {@code create_by / update_by / remark / del_flag} 列，
 * 继承 BaseEntity 会导致 MyBatis-Plus 尝试映射不存在的列。AI 模块统一使用 {@code deleted} (Boolean)
 * 管理软删除，通过 {@code @TableLogic} 显式声明覆盖全局 {@code logic-delete-field=delFlag} 配置。
 *
 * <p><b>P3-2 Phase 1</b>：建立 AiBaseEntity 架构基线，7 个匹配实体已迁移继承。
 * Phase 2（待后续窗口）：迁移 KnowledgeLibrary（createdAt/updatedAt → createTime/updateTime）
 * 与 KnowledgeBase（uploadTime/processTime → createTime/updateTime），需配合 SQL 列重命名。
 * Phase 3（待后续窗口）：Portal 话题模块 isDeleted → delFlag 迁移，需配合 SQL + Mapper + Service 全链路改造。
 *
 * @author moyun
 */
@Data
public class AiBaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 创建时间（INSERT 时自动填充，不覆盖已显式赋值的值） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间（INSERT / UPDATE 时自动填充，不覆盖已显式赋值的值） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 软删除标记：0=未删除，1=已删除
     * <p>MyBatis-Plus 自动处理：
     * <ul>
     *   <li>SELECT 自动追加 WHERE deleted = 0</li>
     *   <li>deleteById / removeById 自动转为 UPDATE SET deleted = 1</li>
     * </ul>
     * 通过 @TableLogic 显式声明，覆盖全局 logic-delete-field=delFlag 配置。
     */
    @TableLogic
    private Boolean deleted;
}
