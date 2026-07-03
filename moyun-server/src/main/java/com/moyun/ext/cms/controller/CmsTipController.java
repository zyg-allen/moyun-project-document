package com.moyun.ext.cms.controller;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ext.cms.service.ICmsTipService;
import com.moyun.portal.domain.entity.PortalTipOrder;
import com.moyun.util.bean.PageUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * CMS 打赏后台管理 Controller（只读查询）
 * <p>
 * 提供打赏流水列表/详情，支持按 targetType/status/时间筛选。
 * 路径前缀 /cms/tip。
 *
 * @author moyun
 */
@Tag(name = "CMS打赏管理", description = "打赏流水后台查询接口")
@RestController
@RequestMapping("/cms/tip")
public class CmsTipController extends BaseController {

    @Autowired
    private ICmsTipService cmsTipService;

    @Operation(summary = "查询打赏流水列表", description = "分页查询打赏流水（含用户/作者信息），支持按 targetType/status/时间筛选")
    @PreAuthorize("@ss.hasPermi('portal:tip:list')")
    @GetMapping("/list")
    public AjaxResult list(@RequestParam(required = false) String targetType,
                           @RequestParam(required = false) String status,
                           @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                           @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        Page<PortalTipOrder> page = PageUtils.startPage();
        cmsTipService.selectTipPage(page, targetType, status, startTime, endTime);
        return success(page);
    }

    @Operation(summary = "获取打赏订单详情", description = "根据ID获取打赏订单详情")
    @PreAuthorize("@ss.hasPermi('portal:tip:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(cmsTipService.selectTipById(id));
    }
}
