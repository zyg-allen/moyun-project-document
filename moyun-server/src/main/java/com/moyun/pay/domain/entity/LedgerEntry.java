package com.moyun.pay.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 资金/分账流水（V11.0 复式记账）
 *
 * <p>每笔支付成功拆两条记分录：平台抽成（PLATFORM/credit）+ 用户所得（USER/credit），
 * 两条金额之和恒等于支付单金额（守恒校验见 LedgerServiceImpl）。
 *
 * @author moyun
 */
@TableName("ledger_entry")
public class LedgerEntry {

    /** 账户角色：平台 */
    public static final String ROLE_PLATFORM = "PLATFORM";
    /** 账户角色：用户 */
    public static final String ROLE_USER = "USER";
    /** 方向：收入（贷） */
    public static final String DIRECTION_CREDIT = "credit";
    /** 方向：支出（借） */
    public static final String DIRECTION_DEBIT = "debit";

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 结算单号（一组分账流水共享）：STL + 时间戳 + 随机 */
    private String settleNo;

    /** 关联支付单号 */
    private String payNo;

    /** 业务类型：tip / withdraw */
    private String bizType;

    /** 业务单号 */
    private String bizNo;

    /** 账户角色：PLATFORM / USER */
    private String accountRole;

    /** 用户 ID（平台分录为 0） */
    private Long userId;

    /** 方向：credit=收入 / debit=支出 */
    private String direction;

    /** 金额（分） */
    private Long amount;

    /** 变动后余额（分；平台分录不追踪余额则为 null） */
    private Long balanceAfter;

    /** 业务摘要，如：打赏收入-作者所得 / 打赏服务费-平台抽成 */
    private String summary;

    private LocalDateTime createTime;

    /** 金额（元，仅展示用，不落库） */
    @TableField(exist = false)
    private transient BigDecimal amountYuan;

    /** 变动后余额（元，仅展示用，不落库） */
    @TableField(exist = false)
    private transient BigDecimal balanceAfterYuan;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSettleNo() { return settleNo; }
    public void setSettleNo(String settleNo) { this.settleNo = settleNo; }
    public String getPayNo() { return payNo; }
    public void setPayNo(String payNo) { this.payNo = payNo; }
    public String getBizType() { return bizType; }
    public void setBizType(String bizType) { this.bizType = bizType; }
    public String getBizNo() { return bizNo; }
    public void setBizNo(String bizNo) { this.bizNo = bizNo; }
    public String getAccountRole() { return accountRole; }
    public void setAccountRole(String accountRole) { this.accountRole = accountRole; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }
    public Long getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(Long balanceAfter) { this.balanceAfter = balanceAfter; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public BigDecimal getAmountYuan() { return amountYuan; }
    public void setAmountYuan(BigDecimal amountYuan) { this.amountYuan = amountYuan; }
    public BigDecimal getBalanceAfterYuan() { return balanceAfterYuan; }
    public void setBalanceAfterYuan(BigDecimal balanceAfterYuan) { this.balanceAfterYuan = balanceAfterYuan; }
}
