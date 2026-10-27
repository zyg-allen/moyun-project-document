package com.moyun.ledger.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.moyun.ledger.domain.entity.LedgerAssetAccount;

import lombok.Data;

/**
 * 资产账户 VO：列表/选择器用，去掉 userId/version 等内部字段。
 */
@Data
public class AssetAccountVO {

    private Long id;
    private String name;
    private String accountType;
    private String description;
    private BigDecimal balance;
    private BigDecimal valuation;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private String settleStatus;
    private LocalDate dueDate;
    private String riskLevel;
    private Integer includeInTotal;
    private String icon;
    private Integer hideBalance;
    private Integer sortOrder;
    private Integer status;

    public static AssetAccountVO from(LedgerAssetAccount a) {
        if (a == null) {
            return null;
        }
        AssetAccountVO v = new AssetAccountVO();
        v.setId(a.getId());
        v.setName(a.getName());
        v.setAccountType(a.getAccountType());
        v.setDescription(a.getDescription());
        v.setBalance(a.getBalance());
        v.setValuation(a.getValuation());
        v.setTotalAmount(a.getTotalAmount());
        v.setPaidAmount(a.getPaidAmount());
        v.setSettleStatus(a.getSettleStatus());
        v.setDueDate(a.getDueDate());
        v.setRiskLevel(a.getRiskLevel());
        v.setIncludeInTotal(a.getIncludeInTotal());
        v.setIcon(a.getIcon());
        v.setHideBalance(a.getHideBalance());
        v.setSortOrder(a.getSortOrder());
        v.setStatus(a.getStatus());
        return v;
    }
}
