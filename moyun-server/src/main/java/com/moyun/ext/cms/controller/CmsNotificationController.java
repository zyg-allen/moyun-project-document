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
import com.moyun.system.domain.entity.SysNotification;
import com.moyun.system.service.ISysNotificationService;

/**
 * CMS通知管理Controller
 * 合并后统一操作 sys_notification 表
 *
 * @author moyun
 */
@Tag(name = "CMS通知管理", description = "CMS通知管理接口")
@RestController
@RequestMapping("/cms/notification")
public class CmsNotificationController extends BaseController {

    @Autowired
    private ISysNotificationService sysNotificationService;

    /**
     * 获取通知列表
     */
    @Operation(summary = "获取通知列表", description = "根据条件分页查询通知列表")
    @PreAuthorize("@ss.hasPermi('cms:notification:list')")
    @GetMapping("/list")
    public AjaxResult list(SysNotification query) {
        Page<SysNotification> page = startPage();
        page = sysNotificationService.selectNotificationPage(page, query);
        return success(page);
    }

    /**
     * 根据通知编号获取详细信息
     */
    @Operation(summary = "获取通知详情", description = "根据通知ID获取通知详细信息")
    @PreAuthorize("@ss.hasPermi('cms:notification:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@Parameter(description = "通知ID") @PathVariable Long id) {
        return success(sysNotificationService.selectNotificationById(id));
    }

    /**
     * 新增通知（个人通知：scope=user，需指定 userId 和 userType）
     * userType: portal=门户用户 / sys=系统用户
     */
    @Operation(summary = "新增通知", description = "创建新通知（scope=user 为个人通知，需指定 userId 和 userType）")
    @PreAuthorize("@ss.hasPermi('cms:notification:add')")
    @Log(title = "通知管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysNotification notification) {
        // 默认为个人通知
        if (notification.getScope() == null) {
            notification.setScope("user");
        }
        // 个人通知默认发给门户用户（兼容旧调用方未传 userType 的情况）
        if ("user".equals(notification.getScope())
                && (notification.getUserType() == null || notification.getUserType().isEmpty())) {
            notification.setUserType("portal");
        }
        return toAjax(sysNotificationService.insertNotification(notification));
    }

    /**
     * 发送广播通知（scope=all，全局广播，单条记录）
     * 替代原逐条 insert 群发逻辑，广播通知只存一条主体记录
     */
    @Operation(summary = "发送广播通知", description = "群发系统通知给所有用户（scope=all，单条记录，已读按需记录）")
    @PreAuthorize("@ss.hasPermi('cms:notification:add')")
    @Log(title = "通知管理", businessType = BusinessType.INSERT)
    @PostMapping("/send-all")
    public AjaxResult sendAll(@Validated @RequestBody SysNotification notification) {
        return toAjax(sysNotificationService.sendBroadcastNotification(notification));
    }

    /**
     * 修改通知
     */
    @Operation(summary = "修改通知", description = "更新通知信息")
    @PreAuthorize("@ss.hasPermi('cms:notification:edit')")
    @Log(title = "通知管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysNotification notification) {
        return toAjax(sysNotificationService.updateNotification(notification));
    }

    /**
     * 删除通知
     */
    @Operation(summary = "删除通知", description = "批量删除通知")
    @PreAuthorize("@ss.hasPermi('cms:notification:remove')")
    @Log(title = "通知管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@Parameter(description = "通知ID数组") @PathVariable Long[] ids) {
        return toAjax(sysNotificationService.deleteNotificationByIds(ids));
    }
}
