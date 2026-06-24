package com.moyun.ext.cms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.portal.domain.entity.PortalHelpCategory;
import com.moyun.ext.cms.service.ICmsHelpCategoryService;
import com.moyun.util.bean.PageUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CMS 帮助分类管理 Controller
 *
 * @author moyun
 */
@Tag(name = "CMS帮助分类管理", description = "帮助中心分类的增删改查接口")
@RestController
@RequestMapping("/cms/help-category")
public class CmsHelpCategoryController extends BaseController {

    @Autowired
    private ICmsHelpCategoryService cmsHelpCategoryService;

    @Operation(summary = "查询帮助分类列表", description = "分页查询帮助分类")
    @PreAuthorize("@ss.hasPermi('cms:help-category:list')")
    @GetMapping("/list")
    public AjaxResult list(PortalHelpCategory category) {
        Page<PortalHelpCategory> page = PageUtils.startPage();
        cmsHelpCategoryService.selectHelpCategoryPage(page, category);
        return success(page);
    }

    @Operation(summary = "查询全部分类", description = "不分页查询所有分类（下拉选择用）")
    @GetMapping("/all")
    public AjaxResult all() {
        return success(cmsHelpCategoryService.selectHelpCategoryList(new PortalHelpCategory()));
    }

    @Operation(summary = "获取分类详情", description = "根据ID获取帮助分类详情")
    @PreAuthorize("@ss.hasPermi('cms:help-category:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(cmsHelpCategoryService.selectHelpCategoryById(id));
    }

    @Operation(summary = "新增分类", description = "新增帮助分类")
    @PreAuthorize("@ss.hasPermi('cms:help-category:add')")
    @Log(title = "帮助分类", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody PortalHelpCategory category) {
        category.setCreateBy(getUsername());
        return toAjax(cmsHelpCategoryService.insertHelpCategory(category));
    }

    @Operation(summary = "修改分类", description = "修改帮助分类")
    @PreAuthorize("@ss.hasPermi('cms:help-category:edit')")
    @Log(title = "帮助分类", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody PortalHelpCategory category) {
        category.setUpdateBy(getUsername());
        return toAjax(cmsHelpCategoryService.updateHelpCategory(category));
    }

    @Operation(summary = "删除分类", description = "批量删除帮助分类")
    @PreAuthorize("@ss.hasPermi('cms:help-category:remove')")
    @Log(title = "帮助分类", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(cmsHelpCategoryService.deleteHelpCategoryByIds(ids));
    }
}
