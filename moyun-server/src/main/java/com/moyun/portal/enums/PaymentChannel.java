package com.moyun.portal.enums;

/**
 * 支付渠道枚举
 *
 * <p>对应数据库表 portal_order.pay_channel / portal_tip_order.pay_channel 字段，
 * 标识订单使用的支付渠道：
 * <ul>
 *   <li>{@link #POINTS}  - 积分：当前 MVP 阶段的唯一可用渠道</li>
 *   <li>{@link #ALIPAY}  - 支付宝：未来接入</li>
 *   <li>{@link #WECHAT}  - 微信支付：未来接入</li>
 *   <li>{@link #WALLET}  - 钱包余额：未来接入（用户充值后使用钱包支付）</li>
 * </ul>
 *
 * <p>使用建议：业务代码中应使用 {@code PaymentChannel.POINTS.getCode()}
 * 替代裸字符串 "points"，避免拼写错误。
 *
 * @author moyun
 */
public enum PaymentChannel {

    POINTS("points", "积分"),
    ALIPAY("alipay", "支付宝"),
    WECHAT("wechat", "微信支付"),
    WALLET("wallet", "钱包余额");

    private final String code;
    private final String desc;

    PaymentChannel(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static PaymentChannel fromCode(String code) {
        if (code == null) {
            return POINTS;
        }
        for (PaymentChannel channel : values()) {
            if (channel.code.equals(code)) {
                return channel;
            }
        }
        return POINTS;
    }
}
