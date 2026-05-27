package com.moyun.portal.controller;

import com.moyun.common.annotation.Log;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.TableDataInfo;
import com.moyun.common.enums.BusinessType;
import com.moyun.util.file.ExcelUtil;
import com.moyun.portal.domain.entity.PortalOrder;
import com.moyun.portal.service.IPortalOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "门户订单", description = "门户订单的增删改查操作接口")
@RestController
@RequestMapping("/portal/order")
public class PortalOrderController extends BaseController {

    @Autowired
    private IPortalOrderService portalOrderService;

    @Operation(summary = "获取订单列表", description = "根据条件分页查询订单列表")
    @GetMapping("/list")
    public TableDataInfo list(PortalOrder portalOrder) {
        startPage();
        List<PortalOrder> list = portalOrderService.selectPortalOrderList(portalOrder);
        return getDataTable(list);
    }

    @Operation(summary = "导出订单", description = "导出订单数据到Excel文件")
    @Log(title = "门户订单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, PortalOrder portalOrder) {
        List<PortalOrder> list = portalOrderService.selectPortalOrderList(portalOrder);
        ExcelUtil<PortalOrder> util = new ExcelUtil<PortalOrder>(PortalOrder.class);
        util.exportExcel(response, list, "门户订单数据");
    }

    @Operation(summary = "获取订单详情", description = "根据订单ID获取订单详细信息")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@Parameter(description = "订单ID") @PathVariable Long id) {
        return success(portalOrderService.selectPortalOrderById(id));
    }

    @Operation(summary = "新增订单", description = "创建新订单")
    @Log(title = "门户订单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody PortalOrder portalOrder) {
        return toAjax(portalOrderService.insertPortalOrder(portalOrder));
    }

    @Operation(summary = "修改订单", description = "更新订单信息")
    @Log(title = "门户订单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody PortalOrder portalOrder) {
        return toAjax(portalOrderService.updatePortalOrder(portalOrder));
    }

    @Operation(summary = "删除订单", description = "批量删除订单")
    @Log(title = "门户订单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@Parameter(description = "订单ID数组") @PathVariable Long[] ids) {
        return toAjax(portalOrderService.deletePortalOrderByIds(ids));
    }
}