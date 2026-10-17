package com.moyun.ledger.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 记账-负债账户（方案A：普通负债/赊账应付 统一一张表，account_type 区分）
 *
 * <p>注意：不继承 BaseEntity；status=0 为手动删除归档，
 * settle_flag=1 为还款结清（正向业务态），两者语义独立。
 *
 * @author moyun
 */
@Data
@TableName("ledger_liability_account")
public class LedgerLiabilityAccount {

    /** 账户类型：普通负债（借款/信用卡/车贷房贷） */
    public static final String ACCOUNT_TYPE_DEBT = "debt";
    /** 账户类型：赊账应付 */
    public static final String ACCOUNT_TYPE_CREDIT = "credit";

    /** 状态：启用（在还） */
    public static final int STATUS_ENABLED = 1;
    /** 状态：停用归档（手动删除） */
    public static final int STATUS_ARCHIVED = 0;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 门户用户ID（portal_user.id） */
    private Long userId;

    /** 负债名称，用户命名："李四（欠款）"、"招商信用卡" */
    private String name;

    /** 账户类型（第二层）：debt/credit */
    private String accountType;

    /** 描述（简单记录情况） */
    private String description;

    /** 当前欠款（元） */
    private BigDecimal balance;

    /** 初始本金（元） */
    private BigDecimal principal;

    /** 年利率（%） */
    private BigDecimal annualRate;

    /** 总期数（月） */
    private Integer totalTerms;

    /** 已还期数（月） */
    private Integer paidTerms;

    /** 每期还款额（元） */
    private BigDecimal monthlyPayment;

    /** 还款日（每月几号，1-28） */
    private Integer repaymentDay;

    /** 到期日 */
    private LocalDate dueDate;

    /** 总额（赊账类用） */
    private BigDecimal totalAmount;

    /** 已还（赊账类用） */
    private BigDecimal paidAmount;

    /** 结算状态：pending/partial/settled（赊账类用） */
    private String settleStatus;

    /** 风险标记：normal/attention/bad_debt */
    private String riskLevel;

    /** 是否计入总负债：1=是 0=否 */
    private Integer includeInTotal;

    /** 图标 */
    private String icon;

    /** 排序 */
    private Integer sortOrder;

    /** 状态：1=启用 0=停用归档（手动删除） */
    private Integer status;

    /** 已结清：1=是 0=否（还款至0自动置位；归档展示、不计入当前总负债） */
    private Integer settleFlag;

    /** 乐观锁版本号 */
    private Integer version;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
