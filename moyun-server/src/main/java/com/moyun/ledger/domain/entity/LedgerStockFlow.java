package com.moyun.ledger.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存流水（所有变动留痕）。
 *
 * <p>带两层货架信息：{@code shelfId} 是本次操作的货架（移库时为源货架），
 * {@code toShelfId} 仅在移库时有值，便于回溯「从哪个架搬到哪个架」。</p>
 */
@Data
@TableName("ledger_stock_flow")
public class LedgerStockFlow {
    public static final String FLOW_IN = "in";
    public static final String FLOW_OUT = "out";
    public static final String FLOW_TRANSFER = "transfer";
    public static final String FLOW_ADJUST = "adjust";
    // 出入库原因（对应产品方案 4.3「状态管理」：在库/已售/自用/损耗/转出）
    public static final String REASON_PURCHASE = "purchase";
    public static final String REASON_SALE = "sale";
    public static final String REASON_SELF_USE = "self_use";
    public static final String REASON_LOSS = "loss";
    public static final String REASON_TRANSFER = "transfer";
    public static final String REASON_GIFT = "gift";
    public static final String REASON_STOCKTAKE = "stocktake";
    public static final String REASON_OTHER = "other";
    public static final int STATUS_NORMAL = 1;
    public static final int STATUS_DELETED = 0;

    private Long id;
    private Long warehouseId;
    private Long productId;
    /** 本次操作货架（移库时为源货架），0=未上架 */
    private Long shelfId;
    /** 移库目标货架（非移库为 0） */
    private Long toShelfId;
    private String flowType;
    /** 原因：purchase/sale/self_use/loss/transfer/gift/stocktake/other */
    private String reason;
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

    // ---------------- 非表字段（流水列表展示用） ----------------
    @TableField(exist = false)
    private String productName;
    @TableField(exist = false)
    private String shelfName;
    @TableField(exist = false)
    private String toShelfName;
    /** 原因中文：采购/销售/自用/损耗/转出/赠送/盘点/其他 */
    @TableField(exist = false)
    private String reasonLabel;
    /** 类型中文：入库/出库/移库/盘点 */
    @TableField(exist = false)
    private String flowTypeLabel;
}
