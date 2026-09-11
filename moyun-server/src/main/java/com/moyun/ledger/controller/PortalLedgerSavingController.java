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
    @PostMapping
    public AjaxResult create(@RequestBody LedgerSavingPlan plan) {
        Long userId = PortalSecurityUtils.getUserId();
        Long id = savingService.createPlan(userId, plan);
        return AjaxResult.success("创建成功", Map.of("id", id));
    }

    /** 存入某一期（actualAmount 可空=按目标金额） */
    @PostMapping("/{id}/records/{recordId}/deposit")
    public AjaxResult deposit(@PathVariable("id") Long id, @PathVariable("recordId") Long recordId,
                              @RequestParam(value = "actualAmount", required = false) BigDecimal actualAmount) {
        Long userId = PortalSecurityUtils.getUserId();
        savingService.deposit(userId, id, recordId, actualAmount);
        return AjaxResult.success("存入成功");
    }

    /** 放弃某一期（记录失败原因） */
    @PostMapping("/{id}/records/{recordId}/fail")
    public AjaxResult fail(@PathVariable("id") Long id, @PathVariable("recordId") Long recordId,
                           @RequestBody(required = false) Map<String, String> body) {
        Long userId = PortalSecurityUtils.getUserId();
        String reason = body == null ? null : body.get("reason");
        savingService.failRecord(userId, id, recordId, reason);
        return AjaxResult.success("已标记放弃");
    }

    /** 删除计划（逻辑删，期次保留） */
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable("id") Long id) {
        Long userId = PortalSecurityUtils.getUserId();
        savingService.deletePlan(userId, id);
        return AjaxResult.success("已删除");
    }
}