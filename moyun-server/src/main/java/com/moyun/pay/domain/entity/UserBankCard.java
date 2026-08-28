package com.moyun.pay.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.moyun.core.base.BaseEntity;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户银行卡（V11.0 提现打款账户）
 *
 * <p>安全：卡号/手机号 AES-GCM 加密落库（card_no_encrypted），
 * 任何查询接口只允许下发脱敏卡号（card_no_masked），密文仅服务端打款时解密使用。
 *
 * @author moyun
 */
@Data
@TableName("pay_user_bank_card")
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

    /** 银行编码（ICBC/CCB/...） */
    private String bankCode;

    /** 银行名称 */
    private String bankName;

    /** 核验状态：PENDING / VERIFIED / REJECTED */
    private String verifyStatus;

    /** 是否默认卡：1=是 0=否 */
    private Integer isDefault;

    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

}
