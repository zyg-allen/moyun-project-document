package com.moyun.pay.gateway;

import com.moyun.pay.domain.entity.PayOrder;

/**
 * 业务回调 SPI（V11.0 公共支付通道）
 *
 * <p>网关在支付成功后按 bizType 路由到对应实现。业务方只处理自己的业务状态推进
 * （如打赏单 PAID），分账/通知等通用动作由网关统一驱动，保证所有业务一致。
 *
 * @author moyun
 */
public interface PayCallbackHandler {

    /** 本实现负责的业务类型（如 "tip"） */
    String bizType();

    /**
     * 支付成功回调（同事务内调用；抛异常则整体回滚）
     *
     * @param payOrder 已支付的支付单
     */
    void onPaySuccess(PayOrder payOrder);
}
