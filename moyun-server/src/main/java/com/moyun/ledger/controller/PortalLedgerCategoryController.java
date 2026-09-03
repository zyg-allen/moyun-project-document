package com.moyun.ledger.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.ledger.domain.entity.LedgerCategory;
import com.moyun.ledger.service.ILedgerCategoryService;
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

/**
 * 门户记账-分类控制器（系统预设 + 自定义合并返回）
 *
 * @author moyun
 */
@RestController
@RequestMapping("/portal/ledger/categories")
public class PortalLedgerCategoryController {

    @Autowired
    private ILedgerCategoryService categoryService;

    /** 可用分类列表（系统预设 + 自定义） */
    @GetMapping
    public AjaxResult list(@RequestParam(required = false) String type) {
        Long userId = PortalSecurityUtils.getUserId();
        List<LedgerCategory> list = categoryService.listAvailable(userId, type);
        return AjaxResult.success(java.util.Map.of("records", list, "total", list.size()));
    }

    /** 新增自定义分类 */
    @PostMapping
    public AjaxResult create(@RequestBody LedgerCategory category) {
        Long userId = PortalSecurityUtils.getUserId();
        return AjaxResult.success(categoryService.createCategory(userId, category));
    }

    /** 修改自定义分类（系统预设不可改） */
    @PutMapping("/{id:[0-9]+}")
    public AjaxResult update(@PathVariable("id") Long id, @RequestBody LedgerCategory category) {
        Long userId = PortalSecurityUtils.getUserId();
        category.setId(id);
        categoryService.updateCategory(userId, category);
        return AjaxResult.success("修改成功");
    }

    /** 停用自定义分类（系统预设不可删） */
    @DeleteMapping("/{id:[0-9]+}")
    public AjaxResult remove(@PathVariable("id") Long id) {
        Long userId = PortalSecurityUtils.getUserId();
        categoryService.deleteCategory(userId, id);
        return AjaxResult.success("删除成功");
    }
}
