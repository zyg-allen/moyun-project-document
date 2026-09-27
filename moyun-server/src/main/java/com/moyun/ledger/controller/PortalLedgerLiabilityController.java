package com.moyun.ledger.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.ledger.domain.entity.LedgerLiabilityAccount;
import com.moyun.ledger.service.ILedgerLiabilityAccountService;
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

import java.util.List;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 门户记账-负债账户控制器
 *
 * @author moyun
 */
@RestController
@RequestMapping("/portal/ledger/liabilities")
public class PortalLedgerLiabilityController {

    @Autowired
    private ILedgerLiabilityAccountService liabilityAccountService;

    /** 负债账户列表（在还中；includeArchived 含结清/归档） */
    @GetMapping
    public AjaxResult list(@RequestParam(required = false, defaultValue = "false") boolean includeArchived) {
        Long userId = PortalSecurityUtils.getUserId();
        List<LedgerLiabilityAccount> list = liabilityAccountService.listByUser(userId, includeArchived);
        return AjaxResult.success(Map.of("records", list, "total", list.size()));
    }

    /** 新增负债账户（initialBalance 初始欠款以 borrow 流水留痕） */
    @PostMapping
    public AjaxResult create(@RequestBody Map<String, Object> body) {
        Long userId = PortalSecurityUtils.getUserId();
        LedgerLiabilityAccount account = new LedgerLiabilityAccount();
        account.setName((String) body.get("name"));
        account.setType((String) body.get("type"));
        account.setIcon((String) body.get("icon"));
        if (body.get("annualRate") != null) {
            account.setAnnualRate(new java.math.BigDecimal(body.get("annualRate").toString()));
        }
        if (body.get("totalTerms") != null) {
            account.setTotalTerms(((Number) body.get("totalTerms")).intValue());
        }
        if (body.get("monthlyPayment") != null) {
            account.setMonthlyPayment(new BigDecimal(body.get("monthlyPayment").toString()));
        }
        if (body.get("repaymentDay") != null) {
            int day = ((Number) body.get("repaymentDay")).intValue();
            if (day < 1 || day > 28) {
                return AjaxResult.error("还款日必须在 1-28 之间");
            }
            account.setRepaymentDay(day);
        }
        if (body.get("includeInTotal") != null) {
            account.setIncludeInTotal(((Number) body.get("includeInTotal")).intValue());
        }
        BigDecimal initialBalance = body.get("initialBalance") == null ? null
                : new BigDecimal(body.get("initialBalance").toString());
        // 元单位边界校验
        final BigDecimal MAX_LIAB = new BigDecimal("10000000000");    // 100 亿元
        final BigDecimal MAX_MONTHLY = new BigDecimal("100000000");   // 1 亿元
        if (initialBalance != null && (initialBalance.compareTo(BigDecimal.ZERO) < 0 || initialBalance.compareTo(MAX_LIAB) > 0)) {
            return AjaxResult.error("初始欠款超出允许范围（0 ~ 100 亿元），请确认金额单位");
        }
        BigDecimal mp = account.getMonthlyPayment();
        if (mp != null && (mp.compareTo(BigDecimal.ZERO) < 0 || mp.compareTo(MAX_MONTHLY) > 0)) {
            return AjaxResult.error("月供超出允许范围（0 ~ 1 亿元），请确认金额单位");
        }
        return AjaxResult.success(liabilityAccountService.createAccount(userId, account, initialBalance));
    }

    /**
     * 修改负债账户（不含 balance / paid_terms，欠款与已还期数走 borrow/repayment 记账）
     *
     * <p>入参用 {@code Map} 而非实体绑定：需要区分"**未传**某字段"与"传了 null"——
     * App 清空"每期还款额 / 还款日 / 总期数"时**显式发 null** 表示清空，而实体绑定 +
     * {@code updateById} 的"null 则跳过"语义会让清空**静默失效**（用户保存后旧值又回来）。
     * 这里显式映射白名单字段，并把 {@code body.keySet()} 透传给 service。</p>
     */
    @PutMapping("/{id:[0-9]+}")
    public AjaxResult update(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        Long userId = PortalSecurityUtils.getUserId();
        LedgerLiabilityAccount account = new LedgerLiabilityAccount();
        account.setId(id);
        // 白名单字段显式映射：balance/paid_terms/version/status/settle_flag 不在其中，客户端传了也无效
        if (body.containsKey("name")) account.setName(LedgerAccountFields.asString(body.get("name")));
        if (body.containsKey("type")) account.setType(LedgerAccountFields.asString(body.get("type")));
        if (body.containsKey("icon")) account.setIcon(LedgerAccountFields.asString(body.get("icon")));
        if (body.containsKey("includeInTotal")) account.setIncludeInTotal(LedgerAccountFields.asInt(body.get("includeInTotal")));
        if (body.containsKey("sortOrder")) account.setSortOrder(LedgerAccountFields.asInt(body.get("sortOrder")));
        if (body.containsKey("principal")) account.setPrincipal(LedgerAccountFields.asDecimal(body.get("principal")));
        if (body.containsKey("annualRate")) account.setAnnualRate(LedgerAccountFields.asDecimal(body.get("annualRate")));
        if (body.containsKey("monthlyPayment")) account.setMonthlyPayment(LedgerAccountFields.asDecimal(body.get("monthlyPayment")));
        if (body.containsKey("totalTerms")) account.setTotalTerms(LedgerAccountFields.asInt(body.get("totalTerms")));
        if (body.containsKey("dueDate")) account.setDueDate(LedgerAccountFields.asLocalDate(body.get("dueDate")));
        if (body.containsKey("repaymentDay")) {
            Integer day = LedgerAccountFields.asInt(body.get("repaymentDay"));
            if (day != null && (day < 1 || day > 28)) {
                return AjaxResult.error("还款日必须在 1-28 之间");
            }
            account.setRepaymentDay(day);
        }
        liabilityAccountService.updateAccount(userId, account, body.keySet());
        return AjaxResult.success("修改成功");
    }

    /** 删除负债账户（status=0 停用归档，流水永久保留） */
    @DeleteMapping("/{id:[0-9]+}")
    public AjaxResult remove(@PathVariable("id") Long id) {
        Long userId = PortalSecurityUtils.getUserId();
        liabilityAccountService.deleteAccount(userId, id);
        return AjaxResult.success("删除成功");
    }
}
