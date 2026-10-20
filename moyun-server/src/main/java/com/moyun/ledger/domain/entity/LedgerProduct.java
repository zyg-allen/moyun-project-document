package com.moyun.ledger.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品档案。
 *
 * <p>类目关联 {@code categoryId}（ledger_product_category.id，一级或二级）；
 * 旧的 {@code category} 文本字段保留但不再写入，仅为历史兼容。</p>
 */
@Data
@TableName("ledger_product")
public class LedgerProduct {
    public static final int STATUS_NORMAL = 1;
    public static final int STATUS_DELETED = 0;

    private Long id;
    private Long warehouseId;
    /** 所在货架（0=未上架）。一个商品只属于一个货架，要分开放就分别登记。 */
    private Long shelfId;
    /** 当前库存数量（原 ledger_stock.quantity） */
    private Integer quantity;
    /** 库存成本价值 = 成本价 × 数量 */
    private java.math.BigDecimal costValue;
    private Long userId;
    /** 提供者：谁把这批货放进仓库（NULL=仓库主） */
    private Long providerUserId;
    private String name;
    /** 类目ID（ledger_product_category.id） */
    private Long categoryId;
    /** 历史自由文本类目，已由 categoryId 取代，不再写入 */
    private String barcode;
    private String unit;
    private BigDecimal costPrice;
    private BigDecimal sellPrice;

    /** 来源（固定枚举）：purchase 采购 / self_made 自产 / used_resale 自用转卖 / gift 赠品 */
    private String sourceType;
    /** 供应商 / 赠送方（自由文本） */
    private String sourceParty;
    /** 成本总额（权威值；单价 = cost_total ÷ 数量） */
    private BigDecimal costTotal;
    /** 其他成本（物流 / 包装 / 时间合并为一项） */
    /** 基础单位（库存与成本一律按它记） */
    private String baseUnit;
    /** 包装单位（如 箱），可为空 */
    private String packUnit; // 暂不使用（整箱进货预留）
    /** 换算系数：1 包装单位 = N 基础单位 */
    private Integer packSize; // 暂不使用（整箱进货预留）
    /** 生产日期 */
    private java.time.LocalDate productionDate;
    /** 保质期天数 */
    private Integer shelfLifeDays;
    /** 到期日期（生产日期+保质期，可手改） */
    private java.time.LocalDate expiryDate;
    /** 包装类型：none无需 / standard标准 / fragile防摔 / custom特殊 */
    private String packagingType;
    /** 标签，逗号分隔 */
    private String tags;
    /** 商品描述 */
    private String description;
    private String imageUrls;
    private String conditionGrade;
    private String sourceChannel;
    private String remark;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 最后动销时间：任一库存变动（入库/出库/校准/移库）时刷新 */
    private LocalDateTime lastMovedAt;

    /** 非持久化：列表展示时的当前库存数量（来自 ledger_stock，查询时回填） */
    @TableField(exist = false)
    private Integer stockQuantity;

    /** 非持久化：类目名称（查询时回填） */
    @TableField(exist = false)
    private String categoryName;

    /** 非持久化：提供者姓名 */
    @TableField(exist = false)
    private String providerName;

    /** 非持久化：距最后动销多少天（滞销判定用） */
    @TableField(exist = false)
    private Integer slowDays;

    /** 非持久化：可售渠道ID列表（1:N） */
    /* 来源枚举 */
    public static final String SRC_PURCHASE = "purchase";
    public static final String SRC_SELF_MADE = "self_made";
    public static final String SRC_USED_RESALE = "used_resale";
    public static final String SRC_GIFT = "gift";

    public static final String PACK_NONE = "none";
    public static final String PACK_STANDARD = "standard";
    public static final String PACK_FRAGILE = "fragile";
    public static final String PACK_CUSTOM = "custom";

    @TableField(exist = false)
    private java.util.List<Long> channelIds;

    /** 非持久化：库存在几个货架上（相似推荐/列表展示用） */
    @TableField(exist = false)
    private Integer shelfCount;
}
