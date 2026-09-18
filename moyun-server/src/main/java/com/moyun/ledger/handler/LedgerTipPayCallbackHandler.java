package com.moyun.ledger.handler;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.moyun.ledger.domain.entity.LedgerTipOrder;
import com.moyun.ledger.mapper.LedgerTipOrderMapper;
import com.moyun.pay.domain.entity.LedgerEntry;
import com.moyun.pay.domain.entity.PayOrder;
import com.moyun.pay.gateway.PayCallbackHandler;
import com.moyun.pay.service.ILedgerService;
import com.moyun.pay.service.INotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 记账App打赏支付回调处理器（bizType="ledger_tip"）
 *
 * <p>网关支付成功后在事务内调用：打赏单 pending→paid → 平台全额分账
 * （无第三方收款人，走 settlePlatform 单条 PLATFORM/credit 流水）→ 打赏者站内通知。
 * 任一步失败整体回滚（网关会因渠道重试再次驱动）。
 *
 * <p>与门户打赏回调（TipPayCallbackHandler, bizType="tip"）互不冲突：本处理器
 * 只处理 platformCode=ledger 的记账App打赏单，按 bizNo 查 ledger_tip_order。
 *
 * @author moyun
 */
@Component
public class LedgerTipPayCallbackHandler implements PayCallbackHandler {

    private static final Logger log = LoggerFactory.getLogger(LedgerTipPayCallbackHandler.class);

    @Autowired
    private LedgerTipOrderMapper tipOrderMapper;

    @Autowired
    private ILedgerService ledgerService;

    @Autowired
    private INotificationService notificationService;

    @Override
    public String bizType() {
        return "ledger_tip";
    }

    @Override
    public void onPaySuccess(PayOrder payOrder) {
        Long tipOrderId = Long.valueOf(payOrder.getBizNo());
        LedgerTipOrder tipOrder = tipOrderMapper.selectById(tipOrderId);
        if (tipOrder == null) {
            throw new IllegalStateException("记账App打赏单不存在：" + tipOrderId);
        }
        // 已支付幂等返回（渠道重试/并发保护）
        if (LedgerTipOrder.STATUS_PAID.equals(tipOrder.getStatus())) {
            log.info("[ledger-tip-callback] 打赏单已支付，幂等返回 tipOrderId={}", tipOrderId);
            return;
        }
        if (!LedgerTipOrder.STATUS_PENDING.equals(tipOrder.getStatus())) {
            throw new IllegalStateException("记账App打赏单状态异常：" + tipOrder.getStatus());
        }

        // 1. 打赏单 pending → paid（条件更新，防并发）
        int rows = tipOrderMapper.update(null, new LambdaUpdateWrapper<LedgerTipOrder>()
                .eq(LedgerTipOrder::getId, tipOrderId)
                .eq(LedgerTipOrder::getStatus, LedgerTipOrder.STATUS_PENDING)
                .set(LedgerTipOrder::getStatus, LedgerTipOrder.STATUS_PAID)
                .set(LedgerTipOrder::getPaidTime, LocalDateTime.now()));
        if (rows == 0) {
            throw new IllegalStateException("记账App打赏单状态推进失败（可能已被并发处理）：" + tipOrderId);
        }

        // 2. 平台全额分账（打赏对象为开发者/平台自身，无第三方收款人，守恒：全额=平台所得）
        LedgerEntry platformEntry = ledgerService.settlePlatform(
                payOrder.getPayNo(), "ledger_tip", String.valueOf(tipOrderId),
                payOrder.getAmount(), "记账App赞赏-平台所得", "ledger");

        // 3. 打赏者站内通知（事务内，与分账同成败）
        notificationService.send(tipOrder.getUserId(), "pay", payOrder.getPayNo(),
                "赞赏支付成功",
                "你的 " + payOrder.getAmount() + " 元赞赏已支付成功，感谢对记账App的支持。", "ledger");

        log.info("[ledger-tip-callback] 打赏闭环完成 tipOrderId={} payNo={} amount={}元 platform={}元",
                tipOrderId, payOrder.getPayNo(), payOrder.getAmount(), platformEntry.getAmount());
    }
}
