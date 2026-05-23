package com.moyun.community.content.controller.app;

import com.moyun.common.core.controller.BaseController;
import com.moyun.common.core.domain.AjaxResult;
import com.moyun.common.core.domain.TableDataInfo;
import com.moyun.common.utils.SecurityUtils;
import com.moyun.community.content.entity.CmsNotification;
import com.moyun.community.content.service.ICmsNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/app/notification")
public class CmsNotificationAppController extends BaseController {

    @Autowired
    private ICmsNotificationService notificationService;

    @GetMapping("/list")
    public TableDataInfo list() {
        Long userId = SecurityUtils.getUserId();
        startPage();
        List<CmsNotification> list = notificationService.selectNotificationList(userId);
        return getDataTable(list);
    }

    @GetMapping("/unread")
    public TableDataInfo unreadList() {
        Long userId = SecurityUtils.getUserId();
        startPage();
        List<CmsNotification> list = notificationService.selectUnreadNotificationList(userId);
        return getDataTable(list);
    }

    @GetMapping("/unread/count")
    public AjaxResult unreadCount() {
        Long userId = SecurityUtils.getUserId();
        int count = notificationService.getUnreadCount(userId);
        return success(Map.of("count", count));
    }

    @PutMapping("/read/{notificationId}")
    public AjaxResult markAsRead(@PathVariable Long notificationId) {
        return toAjax(notificationService.markAsRead(notificationId));
    }

    @PutMapping("/read/all")
    public AjaxResult markAllAsRead() {
        Long userId = SecurityUtils.getUserId();
        return toAjax(notificationService.markAllAsRead(userId));
    }
}