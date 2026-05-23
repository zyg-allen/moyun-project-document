package com.moyun.community.content.controller.admin;

import com.moyun.common.annotation.Log;
import com.moyun.common.core.controller.BaseController;
import com.moyun.common.core.domain.AjaxResult;
import com.moyun.common.core.domain.TableDataInfo;
import com.moyun.common.enums.BusinessType;
import com.moyun.community.content.entity.CmsCategory;
import com.moyun.community.content.service.ICmsCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/category")
public class CmsCategoryAdminController extends BaseController {

    @Autowired
    private ICmsCategoryService categoryService;

    @PreAuthorize("@ss.hasPermi('content:category:list')")
    @GetMapping("/list")
    public TableDataInfo list() {
        startPage();
        List<CmsCategory> list = categoryService.list();
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('content:category:query')")
    @GetMapping(value = "/{categoryId}")
    public AjaxResult getInfo(@PathVariable("categoryId") Long categoryId) {
        return success(categoryService.getById(categoryId));
    }

    @PreAuthorize("@ss.hasPermi('content:category:add')")
    @Log(title = "栏目管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody CmsCategory category) {
        return toAjax(categoryService.save(category));
    }

    @PreAuthorize("@ss.hasPermi('content:category:edit')")
    @Log(title = "栏目管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody CmsCategory category) {
        return toAjax(categoryService.updateById(category));
    }

    @PreAuthorize("@ss.hasPermi('content:category:remove')")
    @Log(title = "栏目管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{categoryIds}")
    public AjaxResult remove(@PathVariable Long[] categoryIds) {
        return toAjax(categoryService.removeByIds(List.of(categoryIds)));
    }
}