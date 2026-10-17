package com.moyun.ledger.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 采购单（买入/赊购）。payment_type=cash 现结 / credit 赊账。 */
@Data
@TableName("ledger_purchase_order")
public class LedgerPurchaseOrder {
    public static final String PAY_CASH = "cash";
    public static final String PAY_CREDIT = "credit";
    public static final int STATUS_NORMAL = 1;
    public static final int STATUS_DELETED = 0;

    private Long id;
    private Long userId;
    private Long warehouseId;
    private String orderNo;
    private String supplierName;
    private Long supplierId;
    private String paymentType;
    private BigDecimal totalAmount;
    private Long linkedAccountId;
    private Long transactionId;
    private Long operatorId;
    private String remark;
    private Integer status;
    private LocalDateTime createTime;
}