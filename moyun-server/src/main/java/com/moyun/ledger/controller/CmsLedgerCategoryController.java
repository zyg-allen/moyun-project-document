package com.moyun.ledger.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ledger.domain.entity.LedgerCategory;
import com.moyun.ledger.service.ILedgerCategoryService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * CMS 记账预设分类管理 Controller
 *
 * <p>仅维护系统预设分类（user_id=0 + is_system=1）；
 * 用户自定义分类属用户隐私域，后台不提供任何入口（脱敏红线）。
 *
 * @author moyun
 */
@Tag(name = "CMS记账预设分类", description = "系统预设收支分类维护")
@RestController
@RequestMapping("/cms/ledger/category")
public class CmsLedgerCategoryController extends BaseController {

    @Autowired
    private ILedgerCategoryService categoryService;

    @Operation(summary = "预设分类列表")
    @PreAuthorize("@ss.hasPermi('cms:ledgerCategory:list')")
    @GetMapping("/list")
    public AjaxResult list(String type) {
        LambdaQueryWrapper<LedgerCategory> qw = new LambdaQueryWrapper<>();
        qw.eq(LedgerCategory::getUserId, 0)
                .eq(LedgerCategory::getIsSystem, 1);
        if (type != null && !type.isEmpty()) {
            qw.eq(LedgerCategory::getType, type);
        }
        qw.orderByAsc(LedgerCategory::getType).orderByAsc(LedgerCategory::getSortOrder);
        List<LedgerCategory> list = categoryService.list(qw);
        return success(Map.of("records", list, "total", list.size()));
    }

    @Operation(summary = "新增预设分类")
    @PreAuthorize("@ss.hasPermi('cms:ledgerCategory:add')")
    @PostMapping
    public AjaxResult add(@RequestBody LedgerCategory category) {
        category.setId(null);
        category.setUserId(0L);
        category.setIsSystem(1);
        if (category.getStatus() == null) {
            category.setStatus(LedgerCategory.STATUS_ENABLED);
        }
        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
        boolean ok = categoryService.save(category);
        return ok ? success(category) : error("新增失败");
    }

    @Operation(summary = "修改预设分类")
    @PreAuthorize("@ss.hasPermi('cms:ledgerCategory:edit')")
    @PutMapping("/{id}")
    public AjaxResult edit(@PathVariable Long id, @RequestBody LedgerCategory category) {
        LambdaQueryWrapper<LedgerCategory> qw = new LambdaQueryWrapper<>();
        qw.eq(LedgerCategory::getId, id)
                .eq(LedgerCategory::getUserId, 0)
                .eq(LedgerCategory::getIsSystem, 1);
        LedgerCategory exist = categoryService.getOne(qw);
        if (exist == null) {
            return error("预设分类不存在");
        }
        category.setId(id);
        category.setUserId(0L);
        category.setIsSystem(1);
        return categoryService.updateById(category) ? success() : error("修改失败");
    }

    @Operation(summary = "停用预设分类")
    @PreAuthorize("@ss.hasPermi('cms:ledgerCategory:edit')")
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        LambdaQueryWrapper<LedgerCategory> qw = new LambdaQueryWrapper<>();
        qw.eq(LedgerCategory::getId, id)
                .eq(LedgerCategory::getUserId, 0)
                .eq(LedgerCategory::getIsSystem, 1);
        LedgerCategory exist = categoryService.getOne(qw);
        if (exist == null) {
            return error("预设分类不存在");
        }
        exist.setStatus(LedgerCategory.STATUS_DISABLED);
        return categoryService.updateById(exist) ? success() : error("停用失败");
    }
}
