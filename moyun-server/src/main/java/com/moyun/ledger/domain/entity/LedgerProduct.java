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
    private Long userId;
    /** 提供者：谁把这批货放进仓库（NULL=仓库主） */
    private Long providerUserId;
    private String name;
    /** 类目ID（ledger_product_category.id） */
    private Long categoryId;
    /** 历史自由文本类目，已由 categoryId 取代，不再写入 */
    private String category;
    private String barcode;
    private String unit;
    private BigDecimal costPrice;
    private BigDecimal sellPrice;
    private String imageUrls;
    private Integer isSecondhand;
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

    /** 非持久化：库存在几个货架上（相似推荐/列表展示用） */
    @TableField(exist = false)
    private Integer shelfCount;
}
