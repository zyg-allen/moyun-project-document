package com.moyun.portal.domain.entity;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import com.moyun.core.base.BaseEntity;

@Data
@TableName("portal_order")
public class PortalOrder extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "订单号不能为空")
    @Size(min = 0, max = 64, message = "订单号长度不能超过64个字符")
    private String orderNo;

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "类型不能为空")
    @Size(min = 0, max = 50, message = "类型长度不能超过50个字符")
    private String type;

    private Long productId;

    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.00", message = "金额不能小于0")
    private BigDecimal amount;

    @Size(min = 0, max = 20, message = "状态长度不能超过20个字符")
    private String status;

    @Size(min = 0, max = 50, message = "支付方式长度不能超过50个字符")
    private String payMethod;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime paidAt;

    public PortalOrder() {
    }

    public PortalOrder(Long id) {
        this.id = id;
    }
}
