package com.moyun.ledger.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 销售单（卖出/赊销）。payment_type=cash 现结 / credit 赊账。 */
@Data
@TableName("ledger_sale_order")
public class LedgerSaleOrder {
    public static final String PAY_CASH = "cash";
    public static final String PAY_CREDIT = "credit";
    public static final int STATUS_NORMAL = 1;
    public static final int STATUS_DELETED = 0;

    private Long id;
    private Long userId;
    private Long warehouseId;
    private String orderNo;
    private String customerName;
    private Long customerId;
    private String paymentType;
    private BigDecimal totalAmount;
    private BigDecimal costAmount;
    private BigDecimal profitAmount;
    private Long linkedAccountId;
    private Long transactionId;
    private Long operatorId;
    private String remark;
    private Integer status;
    private LocalDateTime createTime;
}