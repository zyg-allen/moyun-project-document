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
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.portal.domain.entity.PortalNotification;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.domain.query.NotificationQuery;
import com.moyun.portal.service.IPortalNotificationService;
import com.moyun.portal.util.PortalSecurityUtils;
import com.moyun.util.bean.PageUtils;
import com.moyun.util.file.ExcelUtil;

@Tag(name = "门户通知", description = "门户通知的增删改查操作接口")
@RestController
@RequestMapping("/portal/notification")
public class PortalNotificationController extends BaseController {

    @Autowired
    private IPortalNotificationService portalNotificationService;

    @Operation(summary = "获取通知列表", description = "根据条件分页查询通知列表（按当前登录用户过滤，未登录返回空列表）")
    @GetMapping("/list")
    public AjaxResult list(NotificationQuery query) {
        PortalUser currentUser = PortalSecurityUtils.getUser();
        if (currentUser == null) {
            return success(new Page<PortalNotification>());
        }
        if (query.getUserId() == null) {
            query.setUserId(currentUser.getId());
        }
        Page<PortalNotification> page = PageUtils.buildPage(query);
        Page<PortalNotification> resultPage = portalNotificationService.selectPortalNotificationPage(page, query);
        return success(resultPage);
    }

    @Operation(summary = "导出通知", description = "导出通知数据到Excel文件")
    @Log(title = "门户通知", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, NotificationQuery query) {
        List<PortalNotification> list = portalNotificationService.selectPortalNotificationList(query);
        ExcelUtil<PortalNotification> util = new ExcelUtil<PortalNotification>(PortalNotification.class);
        util.exportExcel(response, list, "门户通知数据");
    }

    @Operation(summary = "获取通知详情", description = "根据通知ID获取通知详细信息")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@Parameter(description = "通知ID") @PathVariable Long id) {
        return success(portalNotificationService.selectPortalNotificationById(id));
    }

    @Operation(summary = "新增通知", description = "创建新通知")
    @Log(title = "门户通知", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody PortalNotification portalNotification) {
        return toAjax(portalNotificationService.insertPortalNotification(portalNotification));
    }

    @Operation(summary = "修改通知", description = "更新通知信息")
    @Log(title = "门户通知", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody PortalNotification portalNotification) {
        return toAjax(portalNotificationService.updatePortalNotification(portalNotification));
    }

    @Operation(summary = "删除通知", description = "批量删除通知")
    @Log(title = "门户通知", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@Parameter(description = "通知ID数组") @PathVariable Long[] ids) {
        return toAjax(portalNotificationService.deletePortalNotificationByIds(ids));
    }
}
