package com.moyun.portal.domain.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import com.moyun.core.base.page.PageDomain;

/**
 * 分类查询对象
 *
 * @author moyun
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "分类查询对象")
public class CategoryQuery extends PageDomain {

    /**
     * 分类名称（模糊查询）
     */
    @Schema(description = "分类名称", example = "技术")
    private String name;

    /**
     * 分类状态
     */
    @Schema(description = "分类状态", example = "0")
    private String status;

    /**
     * 父分类ID
     */
    @Schema(description = "父分类ID", example = "1")
    private Long parentId;

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
