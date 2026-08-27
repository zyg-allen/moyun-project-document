package com.moyun.pay.controller;


import com.moyun.core.base.AjaxResult;
import com.moyun.pay.config.PayProperties;
import com.moyun.pay.domain.entity.LedgerEntry;
import com.moyun.pay.domain.entity.PayOrder;
import com.moyun.pay.domain.entity.UserAccount;
import com.moyun.pay.gateway.IPayGateway;
import com.moyun.pay.service.ILedgerService;
import com.moyun.pay.service.IUserAccountService;
import com.moyun.portal.util.PortalSecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 门户支付控制器（V11.0）
 *
 * <p>收银台轮询 / mock 模拟支付 / 账户总览 / 我的流水。
 *
 * @author moyun
 */
@RestController
@RequestMapping("/portal/pay")
public class PortalPayController {

    @Autowired
    private IPayGateway payGateway;

    @Autowired
    private IUserAccountService userAccountService;

    @Autowired
    private ILedgerService ledgerService;

    @Autowired
    private PayProperties payProperties;

    /**
     * 支付状态轮询（收银台 3s 轮询）
     */
    @GetMapping("/status/{payNo}")
    public AjaxResult status(@PathVariable String payNo) {
        PayOrder order = payGateway.queryStatus(payNo);
        Map<String, Object> data = new HashMap<>();
        data.put("payNo", order.getPayNo());
        data.put("status", order.getStatus());
        data.put("amount", order.getAmount());
        data.put("amountYuan", BigDecimal.valueOf(order.getAmount() == null ? 0 : order.getAmount(), 2));
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
        if (!payProperties.getWechat().isMockEnabled()) {
            return AjaxResult.error("mock 支付未开启");
        }
        Map<String, Object> result = payGateway.mockPaySuccess(payNo);
        return AjaxResult.success(result);
    }

    /**
     * 账户总览（余额/累计收入/累计提现，元）
     */
    @GetMapping("/account/overview")
    public AjaxResult accountOverview() {
        Long userId = PortalSecurityUtils.getUserId();
        UserAccount account = userAccountService.getOrCreate(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("userId", account.getUserId());
        data.put("balance", account.getBalance());
        data.put("balanceYuan", BigDecimal.valueOf(account.getBalance() == null ? 0 : account.getBalance(), 2));
        data.put("totalIncome", account.getTotalIncome());
        data.put("totalIncomeYuan", BigDecimal.valueOf(account.getTotalIncome() == null ? 0 : account.getTotalIncome(), 2));
        data.put("totalWithdraw", account.getTotalWithdraw());
        data.put("totalWithdrawYuan", BigDecimal.valueOf(account.getTotalWithdraw() == null ? 0 : account.getTotalWithdraw(), 2));
        return AjaxResult.success(data);
    }

    /**
     * 我的资金流水（分页，元展示）
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
}
