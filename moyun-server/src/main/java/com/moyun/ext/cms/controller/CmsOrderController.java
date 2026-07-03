package com.moyun.ext.cms.controller;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ext.cms.service.ICmsOrderService;
import com.moyun.portal.domain.entity.PortalTipOrder;
import com.moyun.util.bean.PageUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * CMS 付费阅读订单后台管理 Controller（只读查询）
 * <p>
 * 提供订单列表/详情（购买记录，含用户/文章/金额/状态）。
 * 路径前缀 /cms/order。
 *
 * @author moyun
 */
@Tag(name = "CMS订单管理", description = "付费阅读订单后台查询接口")
@RestController
@RequestMapping("/cms/order")
public class CmsOrderController extends BaseController {

    @Autowired
    private ICmsOrderService cmsOrderService;

    @Operation(summary = "查询付费阅读订单列表", description = "分页查询付费阅读购买记录（含用户昵称、文章标题），支持按 status/时间筛选")
    @PreAuthorize("@ss.hasPermi('portal:order:list')")
    @GetMapping("/list")
    public AjaxResult list(@RequestParam(required = false) String status,
                           @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                           @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        Page<PortalTipOrder> page = PageUtils.startPage();
        cmsOrderService.selectOrderPage(page, status, startTime, endTime);
        return success(page);
    }

    @Operation(summary = "获取订单详情", description = "根据ID获取付费阅读订单详情")
    @PreAuthorize("@ss.hasPermi('portal:order:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(cmsOrderService.selectOrderById(id));
    }
}
