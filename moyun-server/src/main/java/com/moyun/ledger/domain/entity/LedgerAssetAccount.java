package com.moyun.ledger.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 记账-资产账户（方案A：资金/应收/投资/固定资产 统一一张表，account_type 区分）
 *
 * <p>注意：不继承 BaseEntity（表无 create_by/update_by/del_flag 审计字段）；
 * 删除即 status=0 停用归档，流水永久保留。
 *
 * @author moyun
 */
@Data
@TableName("ledger_asset_account")
public class LedgerAssetAccount {

    /** 账户类型：资金账户（流动资产） */
    public static final String ACCOUNT_TYPE_FUND = "fund";
    /** 账户类型：应收款（借出） */
    public static final String ACCOUNT_TYPE_RECEIVABLE = "receivable";
    /** 账户类型：投资 */
    public static final String ACCOUNT_TYPE_INVESTMENT = "investment";
    /** 账户类型：固定资产（折合） */
    public static final String ACCOUNT_TYPE_FIXED = "fixed";

    /** 状态：启用 */
    public static final int STATUS_ENABLED = 1;
    /** 状态：停用归档（删除即归档） */
    public static final int STATUS_ARCHIVED = 0;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 门户用户ID（portal_user.id） */
    private Long userId;

    /** 账户名称，用户命名："微信"、"张三（借款）" */
    private String name;

    /** 账户类型（第二层）：fund/receivable/investment/fixed */
    private String accountType;

    /** 描述（简单记录情况） */
    private String description;

    /** 当前余额（元）。禁止直接编辑，校准必须走 adjust 记账（资金类用） */
    private BigDecimal balance;

    /** 估值（元；投资/固定资产用，可≠balance） */
    private BigDecimal valuation;

    /** 总额（应收/投资/固定类用） */
    private BigDecimal totalAmount;

    /** 已收（应收类用） */
    private BigDecimal paidAmount;

    /** 结算状态：pending/partial/settled（应收类用） */
    private String settleStatus;

    /** 到期日（应收类用） */
    private LocalDate dueDate;

    /** 风险标记：normal/attention/bad_debt */
    private String riskLevel;

    /** 是否计入总资产：1=是 0=否 */
    private Integer includeInTotal;

    /** 图标 */
    private String icon;

    /** 是否隐藏余额（隐私模式）：1=是 0=否 */
    private Integer hideBalance;

    /** 排序 */
    private Integer sortOrder;

    /** 状态：1=启用 0=停用归档 */
    private Integer status;

    /** 乐观锁版本号 */
    private Integer version;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
