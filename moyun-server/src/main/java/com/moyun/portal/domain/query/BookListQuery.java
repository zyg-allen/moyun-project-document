package com.moyun.portal.domain.query;

import com.moyun.core.base.page.PageDomain;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 书单查询对象
 *
 * @author moyun
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "书单查询对象")
public class BookListQuery extends PageDomain
{
    /** 书单标题（模糊查询） */
    @Schema(description = "书单标题", example = "程序员必读书单")
    private String title;

    /** 创建者用户ID */
    @Schema(description = "创建者用户ID", example = "1")
    private Long userId;

    /** 分类ID */
    @Schema(description = "分类ID", example = "1")
    private Long categoryId;

    /** 是否公开 */
    @Schema(description = "是否公开", example = "true")
    private Boolean isPublic;

    /** 状态 */
    @Schema(description = "状态", example = "active")
    private String status;

    /** 是否精选 */
    @Schema(description = "是否精选", example = "true")
    private Boolean isFeatured;
}
