package com.moyun.ledger.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.core.base.AjaxResult;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.pay.config.PayProperties;
import com.moyun.pay.domain.entity.PayOrder;
import com.moyun.pay.gateway.IPayGateway;
import com.moyun.portal.util.PortalSecurityUtils;
import com.moyun.vip.domain.entity.VipTier;
import com.moyun.vip.domain.entity.VipUserCard;
import com.moyun.vip.mapper.VipTierMapper;
import com.moyun.vip.mapper.VipUserCardMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 门户记账-VIP订阅控制器（统一 VIP 体系 v12.0 重写，保持 App 端接口契约不变）
 *
 * <p>链路：GET /packages 上架等级列表（vip_tier，价格后台可配）→
 * POST /subscribe 下单（bizType='vip'，bizNo='ledger:{tier}:{uuid}'）→
 * 返回收银台参数；支付成功由 VipPayCallbackHandler 发卡/续费顺延 +
 * settlePlatform 平台全额分账 + 站内通知。
 *
 * @author moyun
 */
@Tag(name = "门户记账VIP", description = "记账端统一会员（等级/状态/下单）")
@RestController
@RequestMapping("/portal/ledger/vip")
public class PortalLedgerVipController {

    /** 记账端代码 */
    private static final String PLATFORM = "ledger";

    @Autowired
    private VipTierMapper tierMapper;

    @Autowired
    private VipUserCardMapper cardMapper;

    @Autowired
    private IPayGateway payGateway;

    @Autowired
    private PayProperties payProperties;

    /** 上架等级列表（App 会员页展示，价格后台可配；字段沿用旧套餐契约） */
    @Operation(summary = "上架等级列表")
    @GetMapping("/packages")
    public AjaxResult packages() {
        List<VipTier> tiers = tierMapper.selectList(new LambdaQueryWrapper<VipTier>()
                .eq(VipTier::getPlatformCode, PLATFORM)
                .eq(VipTier::getStatus, 1)
                .ne(VipTier::getTierCode, "free")
                .orderByAsc(VipTier::getSortOrder));
        List<Map<String, Object>> records = new ArrayList<>();
        for (VipTier tier : tiers) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", tier.getId());
            item.put("name", tier.getTierName());
            item.put("price", tier.getPrice());
            item.put("originalPrice", tier.getOriginalPrice());
            item.put("durationDays", tier.getDurationDays());
            item.put("description", tier.getDescription());
            item.put("popular", tier.getPopular());
            records.add(item);
        }
        return AjaxResult.success(Map.of("records", records));
    }

    /** 我的会员状态（isVip/vipExpire，按会员卡口径） */
    @Operation(summary = "我的会员状态")
    @GetMapping("/status")
    public AjaxResult status() {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error(401, "登录已过期，请重新登录");
        }
        VipUserCard card = selectActiveCard(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("isVip", card != null);
        data.put("vipExpire", card != null ? card.getExpireTime() : null);
        return AjaxResult.success(data);
    }

    /**
     * 订阅下单（App 端沿用 packageId 字段 = vip_tier.id；亦接受 tierCode）
     */
    @Operation(summary = "订阅下单")
    @PostMapping("/subscribe")
    public AjaxResult subscribe(@RequestBody Map<String, Object> body) {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error(401, "登录已过期，请重新登录");
        }
        String tierCode = body.get("tierCode") == null || String.valueOf(body.get("tierCode")).isBlank()
                ? null : String.valueOf(body.get("tierCode"));
        Long packageId = body.get("packageId") == null ? null
                : Long.valueOf(String.valueOf(body.get("packageId")));
        String clientUuid = body.get("clientUuid") == null || String.valueOf(body.get("clientUuid")).isBlank()
                ? UUID.randomUUID().toString().replace("-", "")
                : String.valueOf(body.get("clientUuid"));

        // 1. 等级校验（packageId=等级ID 兼容 App 旧契约）
        VipTier tier;
        if (tierCode != null) {
            tier = tierMapper.selectOne(new LambdaQueryWrapper<VipTier>()
                    .eq(VipTier::getPlatformCode, PLATFORM)
                    .eq(VipTier::getTierCode, tierCode)
                    .last("LIMIT 1"));
        } else if (packageId != null) {
            tier = tierMapper.selectById(packageId);
            if (tier != null && !PLATFORM.equals(tier.getPlatformCode())) {
                tier = null;
            }
        } else {
            tier = null;
        }
        if (tier == null || tier.getStatus() == null || tier.getStatus() != 1) {
            throw new ServiceException("会员等级不存在或已下架");
        }
        if (tier.getPrice() == null || tier.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("该等级无需购买");
        }

        // 2. 网关统一下单（bizNo=platform:tier:uuid，同 clientUuid 复用未支付单）
        String bizNo = PLATFORM + ":" + tier.getTierCode() + ":" + clientUuid;
        PayOrder payOrder = payGateway.createOrder("vip", bizNo, userId, PLATFORM,
                "wechat", tier.getPrice(), "记账VIP-" + tier.getTierName());
        if (PayOrder.STATUS_PAID.equals(payOrder.getStatus())
                || PayOrder.STATUS_SETTLED.equals(payOrder.getStatus())) {
            throw new ServiceException("该会员已支付成功，请勿重复提交");
        }

        // 3. 收银台参数（沿用 App 收银台契约）
        Map<String, Object> result = new HashMap<>();
        result.put("vipOrderId", payOrder.getId());
        result.put("amount", payOrder.getAmount());
        result.put("packageName", tier.getTierName());
        result.put("status", payOrder.getStatus());
        result.put("payNo", payOrder.getPayNo());
        result.put("codeUrl", payOrder.getCodeUrl());
        result.put("expireTime", payOrder.getExpireTime());
        result.put("mockEnabled", payProperties.getWechat().isMockEnabled());
        return AjaxResult.success(result);
    }

    /** 有效会员卡（status=1 且未过期；永久 expire 为空） */
    private VipUserCard selectActiveCard(Long userId) {
        return cardMapper.selectOne(new LambdaQueryWrapper<VipUserCard>()
                .eq(VipUserCard::getUserId, userId)
                .eq(VipUserCard::getPlatformCode, PLATFORM)
                .eq(VipUserCard::getStatus, 1)
                .and(w -> w.isNull(VipUserCard::getExpireTime)
                        .or().gt(VipUserCard::getExpireTime, LocalDateTime.now()))
                .orderByDesc(VipUserCard::getId)
                .last("LIMIT 1"));
    }
}
