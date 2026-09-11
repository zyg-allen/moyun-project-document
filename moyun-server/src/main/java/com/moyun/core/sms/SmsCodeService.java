package com.moyun.core.sms;

/**
 * 短信验证码服务（V11.1）
 *
 * <p>验证码生命周期：生成 → 频控校验 → Redis 存储（TTL）→ 校验（一次性消费 + 防枚举锁定）。
 * 与发送渠道解耦（{@link SmsSender} SPI），供银行卡绑定、后期会员支付等敏感场景复用。
 *
 * @author moyun
 */
public interface SmsCodeService {

    /**
     * 发送验证码（含频控：间隔锁 + 日限额）
     *
     * @param phone 手机号
     * @param scene 业务场景（bankcard/member/...）
     */
    void sendCode(String phone, String scene);

    /**
     * 校验验证码（通过即消费，一次性；连续错误达阈值后作废）
     *
     * @param phone 手机号
     * @param scene 业务场景
     * @param code  用户输入的验证码
     * @return true=校验通过
     */
    boolean verifyCode(String phone, String scene, String code);
}
