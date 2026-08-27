package com.moyun.pay.controller;


import com.moyun.core.base.AjaxResult;
import com.moyun.pay.domain.entity.PayNotification;
import com.moyun.pay.service.INotificationService;
import com.moyun.portal.util.PortalSecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 门户支付通知控制器（V11.0）
 *
 * @author moyun
 */
@RestController
@RequestMapping("/portal/pay/notifications")
public class PortalPayNotificationController {

    @Autowired
    private INotificationService notificationService;

    /** 我的通知分页 */
    @GetMapping("/list")
    public AjaxResult list(@RequestParam(defaultValue = "1") long current,
                           @RequestParam(defaultValue = "10") long size) {
        Long userId = PortalSecurityUtils.getUserId();
        var page = notificationService.myNotifications(userId, current, size);
        Map<String, Object> data = new HashMap<>();
        data.put("records", page.getRecords());
        data.put("total", page.getTotal());
        data.put("current", page.getCurrent());
        data.put("size", page.getSize());
        data.put("unreadCount", notificationService.unreadCount(userId));
        return AjaxResult.success(data);
    }

    /** 未读数（Navbar 角标） */
    @GetMapping("/unread-count")
    public AjaxResult unreadCount() {
        Long userId = PortalSecurityUtils.getUserId();
        return AjaxResult.success(notificationService.unreadCount(userId));
    }

    /** 标记已读 */
    @PostMapping("/{notificationId}/read")
    public AjaxResult markRead(@PathVariable Long notificationId) {
        Long userId = PortalSecurityUtils.getUserId();
        notificationService.markRead(userId, notificationId);
        return AjaxResult.success();
    }
}
