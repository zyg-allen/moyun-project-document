package com.moyun.ext.cms.controller;

import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ext.cms.domain.query.CmsCategoryQuery;
import com.moyun.ext.cms.domain.vo.CmsCategoryVO;
import com.moyun.ext.cms.service.ICmsCategoryService;
import com.moyun.portal.domain.entity.PortalCategory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CMS分类管理Controller
 *
 * @author moyun
 */
@Tag(name = "CMS分类管理", description = "CMS分类管理接口")
@RestController
@RequestMapping("/cms/category")
public class CmsCategoryController extends BaseController {
    @Autowired
    private ICmsCategoryService cmsCategoryService;

    /**
     * 获取分类列表
     */
    @Operation(summary = "获取分类列表", description = "获取分类列表")
    @PreAuthorize("@ss.hasPermi('cms:category:list')")
    @GetMapping("/list")
    public AjaxResult list(CmsCategoryQuery query) {
        List<CmsCategoryVO> list = cmsCategoryService.selectCategoryList(query);
        return success(list);
    }

    /**
     * 获取分类树
     */
    @Operation(summary = "获取分类树", description = "获取分类树形结构")
    @PreAuthorize("@ss.hasPermi('cms:category:list')")
    @GetMapping("/tree")
    public AjaxResult tree(CmsCategoryQuery query) {
        List<CmsCategoryVO> tree = cmsCategoryService.selectCategoryTree(query);
        return success(tree);
    }

    /**
     * 根据分类编号获取详细信息
     */
    @Operation(summary = "获取分类详情", description = "根据分类ID获取分类详细信息")
    @PreAuthorize("@ss.hasPermi('cms:category:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@Parameter(description = "分类ID") @PathVariable Long id) {
        return success(cmsCategoryService.selectCategoryById(id));
    }

    /**
     * 新增分类
     */
    @Operation(summary = "新增分类", description = "创建新分类")
    @PreAuthorize("@ss.hasPermi('cms:category:add')")
    @Log(title = "分类管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody PortalCategory category) {
        return toAjax(cmsCategoryService.insertCategory(category));
    }

    /**
     * 修改分类
     */
    @Operation(summary = "修改分类", description = "更新分类信息")
    @PreAuthorize("@ss.hasPermi('cms:category:edit')")
    @Log(title = "分类管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody PortalCategory category) {
        return toAjax(cmsCategoryService.updateCategory(category));
    }

    /**
     * 删除分类
     */
    @Operation(summary = "删除分类", description = "批量删除分类")
    @PreAuthorize("@ss.hasPermi('cms:category:remove')")
    @Log(title = "分类管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@Parameter(description = "分类ID数组") @PathVariable Long[] ids) {
        for (Long id : ids) {
            if (cmsCategoryService.hasChildCategory(id)) {
                return error("存在子分类，不能删除");
            }
        }
        return toAjax(cmsCategoryService.deleteCategoryByIds(ids));
    }
}
