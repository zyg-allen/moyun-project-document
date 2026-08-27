package com.moyun.portal.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.moyun.pay.domain.entity.LedgerEntry;
import com.moyun.pay.domain.entity.PayOrder;
import com.moyun.pay.gateway.PayCallbackHandler;
import com.moyun.pay.service.ILedgerService;
import com.moyun.pay.service.INotificationService;
import com.moyun.portal.domain.entity.PortalTipOrder;
import com.moyun.portal.mapper.PortalTipOrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 打赏支付回调处理器（V11.0，bizType="tip"）
 *
 * <p>网关支付成功后在事务内调用：打赏单 pending→paid → 复式分账（平台抽成+作者所得）
 * → 双方站内通知。任一步失败整体回滚（网关会因渠道重试再次驱动）。
 *
 * @author moyun
 */
@Component
public class TipPayCallbackHandler implements PayCallbackHandler {

    private static final Logger log = LoggerFactory.getLogger(TipPayCallbackHandler.class);

    @Autowired
    private PortalTipOrderMapper tipOrderMapper;

    @Autowired
    private ILedgerService ledgerService;

    @Autowired
    private INotificationService notificationService;

    @Override
    public String bizType() {
        return "tip";
    }

    @Override
    public void onPaySuccess(PayOrder payOrder) {
        Long tipOrderId = Long.valueOf(payOrder.getBizNo());
        PortalTipOrder tipOrder = tipOrderMapper.selectById(tipOrderId);
        if (tipOrder == null) {
            throw new IllegalStateException("打赏单不存在：" + tipOrderId);
        }
        // 已支付幂等返回（渠道重试/并发保护）
        if ("paid".equals(tipOrder.getStatus())) {
            log.info("[tip-callback] 打赏单已支付，幂等返回 tipOrderId={}", tipOrderId);
            return;
        }
        if (!"pending".equals(tipOrder.getStatus())) {
            throw new IllegalStateException("打赏单状态异常：" + tipOrder.getStatus());
        }

        // 1. 打赏单 pending → paid（条件更新）
        int rows = tipOrderMapper.update(null, new LambdaUpdateWrapper<PortalTipOrder>()
                .eq(PortalTipOrder::getId, tipOrderId)
                .eq(PortalTipOrder::getStatus, "pending")
                .set(PortalTipOrder::getStatus, "paid")
                .set(PortalTipOrder::getPaidTime, LocalDateTime.now()));
        if (rows == 0) {
            throw new IllegalStateException("打赏单状态推进失败（可能已被并发处理）：" + tipOrderId);
        }

        // 2. 复式分账：平台抽成 + 作者所得（金额守恒）
        List<LedgerEntry> entries = ledgerService.settle(
                payOrder.getPayNo(), "tip", String.valueOf(tipOrderId),
                payOrder.getAmount(), tipOrder.getAuthorId(), "打赏");

        // 3. 双方站内通知（事务内，与分账同成败）
        long platformAmount = 0;
        long authorAmount = 0;
        for (LedgerEntry entry : entries) {
            if (LedgerEntry.ROLE_PLATFORM.equals(entry.getAccountRole())) {
                platformAmount = entry.getAmount();
            } else if (LedgerEntry.ROLE_USER.equals(entry.getAccountRole())
                    && LedgerEntry.DIRECTION_CREDIT.equals(entry.getDirection())) {
                authorAmount = entry.getAmount();
            }
        }
        BigDecimal amountYuan = BigDecimal.valueOf(payOrder.getAmount(), 2);
        BigDecimal authorYuan = BigDecimal.valueOf(authorAmount, 2);
        // 打赏者：支付成功
        notificationService.send(tipOrder.getUserId(), "pay", payOrder.getPayNo(),
                "打赏支付成功",
                "你向 " + tipOrder.getTargetType() + " 打赏的 " + amountYuan + " 元已支付成功，感谢对创作者的支持。");
        // 作者：到账通知（含分账明细）
        notificationService.send(tipOrder.getAuthorId(), "account", payOrder.getPayNo(),
                "收到一笔打赏",
                "你收到一笔 " + amountYuan + " 元打赏，扣除平台服务费后实际到账 " + authorYuan
                        + " 元，已计入钱包余额。");
        log.info("[tip-callback] 打赏闭环完成 tipOrderId={} payNo={} amount={}分 author={}分 platform={}分",
                tipOrderId, payOrder.getPayNo(), payOrder.getAmount(), authorAmount, platformAmount);
    }
}
