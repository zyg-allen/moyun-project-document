package com.moyun.ledger.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.annotation.Log;
import com.moyun.common.annotation.RepeatSubmit;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.TableDataInfo;
import com.moyun.ledger.domain.entity.LedgerProductCategory;
import com.moyun.ledger.service.ICmsLedgerPresetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台-商品类目管理（系统预设，两级树）
 *
 * <p>与门户端 {@code /portal/ledger/product-categories} 完全独立：
 * 后台只管系统预设（userId=0），用户自建类目只读；删除有引用红线。</p>
 *
 * @author moyun
 */
@Tag(name = "CMS记账-商品类目", description = "商品类目系统预设维护（用户自建只读）")
@RestController
@RequestMapping("/cms/ledger/product-category")
public class CmsLedgerProductCategoryController extends BaseController {

    @Autowired
    private ICmsLedgerPresetService presetService;

    @Operation(summary = "商品类目树列表（一级分页 + 二级嵌套 + 引用商品数）")
    @PreAuthorize("@ss.hasPermi('cms:ledgerProductCategory:list')")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(required = false) String name,
                              @RequestParam(required = false) String owner,
                              @RequestParam(required = false) Integer status,
                              @RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<LedgerProductCategory> page = presetService.pageProductCategory(pageNum, pageSize, name, owner, status);
        presetService.fillProductCategoryStats(page.getRecords());
        return getDataTable(page.getRecords(), page.getTotal());
    }

    @Operation(summary = "新增系统预设类目（一级或二级）")
    @PreAuthorize("@ss.hasPermi('cms:ledgerProductCategory:add')")
    @RepeatSubmit(interval = 3000)
    @Log(title = "记账-商品类目", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody LedgerProductCategory body) {
        presetService.createProductCategory(body);
        return success();
    }

    @Operation(summary = "修改系统预设类目")
    @PreAuthorize("@ss.hasPermi('cms:ledgerProductCategory:edit')")
    @RepeatSubmit(interval = 3000)
    @Log(title = "记账-商品类目", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public AjaxResult edit(@PathVariable Long id, @RequestBody LedgerProductCategory body) {
        presetService.updateProductCategory(id, body);
        return success();
    }

    @Operation(summary = "启用/停用系统预设类目")
    @PreAuthorize("@ss.hasPermi('cms:ledgerProductCategory:edit')")
    @Log(title = "记账-商品类目", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/status/{status}")
    public AjaxResult changeStatus(@PathVariable Long id, @PathVariable Integer status) {
        presetService.changeProductCategoryStatus(id, status);
        return success();
    }

    @Operation(summary = "删除系统预设类目（有子类目/被商品引用时拒绝）")
    @PreAuthorize("@ss.hasPermi('cms:ledgerProductCategory:edit')")
    @Log(title = "记账-商品类目", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        presetService.deleteProductCategory(id);
        return success();
    }
}
