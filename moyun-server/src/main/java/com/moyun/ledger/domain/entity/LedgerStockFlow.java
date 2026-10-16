package com.moyun.ledger.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 库存流水（所有变动留痕）。 */
@Data
@TableName("ledger_stock_flow")
public class LedgerStockFlow {
    public static final String FLOW_IN = "in";
    public static final String FLOW_OUT = "out";
    public static final String FLOW_ADJUST = "adjust";
    public static final int STATUS_NORMAL = 1;
    public static final int STATUS_DELETED = 0;

    private Long id;
    private Long warehouseId;
    private Long productId;
    private String flowType;
    private Integer quantityBefore;
    private Integer quantityChange;
    private Integer quantityAfter;
    private BigDecimal costBefore;
    private BigDecimal costAfter;
    private Long operatorId;
    private String relatedOrder;
    private String remark;
    private Integer status;
    private LocalDateTime createTime;
}