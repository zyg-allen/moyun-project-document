package com.moyun.pay.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 用户银行卡（V11.0 提现打款账户）
 *
 * <p>安全：卡号/手机号 AES-GCM 加密落库（card_no_encrypted），
 * 任何查询接口只允许下发脱敏卡号（card_no_masked），密文仅服务端打款时解密使用。
 *
 * @author moyun
 */
@TableName("user_bank_card")
public class UserBankCard {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 持卡人姓名（实名校验通过后绑定） */
    private String holderName;

    /** 卡号密文（AES-GCM） */
    private String cardNoEncrypted;

    /** 卡号脱敏（6217 **** **** 1234，列表展示用） */
    private String cardNoMasked;

    /** 手机号密文（AES-GCM） */
    private String phoneEncrypted;

    /** 手机号脱敏（138****5678） */
    private String phoneMasked;

    /** 银行编码（ICBC/CCB/...） */
    private String bankCode;

    /** 银行名称 */
    private String bankName;

    /** 卡类型：DEBIT(借记卡) / CREDIT(信用卡)——仅支持借记卡提现 */
    private String cardType;

    /** 核验状态：PENDING / VERIFIED / REJECTED */
    private String verifyStatus;

    /** 是否默认卡：1=是 0=否 */
    private Integer isDefault;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getHolderName() { return holderName; }
    public void setHolderName(String holderName) { this.holderName = holderName; }
    public String getCardNoEncrypted() { return cardNoEncrypted; }
    public void setCardNoEncrypted(String cardNoEncrypted) { this.cardNoEncrypted = cardNoEncrypted; }
    public String getCardNoMasked() { return cardNoMasked; }
    public void setCardNoMasked(String cardNoMasked) { this.cardNoMasked = cardNoMasked; }
    public String getPhoneEncrypted() { return phoneEncrypted; }
    public void setPhoneEncrypted(String phoneEncrypted) { this.phoneEncrypted = phoneEncrypted; }
    public String getPhoneMasked() { return phoneMasked; }
    public void setPhoneMasked(String phoneMasked) { this.phoneMasked = phoneMasked; }
    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getCardType() { return cardType; }
    public void setCardType(String cardType) { this.cardType = cardType; }
    public String getVerifyStatus() { return verifyStatus; }
    public void setVerifyStatus(String verifyStatus) { this.verifyStatus = verifyStatus; }
    public Integer getIsDefault() { return isDefault; }
    public void setIsDefault(Integer isDefault) { this.isDefault = isDefault; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
