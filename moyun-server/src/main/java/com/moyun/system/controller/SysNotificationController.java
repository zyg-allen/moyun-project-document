package com.moyun.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.system.domain.entity.SysNotification;
import com.moyun.system.service.ISysNotificationService;
import com.moyun.util.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 后台管理员通知中心 Controller
 * 面向系统用户(sys)，user_type 固定为 "sys"
 * 用于后台管理员接收系统通知（审批待办、系统告警等）
 *
 * @author moyun
 */
@Tag(name = "后台通知中心", description = "后台管理员通知查询与已读操作")
@RestController
@RequestMapping("/system/notification")
public class SysNotificationController extends BaseController {

    /** 系统用户类型标识 */
    private static final String USER_TYPE_SYS = "sys";

    @Autowired
    private ISysNotificationService sysNotificationService;

    /**
     * 获取当前管理员的通知列表（个人 + 广播，含已读状态）
     */
    @Operation(summary = "获取管理员通知列表", description = "查询当前登录管理员的所有通知（个人 + 广播，含已读状态）")
    @GetMapping("/list")
    public AjaxResult list() {
        Long userId = SecurityUtils.getUserId();
        Page<SysNotification> page = startPage();
        Page<SysNotification> result = sysNotificationService.selectUserNotifications(page, userId, USER_TYPE_SYS);
        return success(result);
    }

    /**
     * 获取当前管理员的未读通知数
     */
    @Operation(summary = "获取管理员未读通知数", description = "统计当前登录管理员的未读通知数（个人 + 广播）")
    @GetMapping("/unread-count")
    public AjaxResult unreadCount() {
        Long userId = SecurityUtils.getUserId();
        int count = sysNotificationService.countUnread(userId, USER_TYPE_SYS);
        return success(count);
    }

    /**
     * 标记单条通知已读
     */
    @Operation(summary = "标记单条通知已读", description = "将指定通知标记为已读（INSERT IGNORE 防重复）")
    @Log(title = "后台通知中心", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/read")
    public AjaxResult markAsRead(@Parameter(description = "通知ID") @PathVariable Long id) {
        Long userId = SecurityUtils.getUserId();
        return toAjax(sysNotificationService.markAsRead(id, userId, USER_TYPE_SYS));
    }

    /**
     * 标记所有未读通知为已读
     */
    @Operation(summary = "标记所有通知已读", description = "将当前管理员所有未读通知标记为已读（批量 INSERT IGNORE）")
    @Log(title = "后台通知中心", businessType = BusinessType.UPDATE)
    @PostMapping("/mark-all-read")
    public AjaxResult markAllAsRead() {
        Long userId = SecurityUtils.getUserId();
        return toAjax(sysNotificationService.markAllAsRead(userId, USER_TYPE_SYS));
    }

    /**
     * 获取通知详情
     */
    @Operation(summary = "获取通知详情", description = "根据通知ID获取通知详细信息")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@Parameter(description = "通知ID") @PathVariable Long id) {
        return success(sysNotificationService.selectNotificationById(id));
    }
}
