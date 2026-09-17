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
 * <p>层级模型：大类 = 交易类型（收入/支出/转账/借款/还款/校准），分类挂在大类下（二级）。
 * <p>归属：系统预设（user_id=0 + is_system=1）与用户自定义（is_system=0）均可查看维护；
 * 脱敏红线：不展示任何用户身份信息（仅区分归属），不提供金额/账户等个体数据。
 * <p>删除规则：已绑定有效流水的分类不能删除（仅可停用）。
 *
 * @author moyun
 */
@Tag(name = "CMS记账预设分类", description = "预设/自定义分类维护（大类二级模型）")
@RestController
@RequestMapping("/cms/ledger/category")
public class CmsLedgerCategoryController extends BaseController {

    @Autowired
    private ILedgerCategoryService categoryService;

    /**
     * 分类列表（支持类型/名称/归属筛选；默认全部）
     *
     * @param type  类型：expense/income/transfer/repayment/borrow/adjust（空=全部）
     * @param name  名称模糊搜索（空=不过滤）
     * @param owner 归属：system=系统预设 / user=用户自定义（空=全部）
     */
    @Operation(summary = "分类列表（类型/名称/归属筛选）")
    @PreAuthorize("@ss.hasPermi('cms:ledgerCategory:list')")
    @GetMapping("/list")
    public AjaxResult list(String type, String name, String owner) {
        LambdaQueryWrapper<LedgerCategory> qw = new LambdaQueryWrapper<>();
        if (type != null && !type.isEmpty()) {
            qw.eq(LedgerCategory::getType, type);
        }
        if (name != null && !name.isEmpty()) {
            qw.like(LedgerCategory::getName, name.trim());
        }
        if ("system".equals(owner)) {
            qw.eq(LedgerCategory::getUserId, 0).eq(LedgerCategory::getIsSystem, 1);
        } else if ("user".equals(owner)) {
            qw.gt(LedgerCategory::getUserId, 0).eq(LedgerCategory::getIsSystem, 0);
        }
        qw.orderByAsc(LedgerCategory::getType).orderByAsc(LedgerCategory::getSortOrder);
        List<LedgerCategory> list = categoryService.list(qw);
        return success(Map.of("records", list, "total", list.size()));
    }

    @Operation(summary = "新增系统预设分类（挂指定大类下）")
    @PreAuthorize("@ss.hasPermi('cms:ledgerCategory:add')")
    @PostMapping
    public AjaxResult add(@RequestBody LedgerCategory category) {
        category.setId(null);
        category.setUserId(0L);
        category.setIsSystem(1);
        category.setParentId(0L);
        if (category.getStatus() == null) {
            category.setStatus(LedgerCategory.STATUS_ENABLED);
        }
        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
        boolean ok = categoryService.save(category);
        return ok ? success(category) : error("新增失败");
    }

    /**
     * 修改分类（保留原归属，系统预设/用户自定义均可编辑）
     */
    @Operation(summary = "修改分类（保留归属）")
    @PreAuthorize("@ss.hasPermi('cms:ledgerCategory:edit')")
    @PutMapping("/{id}")
    public AjaxResult edit(@PathVariable Long id, @RequestBody LedgerCategory category) {
        LedgerCategory exist = categoryService.getById(id);
        if (exist == null) {
            return error("分类不存在");
        }
        category.setId(id);
        // 保留归属与层级（不允许通过编辑把用户自定义改成系统预设或挪层级）
        category.setUserId(exist.getUserId());
        category.setIsSystem(exist.getIsSystem());
        category.setParentId(exist.getParentId());
        return categoryService.updateById(category) ? success() : error("修改失败");
    }

    /**
     * 启用/停用分类（下架不删数据，App 端不再展示，历史流水不受影响）
     */
    @Operation(summary = "启用/停用分类")
    @PreAuthorize("@ss.hasPermi('cms:ledgerCategory:edit')")
    @PutMapping("/{id}/status/{status}")
    public AjaxResult changeStatus(@PathVariable Long id, @PathVariable Integer status) {
        LedgerCategory exist = categoryService.getById(id);
        if (exist == null) {
            return error("分类不存在");
        }
        exist.setStatus(status != null && status == LedgerCategory.STATUS_ENABLED
                ? LedgerCategory.STATUS_ENABLED : LedgerCategory.STATUS_DISABLED);
        return categoryService.updateById(exist) ? success() : error("操作失败");
    }

    /**
     * 删除分类（真删除）
     *
     * <p>校验：已绑定有效流水 → 不能删除；存在子分类 → 不能删除。
     */
    @Operation(summary = "删除分类（绑定流水则拒绝）")
    @PreAuthorize("@ss.hasPermi('cms:ledgerCategory:edit')")
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        LedgerCategory exist = categoryService.getById(id);
        if (exist == null) {
            return error("分类不存在");
        }
        categoryService.assertCategoryDeletable(id);
        return categoryService.removeById(id) ? success() : error("删除失败");
    }
}
