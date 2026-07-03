package com.moyun.portal.controller;

import java.util.Map;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ext.cms.service.ICmsBookClubService;
import com.moyun.portal.domain.entity.PortalBookClubActivity;
import com.moyun.util.bean.PageUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * CMS 共读活动后台管理 Controller
 * <p>
 * 提供活动列表/详情/创建/更新/删除/上下架。
 * 路径前缀 /cms/reading/club。
 *
 * @author moyun
 */
@Tag(name = "CMS共读活动管理", description = "共读活动后台管理接口")
@RestController
@RequestMapping("/cms/reading/club")
public class CmsBookClubController extends BaseController {

    @Autowired
    private ICmsBookClubService cmsBookClubService;

    @Operation(summary = "查询活动列表", description = "分页查询共读活动（含所有状态）")
    @PreAuthorize("@ss.hasPermi('portal:bookClub:list')")
    @GetMapping("/list")
    public AjaxResult list(PortalBookClubActivity query) {
        Page<PortalBookClubActivity> page = PageUtils.startPage();
        cmsBookClubService.selectActivityPage(page, query);
        return success(page);
    }

    @Operation(summary = "获取活动详情", description = "根据ID获取共读活动详情")
    @PreAuthorize("@ss.hasPermi('portal:bookClub:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(cmsBookClubService.selectActivityById(id));
    }

    @Operation(summary = "新增活动", description = "新增共读活动")
    @PreAuthorize("@ss.hasPermi('portal:bookClub:add')")
    @Log(title = "共读活动", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody PortalBookClubActivity activity) {
        activity.setCreateBy(getUsername());
        if (activity.getCreatedBy() == null) {
            activity.setCreatedBy(getUserId());
        }
        return toAjax(cmsBookClubService.insertActivity(activity));
    }

    @Operation(summary = "修改活动", description = "修改共读活动")
    @PreAuthorize("@ss.hasPermi('portal:bookClub:edit')")
    @Log(title = "共读活动", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody PortalBookClubActivity activity) {
        activity.setUpdateBy(getUsername());
        return toAjax(cmsBookClubService.updateActivity(activity));
    }

    @Operation(summary = "删除活动", description = "批量删除共读活动")
    @PreAuthorize("@ss.hasPermi('portal:bookClub:remove')")
    @Log(title = "共读活动", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(cmsBookClubService.deleteActivityByIds(ids));
    }

    @Operation(summary = "上下架活动", description = "更新活动状态：upcoming/ongoing/ended")
    @PreAuthorize("@ss.hasPermi('portal:bookClub:edit')")
    @Log(title = "共读活动", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/status")
    public AjaxResult changeStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String status = body.get("status") == null ? null : String.valueOf(body.get("status"));
        return toAjax(cmsBookClubService.updateActivityStatus(id, status));
    }
}
