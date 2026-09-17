package com.moyun.portal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.moyun.pay.domain.entity.PayOrder;
import com.moyun.pay.gateway.PayCallbackHandler;
import com.moyun.pay.service.ILedgerService;
import com.moyun.pay.service.INotificationService;
import com.moyun.portal.domain.entity.PortalInterviewVipOrder;
import com.moyun.portal.mapper.PortalInterviewVipOrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 面试会员订阅支付回调处理器（v11.82，bizType="interview_vip"，平台直收类）
 *
 * <p>网关支付成功后在事务内调用：订单 pending→paid → 权益顺延发放
 * （vip_expire 从 max(now, 现有到期) 顺延 duration_days，续费不折损）→
 * settlePlatform 平台全额分账（无第三方收款人）→ 订阅用户站内通知。
 * 任一步失败整体回滚（网关会因渠道重试再次驱动）。
 *
 * <p>权益解锁功能点（语音面试场次、深度面试报告、题库全量访问）按骨架预留，
 * 各功能读 /portal/interview/vip/status 的 isVip 判断即可。
 *
 * @author moyun
 */
@Component
public class InterviewVipPayCallbackHandler implements PayCallbackHandler {

    private static final Logger log = LoggerFactory.getLogger(InterviewVipPayCallbackHandler.class);

    @Autowired
    private PortalInterviewVipOrderMapper orderMapper;

    @Autowired
    private ILedgerService ledgerService;

    @Autowired
    private INotificationService notificationService;

    @Override
    public String bizType() {
        return "interview_vip";
    }

    @Override
    public void onPaySuccess(PayOrder payOrder) {
        Long vipOrderId = Long.valueOf(payOrder.getBizNo());
        PortalInterviewVipOrder order = orderMapper.selectById(vipOrderId);
        if (order == null) {
            throw new IllegalStateException("面试会员订单不存在：" + vipOrderId);
        }
        // 已支付幂等返回（渠道重试/并发保护）
        if (PortalInterviewVipOrder.STATUS_PAID.equals(order.getStatus())) {
            log.info("[interview-vip-callback] 会员订单已支付，幂等返回 vipOrderId={}", vipOrderId);
            return;
        }
        if (!PortalInterviewVipOrder.STATUS_PENDING.equals(order.getStatus())) {
            throw new IllegalStateException("面试会员订单状态异常：" + order.getStatus());
        }

        // 1. 权益顺延：vip_expire = max(now, 现有到期) + duration_days（续费不折损；SQL MAX 聚合）
        // 注意：首单无已支付订单时聚合返回全 NULL 行，MyBatis selectMaps 会给出 [null] 元素，需过滤
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentExpire = orderMapper.selectMaps(new QueryWrapper<PortalInterviewVipOrder>()
                        .select("COALESCE(MAX(vip_expire), NULL) AS expire")
                        .eq("user_id", order.getUserId())
                        .eq("status", PortalInterviewVipOrder.STATUS_PAID))
                .stream()
                .filter(java.util.Objects::nonNull)
                .map(m -> (LocalDateTime) m.get("expire"))
                .filter(e -> e != null && e.isAfter(now))
                .findFirst()
                .orElse(null);
        LocalDateTime vipStart = currentExpire == null ? now : currentExpire;
        LocalDateTime vipExpire = vipStart.plusDays(order.getDurationDays());

        // 2. 订单 pending → paid + 权益落单（条件更新，防并发）
        int rows = orderMapper.update(null, new LambdaUpdateWrapper<PortalInterviewVipOrder>()
                .eq(PortalInterviewVipOrder::getId, vipOrderId)
                .eq(PortalInterviewVipOrder::getStatus, PortalInterviewVipOrder.STATUS_PENDING)
                .set(PortalInterviewVipOrder::getStatus, PortalInterviewVipOrder.STATUS_PAID)
                .set(PortalInterviewVipOrder::getVipStart, vipStart)
                .set(PortalInterviewVipOrder::getVipExpire, vipExpire)
                .set(PortalInterviewVipOrder::getPaidTime, now));
        if (rows == 0) {
            throw new IllegalStateException("面试会员订单状态推进失败（可能已被并发处理）：" + vipOrderId);
        }

        // 3. 平台全额分账（平台直收类，无第三方收款人）
        ledgerService.settlePlatform(payOrder.getPayNo(), "interview_vip", String.valueOf(vipOrderId),
                payOrder.getAmount(), "面试会员订阅-" + order.getPackageName() + "-平台所得");

        // 4. 订阅用户站内通知（事务内，与分账同成败）
        notificationService.send(order.getUserId(), "pay", payOrder.getPayNo(),
                "面试会员开通成功",
                "你的「" + order.getPackageName() + "」已开通，权益有效期至 " + vipExpire.toLocalDate() + "。");

        log.info("[interview-vip-callback] 面试会员闭环完成 vipOrderId={} payNo={} amount={}元 权益至{}",
                vipOrderId, payOrder.getPayNo(), payOrder.getAmount(), vipExpire);
    }
}
