package com.moyun.ext.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工作流定义实体
 *
 * <p>继承 {@link AiBaseEntity}，复用 createTime / updateTime / deleted 字段（P3-2 Phase 1）。
 * 注意：{@code @Builder} 仅覆盖本类字段，不含继承的 createTime / updateTime / deleted；
 * 构建后如需设置时间，请用 setter（AiBaseEntity 提供 {@code @TableField(fill=...)} 自动填充兜底）。</p>
 *
 * @author laomao
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_workflow")
public class Workflow extends AiBaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工作流名称 */
    private String name;

    /** 工作流描述 */
    private String description;

    /** 工作流图定义(JSON格式，包含nodes和edges) */
    @TableField("graph_data")
    private String graphData;

    /** 全局变量定义(JSON格式) */
    private String variables;

    /** 状态: draft-草稿, published-已发布, disabled-已禁用 */
    private String status;

    /** 版本号 */
    private Integer version;

    /** 是否启用 */
    private Boolean enabled;
}
