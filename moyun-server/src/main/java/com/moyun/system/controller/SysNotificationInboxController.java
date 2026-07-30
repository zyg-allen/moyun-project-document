package com.moyun.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.moyun.common.annotation.Log;
import com.moyun.common.constant.HttpStatus;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.page.PageDomain;
import com.moyun.system.domain.entity.SysNotification;
import com.moyun.system.service.ISysNotificationService;
import com.moyun.util.bean.PageUtils;
import com.moyun.util.security.SecurityUtils;

/**
 * 后台管理员通知收件箱 Controller
 *
 * <p>面向系统用户(sys)，user_type 固定为 "sys"。
 * 与门户端 {@link com.moyun.portal.controller.PortalNotificationController} 对称，
 * 共用 {@link ISysNotificationService}，通过 user_type 区分管理员体系。</p>
 *
 * <p>权限：通知收件箱查询统一使用 system:notification:list 权限码；
 * admin 超管通过代码旁路（*:*:*）自动获得全部权限。</p>
 *
 * <p>路径设计：挂在 /system/notification/inbox 下，与通知管理台账
 * {@link SysNotificationController}(/system/notification) 区分，避免路径 {id} 与 "inbox" 冲突。
 * <ul>
 *   <li>"我的通知"：scope=user 且 user_id=当前管理员 + scope=all 的广播，含已读状态</li>
 *   <li>"全站通知台账"：见 {@link SysNotificationController#list} (system:notification:list)</li>
 * </ul>
 * </p>
 *
 * @author moyun
 */
@Tag(name = "后台通知收件箱", description = "管理员通知收件箱接口（我的通知）")
@RestController
@RequestMapping("/system/notification/inbox")
public class SysNotificationInboxController extends BaseController {

    /** 管理端用户类型固定为 sys */
    private static final String USER_TYPE_SYS = "sys";

    @Autowired
    private ISysNotificationService sysNotificationService;

    private Long currentUserId() {
        return SecurityUtils.getUserId();
    }

    /**
     * 我的通知列表（个人 + 广播，含已读状态）
     */
    @Operation(summary = "我的通知列表", description = "查询当前管理员的所有通知（个人 + 广播，含已读状态）")
    @PreAuthorize("@ss.hasPermi('system:notification:list')")
    @GetMapping("/list")
    public AjaxResult list(PageDomain query) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        Page<SysNotification> page = PageUtils.buildPage(query);
        Page<SysNotification> result = sysNotificationService.selectUserNotifications(page, userId, USER_TYPE_SYS);
        return AjaxResult.success(result);
    }

    /**
     * 未读通知数
     */
    @Operation(summary = "未读通知数", description = "统计当前管理员的未读通知数（个人 + 广播）")
    @PreAuthorize("@ss.hasPermi('system:notification:list')")
    @GetMapping("/unread-count")
    public AjaxResult unreadCount() {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(sysNotificationService.countUnread(userId, USER_TYPE_SYS));
    }

    /**
     * 标记单条通知已读
     */
    @Operation(summary = "标记通知已读", description = "将指定通知标记为已读（INSERT IGNORE 防重复，幂等操作）")
    @PreAuthorize("@ss.hasPermi('system:notification:list')")
    @Log(title = "通知收件箱", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/read")
    public AjaxResult markAsRead(@Parameter(description = "通知ID") @PathVariable("id") Long id) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        // 幂等操作：INSERT IGNORE，已读通知再标记仍返回成功
        sysNotificationService.markAsRead(id, userId, USER_TYPE_SYS);
        return AjaxResult.success();
    }

    /**
     * 全部标记已读
     */
    @Operation(summary = "全部标记已读", description = "将当前管理员所有未读通知标记为已读（幂等操作）")
    @PreAuthorize("@ss.hasPermi('system:notification:list')")
    @Log(title = "通知收件箱", businessType = BusinessType.UPDATE)
    @PostMapping("/read-all")
    public AjaxResult markAllAsRead() {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        // 幂等操作：无未读通知时返回 0 行，仍应返回成功
        sysNotificationService.markAllAsRead(userId, USER_TYPE_SYS);
        return AjaxResult.success();
    }
}
