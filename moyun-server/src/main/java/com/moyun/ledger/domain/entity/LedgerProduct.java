package com.moyun.ledger.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 商品档案。 */
@Data
@TableName("ledger_product")
public class LedgerProduct {
    public static final int STATUS_NORMAL = 1;
    public static final int STATUS_DELETED = 0;

    private Long id;
    private Long warehouseId;
    private Long userId;
    private String name;
    private String barcode;
    private String category;
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
}