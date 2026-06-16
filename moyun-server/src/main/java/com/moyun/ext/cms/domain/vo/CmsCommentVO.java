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

    /** 状态 */
    private String status;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
