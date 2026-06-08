package com.moyun.portal.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评论视图对象
 *
 * @author moyun
 */
@Data
@Schema(description = "评论VO")
public class CommentVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 评论ID
     */
    @Schema(description = "评论ID", example = "1")
    private Long id;

    /**
     * 文章ID
     */
    @Schema(description = "文章ID", example = "1")
    private Long articleId;

    /**
     * 评论者ID
     */
    @Schema(description = "评论者ID", example = "1")
    private Long authorId;

    /**
     * 评论者用户名
     */
    @Schema(description = "评论者用户名", example = "john_doe")
    private String authorUsername;

    /**
     * 评论者昵称
     */
    @Schema(description = "评论者昵称", example = "John Doe")
    private String authorNickname;

    /**
     * 评论者头像
     */
    @Schema(description = "评论者头像", example = "https://example.com/avatar.jpg")
    private String authorAvatar;

    /**
     * 评论内容
     */
    @Schema(description = "评论内容", example = "这篇文章写得真好！")
    private String content;

    /**
     * 父评论ID
     */
    @Schema(description = "父评论ID", example = "0")
    private Long parentId;

    /**
     * 根评论ID（一级评论的ID）
     */
    @Schema(description = "根评论ID", example = "1")
    private Long rootId;

    /**
     * 回复目标用户ID
     */
    @Schema(description = "回复目标用户ID", example = "2")
    private Long replyTo;

    /**
     * 回复目标用户名
     */
    @Schema(description = "回复目标用户名", example = "jane_doe")
    private String replyToUsername;

    /**
     * 回复目标昵称
     */
    @Schema(description = "回复目标昵称", example = "Jane Doe")
    private String replyToNickname;

    /**
     * 被回复的内容摘要
     */
    @Schema(description = "被回复的内容摘要", example = "这篇文章写得真好...")
    private String replyToContent;

    /**
     * 点赞数
     */
    @Schema(description = "点赞数", example = "10")
    private Long likeCount;

    /**
     * 评论状态
     */
    @Schema(description = "评论状态", example = "published")
    private String status;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间", example = "2024-01-01 12:00:00")
    private LocalDateTime createTime;

    /**
     * 子评论列表
     */
    @Schema(description = "子评论列表")
    private java.util.List<CommentVO> replies;
}
