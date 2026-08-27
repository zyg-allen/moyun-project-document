package com.moyun.pay.gateway;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.moyun.pay.channel.PayChannel;
import com.moyun.pay.channel.PayChannelRequest;
import com.moyun.pay.channel.PayChannelResponse;
import com.moyun.pay.config.PayProperties;
import com.moyun.pay.domain.entity.PayOrder;
import com.moyun.pay.mapper.PayOrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一支付网关实现（V11.0 公共支付通道核心）
 *
 * <p>职责收口：
 * <ol>
 *   <li>下单幂等：同 bizType+bizNo 已有未关单的 CREATED 单直接复用（重取 codeUrl）</li>
 *   <li>回调驱动状态机：CREATED → PAID 用条件更新防并发重复推进</li>
 *   <li>事务边界：回调业务分发放在 {@link TransactionTemplate} 内，任一环节失败整体回滚</li>
 *   <li>SETTLED 推进：业务 handler 成功后 PAID → SETTLED（条件更新），分账以 SETTLED 为准</li>
 *   <li>关单：先条件更新本系统关单（幂等），再尽力通知渠道（失败仅记日志不阻断）</li>
 *   <li>超时关单：查询/轮询时惰性关单（已过期且仍 CREATED）</li>
 * </ol>
 *
 * @author moyun
 */
@Service
public class PayGatewayImpl implements IPayGateway {

    private static final Logger log = LoggerFactory.getLogger(PayGatewayImpl.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private PayOrderMapper payOrderMapper;

    @Autowired
    private PayProperties payProperties;

    @Autowired
    private TransactionTemplate transactionTemplate;

    /** 渠道实现（按 channelCode 路由；Spring 自动注入所有 PayChannel Bean） */
    @Autowired
    private List<PayChannel> channels;

    /** 业务回调（按 bizType 路由；Spring 自动注入所有 PayCallbackHandler Bean） */
    @Autowired
    private List<PayCallbackHandler> callbackHandlers;

    @Override
    public PayOrder createOrder(String bizType, String bizNo, String channel, long amount, String subject) {
        if (!payProperties.isEnabled()) {
            throw new IllegalStateException("支付功能未开启（moyun.pay.enabled=false）");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("支付金额必须大于 0");
        }
        PayChannel payChannel = routeChannel(channel);

        // 1. 幂等复用：同业务单存在未终态单据直接返回
        PayOrder existing = payOrderMapper.selectOne(new LambdaQueryWrapper<PayOrder>()
                .eq(PayOrder::getBizType, bizType)
                .eq(PayOrder::getBizNo, bizNo)
                .in(PayOrder::getStatus, PayOrder.STATUS_CREATED, PayOrder.STATUS_PAID, PayOrder.STATUS_SETTLED)
                .orderByDesc(PayOrder::getId)
                .last("LIMIT 1"));
        if (existing != null) {
            if (PayOrder.STATUS_CREATED.equals(existing.getStatus())) {
                // 未支付单复用：过期则先关单再重新下单
                if (isExpired(existing)) {
                    closeOrderInternal(existing, "TIMEOUT");
                } else {
                    log.info("[pay-gateway] 复用未支付单 payNo={} bizType={} bizNo={}",
                            existing.getPayNo(), bizType, bizNo);
                    return existing;
                }
            } else {
                // 已支付/已分账：业务层防重复支付由业务单状态保证，这里直接返回现状
                log.warn("[pay-gateway] 业务单已支付，拒绝重复下单 bizType={} bizNo={} status={}",
                        bizType, bizNo, existing.getStatus());
                return existing;
            }
        }

        // 2. 渠道预下单
        String payNo = generatePayNo();
        PayChannelRequest request = new PayChannelRequest();
        request.setPayNo(payNo);
        request.setAmount(amount);
        request.setSubject(subject);
        request.setExpireTime(LocalDateTime.now().plusMinutes(payProperties.getOrderExpireMinutes()));
        PayChannelResponse response = payChannel.prepay(request);

        // 3. 落库
        PayOrder order = new PayOrder();
        order.setPayNo(payNo);
        order.setBizType(bizType);
        order.setBizNo(bizNo);
        order.setChannel(channel);
        order.setAmount(amount);
        order.setSubject(subject);
        order.setStatus(PayOrder.STATUS_CREATED);
        order.setCodeUrl(response.getCodeUrl());
        order.setTradeState(response.getTradeState());
        order.setExpireTime(request.getExpireTime());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        payOrderMapper.insert(order);
        log.info("[pay-gateway] 下单成功 payNo={} channel={} amount={}分 bizType={} bizNo={}",
                payNo, channel, amount, bizType, bizNo);
        return order;
    }

    @Override
    public PayOrder queryStatus(String payNo) {
        PayOrder order = getByPayNo(payNo);
        if (order == null) {
            throw new IllegalArgumentException("支付单不存在：" + payNo);
        }
        // 惰性超时关单：已过期且仍待支付
        if (PayOrder.STATUS_CREATED.equals(order.getStatus()) && isExpired(order)) {
            closeOrderInternal(order, "TIMEOUT");
            order = getByPayNo(payNo);
        }
        return order;
    }

    @Override
    public PayOrder closeOrder(String payNo, String reason) {
        PayOrder order = getByPayNo(payNo);
        if (order == null) {
            throw new IllegalArgumentException("支付单不存在：" + payNo);
        }
        closeOrderInternal(order, reason);
        return getByPayNo(payNo);
    }

    @Override
    public PayOrder getByPayNo(String payNo) {
        return payOrderMapper.selectOne(new LambdaQueryWrapper<PayOrder>()
                .eq(PayOrder::getPayNo, payNo)
                .last("LIMIT 1"));
    }

    @Override
    public Map<String, Object> mockPaySuccess(String payNo) {
        if (!payProperties.getWechat().isMockEnabled()) {
            throw new IllegalStateException("mock 支付未开启（moyun.pay.wechat.mock-enabled=false）");
        }
        PayOrder order = getByPayNo(payNo);
        if (order == null) {
            throw new IllegalArgumentException("支付单不存在：" + payNo);
        }
        Map<String, Object> result = new HashMap<>();
        if (!PayOrder.STATUS_CREATED.equals(order.getStatus())) {
            result.put("payNo", payNo);
            result.put("status", order.getStatus());
            result.put("message", "当前状态不可模拟支付：" + order.getStatus());
            return result;
        }
        // 与真实回调一致的驱动路径：标记渠道支付 → 回调分发
        boolean ok = transactionTemplate.execute(status -> {
            markPaid(order.getPayNo(), "mock-txn-" + System.currentTimeMillis(), LocalDateTime.now());
            dispatchBusiness(order.getPayNo());
            return Boolean.TRUE;
        });
        PayOrder latest = getByPayNo(payNo);
        result.put("payNo", payNo);
        result.put("status", latest == null ? null : latest.getStatus());
        result.put("message", Boolean.TRUE.equals(ok) ? "模拟支付成功" : "模拟支付失败");
        return result;
    }

    /**
     * 回调统一入口（PayCallbackController 调用）：验签 → 落审计 → 状态机 → 事务分发
     *
     * <p>包内可见，供回调控制器使用。
     */
    public void handleNotify(String channelCode, Map<String, String> headers, String body) {
        PayChannel channel = routeChannel(channelCode);
        boolean verified = channel.verifyNotify(headers, body);
        if (!verified) {
            log.warn("[pay-gateway] 回调验签失败 channel={} body={}", channelCode, abbreviate(body));
            throw new IllegalStateException("回调验签失败");
        }
        com.moyun.pay.channel.PayNotifyMessage message = channel.parseNotify(body);
        PayOrder order = getByPayNo(message.getPayNo());
        if (order == null) {
            log.warn("[pay-gateway] 回调未知支付单 payNo={}", message.getPayNo());
            throw new IllegalStateException("未知支付单：" + message.getPayNo());
        }
        if (!"SUCCESS".equalsIgnoreCase(message.getTradeState())) {
            log.info("[pay-gateway] 非成功状态回调，忽略 payNo={} tradeState={}",
                    message.getPayNo(), message.getTradeState());
            return;
        }
        if (!PayOrder.STATUS_CREATED.equals(order.getStatus())) {
            log.info("[pay-gateway] 重复回调幂等返回 payNo={} status={}", order.getPayNo(), order.getStatus());
            return;
        }
        Boolean ok = transactionTemplate.execute(status -> {
            markPaid(order.getPayNo(), message.getChannelOrderNo(), LocalDateTime.now());
            dispatchBusiness(order.getPayNo());
            return Boolean.TRUE;
        });
        log.info("[pay-gateway] 回调处理完成 payNo={} result={}", order.getPayNo(), ok);
    }

    /**
     * CREATED → PAID 条件更新（并发回调天然幂等，0 行即已被处理）
     */
    private void markPaid(String payNo, String channelOrderNo, LocalDateTime payTime) {
        int rows = payOrderMapper.update(null, new LambdaUpdateWrapper<PayOrder>()
                .eq(PayOrder::getPayNo, payNo)
                .eq(PayOrder::getStatus, PayOrder.STATUS_CREATED)
                .set(PayOrder::getStatus, PayOrder.STATUS_PAID)
                .set(PayOrder::getChannelOrderNo, channelOrderNo)
                .set(PayOrder::getTradeState, "SUCCESS")
                .set(PayOrder::getPaySuccessTime, payTime)
                .set(PayOrder::getUpdateTime, LocalDateTime.now()));
        if (rows == 0) {
            throw new IllegalStateException("支付单状态推进失败（可能已被并发处理）：" + payNo);
        }
    }

    /**
     * 业务回调分发（事务内）：路由到 bizType 对应 handler，成功后 PAID → SETTLED
     */
    private void dispatchBusiness(String payNo) {
        PayOrder order = getByPayNo(payNo);
        if (order == null) {
            throw new IllegalStateException("支付单不存在：" + payNo);
        }
        PayCallbackHandler handler = routeHandler(order.getBizType());
        if (handler == null) {
            throw new IllegalStateException("业务类型未注册回调：" + order.getBizType());
        }
        handler.onPaySuccess(order);
        // 业务处理成功 → SETTLED（条件更新，保证分账只被推进一次）
        int rows = payOrderMapper.update(null, new LambdaUpdateWrapper<PayOrder>()
                .eq(PayOrder::getPayNo, payNo)
                .eq(PayOrder::getStatus, PayOrder.STATUS_PAID)
                .set(PayOrder::getStatus, PayOrder.STATUS_SETTLED)
                .set(PayOrder::getSettleTime, LocalDateTime.now())
                .set(PayOrder::getUpdateTime, LocalDateTime.now()));
        if (rows == 0) {
            throw new IllegalStateException("分账状态推进失败（可能已被并发处理）：" + payNo);
        }
        log.info("[pay-gateway] 分账完成 payNo={} bizType={}", payNo, order.getBizType());
    }

    /**
     * 关单内部实现：本系统条件更新关单（幂等）→ 尽力通知渠道
     */
    private void closeOrderInternal(PayOrder order, String reason) {
        if (!PayOrder.STATUS_CREATED.equals(order.getStatus())) {
            log.info("[pay-gateway] 非待支付状态跳过关单 payNo={} status={}", order.getPayNo(), order.getStatus());
            return;
        }
        int rows = payOrderMapper.update(null, new LambdaUpdateWrapper<PayOrder>()
                .eq(PayOrder::getPayNo, order.getPayNo())
                .eq(PayOrder::getStatus, PayOrder.STATUS_CREATED)
                .set(PayOrder::getStatus, PayOrder.STATUS_CLOSED)
                .set(PayOrder::getCloseReason, reason)
                .set(PayOrder::getClosedTime, LocalDateTime.now())
                .set(PayOrder::getTradeState, "CLOSED")
                .set(PayOrder::getUpdateTime, LocalDateTime.now()));
        if (rows == 0) {
            log.info("[pay-gateway] 关单并发冲突（已被处理）payNo={}", order.getPayNo());
            return;
        }
        log.info("[pay-gateway] 关单成功 payNo={} reason={}", order.getPayNo(), reason);
        try {
            routeChannel(order.getChannel()).close(order.getPayNo());
        } catch (Exception e) {
            log.warn("[pay-gateway] 渠道关单失败（不阻断本系统）payNo={} err={}", order.getPayNo(), e.getMessage());
        }
    }

    private PayChannel routeChannel(String channelCode) {
        for (PayChannel channel : channels) {
            if (channel.channelCode().equals(channelCode)) {
                return channel;
            }
        }
        throw new IllegalArgumentException("不支持的支付渠道：" + channelCode);
    }

    private PayCallbackHandler routeHandler(String bizType) {
        for (PayCallbackHandler handler : callbackHandlers) {
            if (handler.bizType().equals(bizType)) {
                return handler;
            }
        }
        return null;
    }

    private boolean isExpired(PayOrder order) {
        return order.getExpireTime() != null && LocalDateTime.now().isAfter(order.getExpireTime());
    }

    private String generatePayNo() {
        String ts = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .format(java.time.LocalDateTime.now());
        StringBuilder sb = new StringBuilder("PAY").append(ts);
        for (int i = 0; i < 6; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    private String abbreviate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= 200 ? text : text.substring(0, 200) + "...";
    }
}
