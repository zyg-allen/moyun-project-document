package com.moyun.ledger.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.ledger.domain.dto.TransactionCreateDTO;
import com.moyun.ledger.domain.query.TransactionQuery;
import com.moyun.ledger.service.ILedgerTransactionService;
import com.moyun.portal.util.PortalSecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 门户记账-流水控制器（记账核心入口）
 *
 * @author moyun
 */
@RestController
@RequestMapping("/portal/ledger/transactions")
public class PortalLedgerTransactionController {

    @Autowired
    private ILedgerTransactionService transactionService;

    /** 新增记账（联动更新余额，单事务）；返回站内预算提醒（如有） */
    @PostMapping
    public AjaxResult create(@RequestBody TransactionCreateDTO dto) {
        Long userId = PortalSecurityUtils.getUserId();
        // 创建人从登录态填充（数据隔离/溯源），前端不传
        String username = PortalSecurityUtils.getUsername();
        dto.setCreateBy(username != null && !username.isEmpty() ? username : String.valueOf(userId));
        Long id = transactionService.createTransaction(userId, dto);
        // 事务提交后判断预算阈值（方法内部已兜底，失败不影响记账）
        String budgetAlert = transactionService.checkBudgetAlert(userId);
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("id", id);
        data.put("budgetAlert", budgetAlert);
        return AjaxResult.success(data);
    }

    /** 流水分页查询（多条件筛选） */
    @GetMapping
    public AjaxResult list(TransactionQuery query) {
        Long userId = PortalSecurityUtils.getUserId();
        return AjaxResult.success(transactionService.pageTransactions(userId, query));
    }

    /** 修改记账（旧记录冲正 → 新记录重放） */
    @PutMapping("/{id:[0-9]+}")
    public AjaxResult update(@PathVariable("id") Long id, @RequestBody TransactionCreateDTO dto) {
        Long userId = PortalSecurityUtils.getUserId();
        transactionService.updateTransaction(userId, id, dto);
        return AjaxResult.success("修改成功");
    }

    /** 删除记账（冲正后逻辑删除归档） */
    @DeleteMapping("/{id:[0-9]+}")
    public AjaxResult remove(@PathVariable("id") Long id) {
        Long userId = PortalSecurityUtils.getUserId();
        transactionService.deleteTransaction(userId, id);
        return AjaxResult.success("删除成功");
    }
}
