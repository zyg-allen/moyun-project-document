package com.moyun.ledger.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 实时库存（仓库×商品唯一）。 */
@Data
@TableName("ledger_stock")
public class LedgerStock {
    public static final int STATUS_NORMAL = 1;
    public static final int STATUS_DELETED = 0;

    private Long id;
    private Long warehouseId;
    private Long productId;
    private Integer quantity;
    private BigDecimal costValue;
    private LocalDateTime lastUpdated;
    private Integer status;
}