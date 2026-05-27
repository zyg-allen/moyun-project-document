package com.moyun.ext.cms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.TableDataInfo;
import com.moyun.ext.cms.domain.query.CmsTagQuery;
import com.moyun.ext.cms.domain.vo.CmsTagVO;
import com.moyun.ext.cms.service.ICmsTagService;
import com.moyun.portal.domain.entity.PortalTag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * CMS标签管理Controller
 *
 * @author moyun
 */
@Tag(name = "CMS标签管理", description = "CMS标签管理接口")
@RestController
@RequestMapping("/cms/tag")
public class CmsTagController extends BaseController {
    @Autowired
    private ICmsTagService cmsTagService;

    /**
     * 获取标签列表
     */
    @Operation(summary = "获取标签列表", description = "根据条件分页查询标签列表")
    @PreAuthorize("@ss.hasPermi('cms:tag:list')")
    @GetMapping("/list")
    public TableDataInfo list(CmsTagQuery query) {
        startPage();
        Page<CmsTagVO> page = cmsTagService.selectTagPage(query);
        return getDataTable(page.getRecords(), page.getTotal());
    }

    /**
     * 根据标签编号获取详细信息
     */
    @Operation(summary = "获取标签详情", description = "根据标签ID获取标签详细信息")
    @PreAuthorize("@ss.hasPermi('cms:tag:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@Parameter(description = "标签ID") @PathVariable Long id) {
        return success(cmsTagService.selectTagById(id));
    }

    /**
     * 新增标签
     */
    @Operation(summary = "新增标签", description = "创建新标签")
    @PreAuthorize("@ss.hasPermi('cms:tag:add')")
    @Log(title = "标签管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody PortalTag tag) {
        return toAjax(cmsTagService.insertTag(tag));
    }

    /**
     * 修改标签
     */
    @Operation(summary = "修改标签", description = "更新标签信息")
    @PreAuthorize("@ss.hasPermi('cms:tag:edit')")
    @Log(title = "标签管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody PortalTag tag) {
        return toAjax(cmsTagService.updateTag(tag));
    }

    /**
     * 删除标签
     */
    @Operation(summary = "删除标签", description = "批量删除标签")
    @PreAuthorize("@ss.hasPermi('cms:tag:remove')")
    @Log(title = "标签管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@Parameter(description = "标签ID数组") @PathVariable Long[] ids) {
        return toAjax(cmsTagService.deleteTagByIds(ids));
    }
}
