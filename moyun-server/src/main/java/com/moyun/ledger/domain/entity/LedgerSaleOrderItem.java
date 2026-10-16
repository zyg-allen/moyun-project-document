package com.moyun.ledger.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;

/** 销售明细。 */
@Data
@TableName("ledger_sale_order_item")
public class LedgerSaleOrderItem {
    public static final int STATUS_NORMAL = 1;

    private Long id;
    private Long saleId;
    private Long productId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal costPrice;
    private BigDecimal subtotal;
    private BigDecimal costSubtotal;
    private BigDecimal profit;
    private Integer status;
}