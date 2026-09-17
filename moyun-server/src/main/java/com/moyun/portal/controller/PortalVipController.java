package com.moyun.portal.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.pay.config.PayProperties;
import com.moyun.pay.domain.entity.PayOrder;
import com.moyun.pay.gateway.IPayGateway;
import com.moyun.portal.util.PortalSecurityUtils;
import com.moyun.vip.domain.entity.VipTier;
import com.moyun.vip.mapper.VipTierMapper;
import com.moyun.vip.service.IVipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 门户端统一会员 Controller（替代旧 interviewVip / resumeOptimizeVip 两套订阅体系）
 *
 * <p>链路：GET /tiers 等级+权益清单（售卖页）→ GET /status 我的会员详情 →
 * POST /subscribe 下单（bizType='vip'，bizNo='portal:{tier}:{uuid}'，网关幂等复用）
 * → 返回收银台参数（前端跳 /pay/cashier）；支付成功由 VipPayCallbackHandler
 * 发卡 + settlePlatform 平台全额分账 + 站内通知。
 *
 * @author moyun
 */
@Tag(name = "门户会员", description = "门户端统一会员（等级/详情/订阅）")
@RestController
@RequestMapping("/portal/vip")
public class PortalVipController extends BaseController {

    /** 门户端代码 */
    private static final String PLATFORM = "portal";

    @Autowired
    private IVipService vipService;

    @Autowired
    private VipTierMapper tierMapper;

    @Autowired
    private IPayGateway payGateway;

    @Autowired
    private PayProperties payProperties;

    /** 等级列表 + 各等级权益清单（游客可看，售卖页展示） */
    @Operation(summary = "等级与权益清单")
    @GetMapping("/tiers")
    public AjaxResult tiers() {
        return AjaxResult.success(Map.of("records", vipService.listTiers(PLATFORM)));
    }

    /** 我的会员详情（等级/到期/各权益已用剩余） */
    @Operation(summary = "我的会员详情")
    @GetMapping("/status")
    public AjaxResult status() {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error(401, "登录已过期，请重新登录");
        }
        return AjaxResult.success(vipService.getVipDetail(userId, PLATFORM));
    }

    /**
     * 订阅下单（公共通道，返回收银台参数）
     *
     * <p>body：{ tierCode, clientUuid }；clientUuid 幂等（同 uuid 复用未支付单），
     * 缺省由服务端生成（每次点击即新单）。
     */
    @Operation(summary = "订阅下单")
    @PostMapping("/subscribe")
    public AjaxResult subscribe(@RequestBody Map<String, Object> body) {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error(401, "登录已过期，请重新登录");
        }
        String tierCode = body.get("tierCode") == null ? null : String.valueOf(body.get("tierCode"));
        String clientUuid = body.get("clientUuid") == null || String.valueOf(body.get("clientUuid")).isBlank()
                ? UUID.randomUUID().toString().replace("-", "")
                : String.valueOf(body.get("clientUuid"));
        if (tierCode == null || tierCode.isBlank()) {
            throw new ServiceException("请选择会员等级");
        }

        // 1. 等级校验（上架中且为付费等级）
        VipTier tier = tierMapper.selectOne(new LambdaQueryWrapper<VipTier>()
                .eq(VipTier::getPlatformCode, PLATFORM)
                .eq(VipTier::getTierCode, tierCode)
                .last("LIMIT 1"));
        if (tier == null || tier.getStatus() == null || tier.getStatus() != 1) {
            throw new ServiceException("会员等级不存在或已下架");
        }
        if (tier.getPrice() == null || tier.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("该等级无需购买");
        }

        // 2. 网关统一下单（bizNo=platform:tier:uuid，同 clientUuid 复用未支付单）
        String bizNo = PLATFORM + ":" + tierCode + ":" + clientUuid;
        PayOrder payOrder = payGateway.createOrder("vip", bizNo, userId, PLATFORM,
                "wechat", tier.getPrice(), "会员订阅-" + tier.getTierName());
        if (PayOrder.STATUS_PAID.equals(payOrder.getStatus())
                || PayOrder.STATUS_SETTLED.equals(payOrder.getStatus())) {
            throw new ServiceException("该会员已支付成功，请勿重复提交");
        }

        // 3. 收银台参数（前端跳 /pay/cashier）
        Map<String, Object> result = new HashMap<>();
        result.put("amount", payOrder.getAmount());
        result.put("tierName", tier.getTierName());
        result.put("durationDays", tier.getDurationDays());
        result.put("payNo", payOrder.getPayNo());
        result.put("codeUrl", payOrder.getCodeUrl());
        result.put("expireTime", payOrder.getExpireTime());
        result.put("mockEnabled", payProperties.getWechat().isMockEnabled());
        return AjaxResult.success(result);
    }
}
