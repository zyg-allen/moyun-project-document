package com.moyun.ledger.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.moyun.ledger.domain.entity.LedgerTransaction;

import lombok.Data;

/**
 * 流水列表 VO：显式收口对外字段，避免 Entity 增删字段直接影响客户端。
 */
@Data
public class LedgerTransactionVO {

    private Long id;
    private String type;
    private BigDecimal amount;
    private Integer stockIn;
    private Long categoryId;
    private Long subCategoryId;
    private Long accountId;
    private Long liabilityId;
    private Long receivableId;
    private Long payableId;
    private Long targetAccountId;
    private BigDecimal balanceAfter;
    private BigDecimal targetBalanceAfter;
    private BigDecimal liabilityBalanceAfter;
    private String description;
    private LocalDate transactionDate;
    private String transactionTime;
    private String merchant;
    private String voucherUrl;
    private Integer isBudget;
    private Integer status;
    private String accountName;
    private String targetAccountName;
    private String liabilityName;
    private String categoryName;
    private String subCategoryName;
    private LocalDateTime createTime;

    public static LedgerTransactionVO from(LedgerTransaction t) {
        if (t == null) {
            return null;
        }
        LedgerTransactionVO v = new LedgerTransactionVO();
        v.setId(t.getId());
        v.setType(t.getType());
        v.setAmount(t.getAmount());
        v.setStockIn(t.getStockIn());
        v.setCategoryId(t.getCategoryId());
        v.setSubCategoryId(t.getSubCategoryId());
        v.setAccountId(t.getAccountId());
        v.setLiabilityId(t.getLiabilityId());
        v.setReceivableId(t.getReceivableId());
        v.setPayableId(t.getPayableId());
        v.setTargetAccountId(t.getTargetAccountId());
        v.setBalanceAfter(t.getBalanceAfter());
        v.setTargetBalanceAfter(t.getTargetBalanceAfter());
        v.setLiabilityBalanceAfter(t.getLiabilityBalanceAfter());
        v.setDescription(t.getDescription());
        v.setTransactionDate(t.getTransactionDate());
        v.setTransactionTime(t.getTransactionTime());
        v.setMerchant(t.getMerchant());
        v.setVoucherUrl(t.getVoucherUrl());
        v.setIsBudget(t.getIsBudget());
        v.setStatus(t.getStatus());
        v.setAccountName(t.getAccountName());
        v.setTargetAccountName(t.getTargetAccountName());
        v.setLiabilityName(t.getLiabilityName());
        v.setCategoryName(t.getCategoryName());
        v.setSubCategoryName(t.getSubCategoryName());
        v.setCreateTime(t.getCreateTime());
        return v;
    }
}
