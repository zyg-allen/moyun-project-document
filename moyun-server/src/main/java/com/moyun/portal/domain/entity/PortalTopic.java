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
 * 话题主表
 *
 * @author moyun
 */
@Data
@TableName("portal_topic")
public class PortalTopic extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 话题标题 */
    private String title;

    /** 话题描述/导语 */
    private String description;

    /** 封面图 URL */
    private String cover;

    /** 发起人 portal_user.id（必须是认证创作者） */
    private Long creatorId;

    /**
     * 状态：pending 待审核/active 活跃/archived 归档/deleted 删除/rejected 审核驳回
     * 新话题默认 pending，审核通过后 active
     */
    private String status;

    /** 审核人ID（系统用户ID，审核时写入） */
    private Long auditorId;

    /** 审核意见/驳回原因 */
    private String auditRemark;

    /** 审核时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditTime;

    /** 是否置顶：0 否/1 是 */
    private Integer pinned;

    /** 浏览数 */
    private Integer viewCount;

    /** 观点数 */
    private Integer postCount;

    /** 话题被赞数 */
    private Integer likeCount;

    /** 是否精选：0 否/1 是 */
    private Integer isFeatured;

    /** 评论数（一级评论） */
    private Integer commentCount;

    /** 最后观点时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastPostTime;

    /** 最后观点用户 */
    private Long lastPosterId;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedTime;

    // BaseEntity 公共字段对应列在 portal_topic 表中不存在，排除 MyBatis-Plus 映射
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
