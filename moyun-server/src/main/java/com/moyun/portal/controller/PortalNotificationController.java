package com.moyun.portal.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.moyun.common.annotation.Log;
import com.moyun.common.constant.HttpStatus;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.util.PortalSecurityUtils;
import com.moyun.system.domain.entity.SysNotification;
import com.moyun.system.service.ISysNotificationService;
import com.moyun.util.bean.PageUtils;

/**
 * 门户通知 Controller
 * 面向门户用户(portal)，user_type 固定为 "portal"
 *
 * 清理说明：原 markAllAsRead / getInfo / remove / export 四个接口因前端不再调用属于死接口，
 * 已于本次清理中从 Controller 层移除。对应的 Service / Mapper / XML 实现予以保留，
 * 后续如需恢复可直接重新暴露。
 *
 * @author moyun
 */
@Tag(name = "门户通知", description = "门户通知的增删改查操作接口")
@RestController
@RequestMapping("/portal/notification")
public class PortalNotificationController extends BaseController {

    /** 门户用户类型标识 */
    private static final String USER_TYPE_PORTAL = "portal";

    @Autowired
    private ISysNotificationService sysNotificationService;

    @Operation(summary = "获取通知列表", description = "登录用户返回个人 + 广播通知；未登录用户仅返回广播通知")
    @GetMapping("/list")
    public AjaxResult list(com.moyun.portal.domain.query.NotificationQuery query) {
        PortalUser currentUser = PortalSecurityUtils.getUser();
        Page<SysNotification> page = PageUtils.buildPage(query);
        // 未登录用户也能看到广播通知（如版本发布、公告）
        if (currentUser == null) {
            Page<SysNotification> broadcast = sysNotificationService
                    .selectBroadcastNotifications(page, null, null);
            return success(broadcast);
        }
        Page<SysNotification> result = sysNotificationService.selectUserNotifications(page, currentUser.getId(), USER_TYPE_PORTAL);
        return success(result);
    }

    @Operation(summary = "获取未读通知数", description = "未登录用户返回广播通知总数；登录用户返回个人 + 广播未读数")
    @GetMapping("/unread-count")
    public AjaxResult unreadCount() {
        PortalUser currentUser = PortalSecurityUtils.getUser();
        if (currentUser == null) {
            // 未登录用户：返回广播通知总数作为提示
            Page<SysNotification> page = new Page<>(1, 1);
            Page<SysNotification> broadcast = sysNotificationService
                    .selectBroadcastNotifications(page, null, null);
            return success((int) broadcast.getTotal());
        }
        int count = sysNotificationService.countUnread(currentUser.getId(), USER_TYPE_PORTAL);
        return success(count);
    }

    @Operation(summary = "获取公开广播通知", description = "未登录用户可查看 scope=all 的系统通知（如版本发布、公告）。已登录用户返回 isRead 状态，未登录用户 isRead 固定为 false。")
    @GetMapping("/broadcast")
    public AjaxResult broadcast(com.moyun.portal.domain.query.NotificationQuery query) {
        PortalUser currentUser = PortalSecurityUtils.getUser();
        Long userId = currentUser != null ? currentUser.getId() : null;
        String userType = currentUser != null ? USER_TYPE_PORTAL : null;
        Page<SysNotification> page = PageUtils.buildPage(query);
        Page<SysNotification> result = sysNotificationService.selectBroadcastNotifications(page, userId, userType);
        return success(result);
    }

    @Operation(summary = "标记单条通知已读", description = "将指定通知标记为已读（INSERT IGNORE 防重复，幂等操作）")
    @Log(title = "门户通知", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/read")
    public AjaxResult markAsRead(@Parameter(description = "通知ID") @PathVariable("id") Long id) {
        PortalUser currentUser = PortalSecurityUtils.getUser();
        if (currentUser == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        // 幂等操作：INSERT IGNORE，已读通知再标记仍返回成功
        sysNotificationService.markAsRead(id, currentUser.getId(), USER_TYPE_PORTAL);
        return AjaxResult.success();
    }
}
