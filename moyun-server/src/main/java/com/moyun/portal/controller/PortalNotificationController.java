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

    @Operation(summary = "获取通知列表", description = "查询当前登录用户的所有通知（个人 + 广播，含已读状态）")
    @GetMapping("/list")
    public AjaxResult list(com.moyun.portal.domain.query.NotificationQuery query) {
        PortalUser currentUser = PortalSecurityUtils.getUser();
        if (currentUser == null) {
            return success(new Page<SysNotification>());
        }
        Page<SysNotification> page = PageUtils.buildPage(query);
        Page<SysNotification> result = sysNotificationService.selectUserNotifications(page, currentUser.getId(), USER_TYPE_PORTAL);
        return success(result);
    }

    @Operation(summary = "获取未读通知数", description = "统计当前登录用户的未读通知数（个人 + 广播）")
    @GetMapping("/unread-count")
    public AjaxResult unreadCount() {
        PortalUser currentUser = PortalSecurityUtils.getUser();
        if (currentUser == null) {
            return success(0);
        }
        int count = sysNotificationService.countUnread(currentUser.getId(), USER_TYPE_PORTAL);
        return success(count);
    }

    @Operation(summary = "标记单条通知已读", description = "将指定通知标记为已读（INSERT IGNORE 防重复）")
    @Log(title = "门户通知", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/read")
    public AjaxResult markAsRead(@Parameter(description = "通知ID") @PathVariable Long id) {
        PortalUser currentUser = PortalSecurityUtils.getUser();
        if (currentUser == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        return toAjax(sysNotificationService.markAsRead(id, currentUser.getId(), USER_TYPE_PORTAL));
    }
}
