package com.moyun.portal.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.pay.config.PayProperties;
import com.moyun.pay.domain.entity.PayOrder;
import com.moyun.pay.gateway.IPayGateway;
import com.moyun.portal.domain.entity.PortalResumeOptimizeOrder;
import com.moyun.portal.domain.entity.PortalResumeOptimizePackage;
import com.moyun.portal.mapper.PortalResumeOptimizeOrderMapper;
import com.moyun.portal.mapper.PortalResumeOptimizePackageMapper;
import com.moyun.portal.service.PortalFreeTrialService;
import com.moyun.portal.util.PortalSecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 门户简历优化-会员订阅控制器（接入公共支付通道）
 *
 * <p>链路：GET /packages 上架套餐列表（后台可配价格）→ POST /subscribe 下单
 * （快照套餐名/时长，clientUuid 幂等）→ 落 pending 单 → payGateway 统一下单
 * (bizType=resume_optimize, platform=portal) → 返回收银台参数（前端跳 /pay/cashier）；
 * 支付状态轮询复用 /portal/pay/status/{payNo}，mock 模拟支付复用 /portal/pay/mock/{payNo}；
 * 支付成功由 ResumeOptimizeVipPayCallbackHandler 在回调事务内推进 pending→paid +
 * 权益顺延 + settlePlatform 平台全额分账。
 *
 * <p>会员权益：深度优化提交接口（同步 /deep 与异步 /deep/async）校验会员身份，
 * 未开通返回错误码引导前端跳转购买页。
 *
 * @author moyun
 */
@Tag(name = "门户简历优化会员", description = "简历深度优化会员订阅（套餐/状态/下单）")
@RestController
@RequestMapping("/portal/resume/optimize/vip")
public class PortalResumeOptimizeVipController extends BaseController {

    /** 未开通会员错误码（前端据此跳转购买页） */
    public static final String CODE_NOT_VIP = "RESUME_VIP_REQUIRED";

    @Autowired
    private PortalResumeOptimizePackageMapper packageMapper;

    @Autowired
    private PortalResumeOptimizeOrderMapper orderMapper;

    @Autowired
    private IPayGateway payGateway;

    @Autowired
    private PayProperties payProperties;

    /** 免费体验次数服务（非会员每场景 2 次） */
    @Autowired
    private PortalFreeTrialService freeTrialService;

    /** 上架套餐列表（简历优化会员页展示，价格后台可配） */
    @Operation(summary = "上架套餐列表")
    @GetMapping("/packages")
    public AjaxResult packages() {
        List<PortalResumeOptimizePackage> list = packageMapper.selectList(new LambdaQueryWrapper<PortalResumeOptimizePackage>()
                .eq(PortalResumeOptimizePackage::getStatus, Boolean.TRUE)
                .orderByAsc(PortalResumeOptimizePackage::getSort)
                .orderByAsc(PortalResumeOptimizePackage::getId));
        return AjaxResult.success(Map.of("records", list));
    }

    /** 我的会员状态（isVip/vipExpire：按已支付订单权益到期最大值，SQL MAX 聚合） */
    @Operation(summary = "我的会员状态")
    @GetMapping("/status")
    public AjaxResult status() {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error(401, "登录已过期，请重新登录");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("isVip", isVip(userId));
        data.put("vipExpire", currentVipExpire(userId));
        // 非会员剩余免费体验次数（简历深度优化每用户 2 次）
        data.put("freeTrialLeft", freeTrialService.leftTimes(userId, PortalFreeTrialService.SCENE_RESUME_DEEP));
        return AjaxResult.success(data);
    }

    /**
     * 订阅下单（公共通道：pending 单 + 网关统一下单，返回收银台参数）
     */
    @Operation(summary = "订阅下单")
    @PostMapping("/subscribe")
    public AjaxResult subscribe(@RequestBody Map<String, Object> body) {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error(401, "登录已过期，请重新登录");
        }
        Long packageId = Long.valueOf(String.valueOf(body.get("packageId")));
        String clientUuid = body.get("clientUuid") == null ? null : String.valueOf(body.get("clientUuid"));

        // 1. 套餐校验（上架中；快照 name/duration/price）
        PortalResumeOptimizePackage pkg = packageMapper.selectById(packageId);
        if (pkg == null || !Boolean.TRUE.equals(pkg.getStatus())) {
            throw new ServiceException("套餐不存在或已下架");
        }

        // 2. clientUuid 幂等（对齐打赏/面试VIP防重机制）
        if (clientUuid != null && !clientUuid.isBlank()) {
            PortalResumeOptimizeOrder existing = orderMapper.selectOne(new LambdaQueryWrapper<PortalResumeOptimizeOrder>()
                    .eq(PortalResumeOptimizeOrder::getClientUuid, clientUuid)
                    .orderByDesc(PortalResumeOptimizeOrder::getId)
                    .last("LIMIT 1"));
            if (existing != null) {
                if (PortalResumeOptimizeOrder.STATUS_PAID.equals(existing.getStatus())) {
                    throw new ServiceException("该套餐已订阅成功，请勿重复提交");
                }
                // pending 单：网关同 bizType+bizNo 复用（过期自动重下，重取 codeUrl）
                PayOrder reused = payGateway.createOrder("resume_optimize", String.valueOf(existing.getId()),
                        userId, "portal", existing.getPayChannel(), existing.getAmount(),
                        "简历优化会员-" + existing.getPackageName());
                existing.setPayNo(reused.getPayNo());
                orderMapper.updateById(existing);
                return AjaxResult.success(cashierParams(existing, reused));
            }
        }

        // 3. 落 pending 订单（快照套餐信息）
        PortalResumeOptimizeOrder order = new PortalResumeOptimizeOrder();
        order.setUserId(userId);
        order.setPackageId(pkg.getId());
        order.setPackageName(pkg.getName());
        order.setDurationDays(pkg.getDurationDays());
        order.setAmount(pkg.getPrice());
        order.setPayChannel("wechat");
        order.setClientUuid(clientUuid);
        order.setStatus(PortalResumeOptimizeOrder.STATUS_PENDING);
        order.setCreateTime(LocalDateTime.now());
        orderMapper.insert(order);

        // 4. 网关统一下单
        PayOrder payOrder = payGateway.createOrder("resume_optimize", String.valueOf(order.getId()),
                userId, "portal", order.getPayChannel(), order.getAmount(),
                "简历优化会员-" + order.getPackageName());

        // 5. 回填通道单据号并返回收银台参数
        order.setPayNo(payOrder.getPayNo());
        orderMapper.updateById(order);
        return AjaxResult.success(cashierParams(order, payOrder));
    }

    /** 收银台参数（payNo/codeUrl/amount/expireTime/orderId/mockEnabled，前端跳 /pay/cashier） */
    private Map<String, Object> cashierParams(PortalResumeOptimizeOrder order, PayOrder payOrder) {
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

    /** 我的当前权益到期时间（按已支付订单 MAX(vip_expire)，SQL 聚合） */
    private LocalDateTime currentVipExpire(Long userId) {
        List<Map<String, Object>> rows = orderMapper.selectMaps(new QueryWrapper<PortalResumeOptimizeOrder>()
                .select("COALESCE(MAX(vip_expire), NULL) AS expire")
                .eq("user_id", userId)
                .eq("status", PortalResumeOptimizeOrder.STATUS_PAID));
        return (rows != null && !rows.isEmpty() && rows.get(0) != null)
                ? (LocalDateTime) rows.get(0).get("expire") : null;
    }

    /** 是否有效会员（存在未到期的 vip_expire） */
    public boolean isVip(Long userId) {
        LocalDateTime expire = currentVipExpire(userId);
        return expire != null && expire.isAfter(LocalDateTime.now());
    }
}
