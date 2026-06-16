package com.moyun.portal.controller;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.moyun.common.annotation.Anonymous;
import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.portal.domain.entity.PortalCategory;
import com.moyun.portal.domain.query.CategoryQuery;
import com.moyun.portal.service.IPortalCategoryService;
import com.moyun.util.bean.PageUtils;
import com.moyun.util.file.ExcelUtil;

@Tag(name = "门户分类", description = "门户分类的增删改查操作接口")
@RestController
@RequestMapping("/portal/category")
public class PortalCategoryController extends BaseController {

    @Autowired
    private IPortalCategoryService portalCategoryService;

    @Operation(summary = "获取所有分类列表", description = "获取所有分类，不分页")
    @GetMapping("/list")
    public AjaxResult getAllCategories(CategoryQuery query) {
        List<PortalCategory> list = portalCategoryService.selectPortalCategoryList(query);
        return success(list);
    }

    @Operation(summary = "获取树形分类列表", description = "获取树形结构的分类")
    @GetMapping("/tree")
    public AjaxResult getCategoryTree(CategoryQuery query) {
        List<PortalCategory> list = portalCategoryService.selectPortalCategoryTree(query);
        return success(list);
    }

    @Anonymous
    @Operation(summary = "获取公开树形分类列表", description = "获取公开的树形分类")
    @GetMapping("/public/tree")
    public AjaxResult getPublicCategoryTree(CategoryQuery query) {
        query.setStatus("0");
        List<PortalCategory> list = portalCategoryService.selectPortalCategoryTree(query);
        return success(list);
    }

    @Operation(summary = "获取分类列表（分页）", description = "根据条件分页查询分类列表")
    @GetMapping("/page")
    public AjaxResult page(CategoryQuery query) {
        Page<PortalCategory> page = PageUtils.buildPage(query);
        Page<PortalCategory> resultPage = portalCategoryService.selectPortalCategoryPage(page, query);
        return success(resultPage);
    }

    @Operation(summary = "导出分类", description = "导出分类数据到Excel文件")
    @Log(title = "门户分类", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CategoryQuery query) {
        List<PortalCategory> list = portalCategoryService.selectPortalCategoryList(query);
        ExcelUtil<PortalCategory> util = new ExcelUtil<PortalCategory>(PortalCategory.class);
        util.exportExcel(response, list, "门户分类数据");
    }

    @Operation(summary = "获取分类详情", description = "根据分类ID获取分类详细信息")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@Parameter(description = "分类ID") @PathVariable Long id) {
        return success(portalCategoryService.selectPortalCategoryById(id));
    }

    @Operation(summary = "新增分类", description = "创建新分类")
    @Log(title = "门户分类", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody PortalCategory portalCategory) {
        return toAjax(portalCategoryService.insertPortalCategory(portalCategory));
    }

    @Operation(summary = "修改分类", description = "更新分类信息")
    @Log(title = "门户分类", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody PortalCategory portalCategory) {
        return toAjax(portalCategoryService.updatePortalCategory(portalCategory));
    }

    @Operation(summary = "删除分类", description = "批量删除分类")
    @Log(title = "门户分类", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@Parameter(description = "分类ID数组") @PathVariable Long[] ids) {
        return toAjax(portalCategoryService.deletePortalCategoryByIds(ids));
    }
}