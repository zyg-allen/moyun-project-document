package com.moyun.ledger.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 记账-负债账户
 *
 * <p>注意：不继承 BaseEntity；status=0 为手动删除归档，
 * settle_flag=1 为还款结清（正向业务态），两者语义独立。
 *
 * @author moyun
 */
@Data
@TableName("ledger_liability_account")
public class LedgerLiabilityAccount {

    /** 类型：信用卡 */
    public static final String TYPE_CREDIT_CARD = "credit_card";
    /** 类型：消费贷 */
    public static final String TYPE_CONSUMER_LOAN = "consumer_loan";
    /** 类型：银行贷款 */
    public static final String TYPE_BANK_LOAN = "bank_loan";
    /** 类型：个人借款 */
    public static final String TYPE_PERSONAL_LOAN = "personal_loan";
    /** 类型：其他 */
    public static final String TYPE_OTHER = "other";

    /** 状态：启用（在还） */
    public static final int STATUS_ENABLED = 1;
    /** 状态：停用归档（手动删除） */
    public static final int STATUS_ARCHIVED = 0;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 门户用户ID（portal_user.id） */
    private Long userId;

    /** 负债名称，如“招行信用卡” */
    private String name;

    /** 类型：credit_card/consumer_loan/bank_loan/personal_loan/other */
    private String type;

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
