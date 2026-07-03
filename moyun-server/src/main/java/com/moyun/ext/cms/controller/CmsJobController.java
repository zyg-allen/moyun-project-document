package com.moyun.ext.cms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ext.cms.service.ICmsJobService;
import com.moyun.portal.domain.entity.PortalJob;
import com.moyun.util.bean.PageUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * CMS 职位管理 Controller
 * <p>
 * 提供职位 CRUD。
 *
 * @author moyun
 */
@Tag(name = "CMS职位管理", description = "职位的增删改查接口")
@RestController
@RequestMapping("/cms/job")
public class CmsJobController extends BaseController {

    @Autowired
    private ICmsJobService cmsJobService;

    @Operation(summary = "查询职位列表", description = "分页查询职位（含所有状态）")
    @PreAuthorize("@ss.hasPermi('portal:job:list')")
    @GetMapping("/list")
    public AjaxResult list(PortalJob job) {
        Page<PortalJob> page = PageUtils.startPage();
        cmsJobService.selectJobPage(page, job);
        return success(page);
    }

    @Operation(summary = "获取职位详情", description = "根据ID获取职位详情")
    @PreAuthorize("@ss.hasPermi('portal:job:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(cmsJobService.selectJobById(id));
    }

    @Operation(summary = "新增职位", description = "新增职位")
    @PreAuthorize("@ss.hasPermi('portal:job:add')")
    @Log(title = "职位", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody PortalJob job) {
        job.setCreateBy(getUsername());
        return toAjax(cmsJobService.insertJob(job));
    }

    @Operation(summary = "修改职位", description = "修改职位")
    @PreAuthorize("@ss.hasPermi('portal:job:edit')")
    @Log(title = "职位", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody PortalJob job) {
        job.setUpdateBy(getUsername());
        return toAjax(cmsJobService.updateJob(job));
    }

    @Operation(summary = "删除职位", description = "批量删除职位")
    @PreAuthorize("@ss.hasPermi('portal:job:remove')")
    @Log(title = "职位", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(cmsJobService.deleteJobByIds(ids));
    }
}
