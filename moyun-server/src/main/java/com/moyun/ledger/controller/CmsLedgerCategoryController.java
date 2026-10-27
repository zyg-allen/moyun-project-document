package com.moyun.ledger.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.annotation.Log;
import com.moyun.common.annotation.RepeatSubmit;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.TableDataInfo;
import com.moyun.ledger.domain.entity.LedgerCategory;
import com.moyun.ledger.domain.entity.LedgerTransaction;
import com.moyun.ledger.mapper.LedgerTransactionMapper;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 后台-记账分类管理
 *
 * <p>层级：交易类型 → 一级分类（parent_id=0）→ 二级分类（parent_id=一级 id），最多两级。</p>
 *
 * <p>合规口径：
 * <ul>
 *   <li>系统预设（user_id=0 + is_system=1）可维护；</li>
 *   <li>用户自建（user_id&gt;0）属用户数据，<b>只读</b>，且列表只显示「用户#编号」，不暴露昵称；</li>
 *   <li>已绑定有效流水 / 被作为二级引用 / 存在子分类 → 禁止删除，只能停用。</li>
 * </ul>
 *
 * @author moyun
 */
@Tag(name = "CMS记账分类", description = "分类维护（两级树 + 系统预设专管）")
@RestController
@RequestMapping("/cms/ledger/category")
public class CmsLedgerCategoryController extends BaseController {

    @Autowired
    private ILedgerCategoryService categoryService;

    @Autowired
    private LedgerTransactionMapper transactionMapper;

    @Operation(summary = "分类树列表（一级分页 + 二级嵌套 + 引用笔数）")
    @PreAuthorize("@ss.hasPermi('cms:ledgerCategory:list')")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(required = false) String type,
                              @RequestParam(required = false) String name,
                              @RequestParam(required = false) String owner,
                              @RequestParam(required = false) Integer status,
                              @RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize) {
        LambdaQueryWrapper<LedgerCategory> qw = new LambdaQueryWrapper<>();
        // 只分页一级分类，二级随父一起返回
        qw.and(w -> w.isNull(LedgerCategory::getParentId).or().eq(LedgerCategory::getParentId, 0L));
        if (type != null && !type.isEmpty()) {
            qw.eq(LedgerCategory::getType, type);
        }
        if (name != null && !name.isBlank()) {
            qw.like(LedgerCategory::getName, name.trim());
        }
        if ("system".equals(owner)) {
            qw.eq(LedgerCategory::getUserId, 0L).eq(LedgerCategory::getIsSystem, 1);
        } else if ("user".equals(owner)) {
            qw.gt(LedgerCategory::getUserId, 0L);
        }
        if (status != null) {
            qw.eq(LedgerCategory::getStatus, status);
        }
        qw.orderByAsc(LedgerCategory::getType).orderByAsc(LedgerCategory::getSortOrder);

        Page<LedgerCategory> page = categoryService.page(new Page<>(pageNum, pageSize), qw);
        List<LedgerCategory> roots = page.getRecords();

        if (!roots.isEmpty()) {
            List<Long> rootIds = roots.stream().map(LedgerCategory::getId).toList();
            List<LedgerCategory> children = categoryService.list(new LambdaQueryWrapper<LedgerCategory>()
                    .in(LedgerCategory::getParentId, rootIds)
                    .orderByAsc(LedgerCategory::getSortOrder).orderByAsc(LedgerCategory::getId));
            Map<Long, List<LedgerCategory>> byParent = children.stream()
                    .collect(Collectors.groupingBy(LedgerCategory::getParentId));
            roots.forEach(r -> r.setChildren(byParent.getOrDefault(r.getId(), new ArrayList<>())));
            fillOwnerLabels(roots);
            fillOwnerLabels(children);
            fillUsedCounts(roots, children);
        }
        return getDataTable(roots, page.getTotal());
    }

    /** 只回填「用户#编号」，不返回昵称（用户数据最小必要） */
    private void fillOwnerLabels(List<LedgerCategory> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        list.forEach(c -> {
            Long uid = c.getUserId();
            c.setUserName(uid == null || uid == 0 ? "系统预设" : "用户#" + uid);
        });
    }

    /** 引用笔数：category_id 与 sub_category_id 分别聚合，父类目含子类目汇总 */
    private void fillUsedCounts(List<LedgerCategory> roots, List<LedgerCategory> children) {
        List<Long> allIds = new ArrayList<>(roots.stream().map(LedgerCategory::getId).toList());
        children.forEach(c -> allIds.add(c.getId()));
        Map<Long, Long> byCategory = countByColumn("category_id", allIds);
        Map<Long, Long> bySubCategory = countByColumn("sub_category_id", allIds);

        children.forEach(c -> c.setUsedCount(
                byCategory.getOrDefault(c.getId(), 0L) + bySubCategory.getOrDefault(c.getId(), 0L)));
        roots.forEach(r -> {
            long total = byCategory.getOrDefault(r.getId(), 0L) + bySubCategory.getOrDefault(r.getId(), 0L);
            for (LedgerCategory child : r.getChildren()) {
                total += byCategory.getOrDefault(child.getId(), 0L) + bySubCategory.getOrDefault(child.getId(), 0L);
            }
            r.setUsedCount(total);
        });
    }

    private Map<Long, Long> countByColumn(String column, List<Long> ids) {
        Map<Long, Long> result = new HashMap<>();
        if (ids.isEmpty()) {
            return result;
        }
        QueryWrapper<LedgerTransaction> qw = new QueryWrapper<>();
        qw.select(column, "COUNT(*) AS cnt")
                .eq("status", LedgerTransaction.STATUS_NORMAL)
                .in(column, ids)
                .groupBy(column);
        for (Map<String, Object> row : transactionMapper.selectMaps(qw)) {
            Object id = row.get(column);
            Object cnt = row.get("cnt");
            if (id != null) {
                result.put(Long.valueOf(id.toString()), Long.valueOf(cnt.toString()));
            }
        }
        return result;
    }

    @Operation(summary = "新增系统预设分类（可挂一级或二级）")
    @PreAuthorize("@ss.hasPermi('cms:ledgerCategory:add')")
    @RepeatSubmit(interval = 3000)
    @Log(title = "记账-预设分类", businessType = BusinessType.INSERT)
    @com.moyun.common.annotation.RateLimiter(key = "cmsledgercategory:", time = 60, count = 120)
    @PostMapping
    public AjaxResult add(@RequestBody LedgerCategory category) {
        category.setId(null);
        category.setUserId(0L);
        category.setIsSystem(1);
        // parentId 空/0 = 一级分类，统一存 NULL（与既有预设数据一致，避免 0 与 NULL 两套表达）
        Long pid = category.getParentId();
        if (pid != null && pid == 0L) {
            pid = null;
        }
        category.setParentId(pid);
        categoryService.assertParentValid(pid, category.getType());
        categoryService.assertNameUnique(0L, category.getType(), pid, category.getName(), null);
        if (category.getStatus() == null) {
            category.setStatus(LedgerCategory.STATUS_ENABLED);
        }
        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
        boolean ok = categoryService.save(category);
        return ok ? success(category) : error("新增失败");
    }

    @Operation(summary = "修改系统预设分类")
    @PreAuthorize("@ss.hasPermi('cms:ledgerCategory:edit')")
    @RepeatSubmit(interval = 3000)
    @Log(title = "记账-预设分类", businessType = BusinessType.UPDATE)
    @com.moyun.common.annotation.RateLimiter(key = "cmsledgercategory:", time = 60, count = 120)
    @PutMapping("/{id}")
    public AjaxResult edit(@PathVariable Long id, @RequestBody LedgerCategory category) {
        LedgerCategory exist = categoryService.getById(id);
        if (exist == null) {
            return error("分类不存在");
        }
        if (isUserOwned(exist)) {
            return error("用户自定义分类不归后台维护，不能修改");
        }
        category.setId(id);
        category.setUserId(exist.getUserId());
        category.setIsSystem(exist.getIsSystem());
        // parentId 0/null 都归一为一级（存 NULL）
        Long pid = category.getParentId();
        if (pid == null) {
            pid = exist.getParentId();
        }
        if (pid != null && pid == 0L) {
            pid = null;
        }
        category.setParentId(pid);
        categoryService.assertParentValid(pid, category.getType());
        categoryService.assertNameUnique(exist.getUserId(), category.getType(),
                pid, category.getName(), id);
        boolean ok = categoryService.updateById(category);
        // updateById 跳过 null 字段：从二级改回一级时，需显式把 parent_id 置空
        if (ok && pid == null && exist.getParentId() != null) {
            categoryService.lambdaUpdate()
                    .eq(LedgerCategory::getId, id)
                    .set(LedgerCategory::getParentId, null)
                    .update();
        }
        return ok ? success() : error("修改失败");
    }

    @Operation(summary = "启用/停用系统预设分类")
    @PreAuthorize("@ss.hasPermi('cms:ledgerCategory:edit')")
    @Log(title = "记账-预设分类", businessType = BusinessType.UPDATE)
    @com.moyun.common.annotation.RepeatSubmit(interval = 2000)
    @com.moyun.common.annotation.RateLimiter(key = "cmsledgercategory:", time = 60, count = 120)
    @PutMapping("/{id}/status/{status}")
    public AjaxResult changeStatus(@PathVariable Long id, @PathVariable Integer status) {
        LedgerCategory exist = categoryService.getById(id);
        if (exist == null) {
            return error("分类不存在");
        }
        if (isUserOwned(exist)) {
            return error("用户自定义分类不归后台维护，不能停用");
        }
        exist.setStatus(status != null && status == LedgerCategory.STATUS_ENABLED
                ? LedgerCategory.STATUS_ENABLED : LedgerCategory.STATUS_DISABLED);
        return categoryService.updateById(exist) ? success() : error("操作失败");
    }

    @Operation(summary = "删除系统预设分类（校验流水与子分类引用）")
    @PreAuthorize("@ss.hasPermi('cms:ledgerCategory:edit')")
    @Log(title = "记账-预设分类", businessType = BusinessType.DELETE)
    @com.moyun.common.annotation.RepeatSubmit(interval = 2000)
    @com.moyun.common.annotation.RateLimiter(key = "cmsledgercategory:", time = 60, count = 120)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        LedgerCategory exist = categoryService.getById(id);
        if (exist == null) {
            return error("分类不存在");
        }
        if (isUserOwned(exist)) {
            return error("用户自定义分类不归后台维护，不能删除");
        }
        categoryService.assertCategoryDeletable(id);
        return categoryService.removeById(id) ? success() : error("删除失败");
    }

    /** 是否用户自定义（非系统预设） */
    private boolean isUserOwned(LedgerCategory c) {
        return c.getUserId() != null && c.getUserId() > 0;
    }
}
