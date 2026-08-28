package com.moyun.pay.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 提现单（V11.0 钱包提现；本期预留表结构+枚举，打款通道后续接入）
 *
 * <p>状态机：AUDITING(审核中) → PAID(已打款) / REJECTED(已驳回)
 *
 * @author moyun
 */
@TableName("pay_withdraw_order")
public class WithdrawOrder {

    public static final String STATUS_AUDITING = "AUDITING";
    public static final String STATUS_PAID = "PAID";
    public static final String STATUS_REJECTED = "REJECTED";

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 提现单号：WD + 时间戳 + 随机 */
    private String withdrawNo;

    private Long userId;

    /** 提现金额（分） */
    private Long amount;

    /** 手续费（分） */
    private Long fee;

    /** 打款银行卡 ID（pay_user_bank_card.id） */
    private Long bankCardId;

    /** 状态：AUDITING / PAID / REJECTED */
    private String status;

    /** 审核时间 */
    private LocalDateTime auditTime;

    /** 驳回原因 */
    private String rejectReason;

    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getWithdrawNo() { return withdrawNo; }
    public void setWithdrawNo(String withdrawNo) { this.withdrawNo = withdrawNo; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }
    public Long getFee() { return fee; }
    public void setFee(Long fee) { this.fee = fee; }
    public Long getBankCardId() { return bankCardId; }
    public void setBankCardId(Long bankCardId) { this.bankCardId = bankCardId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getAuditTime() { return auditTime; }
    public void setAuditTime(LocalDateTime auditTime) { this.auditTime = auditTime; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
