package com.moyun.pay.controller;


import com.moyun.core.base.AjaxResult;
import com.moyun.pay.config.PayProperties;
import com.moyun.pay.domain.entity.PayOrder;
import com.moyun.pay.gateway.IPayGateway;
import com.moyun.pay.service.ILedgerService;
import com.moyun.pay.service.IWithdrawOrderService;
import com.moyun.portal.util.PortalSecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 门户支付控制器
 *
 * <p>收银台轮询 / mock 模拟支付 / 账户总览 / 我的流水。
 *
 * <p>金额单位：元（统一，接口所见即所得，无分/元换算字段）。
 *
 * @author moyun
 */
@RestController
@RequestMapping("/portal/pay")
public class PortalPayController {

    @Autowired
    private IPayGateway payGateway;

    @Autowired
    private ILedgerService ledgerService;

    @Autowired
    private PayProperties payProperties;

    @Autowired
    private IWithdrawOrderService withdrawOrderService;

    /**
     * 支付状态轮询（收银台 3s 轮询）
     */
    @GetMapping("/status/{payNo}")
    public AjaxResult status(@PathVariable String payNo) {
        if (PortalSecurityUtils.getUserId() == null) {
            return AjaxResult.error(401, "登录已过期，请重新登录");
        }
        PayOrder order = payGateway.queryStatus(payNo);
        Map<String, Object> data = new HashMap<>();
        data.put("payNo", order.getPayNo());
        data.put("status", order.getStatus());
        data.put("amount", order.getAmount());
        data.put("expireTime", order.getExpireTime());
        data.put("codeUrl", order.getCodeUrl());
        data.put("mockEnabled", payProperties.getWechat().isMockEnabled());
        return AjaxResult.success(data);
    }

    /**
     * mock 模式：模拟渠道支付成功（触发与真实回调一致的后续链路）
     */
    @PostMapping("/mock/{payNo}")
    public AjaxResult mockPay(@PathVariable String payNo) {
        if (PortalSecurityUtils.getUserId() == null) {
            return AjaxResult.error(401, "登录已过期，请重新登录");
        }
        if (!payProperties.getWechat().isMockEnabled()) {
            return AjaxResult.error("mock 支付未开启");
        }
        Map<String, Object> result = payGateway.mockPaySuccess(payNo);
        return AjaxResult.success(result);
    }

    /**
     * 账户总览（余额/累计收入/累计提现/审核中，元）
     */
    @GetMapping("/account/overview")
    public AjaxResult accountOverview() {
        Long userId = PortalSecurityUtils.getUserId();
        return AjaxResult.success(withdrawOrderService.userSummary(userId));
    }

    /**
     * 我的资金流水（分页，元）
     */
    @GetMapping("/account/ledger")
    public AjaxResult myLedger(@RequestParam(defaultValue = "1") long current,
                               @RequestParam(defaultValue = "10") long size) {
        Long userId = PortalSecurityUtils.getUserId();
        var page = ledgerService.myEntries(userId, current, size);
        Map<String, Object> data = new HashMap<>();
        data.put("records", page.getRecords());
        data.put("total", page.getTotal());
        data.put("current", page.getCurrent());
        data.put("size", page.getSize());
        return AjaxResult.success(data);
    }

    // ==================== 提现闭环 ====================

    /**
     * 发起提现（校验余额/绑卡，落 auditing 单，不扣款）
     */
    @PostMapping("/withdraw/apply")
    public AjaxResult withdrawApply(@RequestBody Map<String, Object> body) {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error(401, "登录已过期，请重新登录");
        }
        java.math.BigDecimal amount = new java.math.BigDecimal(String.valueOf(body.get("amount")));
        Long bankCardId = body.get("bankCardId") == null ? null : Long.valueOf(String.valueOf(body.get("bankCardId")));
        var order = withdrawOrderService.apply(userId, amount, bankCardId);
        Map<String, Object> data = new HashMap<>();
        data.put("withdrawNo", order.getWithdrawNo());
        data.put("amount", order.getAmount());
        data.put("status", order.getStatus());
        return AjaxResult.success("提现申请已提交，等待平台审核", data);
    }

    /**
     * 我的提现单（分页）
     */
    @GetMapping("/withdraw/my")
    public AjaxResult myWithdrawals(@RequestParam(defaultValue = "1") long current,
                                    @RequestParam(defaultValue = "10") long size) {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error(401, "登录已过期，请重新登录");
        }
        var page = withdrawOrderService.myWithdrawals(userId, current, size);
        Map<String, Object> data = new HashMap<>();
        data.put("records", page.getRecords());
        data.put("total", page.getTotal());
        data.put("current", page.getCurrent());
        data.put("size", page.getSize());
        return AjaxResult.success(data);
    }
}
