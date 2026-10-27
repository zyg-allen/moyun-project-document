package com.moyun.ledger.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.ledger.domain.entity.LedgerSavingPlan;
import com.moyun.ledger.service.ILedgerSavingService;
import com.moyun.portal.util.PortalSecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 门户记账-存钱计划控制器
 *
 * <p>支持 52周存钱法 / 固定金额 / 每月固定 / 自定义递增四种方式，
 * 计划列表 + 计划详情（期次流水，成功/失败状态）。
 *
 * @author moyun
 */
@RestController
@RequestMapping("/portal/ledger/savings")
public class PortalLedgerSavingController {

    @Autowired
    private ILedgerSavingService savingService;

    /** 计划列表 + 汇总（剩余需存/累计存入/目标金额） */
    @GetMapping
    public AjaxResult list() {
        Long userId = PortalSecurityUtils.getUserId();
        Map<String, Object> data = savingService.listPlans(userId);
        return AjaxResult.success(data);
    }

    /** 计划详情（计划 + 期次流水） */
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable("id") Long id) {
        Long userId = PortalSecurityUtils.getUserId();
        return AjaxResult.success(savingService.planDetail(userId, id));
    }

    /** 新建计划（按方式自动生成期次） */
    @com.moyun.common.annotation.RepeatSubmit(interval = 2000)
    @com.moyun.common.annotation.RateLimiter(key = "portalledgersaving:", time = 60, count = 120)
    @PostMapping
    public AjaxResult create(@RequestBody LedgerSavingPlan plan) {
        Long userId = PortalSecurityUtils.getUserId();
        Long id = savingService.createPlan(userId, plan);
        return AjaxResult.success("创建成功", Map.of("id", id));
    }

    /** 存入某一期（actualAmount 可空=按目标金额） */
    @com.moyun.common.annotation.RepeatSubmit(interval = 2000)
    @com.moyun.common.annotation.RateLimiter(key = "portalledgersaving:", time = 60, count = 120)
    @PostMapping("/{id}/records/{recordId}/deposit")
    public AjaxResult deposit(@PathVariable("id") Long id, @PathVariable("recordId") Long recordId,
                              @RequestParam(value = "actualAmount", required = false) BigDecimal actualAmount) {
        Long userId = PortalSecurityUtils.getUserId();
        savingService.deposit(userId, id, recordId, actualAmount);
        return AjaxResult.success("存入成功");
    }

    /** 放弃某一期（记录失败原因） */
    @com.moyun.common.annotation.RepeatSubmit(interval = 2000)
    @com.moyun.common.annotation.RateLimiter(key = "portalledgersaving:", time = 60, count = 120)
    @PostMapping("/{id}/records/{recordId}/fail")
    public AjaxResult fail(@PathVariable("id") Long id, @PathVariable("recordId") Long recordId,
                           @RequestBody(required = false) Map<String, String> body) {
        Long userId = PortalSecurityUtils.getUserId();
        String reason = body == null ? null : body.get("reason");
        savingService.failRecord(userId, id, recordId, reason);
        return AjaxResult.success("已标记放弃");
    }

    /** 标记期次：status 1=已存 2=未完成 0=重置 */
    @com.moyun.common.annotation.RepeatSubmit(interval = 2000)
    @com.moyun.common.annotation.RateLimiter(key = "ledger_saving:", time = 60, count = 60)
    @PostMapping("/{id}/records/{recordId}/mark")
    public AjaxResult mark(@PathVariable("id") Long id, @PathVariable("recordId") Long recordId,
                           @jakarta.validation.Valid @RequestBody com.moyun.ledger.domain.dto.SavingMarkDTO body) {
        savingService.markRecord(PortalSecurityUtils.getUserId(), id, recordId,
                body.getStatus(), body.getAmount(), body.getReason());
        return AjaxResult.success("已标记");
    }

    /** 关联已有流水 */
    @com.moyun.common.annotation.RepeatSubmit(interval = 2000)
    @com.moyun.common.annotation.RateLimiter(key = "ledger_saving:", time = 60, count = 60)
    @PostMapping("/{id}/records/{recordId}/link")
    public AjaxResult link(@PathVariable("id") Long id, @PathVariable("recordId") Long recordId,
                           @jakarta.validation.Valid @RequestBody com.moyun.ledger.domain.dto.SavingLinkDTO body) {
        savingService.linkTransaction(PortalSecurityUtils.getUserId(), id, recordId, body.getTransactionId());
        return AjaxResult.success("已关联");
    }

    /** 创建转账并关联 */
    @com.moyun.common.annotation.RepeatSubmit(interval = 3000)
    @com.moyun.common.annotation.RateLimiter(key = "ledger_saving:", time = 60, count = 60)
    @PostMapping("/{id}/records/{recordId}/transfer")
    public AjaxResult transfer(@PathVariable("id") Long id, @PathVariable("recordId") Long recordId,
                               @jakarta.validation.Valid @RequestBody(required = false)
                               com.moyun.ledger.domain.dto.SavingTransferDTO body) {
        com.moyun.ledger.domain.dto.TransactionCreateDTO dto = new com.moyun.ledger.domain.dto.TransactionCreateDTO();
        if (body != null) {
            dto.setAccountId(body.getAccountId());
            dto.setTargetAccountId(body.getTargetAccountId());
            dto.setAmount(body.getAmount());
            dto.setTransactionDate(body.getTransactionDate());
            dto.setDescription(body.getRemark());
        }
        Long txnId = savingService.createTransferAndLink(PortalSecurityUtils.getUserId(), id, recordId, dto);
        return AjaxResult.success("已创建并关联", java.util.Map.of("transactionId", txnId));
    }

    /** 解除关联 */
    @com.moyun.common.annotation.RepeatSubmit(interval = 2000)
    @com.moyun.common.annotation.RateLimiter(key = "ledger_saving:", time = 60, count = 60)
    @PostMapping("/{id}/records/{recordId}/unlink")
    public AjaxResult unlink(@PathVariable("id") Long id, @PathVariable("recordId") Long recordId,
                             @RequestBody(required = false) java.util.Map<String, Object> body) {
        boolean deleteTxn = body != null && Boolean.parseBoolean(String.valueOf(body.get("deleteTransaction")));
        savingService.unlinkTransaction(PortalSecurityUtils.getUserId(), id, recordId, deleteTxn);
        return AjaxResult.success("已解除关联");
    }

    /** 可关联流水候选 */
    @GetMapping("/{id}/linkable")
    public AjaxResult linkable(@PathVariable("id") Long id,
                               @RequestParam(value = "date", required = false) String date,
                               @RequestParam(value = "amount", required = false) BigDecimal amount) {
        java.time.LocalDate d = date == null || date.isBlank() ? null : java.time.LocalDate.parse(date);
        return AjaxResult.success(savingService.listLinkableTransactions(PortalSecurityUtils.getUserId(), d, amount));
    }

    /** 更新计划绑定账户 */
    @PutMapping("/{id}/accounts")
    public AjaxResult accounts(@PathVariable("id") Long id, @RequestBody java.util.Map<String, Object> body) {
        Long source = body.get("sourceAccountId") == null ? null : Long.valueOf(String.valueOf(body.get("sourceAccountId")));
        Long savings = body.get("savingsAccountId") == null ? null : Long.valueOf(String.valueOf(body.get("savingsAccountId")));
        savingService.updateAccounts(PortalSecurityUtils.getUserId(), id, source, savings);
        return AjaxResult.success("已保存");
    }

    /** 删除计划（逻辑删，期次保留） */
    @com.moyun.common.annotation.RepeatSubmit(interval = 2000)
    @com.moyun.common.annotation.RateLimiter(key = "ledger_saving:", time = 60, count = 60)
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable("id") Long id) {
        Long userId = PortalSecurityUtils.getUserId();
        savingService.deletePlan(userId, id);
        return AjaxResult.success("已删除");
    }
}
