package com.moyun.portal.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 评论数据传输对象
 *
 * @author moyun
 */
@Data
@Schema(description = "评论DTO")
public class CommentDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 评论ID（更新时使用）
     */
    @Schema(description = "评论ID（更新时使用）", example = "1")
    private Long id;

    /**
     * 文章ID
     */
    @NotNull(message = "文章ID不能为空")
    @Schema(description = "文章ID", example = "1")
    private Long articleId;

    /**
     * 评论内容
     */
    @NotBlank(message = "评论内容不能为空")
    @Size(min = 1, max = 2000, message = "评论内容长度必须在1-2000个字符之间")
    @Schema(description = "评论内容", example = "这篇文章写得真好！")
    private String content;

    /**
     * 父评论ID（回复评论时使用）
     */
    @Schema(description = "父评论ID", example = "0")
    private Long parentId;

    /**
     * 回复目标用户ID
     */
    @Schema(description = "回复目标用户ID", example = "2")
    private Long replyTo;

    /**
     * 评论状态
     */
    @Schema(description = "评论状态", example = "pending")
    private String status;
}
