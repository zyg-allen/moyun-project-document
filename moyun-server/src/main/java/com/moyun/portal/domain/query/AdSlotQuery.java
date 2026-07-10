package com.moyun.portal.domain.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import com.moyun.core.base.page.PageDomain;

/**
 * 广告位查询对象
 *
 * @author moyun
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "广告位查询对象")
public class AdSlotQuery extends PageDomain {

    /**
     * 广告位标识
     */
    @Schema(description = "广告位标识", example = "article_detail_bottom")
    private String slotKey;

    /**
     * 广告标题（模糊查询）
     */
    @Schema(description = "广告标题", example = "招聘")
    private String title;

    /**
     * 状态
     */
    @Schema(description = "状态", example = "0")
    private String status;

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
