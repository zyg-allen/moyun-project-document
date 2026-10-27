package com.moyun.ledger.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.moyun.ledger.domain.entity.LedgerProduct;

import lombok.Data;

/**
 * 商品列表 VO：按 App 实际使用字段收口，去掉 userId/providerUserId/updateTime。
 */
@Data
public class ProductVO {

    private Long id;
    private Long warehouseId;
    private Long shelfId;
    private Integer quantity;
    private Integer stockQuantity;
    private String name;
    private Long categoryId;
    private String categoryName;
    private String barcode;
    private String unit;
    private String baseUnit;
    private BigDecimal costPrice;
    private BigDecimal sellPrice;
    private BigDecimal costTotal;
    private BigDecimal costValue;
    private String sourceType;
    private String sourceParty;
    private String sourceChannel;
    private LocalDate productionDate;
    private Integer shelfLifeDays;
    private LocalDate expiryDate;
    private String packagingType;
    private String tags;
    private String description;
    private String imageUrls;
    private String conditionGrade;
    private String remark;
    private Integer status;
    private Integer shelfCount;
    private Integer slowDays;
    private LocalDateTime lastMovedAt;
    private LocalDateTime createTime;
    private List<Long> channelIds;

    public static ProductVO from(LedgerProduct p) {
        if (p == null) {
            return null;
        }
        ProductVO v = new ProductVO();
        v.setId(p.getId());
        v.setWarehouseId(p.getWarehouseId());
        v.setShelfId(p.getShelfId());
        v.setQuantity(p.getQuantity());
        v.setStockQuantity(p.getStockQuantity());
        v.setName(p.getName());
        v.setCategoryId(p.getCategoryId());
        v.setCategoryName(p.getCategoryName());
        v.setBarcode(p.getBarcode());
        v.setUnit(p.getUnit());
        v.setBaseUnit(p.getBaseUnit());
        v.setCostPrice(p.getCostPrice());
        v.setSellPrice(p.getSellPrice());
        v.setCostTotal(p.getCostTotal());
        v.setCostValue(p.getCostValue());
        v.setSourceType(p.getSourceType());
        v.setSourceParty(p.getSourceParty());
        v.setSourceChannel(p.getSourceChannel());
        v.setProductionDate(p.getProductionDate());
        v.setShelfLifeDays(p.getShelfLifeDays());
        v.setExpiryDate(p.getExpiryDate());
        v.setPackagingType(p.getPackagingType());
        v.setTags(p.getTags());
        v.setDescription(p.getDescription());
        v.setImageUrls(p.getImageUrls());
        v.setConditionGrade(p.getConditionGrade());
        v.setRemark(p.getRemark());
        v.setStatus(p.getStatus());
        v.setShelfCount(p.getShelfCount());
        v.setSlowDays(p.getSlowDays());
        v.setLastMovedAt(p.getLastMovedAt());
        v.setCreateTime(p.getCreateTime());
        v.setChannelIds(p.getChannelIds());
        return v;
    }
}
