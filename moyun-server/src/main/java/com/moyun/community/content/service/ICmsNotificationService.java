package com.moyun.community.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.community.content.entity.CmsNotification;

import java.util.List;

public interface ICmsNotificationService extends IService<CmsNotification> {

    List<CmsNotification> selectNotificationList(Long userId);

    List<CmsNotification> selectUnreadNotificationList(Long userId);

    boolean markAsRead(Long notificationId);

    boolean markAllAsRead(Long userId);

    int getUnreadCount(Long userId);

    boolean sendNotification(CmsNotification notification);
}