package com.moyun.portal.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单创建数据传输对象
 *
 * @author moyun
 */
@Data
@Schema(description = "订单创建DTO")
public class OrderCreateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 订单类型
     */
    @NotBlank(message = "订单类型不能为空")
    @Size(max = 50, message = "订单类型长度不能超过50个字符")
    @Schema(description = "订单类型", example = "vip")
    private String type;

    /**
     * 产品ID
     */
    @NotNull(message = "产品ID不能为空")
    @Schema(description = "产品ID", example = "1")
    private Long productId;

    /**
     * 支付金额
     */
    @NotNull(message = "支付金额不能为空")
    @DecimalMin(value = "0.01", message = "支付金额必须大于0")
    @Schema(description = "支付金额", example = "99.00")
    private BigDecimal amount;

    /**
     * 支付方式
     */
    @NotBlank(message = "支付方式不能为空")
    @Size(max = 50, message = "支付方式长度不能超过50个字符")
    @Schema(description = "支付方式", example = "alipay")
    private String payMethod;

    /**
     * 订单备注
     */
    @Size(max = 500, message = "订单备注长度不能超过500个字符")
    @Schema(description = "订单备注", example = "购买VIP会员")
    private String remark;
}
