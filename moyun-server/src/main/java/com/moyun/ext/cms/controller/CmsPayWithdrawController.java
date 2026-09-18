package com.moyun.ext.cms.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.pay.service.IWithdrawOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * CMS 提现审核 Controller（收入管理模块）
 *
 * <p>资金模型：审核通过 = 原子扣减用户虚拟余额 + 记 debit 流水 + 商户号出金（预留）；
 * 审核驳回 = 单据关闭，余额不动。
 *
 * @author moyun
 */
@Tag(name = "CMS提现审核", description = "用户提现单审核（通过=打款 / 驳回=退回）")
@RestController
@RequestMapping("/cms/pay/withdraw")
public class CmsPayWithdrawController extends BaseController {

    @Autowired
    private IWithdrawOrderService withdrawOrderService;

    @Operation(summary = "提现单列表", description = "状态/用户ID/端筛选，含审核中/已打款/已驳回汇总")
    @PreAuthorize("@ss.hasPermi('cms:payWithdraw:list')")
    @GetMapping("/list")
    public AjaxResult list(@RequestParam(required = false) String status,
                           @RequestParam(required = false) Long userId,
                           @RequestParam(required = false) String platformCode,
                           @RequestParam(defaultValue = "1") long current,
                           @RequestParam(defaultValue = "10") long size) {
        return success(withdrawOrderService.adminList(status, userId, platformCode, current, size));
    }

    @Operation(summary = "审核通过", description = "原子扣减余额+写流水+置paid（余额不足自动驳回）")
    @PreAuthorize("@ss.hasPermi('cms:payWithdraw:audit')")
    @PostMapping("/{id}/pass")
    public AjaxResult pass(@PathVariable Long id) {
        withdrawOrderService.auditPass(id);
        return success("已通过并完成打款记账");
    }

    @Operation(summary = "审核驳回", description = "置rejected+驳回原因，余额不动")
    @PreAuthorize("@ss.hasPermi('cms:payWithdraw:audit')")
    @PostMapping("/{id}/reject")
    public AjaxResult reject(@PathVariable Long id, @RequestParam(required = false) String reason) {
        withdrawOrderService.auditReject(id, reason);
        return success("已驳回");
    }
}
