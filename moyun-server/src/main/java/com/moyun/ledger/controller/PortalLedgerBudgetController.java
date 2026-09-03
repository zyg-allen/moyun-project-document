package com.moyun.ledger.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.ledger.domain.entity.LedgerBudget;
import com.moyun.ledger.service.ILedgerBudgetService;
import com.moyun.portal.util.PortalSecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 门户记账-预算控制器
 *
 * @author moyun
 */
@RestController
@RequestMapping("/portal/ledger/budgets")
public class PortalLedgerBudgetController {

    @Autowired
    private ILedgerBudgetService budgetService;

    /** 指定月份预算列表（null=当年当月） */
    @GetMapping
    public AjaxResult list(@RequestParam(required = false) Integer year,
                           @RequestParam(required = false) Integer month) {
        Long userId = PortalSecurityUtils.getUserId();
        List<LedgerBudget> list = budgetService.listByMonth(userId, year, month);
        return AjaxResult.success(java.util.Map.of("records", list, "total", list.size()));
    }

    /** 设置预算（总预算 categoryId=null，分类预算非空；存在即更新） */
    @PostMapping
    public AjaxResult save(@RequestBody LedgerBudget budget) {
        Long userId = PortalSecurityUtils.getUserId();
        budgetService.saveBudget(userId, budget);
        return AjaxResult.success("保存成功");
    }
}
