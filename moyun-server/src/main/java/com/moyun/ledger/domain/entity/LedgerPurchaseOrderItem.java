package com.moyun.ledger.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;

/** 采购明细。 */
@Data
@TableName("ledger_purchase_order_item")
public class LedgerPurchaseOrderItem {
    public static final int STATUS_NORMAL = 1;

    private Long id;
    private Long purchaseId;
    private Long productId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
    private Integer status;
}