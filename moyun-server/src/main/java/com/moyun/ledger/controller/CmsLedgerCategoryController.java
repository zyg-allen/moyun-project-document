package com.moyun.ledger.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ledger.domain.entity.LedgerCategory;
import com.moyun.ledger.service.ILedgerCategoryService;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.mapper.PortalUserMapper;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CMS 记账分类管理 Controller
 *
 * <p>层级模型：大类（交易类型）→ 一级分类（parent_id=0）→ 二级分类（parent_id=一级ID），最多两级。
 * <p>归属：系统预设（user_id=0 + is_system=1）由后台维护；
 * 用户自定义（user_id>0）只读展示、可查看不可改删（避免后台误删用户数据）。
 * <p>删除红线：已绑定有效流水（category_id 或 sub_category_id）、或存在子分类 → 一律拒绝。
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
    private PortalUserMapper portalUserMapper;

    /**
     * 分类树列表（一级分页，自带其二级子分类）
     *
     * @param type     大类筛选（空=全部）
     * @param name     名称模糊搜索
     * @param owner    归属：system=系统预设 / user=用户自定义
     * @param pageNum  页码
     * @param pageSize 每页条数（按一级分类计数）
     */
    @Operation(summary = "分类树列表（一级分页 + 二级嵌套）")
    @PreAuthorize("@ss.hasPermi('cms:ledgerCategory:list')")
    @GetMapping("/list")
    public AjaxResult list(String type, String name, String owner,
                           @RequestParam(defaultValue = "1") Integer pageNum,
                           @RequestParam(defaultValue = "10") Integer pageSize) {
        LambdaQueryWrapper<LedgerCategory> qw = new LambdaQueryWrapper<>();
        // 只分页一级分类，二级随父一起返回
        qw.apply("(parent_id IS NULL OR parent_id = 0)");
        if (type != null && !type.isEmpty()) {
            qw.eq(LedgerCategory::getType, type);
        }
        if (name != null && !name.isEmpty()) {
            qw.like(LedgerCategory::getName, name.trim());
        }
        if ("system".equals(owner)) {
            qw.eq(LedgerCategory::getUserId, 0).eq(LedgerCategory::getIsSystem, 1);
        } else if ("user".equals(owner)) {
            qw.gt(LedgerCategory::getUserId, 0);
        }
        qw.orderByAsc(LedgerCategory::getType).orderByAsc(LedgerCategory::getSortOrder);

        IPage<LedgerCategory> page = categoryService.page(new Page<>(pageNum, pageSize), qw);
        List<LedgerCategory> roots = page.getRecords();

        if (!roots.isEmpty()) {
            List<Long> rootIds = roots.stream().map(LedgerCategory::getId).toList();
            List<LedgerCategory> children = categoryService.list(new LambdaQueryWrapper<LedgerCategory>()
                    .in(LedgerCategory::getParentId, rootIds)
                    .orderByAsc(LedgerCategory::getSortOrder).orderByAsc(LedgerCategory::getId));
            Map<Long, List<LedgerCategory>> byParent = children.stream()
                    .collect(Collectors.groupingBy(LedgerCategory::getParentId));
            roots.forEach(r -> r.setChildren(byParent.getOrDefault(r.getId(), new ArrayList<>())));
            fillUserNames(roots);
            roots.forEach(r -> fillUserNames(r.getChildren()));
        }
        return success(Map.of("records", roots, "total", page.getTotal()));
    }

    /** 回填归属用户名（user_id=0 显示"系统预设"，>0 取 portal_user 昵称/账号） */
    private void fillUserNames(List<LedgerCategory> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Long> userIds = list.stream().map(LedgerCategory::getUserId)
                .filter(uid -> uid != null && uid > 0).distinct().toList();
        Map<Long, PortalUser> userMap = userIds.isEmpty() ? Map.of()
                : portalUserMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(PortalUser::getId, u -> u));
        list.forEach(c -> {
            Long uid = c.getUserId();
            if (uid == null || uid == 0) {
                c.setUserName("系统预设");
                return;
            }
            PortalUser u = userMap.get(uid);
            if (u == null) {
                c.setUserName("用户#" + uid);
            } else if (u.getNickname() != null && !u.getNickname().isBlank()) {
                c.setUserName(u.getNickname());
            } else {
                c.setUserName(u.getUsername());
            }
        });
    }

    @Operation(summary = "新增系统预设分类（可挂一级或二级）")
    @PreAuthorize("@ss.hasPermi('cms:ledgerCategory:add')")
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
