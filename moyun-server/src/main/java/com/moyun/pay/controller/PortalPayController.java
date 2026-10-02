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
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error(401, "登录已过期，请重新登录");
        }
        // 越权防护——payNo 可被枚举/猜测，仅校验"已登录"时，
        // 任意登录用户都能读到**他人订单**的金额/支付链接/过期时间（IDOR）
        PayOrder order = ownOrderOrNull(payNo, userId);
        if (order == null) {
            return AjaxResult.error(403, "订单不存在或无权操作");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("payNo", order.getPayNo());
        data.put("status", order.getStatus());
        data.put("amount", order.getAmount());
        // 清单 P2：收银台原先只显示金额与支付单号，用户看不到"买的是什么"；
        // 而下单时订单已写入 subject（如"会员订阅-XX"）与 bizType —— 这里如实下发。
        data.put("subject", order.getSubject());
        data.put("bizType", order.getBizType());
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
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error(401, "登录已过期，请重新登录");
        }
        // 越权防护（必须早于 mockPaySuccess）——若只校验"已登录 + mock 已开启"，
        // 任意登录用户可把**他人订单**置为支付成功，进而触发真实后续链路（发卡/记账/打赏到账）
        if (ownOrderOrNull(payNo, userId) == null) {
            return AjaxResult.error(403, "订单不存在或无权操作");
        }
        if (!payProperties.getWechat().isMockEnabled()) {
            return AjaxResult.error("mock 支付未开启");
        }
        Map<String, Object> result = payGateway.mockPaySuccess(payNo);
        return AjaxResult.success(result);
    }

    /**
     * 取"当前用户名下"的订单（越权防护收口）
     *
     * <p>订单归属校验只此一处：{@code /status} 与 {@code /mock} 都走它，避免各端点各写一份、
     * 新增端点时漏掉归属判断。查不到订单与订单不属于自己返回同一个 {@code null}（不泄露订单是否存在）。</p>
     */
    private PayOrder ownOrderOrNull(String payNo, Long userId) {
        PayOrder order = payGateway.queryStatus(payNo);
        if (order == null || order.getUserId() == null || !order.getUserId().equals(userId)) {
            return null;
        }
        return order;
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
