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
import com.moyun.portal.domain.entity.PortalOrder;
import com.moyun.portal.domain.query.OrderQuery;
import com.moyun.portal.service.IPortalOrderService;
import com.moyun.portal.util.PortalSecurityUtils;
import com.moyun.util.bean.PageUtils;
import com.moyun.util.file.ExcelUtil;

/**
 * 门户订单 Controller（v7.0 第六阶段预留）
 *
 * <p>安全说明：订单属敏感资源，读操作（list/getInfo）需登录，
 * 写操作（export/add/edit/remove）仅管理员可调用。</p>
 */
@Tag(name = "门户订单", description = "门户订单的增删改查操作接口")
@RestController
@RequestMapping("/portal/order")
public class PortalOrderController extends BaseController {

    @Autowired
    private IPortalOrderService portalOrderService;

    @Operation(summary = "获取订单列表", description = "根据条件分页查询订单列表")
    @GetMapping("/list")
    public AjaxResult list(OrderQuery query) {
        if (PortalSecurityUtils.getUserId() == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        Page<PortalOrder> page = PageUtils.buildPage(query);
        Page<PortalOrder> resultPage = portalOrderService.selectPortalOrderPage(page, query);
        return success(resultPage);
    }

    @Operation(summary = "导出订单", description = "导出订单数据到Excel文件（仅管理员）")
    @Log(title = "门户订单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, OrderQuery query) {
        if (!PortalSecurityUtils.isAdmin()) {
            throw new com.moyun.common.exception.system.ServiceException("无权限，仅管理员可操作");
        }
        List<PortalOrder> list = portalOrderService.selectPortalOrderList(query);
        ExcelUtil<PortalOrder> util = new ExcelUtil<PortalOrder>(PortalOrder.class);
        util.exportExcel(response, list, "门户订单数据");
    }

    @Operation(summary = "获取订单详情", description = "根据订单ID获取订单详细信息")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@Parameter(description = "订单ID") @PathVariable Long id) {
        if (PortalSecurityUtils.getUserId() == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        return success(portalOrderService.selectPortalOrderById(id));
    }

    @Operation(summary = "新增订单", description = "创建新订单（仅管理员）")
    @Log(title = "门户订单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody PortalOrder portalOrder) {
        if (!PortalSecurityUtils.isAdmin()) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "无权限，仅管理员可操作");
        }
        return toAjax(portalOrderService.insertPortalOrder(portalOrder));
    }

    @Operation(summary = "修改订单", description = "更新订单信息（仅管理员）")
    @Log(title = "门户订单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody PortalOrder portalOrder) {
        if (!PortalSecurityUtils.isAdmin()) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "无权限，仅管理员可操作");
        }
        return toAjax(portalOrderService.updatePortalOrder(portalOrder));
    }

    @Operation(summary = "删除订单", description = "批量删除订单（仅管理员）")
    @Log(title = "门户订单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@Parameter(description = "订单ID数组") @PathVariable Long[] ids) {
        if (!PortalSecurityUtils.isAdmin()) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "无权限，仅管理员可操作");
        }
        return toAjax(portalOrderService.deletePortalOrderByIds(ids));
    }
}
