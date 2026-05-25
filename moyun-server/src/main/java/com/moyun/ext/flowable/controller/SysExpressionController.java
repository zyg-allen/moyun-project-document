package com.moyun.ext.flowable.controller;

import com.moyun.core.base.BaseController;
import com.moyun.core.base.AjaxResult;
import com.moyun.ext.flowable.domain.entity.SysExpression;
import com.moyun.ext.flowable.service.ISysExpressionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 流程表达式配置
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/flow/expression")
public class SysExpressionController extends BaseController {

    @Autowired
    private ISysExpressionService sysExpressionService;

    @PreAuthorize("@ss.hasPermi('flow:expression:list')")
    @GetMapping("/list")
    public AjaxResult list() {
        return success();
    }

    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(sysExpressionService.selectExpressionById(id));
    }

    @PostMapping
    public AjaxResult add(@RequestBody SysExpression sysExpression) {
        return toAjax(sysExpressionService.insertExpression(sysExpression));
    }

    @PutMapping
    public AjaxResult edit(@RequestBody SysExpression sysExpression) {
        return toAjax(sysExpressionService.updateExpression(sysExpression));
    }

    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable("id") Long id) {
        return toAjax(sysExpressionService.deleteExpressionById(id));
    }
}
