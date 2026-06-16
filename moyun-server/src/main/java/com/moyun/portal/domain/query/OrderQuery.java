package com.moyun.portal.domain.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import com.moyun.core.base.page.PageDomain;

/**
 * 订单查询对象
 *
 * @author moyun
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "订单查询对象")
public class OrderQuery extends PageDomain {

    /**
     * 订单号（模糊查询）
     */
    @Schema(description = "订单号", example = "ORD123456")
    private String orderNo;

    /**
     * 用户ID
     */
    @Schema(description = "用户ID", example = "1")
    private Long userId;

    /**
     * 订单类型
     */
    @Schema(description = "订单类型", example = "vip")
    private String type;

    /**
     * 商品ID
     */
    @Schema(description = "商品ID", example = "1")
    private Long productId;

    /**
     * 订单状态
     */
    @Schema(description = "订单状态", example = "paid")
    private String status;

    /**
     * 支付方式
     */
    @Schema(description = "支付方式", example = "alipay")
    private String payMethod;

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
