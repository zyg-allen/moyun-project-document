package com.moyun.vip.handler;

import com.moyun.pay.domain.entity.PayOrder;
import com.moyun.pay.gateway.PayCallbackHandler;
import com.moyun.pay.service.ILedgerService;
import com.moyun.pay.service.INotificationService;
import com.moyun.vip.service.IVipService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 统一 VIP 订阅支付回调处理器（bizType="vip"，平台直收类）
 *
 * <p>bizNo 格式：{platform}:{tierCode}:{clientUuid}（网关按 bizType+bizNo 幂等复用，
 * 唯一段保证重复购买可再下单）。支付成功 → 发卡/续费顺延（grantCard）→
 * settlePlatform 平台全额分账 → 站内通知。重复回调由网关侧状态幂等保证。
 *
 * @author moyun
 */
@Component
public class VipPayCallbackHandler implements PayCallbackHandler {

    private static final Logger log = LoggerFactory.getLogger(VipPayCallbackHandler.class);

    @Autowired
    private IVipService vipService;

    @Autowired
    private ILedgerService ledgerService;

    @Autowired
    private INotificationService notificationService;

    @Override
    public String bizType() {
        return "vip";
    }

    @Override
    public void onPaySuccess(PayOrder payOrder) {
        // bizNo = platform:tier:uuid（split limit 3，uuid 内可能含冒号也不受影响）
        String[] parts = payOrder.getBizNo().split(":", 3);
        if (parts.length < 2) {
            throw new IllegalStateException("[vip-callback] bizNo 格式异常：" + payOrder.getBizNo());
        }
        String platformCode = parts[0];
        String tierCode = parts[1];

        // 1. 发卡/续费顺延（事务内）
        vipService.grantCard(payOrder.getUserId(), platformCode, tierCode, payOrder.getId());

        // 2. 平台全额分账（平台直收类，无第三方收款人）
        ledgerService.settlePlatform(payOrder.getPayNo(), "vip", payOrder.getBizNo(),
                payOrder.getAmount(), "VIP订阅-" + platformCode + "/" + tierCode + "-平台所得", platformCode);

        // 3. 订阅用户站内通知
        notificationService.send(payOrder.getUserId(), "pay", payOrder.getPayNo(),
                "会员开通成功",
                "你的「" + platformCode + " · " + tierCode + "」会员已开通，感谢支持。", platformCode);

        log.info("[vip-callback] VIP 发卡闭环完成 payNo={} userId={} platform={} tier={} amount={}元",
                payOrder.getPayNo(), payOrder.getUserId(), platformCode, tierCode, payOrder.getAmount());
    }
}
