package com.moyun.portal.domain.query;

import com.moyun.core.base.page.PageDomain;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 评论查询对象
 *
 * @author moyun
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "评论查询对象")
public class CommentQuery extends PageDomain {

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
     * 评论内容（模糊查询）
     */
    @Schema(description = "评论内容", example = "好")
    private String content;

    /**
     * 评论状态
     */
    @Schema(description = "评论状态", example = "published")
    private String status;

    /**
     * 是否查询子评论
     */
    @Schema(description = "是否查询子评论", example = "true")
    private Boolean includeChildren = true;

    /**
     * 开始时间
     */
    @Schema(description = "开始时间", example = "2024-01-01")
    private String startTime;

    /**
     * 结束时间
     */
    @Schema(description = "结束时间", example = "2024-12-31")
    private String endTime;
}
