package com.moyun.pay.service;

import com.moyun.pay.domain.entity.UserBankCard;

import java.util.List;

/**
 * 用户银行卡服务（V11.0 提现打款账户）
 *
 * <p>密文（cardNoEncrypted/phoneEncrypted）只存在于服务端，
 * 对外返回实体前一律剥离密文字段（Controller 层执行）。
 *
 * @author moyun
 */
public interface IBankCardService {

    /**
     * 绑定（AES-GCM 加密落库 + 实名预校验 + 卡数上限 + 短信验证码闭环）
     *
     * @param smsCode 短信验证码（moyun.pay.security.bank-card-sms-verify=true 时必填）
     */
    UserBankCard bind(Long userId, String holderName, String cardNo, String phone,
                      String bankCode, String bankName, String smsCode);

    /** 本人卡列表（脱敏） */
    List<UserBankCard> listByUser(Long userId);

    /** 删除（限本人） */
    void remove(Long userId, Long cardId);

    /** 设默认卡（限本人） */
    void setDefault(Long userId, Long cardId);
}
