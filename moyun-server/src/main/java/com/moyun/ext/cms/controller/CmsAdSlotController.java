package com.moyun.ext.cms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.portal.domain.entity.PortalAdSlot;
import com.moyun.portal.domain.query.AdSlotQuery;
import com.moyun.portal.service.IPortalAdSlotService;
import com.moyun.util.bean.PageUtils;

@Tag(name = "CMS广告位管理", description = "CMS广告位管理接口")
@RestController
@RequestMapping("/cms/ad")
public class CmsAdSlotController extends BaseController
{
    @Autowired
    private IPortalAdSlotService portalAdSlotService;

    @Operation(summary = "获取广告位列表", description = "根据条件分页查询广告位列表")
    @PreAuthorize("@ss.hasPermi('portal:ad:list')")
    @GetMapping("/list")
    public AjaxResult list(AdSlotQuery query)
    {
        Page<PortalAdSlot> page = PageUtils.buildPage(query);
        page = portalAdSlotService.selectPortalAdSlotPage(page, query);
        return success(page);
    }

    @Operation(summary = "获取广告位详情", description = "根据广告位ID获取广告位详细信息")
    @PreAuthorize("@ss.hasPermi('portal:ad:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@Parameter(description = "广告位ID") @PathVariable Long id)
    {
        return success(portalAdSlotService.selectPortalAdSlotById(id));
    }

    @Operation(summary = "新增广告位", description = "创建新广告位")
    @PreAuthorize("@ss.hasPermi('portal:ad:add')")
    @Log(title = "广告位管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody PortalAdSlot portalAdSlot)
    {
        return toAjax(portalAdSlotService.insertPortalAdSlot(portalAdSlot));
    }

    @Operation(summary = "修改广告位", description = "更新广告位信息")
    @PreAuthorize("@ss.hasPermi('portal:ad:edit')")
    @Log(title = "广告位管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody PortalAdSlot portalAdSlot)
    {
        return toAjax(portalAdSlotService.updatePortalAdSlot(portalAdSlot));
    }

    @Operation(summary = "删除广告位", description = "批量删除广告位")
    @PreAuthorize("@ss.hasPermi('portal:ad:remove')")
    @Log(title = "广告位管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@Parameter(description = "广告位ID数组") @PathVariable Long[] ids)
    {
        return toAjax(portalAdSlotService.deletePortalAdSlotByIds(ids));
    }
}
