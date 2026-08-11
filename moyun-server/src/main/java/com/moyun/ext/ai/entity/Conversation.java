package com.moyun.ext.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 对话会话实体
 *
 * <p>对应数据库表 conversation，存储用户与智能体的对话会话</p>
 *
 * <p>继承 {@link AiBaseEntity}，复用 createTime / updateTime / deleted 字段（P3-2 Phase 1）。
 * 原有的 {@code @TableField(fill=...)} 自动填充注解由 AiBaseEntity 统一提供。</p>
 *
 * @author laomao
 */
@Data
@TableName("ai_conversation")
public class Conversation extends AiBaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 智能体ID
     */
    private Long agentId;

    /**
     * 会话标题
     */
    private String title;

    /**
     * 用户ID（预留字段）
     */
    private String userId;

    /**
     * 消息数量
     */
    private Integer messageCount;
}
