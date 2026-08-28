package com.moyun.core.sms;

/**
 * 短信发送 SPI（V11.1）
 *
 * <p>实现类按 moyun.sms.mock-enabled 条件装配：
 * mock=true 装配 {@link MockSmsSender}，false 装配 {@link AliyunSmsSender}。
 * 后期接入腾讯云等新渠道，实现本接口即可。
 *
 * @author moyun
 */
public interface SmsSender {

    /**
     * 发送验证码短信
     *
     * @param phone 手机号
     * @param code  验证码（6 位数字）
     * @return true=发送成功
     */
    boolean sendCode(String phone, String code);
}
