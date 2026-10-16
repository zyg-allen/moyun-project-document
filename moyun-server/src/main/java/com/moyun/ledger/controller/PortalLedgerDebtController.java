package com.moyun.ledger.controller;

import com.moyun.core.base.AjaxResult;

import com.moyun.ledger.domain.entity.LedgerObligation;
import com.moyun.ledger.service.ILedgerDebtService;
import com.moyun.portal.util.PortalSecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * 门户-债权债务（应收/应付/核销）。
 *
 * <p>所有端点均以当前登录用户为作用域，userId 一律取自会话，绝不接受前端传入，
 * 避免横向越权读到他人债务。</p>
 *
 * @author moyun
 */
@Tag(name = "门户-债权债务")
@RestController
@RequestMapping("/portal/ledger/debts")
public class PortalLedgerDebtController {

    @Autowired
    private ILedgerDebtService debtService;

    // ---------------- 应收 ----------------

    @Operation(summary = "应收款列表")
    @GetMapping("/receivables")
    public AjaxResult listReceivables(@RequestParam(value = "keyword", required = false) String keyword,
                                      @RequestParam(value = "settleStatus", required = false) String settleStatus,
                                      @RequestParam(value = "pageNum", required = false) Integer pageNum,
                                      @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return AjaxResult.success(debtService.listReceivables(
                PortalSecurityUtils.getUserId(), keyword, settleStatus, pageNum, pageSize));
    }

    @Operation(summary = "应收款详情")
    @GetMapping("/receivables/{id}")
    public AjaxResult getReceivable(@PathVariable("id") Long id) {
        return AjaxResult.success(debtService.getReceivable(PortalSecurityUtils.getUserId(), id));
    }

    @Operation(summary = "新增应收款")
    @PostMapping("/receivables")
    public AjaxResult createReceivable(@RequestBody LedgerObligation bo) {
        return AjaxResult.success(debtService.createReceivable(PortalSecurityUtils.getUserId(), bo));
    }

    @Operation(summary = "修改应收款")
    @PutMapping("/receivables/{id}")
    public AjaxResult updateReceivable(@PathVariable("id") Long id, @RequestBody LedgerObligation bo) {
        debtService.updateReceivable(PortalSecurityUtils.getUserId(), id, bo);
        return AjaxResult.success();
    }

    @Operation(summary = "删除应收款")
    @DeleteMapping("/receivables/{id}")
    public AjaxResult deleteReceivable(@PathVariable("id") Long id) {
        debtService.deleteReceivable(PortalSecurityUtils.getUserId(), id);
        return AjaxResult.success();
    }

    // ---------------- 应付 ----------------

    @Operation(summary = "应付款列表")
    @GetMapping("/payables")
    public AjaxResult listPayables(@RequestParam(value = "keyword", required = false) String keyword,
                                   @RequestParam(value = "settleStatus", required = false) String settleStatus,
                                   @RequestParam(value = "pageNum", required = false) Integer pageNum,
                                   @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return AjaxResult.success(debtService.listPayables(
                PortalSecurityUtils.getUserId(), keyword, settleStatus, pageNum, pageSize));
    }

    @Operation(summary = "应付款详情")
    @GetMapping("/payables/{id}")
    public AjaxResult getPayable(@PathVariable("id") Long id) {
        return AjaxResult.success(debtService.getPayable(PortalSecurityUtils.getUserId(), id));
    }

    @Operation(summary = "新增应付款")
    @PostMapping("/payables")
    public AjaxResult createPayable(@RequestBody LedgerObligation bo) {
        return AjaxResult.success(debtService.createPayable(PortalSecurityUtils.getUserId(), bo));
    }

    @Operation(summary = "修改应付款")
    @PutMapping("/payables/{id}")
    public AjaxResult updatePayable(@PathVariable("id") Long id, @RequestBody LedgerObligation bo) {
        debtService.updatePayable(PortalSecurityUtils.getUserId(), id, bo);
        return AjaxResult.success();
    }

    @Operation(summary = "删除应付款")
    @DeleteMapping("/payables/{id}")
    public AjaxResult deletePayable(@PathVariable("id") Long id) {
        debtService.deletePayable(PortalSecurityUtils.getUserId(), id);
        return AjaxResult.success();
    }

    // ---------------- 核销与汇总 ----------------

    @Operation(summary = "核销债务（收款/还款）")
    @PostMapping("/settle")
    public AjaxResult settle(@RequestBody Map<String, Object> bo) {
        String debtType = bo.get("debtType") == null ? null : String.valueOf(bo.get("debtType"));
        Long debtId = bo.get("debtId") == null ? null : Long.valueOf(String.valueOf(bo.get("debtId")));
        BigDecimal amount = bo.get("amount") == null ? null : new BigDecimal(String.valueOf(bo.get("amount")));
        Long accountId = bo.get("accountId") == null ? null : Long.valueOf(String.valueOf(bo.get("accountId")));
        LocalDate repayDate = bo.get("repayDate") == null || String.valueOf(bo.get("repayDate")).isBlank()
                ? LocalDate.now() : LocalDate.parse(String.valueOf(bo.get("repayDate")));
        String remark = bo.get("remark") == null ? null : String.valueOf(bo.get("remark"));
        return AjaxResult.success(debtService.settle(
                PortalSecurityUtils.getUserId(), debtType, debtId, amount, accountId, repayDate, remark));
    }

    @Operation(summary = "某笔债务的核销记录")
    @GetMapping("/repayments")
    public AjaxResult listRepayments(@RequestParam(value = "debtType", required = false) String debtType,
                                     @RequestParam(value = "debtId", required = false) Long debtId) {
        return AjaxResult.success(debtService.listRepayments(
                PortalSecurityUtils.getUserId(), debtType, debtId));
    }

    @Operation(summary = "债务汇总（资产总览用：应收未结 / 应付未结）")
    @GetMapping("/summary")
    public AjaxResult summary() {
        return AjaxResult.success(debtService.summary(PortalSecurityUtils.getUserId()));
    }
}