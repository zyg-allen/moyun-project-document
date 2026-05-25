package com.moyun.ext.flowable.controller;

import com.moyun.core.base.BaseController;
import com.moyun.core.base.AjaxResult;
import com.moyun.ext.flowable.domain.entity.SysForm;
import com.moyun.ext.flowable.service.ISysFormService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 流程表单配置
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/flow/form")
public class SysFormController extends BaseController {

    @Autowired
    private ISysFormService sysFormService;

    @PreAuthorize("@ss.hasPermi('flow:form:list')")
    @GetMapping("/list")
    public AjaxResult list() {
        return success();
    }

    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(sysFormService.selectFormById(id));
    }

    @PostMapping
    public AjaxResult add(@RequestBody SysForm sysForm) {
        return toAjax(sysFormService.insertForm(sysForm));
    }

    @PutMapping
    public AjaxResult edit(@RequestBody SysForm sysForm) {
        return toAjax(sysFormService.updateForm(sysForm));
    }

    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable("id") Long id) {
        return toAjax(sysFormService.deleteFormById(id));
    }
}
