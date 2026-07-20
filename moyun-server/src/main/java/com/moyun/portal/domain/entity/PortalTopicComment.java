package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.moyun.core.base.BaseEntity;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 话题评论（多态：可评论话题或观点）
 *
 * @author moyun
 */
@Data
@TableName("portal_topic_comment")
public class PortalTopicComment extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 目标类型：topic 话题评论 / post 观点评论 */
    private String targetType;

    /** 目标 ID */
    private Long targetId;

    /** 评论者 portal_user.id */
    private Long authorId;

    /** 评论内容 */
    private String content;

    /** 父评论 ID（0=一级评论） */
    private Long parentId;

    /** 根评论 ID（一级评论 root_id=0） */
    private Long rootId;

    /** 被回复的用户 ID */
    private Long replyTo;

    /** 被回复内容摘要 */
    private String replyToContent;

    /** 点赞数 */
    private Integer likeCount;

    /** 回复数（仅一级评论维护） */
    private Integer replyCount;

    /** 软删：0 否/1 是 */
    private Integer isDeleted;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedTime;

    // BaseEntity 公共字段对应列在 portal_topic_comment 表中不存在，排除 MyBatis-Plus 映射
    @TableField(exist = false)
    private String createBy;
    @TableField(exist = false)
    private LocalDateTime createTime;
    @TableField(exist = false)
    private String updateBy;
    @TableField(exist = false)
    private LocalDateTime updateTime;
    @TableField(exist = false)
    private String remark;
}
