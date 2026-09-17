package com.moyun.ledger.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.ledger.domain.entity.LedgerTipOrder;
import com.moyun.ledger.service.ILedgerTipService;
import com.moyun.portal.util.PortalSecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 门户记账-打赏控制器（接入公共支付通道）
 *
 * <p>链路：POST /tips 落 pending 单并经网关统一下单（bizType=ledger_tip），
 * 返回收银台参数（payNo/codeUrl/mockEnabled）；支付状态轮询复用 /portal/pay/status/{payNo}，
 * mock 模拟支付复用 /portal/pay/mock/{payNo}；支付成功由 LedgerTipPayCallbackHandler
 * 在回调事务内推进 pending→paid + 平台全额分账。
 *
 * <p>入参 pay_channel 统一（兼容旧 payWay 字段名）。
 *
 * @author moyun
 */
@RestController
@RequestMapping("/portal/ledger/tips")
public class PortalLedgerTipController {

    @Autowired
    private ILedgerTipService tipService;

    /** 累计打赏金额（status=paid） */
    @GetMapping("/total")
    public AjaxResult total() {
        Long userId = PortalSecurityUtils.getUserId();
        BigDecimal total = tipService.totalAmount(userId);
        return AjaxResult.success(Map.of("totalAmount", total));
    }

    /** 我的赞赏记录（分页，含 pending/paid） */
    @GetMapping("/my")
    public AjaxResult my(@RequestParam(defaultValue = "1") long current,
                         @RequestParam(defaultValue = "10") long size) {
        Long userId = PortalSecurityUtils.getUserId();
        var page = tipService.myTips(userId, current, size);
        Map<String, Object> data = new HashMap<>();
        data.put("records", page.getRecords());
        data.put("total", page.getTotal());
        data.put("current", page.getCurrent());
        data.put("size", page.getSize());
        return AjaxResult.success(data);
    }

    /**
     * 发起打赏下单（公共通道：pending 单 + 网关统一下单，返回收银台参数）
     */
    @PostMapping
    public AjaxResult create(@RequestBody Map<String, Object> body) {
        Long userId = PortalSecurityUtils.getUserId();
        BigDecimal amount = new BigDecimal(String.valueOf(body.get("amount")));
        // 统一 pay_channel；兼容旧客户端 payWay
        String payChannel = body.get("payChannel") != null ? String.valueOf(body.get("payChannel"))
                : (body.get("payWay") == null ? null : String.valueOf(body.get("payWay")));
        String target = body.get("target") == null ? null : String.valueOf(body.get("target"));
        String reason = body.get("reason") == null ? null : String.valueOf(body.get("reason"));
        String clientUuid = body.get("clientUuid") == null ? null : String.valueOf(body.get("clientUuid"));
        Map<String, Object> cashier = tipService.createTipOrder(userId, amount, payChannel, target, reason, clientUuid);
        return AjaxResult.success(cashier);
    }
}
