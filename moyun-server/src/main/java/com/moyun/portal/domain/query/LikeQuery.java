package com.moyun.portal.domain.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import com.moyun.core.base.page.PageDomain;

/**
 * 点赞查询对象
 *
 * @author moyun
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "点赞查询对象")
public class LikeQuery extends PageDomain {

    /**
     * 用户ID
     */
    @Schema(description = "用户ID", example = "1")
    private Long userId;

    /**
     * 文章ID
     */
    @Schema(description = "文章ID", example = "1")
    private Long articleId;

    /**
     * 评论ID
     */
    @Schema(description = "评论ID", example = "1")
    private Long commentId;

    /**
     * 点赞类型
     */
    @Schema(description = "点赞类型", example = "article")
    private String type;

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
