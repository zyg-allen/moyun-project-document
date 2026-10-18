package com.moyun.ledger.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 实时库存（商品 × 货架 唯一）。
 *
 * <p>{@code shelfId = 0} 表示「未上架」。用 0 而非 NULL：MySQL 唯一索引把 NULL
 * 视为互不相等，可空会让同一商品出现多条「未上架」行、唯一约束失效。
 * 商品总库存 = 该商品各货架 quantity 之和。</p>
 */
@Data
@TableName("ledger_stock")
public class LedgerStock {
    public static final int STATUS_NORMAL = 1;
    public static final int STATUS_DELETED = 0;
    /** 未上架（默认库位）的货架ID */
    public static final long NO_SHELF = 0L;

    private Long id;
    private Long warehouseId;
    private Long productId;
    /** 货架ID，0=未上架 */
    private Long shelfId;
    private Integer quantity;
    private BigDecimal costValue;
    private LocalDateTime lastUpdated;
    private Integer status;
}
