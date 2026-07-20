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
 * 话题观点（楼层）
 *
 * @author moyun
 */
@Data
@TableName("portal_topic_post")
public class PortalTopicPost extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属话题 */
    private Long topicId;

    /** 发布者 portal_user.id */
    private Long userId;

    /** 观点内容（Markdown） */
    private String content;

    /** 图片 URL 列表，最多 9 张（JSON 字符串，由 Service 层序列化/反序列化） */
    private String images;

    /** 父观点 ID（楼中楼，NULL 为一级观点） */
    private Long parentPostId;

    /** 回复的用户 ID */
    private Long replyToUserId;

    /** 楼层号 */
    private Integer floor;

    /** 点赞数 */
    private Integer likeCount;

    /** 评论数 */
    private Integer commentCount;

    /** 软删：0 否/1 是 */
    private Integer isDeleted;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedTime;

    // BaseEntity 公共字段对应列在 portal_topic_post 表中不存在，排除 MyBatis-Plus 映射
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
