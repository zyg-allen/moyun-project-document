package com.moyun.pay.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提现单（钱包提现；状态统一小写字符串枚举）
 *
 * <p>资金模型：真钱集中于平台公账商户号，虚拟余额为记账数字；提现是唯一动真钱的时机
 * （审核通过时记账扣减 + 商户号出金到用户银行卡）。
 *
 * <p>状态机：auditing(审核中) → paid(已打款) / rejected(已驳回)
 *
 * <p>金额单位：元（人民币，DECIMAL(18,2)，统一）。
 *
 * @author moyun
 */
@TableName("pay_withdraw_order")
public class WithdrawOrder {

    public static final String STATUS_AUDITING = "auditing";
    public static final String STATUS_PAID = "paid";
    public static final String STATUS_REJECTED = "rejected";

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 提现单号：WD + 时间戳 + 随机 */
    private String withdrawNo;

    private Long userId;

    /** 提现金额（元） */
    private BigDecimal amount;

    /** 手续费（元） */
    private BigDecimal fee;

    /** 打款银行卡 ID（pay_user_bank_card.id） */
    private Long bankCardId;

    /** 状态：auditing / paid / rejected */
    private String status;

    /** 审核时间 */
    private LocalDateTime auditTime;

    /** 驳回原因 */
    private String rejectReason;

    /** 打款完成时间（真实出金到账时间） */
    private LocalDateTime paidTime;

    private LocalDateTime createTime;

    /** 用户昵称（后台展示，非持久化） */
    @TableField(exist = false)
    private String nickname;

    /** 银行卡脱敏描述（后台展示，非持久化） */
    @TableField(exist = false)
    private String bankCardDesc;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getWithdrawNo() { return withdrawNo; }
    public void setWithdrawNo(String withdrawNo) { this.withdrawNo = withdrawNo; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getFee() { return fee; }
    public void setFee(BigDecimal fee) { this.fee = fee; }
    public Long getBankCardId() { return bankCardId; }
    public void setBankCardId(Long bankCardId) { this.bankCardId = bankCardId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getAuditTime() { return auditTime; }
    public void setAuditTime(LocalDateTime auditTime) { this.auditTime = auditTime; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public LocalDateTime getPaidTime() { return paidTime; }
    public void setPaidTime(LocalDateTime paidTime) { this.paidTime = paidTime; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getBankCardDesc() { return bankCardDesc; }
    public void setBankCardDesc(String bankCardDesc) { this.bankCardDesc = bankCardDesc; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
