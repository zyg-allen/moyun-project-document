package com.moyun.portal.domain.query;

import com.moyun.core.base.page.PageDomain;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * VIP套餐查询对象
 *
 * @author moyun
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "VIP套餐查询对象")
public class VipPackageQuery extends PageDomain {

    /**
     * 套餐名称（模糊查询）
     */
    @Schema(description = "套餐名称", example = "月度会员")
    private String name;

    /**
     * 套餐状态
     */
    @Schema(description = "套餐状态", example = "0")
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
