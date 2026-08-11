package com.moyun.portal.enums;

/**
 * 支付订单状态枚举
 *
 * <p>对应数据库表 portal_order.status / portal_tip_order.status 字段，
 * 标识订单/打赏单的支付生命周期：
 * <ul>
 *   <li>{@link #PENDING}   - 待支付：已下单，等待用户支付</li>
 *   <li>{@link #PAID}      - 已支付：支付成功（积分扣减或第三方回调确认）</li>
 *   <li>#REFUNDED  - 已退款：已全额退款（未来支付渠道接入后启用）</li>
 *   <li>{@link #CLOSED}    - 已关闭：超时未支付或手动关闭</li>
 *   <li>{@link #FAILED}    - 支付失败：渠道方返回失败</li>
 * </ul>
 *
 * <p>使用建议：业务代码中应使用 {@code PaymentStatus.PAID.getCode()}
 * 替代裸字符串 "paid"，避免拼写错误导致状态匹配失败。
 *
 * <p>注意：当前项目 MVP 阶段仅采用积分体系，订单创建即置 PAID（积分扣减成功即视为已支付），
 * PENDING/FAILED/CLOSED 等状态为未来接入真实支付渠道预留。
 *
 * @author moyun
 */
public enum PaymentStatus {

    PENDING("pending", "待支付"),
    PAID("paid", "已支付"),
    REFUNDED("refunded", "已退款"),
    CLOSED("closed", "已关闭"),
    FAILED("failed", "支付失败");

    private final String code;
    private final String desc;

    PaymentStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static PaymentStatus fromCode(String code) {
        if (code == null) {
            return PENDING;
        }
        for (PaymentStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return PENDING;
    }
}
