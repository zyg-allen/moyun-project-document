package com.moyun.ext.cms.domain.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import com.moyun.core.base.BaseEntity;

/**
 * 评论视图对象
 *
 * @author moyun
 */
@Data
public class CmsCommentVO extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 评论ID */
    private Long id;

    /** 文章ID */
    private Long articleId;

    /** 文章标题 */
    private String articleTitle;

    /** 评论者ID */
    private Long authorId;

    /** 评论者昵称 */
    private String authorNickname;

    /** 评论者头像 */
    private String authorAvatar;

    /** 评论内容 */
    private String content;

    /** 父评论ID */
    private Long parentId;

    /** 回复目标ID */
    private Long replyTo;

    /** 点赞数 */
    private Long likeCount;

    /** 状态：0=待审核 1=已发布 2=审核驳回 */
    private String status;

    /** 审核人ID（sys_user.user_id） */
    private Long auditorId;

    /** 审核人昵称（sys_user.nick_name，CMS审核台展示用） */
    private String auditorNickname;

    /** 审核时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditTime;

    /** 审核意见/驳回原因 */
    private String auditRemark;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
