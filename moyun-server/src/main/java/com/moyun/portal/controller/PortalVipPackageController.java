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

import com.moyun.common.annotation.Log;
import com.moyun.common.constant.HttpStatus;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.portal.domain.entity.PortalVipPackage;
import com.moyun.portal.domain.query.VipPackageQuery;
import com.moyun.portal.service.IPortalVipPackageService;
import com.moyun.portal.util.PortalSecurityUtils;
import com.moyun.util.bean.PageUtils;
import com.moyun.util.file.ExcelUtil;

/**
 * 门户 VIP 套餐 Controller（v7.0 第六阶段预留）
 *
 * <p>安全说明：套餐列表/详情允许游客查询（用于售卖页展示），
 * 写操作（export/add/edit/remove）仅管理员可调用。</p>
 */
@Tag(name = "门户VIP套餐", description = "门户VIP套餐的增删改查操作接口")
@RestController
@RequestMapping("/portal/vip-package")
public class PortalVipPackageController extends BaseController {

    @Autowired
    private IPortalVipPackageService portalVipPackageService;

    @Operation(summary = "获取VIP套餐列表", description = "根据条件分页查询VIP套餐列表")
    @GetMapping("/list")
    public AjaxResult list(VipPackageQuery query) {
        Page<PortalVipPackage> page = PageUtils.buildPage(query);
        Page<PortalVipPackage> resultPage = portalVipPackageService.selectPortalVipPackagePage(page, query);
        return success(resultPage);
    }

    @Operation(summary = "导出VIP套餐", description = "导出VIP套餐数据到Excel文件（仅管理员）")
    @Log(title = "门户VIP套餐", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, VipPackageQuery query) {
        if (!PortalSecurityUtils.isAdmin()) {
            throw new com.moyun.common.exception.system.ServiceException("无权限，仅管理员可操作");
        }
        List<PortalVipPackage> list = portalVipPackageService.selectPortalVipPackageList(query);
        ExcelUtil<PortalVipPackage> util = new ExcelUtil<PortalVipPackage>(PortalVipPackage.class);
        util.exportExcel(response, list, "门户VIP套餐数据");
    }

    @Operation(summary = "获取VIP套餐详情", description = "根据VIP套餐ID获取VIP套餐详细信息")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@Parameter(description = "VIP套餐ID") @PathVariable Long id) {
        return success(portalVipPackageService.selectPortalVipPackageById(id));
    }

    @Operation(summary = "新增VIP套餐", description = "创建新VIP套餐（仅管理员）")
    @Log(title = "门户VIP套餐", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody PortalVipPackage portalVipPackage) {
        if (!PortalSecurityUtils.isAdmin()) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "无权限，仅管理员可操作");
        }
        return toAjax(portalVipPackageService.insertPortalVipPackage(portalVipPackage));
    }

    @Operation(summary = "修改VIP套餐", description = "更新VIP套餐信息（仅管理员）")
    @Log(title = "门户VIP套餐", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody PortalVipPackage portalVipPackage) {
        if (!PortalSecurityUtils.isAdmin()) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "无权限，仅管理员可操作");
        }
        return toAjax(portalVipPackageService.updatePortalVipPackage(portalVipPackage));
    }

    @Operation(summary = "删除VIP套餐", description = "批量删除VIP套餐（仅管理员）")
    @Log(title = "门户VIP套餐", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@Parameter(description = "VIP套餐ID数组") @PathVariable Long[] ids) {
        if (!PortalSecurityUtils.isAdmin()) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "无权限，仅管理员可操作");
        }
        return toAjax(portalVipPackageService.deletePortalVipPackageByIds(ids));
    }
}