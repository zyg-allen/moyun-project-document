package com.moyun.ledger.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.ledger.domain.entity.LedgerTipOrder;
import com.moyun.ledger.mapper.LedgerTipOrderMapper;
import com.moyun.ledger.service.ILedgerTipService;
import com.moyun.pay.config.PayProperties;
import com.moyun.pay.domain.entity.PayOrder;
import com.moyun.pay.gateway.IPayGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 记账App打赏服务实现（接入公共支付通道）
 *
 * <p>链路：校验（金额区间/小数位/幂等号）→ 落 pending 打赏单 → payGateway 统一下单
 * (bizType=ledger_tip, platform=ledger_app, channel=wechat) → 回填 pay_no → 返回收银台参数。
 * 支付成功由 LedgerTipPayCallbackHandler 在网关回调事务内推进：pending→paid + 平台全额分账。
 *
 * <p>金额单位：元（BigDecimal，统一）；状态枚举 统一字符串。
 *
 * @author moyun
 */
@Service
public class LedgerTipServiceImpl extends ServiceImpl<LedgerTipOrderMapper, LedgerTipOrder>
        implements ILedgerTipService {

    /** 单笔上限（元，与门户打赏 0.01~10000 标准对齐） */
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("10000");

    @Autowired
    private IPayGateway payGateway;

    @Autowired
    private PayProperties payProperties;

    @Override
    public Map<String, Object> createTipOrder(Long userId, BigDecimal amount, String payChannel,
                                              String target, String reason, String clientUuid) {
        // 1. 金额校验（对齐全平台标准：>0、scale≤2、上限与门户打赏一致）
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("请输入有效的赞赏金额");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new ServiceException("赞赏金额最多两位小数");
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new ServiceException("单笔赞赏不可超过 10000 元");
        }

        // 2. clientUuid 幂等（对齐记一笔防重机制）
        if (clientUuid != null && !clientUuid.isBlank()) {
            LedgerTipOrder existing = this.getOne(new LambdaQueryWrapper<LedgerTipOrder>()
                    .eq(LedgerTipOrder::getClientUuid, clientUuid)
                    .orderByDesc(LedgerTipOrder::getId)
                    .last("LIMIT 1"));
            if (existing != null) {
                if (LedgerTipOrder.STATUS_PAID.equals(existing.getStatus())) {
                    throw new ServiceException("该笔赞赏已支付，请勿重复提交");
                }
                // pending 单：复用（网关同 bizType+bizNo 未支付单复用/过期自动重下，重取 codeUrl）
                PayOrder reused = payGateway.createOrder("ledger_tip", String.valueOf(existing.getId()),
                        existing.getUserId(), "ledger_app", existing.getPayChannel(), existing.getAmount(),
                        "记账App赞赏-" + existing.getTarget());
                existing.setPayNo(reused.getPayNo());
                this.updateById(existing);
                return cashierParams(existing);
            }
        }

        // 3. 落 pending 打赏单（支付成功由回调推进，不再直落 paid）
        LedgerTipOrder order = new LedgerTipOrder();
        order.setUserId(userId);
        order.setAmount(amount);
        // 渠道标准化：当前公共通道仅开通微信（alipay 通道接入后放开）
        order.setPayChannel("alipay".equals(payChannel) ? "alipay" : "wechat");
        order.setTarget(LedgerTipOrder.TARGET_PLATFORM.equals(target)
                ? LedgerTipOrder.TARGET_PLATFORM : LedgerTipOrder.TARGET_DEVELOPER);
        order.setReason(reason);
        order.setClientUuid(clientUuid);
        order.setStatus(LedgerTipOrder.STATUS_PENDING);
        order.setCreateTime(LocalDateTime.now());
        this.save(order);

        // 4. 网关统一下单（幂等：同 bizType+bizNo 未支付单复用；透传 userId/platform 对账维度）
        String subject = "记账App赞赏-" + order.getTarget();
        PayOrder payOrder = payGateway.createOrder("ledger_tip", String.valueOf(order.getId()),
                userId, "ledger_app", order.getPayChannel(), amount, subject);

        // 5. 回填通道单据号并返回收银台参数
        order.setPayNo(payOrder.getPayNo());
        this.updateById(order);
        return cashierParams(order);
    }

    /** 收银台参数（payNo/codeUrl/amount/expireTime/tipOrderId/mockEnabled） */
    private Map<String, Object> cashierParams(LedgerTipOrder order) {
        PayOrder payOrder = order.getPayNo() == null ? null : payGateway.queryStatus(order.getPayNo());
        Map<String, Object> result = new HashMap<>();
        result.put("tipOrderId", order.getId());
        result.put("amount", order.getAmount());
        result.put("status", order.getStatus());
        if (payOrder != null) {
            result.put("payNo", payOrder.getPayNo());
            result.put("codeUrl", payOrder.getCodeUrl());
            result.put("expireTime", payOrder.getExpireTime());
        }
        result.put("mockEnabled", payProperties.getWechat().isMockEnabled());
        return result;
    }

    @Override
    public BigDecimal totalAmount(Long userId) {
        // SQL SUM 聚合（替代原 selectList 内存累加，遵守"禁止全表内存聚合"铁律）
        List<Map<String, Object>> rows = this.listMaps(new QueryWrapper<LedgerTipOrder>()
                .select("COALESCE(SUM(amount), 0) AS total")
                .eq("user_id", userId)
                .eq("status", LedgerTipOrder.STATUS_PAID));
        if (rows == null || rows.isEmpty() || rows.get(0) == null) {
            return BigDecimal.ZERO;
        }
        Object total = rows.get(0).get("total");
        return total == null ? BigDecimal.ZERO : new BigDecimal(total.toString());
    }

    @Override
    public Page<LedgerTipOrder> myTips(Long userId, long current, long size) {
        return this.page(new Page<>(current, size), new LambdaQueryWrapper<LedgerTipOrder>()
                .eq(LedgerTipOrder::getUserId, userId)
                .orderByDesc(LedgerTipOrder::getId));
    }
}
