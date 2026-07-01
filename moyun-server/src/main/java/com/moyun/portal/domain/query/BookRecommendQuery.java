package com.moyun.portal.domain.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import com.moyun.core.base.page.PageDomain;

/**
 * 书籍推荐查询对象
 *
 * @author moyun
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "书籍推荐查询对象")
public class BookRecommendQuery extends PageDomain {

    /** 推荐位置：home_banner/home_hot/category_top/limit_free/discover_banner */
    @Schema(description = "推荐位置", example = "home_banner")
    private String position;

    /** 是否启用 */
    @Schema(description = "是否启用", example = "true")
    private Boolean isActive;

    /** 书名（模糊查询，用于 JOIN 查询） */
    @Schema(description = "书名（模糊）", example = "三体")
    private String bookTitle;

    /** 排序方式：latest=按创建时间 / sort=按排序值 */
    @Schema(description = "排序方式", example = "sort")
    private String orderBy;
}
