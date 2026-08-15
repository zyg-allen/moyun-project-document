package com.moyun.ext.cms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.portal.domain.entity.PortalVipPackage;
import com.moyun.portal.domain.query.VipPackageQuery;
import com.moyun.portal.service.IPortalVipPackageService;
import com.moyun.util.bean.PageUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * CMS VIP 套餐管理 Controller
 *
 * 后台管理 VIP 套餐的增删改查，复用 Portal 侧 Service。
 *
 * @author moyun
 */
@Tag(name = "CMS VIP套餐管理", description = "VIP套餐的增删改查接口")
@RestController
@RequestMapping("/cms/vip")
public class CmsVipController extends BaseController {

    @Autowired
    private IPortalVipPackageService portalVipPackageService;

    @Operation(summary = "查询VIP套餐列表", description = "分页查询VIP套餐")
    @PreAuthorize("@ss.hasPermi('cms:vip:list')")
    @GetMapping("/list")
    public AjaxResult list(VipPackageQuery query) {
        Page<PortalVipPackage> page = PageUtils.startPage();
        portalVipPackageService.selectPortalVipPackagePage(page, query);
        return success(page);
    }

    @Operation(summary = "获取VIP套餐详情", description = "根据ID获取VIP套餐详情")
    @PreAuthorize("@ss.hasPermi('cms:vip:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(portalVipPackageService.selectPortalVipPackageById(id));
    }

    @Operation(summary = "新增VIP套餐", description = "新增VIP套餐")
    @PreAuthorize("@ss.hasPermi('cms:vip:add')")
    @Log(title = "VIP套餐", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody PortalVipPackage vipPackage) {
        vipPackage.setCreateBy(getUsername());
        return toAjax(portalVipPackageService.insertPortalVipPackage(vipPackage));
    }

    @Operation(summary = "修改VIP套餐", description = "修改VIP套餐")
    @PreAuthorize("@ss.hasPermi('cms:vip:edit')")
    @Log(title = "VIP套餐", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody PortalVipPackage vipPackage) {
        vipPackage.setUpdateBy(getUsername());
        return toAjax(portalVipPackageService.updatePortalVipPackage(vipPackage));
    }

    @Operation(summary = "删除VIP套餐", description = "批量删除VIP套餐")
    @PreAuthorize("@ss.hasPermi('cms:vip:remove')")
    @Log(title = "VIP套餐", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(portalVipPackageService.deletePortalVipPackageByIds(ids));
    }
}
