package com.moyun.ledger.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.moyun.core.base.AjaxResult;
import com.moyun.ledger.domain.entity.LedgerVipOrder;
import com.moyun.ledger.domain.entity.LedgerVipPackage;
import com.moyun.ledger.mapper.LedgerVipOrderMapper;
import com.moyun.ledger.mapper.LedgerVipPackageMapper;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.pay.config.PayProperties;
import com.moyun.pay.domain.entity.PayOrder;
import com.moyun.pay.gateway.IPayGateway;
import com.moyun.portal.util.PortalSecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 门户记账-VIP订阅控制器（v11.81 接入公共支付通道）
 *
 * <p>链路：GET /packages 上架套餐列表（后台可配价格）→ POST /subscribe 下单
 * （快照套餐名/时长，clientUuid 幂等）→ 落 pending 单 → payGateway 统一下单
 * (bizType=ledger_vip, platform=ledger_app) → 返回收银台参数；支付状态轮询复用
 * /portal/pay/status/{payNo}，mock 模拟支付复用 /portal/pay/mock/{payNo}；
 * 支付成功由 LedgerVipPayCallbackHandler 在回调事务内推进 pending→paid +
 * 权益顺延 + 平台全额分账。
 *
 * @author moyun
 */
@RestController
@RequestMapping("/portal/ledger/vip")
public class PortalLedgerVipController {

    @Autowired
    private LedgerVipPackageMapper packageMapper;

    @Autowired
    private LedgerVipOrderMapper orderMapper;

    @Autowired
    private IPayGateway payGateway;

    @Autowired
    private PayProperties payProperties;

    /** 上架套餐列表（App 会员页展示，价格后台可配） */
    @GetMapping("/packages")
    public AjaxResult packages() {
        List<LedgerVipPackage> list = packageMapper.selectList(new LambdaQueryWrapper<LedgerVipPackage>()
                .eq(LedgerVipPackage::getStatus, Boolean.TRUE)
                .orderByAsc(LedgerVipPackage::getSort)
                .orderByAsc(LedgerVipPackage::getId));
        return AjaxResult.success(Map.of("records", list));
    }

    /** 我的会员状态（isVip/vipExpire：按已支付订单权益到期最大值） */
    @GetMapping("/status")
    public AjaxResult status() {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error(401, "登录已过期，请重新登录");
        }
        // SQL MAX 聚合（避免全量内存比较）
        List<Map<String, Object>> rows = orderMapper.selectMaps(new QueryWrapper<LedgerVipOrder>()
                .select("COALESCE(MAX(vip_expire), NULL) AS expire")
                .eq("user_id", userId)
                .eq("status", LedgerVipOrder.STATUS_PAID));
        LocalDateTime expire = (rows != null && !rows.isEmpty() && rows.get(0) != null)
                ? (LocalDateTime) rows.get(0).get("expire") : null;
        Map<String, Object> data = new HashMap<>();
        data.put("isVip", expire != null && expire.isAfter(LocalDateTime.now()));
        data.put("vipExpire", expire);
        return AjaxResult.success(data);
    }

    /**
     * 订阅下单（V11.81 公共通道：pending 单 + 网关统一下单，返回收银台参数）
     */
    @PostMapping("/subscribe")
    public AjaxResult subscribe(@RequestBody Map<String, Object> body) {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error(401, "登录已过期，请重新登录");
        }
        Long packageId = Long.valueOf(String.valueOf(body.get("packageId")));
        String clientUuid = body.get("clientUuid") == null ? null : String.valueOf(body.get("clientUuid"));

        // 1. 套餐校验（上架中；快照 name/duration/price）
        LedgerVipPackage pkg = packageMapper.selectById(packageId);
        if (pkg == null || !Boolean.TRUE.equals(pkg.getStatus())) {
            throw new ServiceException("套餐不存在或已下架");
        }

        // 2. clientUuid 幂等（对齐打赏/记一笔防重机制）
        if (clientUuid != null && !clientUuid.isBlank()) {
            LedgerVipOrder existing = orderMapper.selectOne(new LambdaQueryWrapper<LedgerVipOrder>()
                    .eq(LedgerVipOrder::getClientUuid, clientUuid)
                    .orderByDesc(LedgerVipOrder::getId)
                    .last("LIMIT 1"));
            if (existing != null) {
                if (LedgerVipOrder.STATUS_PAID.equals(existing.getStatus())) {
                    throw new ServiceException("该套餐已订阅成功，请勿重复提交");
                }
                // pending 单：网关同 bizType+bizNo 复用（过期自动重下，重取 codeUrl）
                PayOrder reused = payGateway.createOrder("ledger_vip", String.valueOf(existing.getId()),
                        userId, "ledger_app", existing.getPayChannel(), existing.getAmount(),
                        "记账VIP-" + existing.getPackageName());
                existing.setPayNo(reused.getPayNo());
                orderMapper.updateById(existing);
                return AjaxResult.success(cashierParams(existing, reused));
            }
        }

        // 3. 落 pending 订单（快照套餐信息）
        LedgerVipOrder order = new LedgerVipOrder();
        order.setUserId(userId);
        order.setPackageId(pkg.getId());
        order.setPackageName(pkg.getName());
        order.setDurationDays(pkg.getDurationDays());
        order.setAmount(pkg.getPrice());
        order.setPayChannel("wechat");
        order.setClientUuid(clientUuid);
        order.setStatus(LedgerVipOrder.STATUS_PENDING);
        order.setCreateTime(LocalDateTime.now());
        orderMapper.insert(order);

        // 4. 网关统一下单
        PayOrder payOrder = payGateway.createOrder("ledger_vip", String.valueOf(order.getId()),
                userId, "ledger_app", order.getPayChannel(), order.getAmount(),
                "记账VIP-" + order.getPackageName());

        // 5. 回填通道单据号并返回收银台参数
        order.setPayNo(payOrder.getPayNo());
        orderMapper.updateById(order);
        return AjaxResult.success(cashierParams(order, payOrder));
    }

    /** 收银台参数（payNo/codeUrl/amount/expireTime/OrderId/mockEnabled） */
    private Map<String, Object> cashierParams(LedgerVipOrder order, PayOrder payOrder) {
        Map<String, Object> result = new HashMap<>();
        result.put("vipOrderId", order.getId());
        result.put("amount", order.getAmount());
        result.put("packageName", order.getPackageName());
        result.put("status", order.getStatus());
        result.put("payNo", payOrder.getPayNo());
        result.put("codeUrl", payOrder.getCodeUrl());
        result.put("expireTime", payOrder.getExpireTime());
        result.put("mockEnabled", payProperties.getWechat().isMockEnabled());
        return result;
    }
}
