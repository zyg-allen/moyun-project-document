package com.moyun.portal.domain.query;

import com.moyun.core.base.page.PageDomain;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文章查询对象
 *
 * @author moyun
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "文章查询对象")
public class ArticleQuery extends PageDomain {

    /**
     * 文章标题（模糊查询）
     */
    @Schema(description = "文章标题", example = "Spring")
    private String title;

    /**
     * 作者ID
     */
    @Schema(description = "作者ID", example = "1")
    private Long authorId;

    /**
     * 分类ID
     */
    @Schema(description = "分类ID", example = "1")
    private Long categoryId;

    /**
     * 顶级分类ID
     */
    @Schema(description = "顶级分类ID", example = "1")
    private Long rootCategoryId;

    /**
     * 分类名称（模糊查询）
     */
    @Schema(description = "分类名称", example = "技术")
    private String categoryName;

    /**
     * 文章状态
     */
    @Schema(description = "文章状态", example = "published")
    private String status;

    /**
     * 是否精选
     */
    @Schema(description = "是否精选", example = "true")
    private Boolean isFeatured;

    /**
     * 是否置顶
     */
    @Schema(description = "是否置顶", example = "false")
    private Boolean isTop;

    /**
     * 是否轮播
     */
    @Schema(description = "是否轮播", example = "false")
    private Boolean isCarousel;

    /**
     * 是否栏目推荐
     */
    @Schema(description = "是否栏目推荐", example = "false")
    private Boolean isCategoryRecommended;

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
