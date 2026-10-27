package com.moyun.ledger.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.ledger.domain.entity.LedgerProductCategory;
import com.moyun.ledger.service.ILedgerProductCategoryService;
import com.moyun.portal.util.PortalSecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 门户-商品类目（两级树）。
 *
 * <p>系统预设（user_id=0）对所有用户可见但只读；用户自建类目可增删改。
 * 有子类目或被商品引用时禁止删除。</p>
 *
 * @author moyun
 */
@Tag(name = "门户-商品类目")
@RestController
@RequestMapping("/portal/ledger/product-categories")
public class PortalLedgerProductCategoryController {

    @Autowired
    private ILedgerProductCategoryService categoryService;

    @Operation(summary = "类目树（系统预设 + 本人自建）")
    @GetMapping
    public AjaxResult list() {
        return AjaxResult.success(categoryService.listTree(PortalSecurityUtils.getUserId()));
    }

    @Operation(summary = "新增类目")
    @com.moyun.common.annotation.RepeatSubmit(interval = 2000)
    @com.moyun.common.annotation.RateLimiter(key = "portalledgerproductcategory:", time = 60, count = 120)
    @PostMapping
    public AjaxResult create(@RequestBody LedgerProductCategory bo) {
        return AjaxResult.success(categoryService.create(PortalSecurityUtils.getUserId(), bo));
    }

    @Operation(summary = "修改类目")
    @com.moyun.common.annotation.RepeatSubmit(interval = 2000)
    @com.moyun.common.annotation.RateLimiter(key = "portalledgerproductcategory:", time = 60, count = 120)
    @PutMapping("/{id}")
    public AjaxResult update(@PathVariable("id") Long id, @RequestBody LedgerProductCategory bo) {
        categoryService.update(PortalSecurityUtils.getUserId(), id, bo);
        return AjaxResult.success();
    }

    @Operation(summary = "删除类目")
    @com.moyun.common.annotation.RepeatSubmit(interval = 2000)
    @com.moyun.common.annotation.RateLimiter(key = "portalledgerproductcategory:", time = 60, count = 120)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable("id") Long id) {
        categoryService.delete(PortalSecurityUtils.getUserId(), id);
        return AjaxResult.success();
    }
}
