package com.moyun.portal.domain.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import com.moyun.core.base.BaseEntity;

/**
 * 面经评论
 *
 * @author moyun
 */
@Data
@TableName("portal_interview_comment")
public class PortalInterviewComment extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 面经ID */
    private Long experienceId;

    /** 评论用户ID */
    private Long userId;

    /** 父评论ID（支持多级） */
    private Long parentId;

    /** 被回复的用户ID */
    private Long replyToUserId;

    /** 评论内容 */
    @NotBlank(message = "评论内容不能为空")
    private String content;

    /** 点赞数 */
    private Long likeCount;

    /** 排序 */
    private Integer sort;

    /** 状态：pending/published/rejected/deleted */
    @Size(min = 0, max = 20, message = "状态长度不能超过20个字符")
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
