package com.moyun.community.content.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.community.content.entity.CmsNotification;
import com.moyun.community.content.mapper.CmsNotificationMapper;
import com.moyun.community.content.service.ICmsNotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CmsNotificationServiceImpl extends ServiceImpl<CmsNotificationMapper, CmsNotification> implements ICmsNotificationService {

    @Override
    public List<CmsNotification> selectNotificationList(Long userId) {
        return baseMapper.selectNotificationList(userId);
    }

    @Override
    public List<CmsNotification> selectUnreadNotificationList(Long userId) {
        return baseMapper.selectUnreadNotificationList(userId);
    }

    @Override
    @Transactional
    public boolean markAsRead(Long notificationId) {
        return baseMapper.markAsRead(notificationId) > 0;
    }

    @Override
    @Transactional
    public boolean markAllAsRead(Long userId) {
        return baseMapper.markAllAsRead(userId) > 0;
    }

    @Override
    public int getUnreadCount(Long userId) {
        return baseMapper.getUnreadCount(userId);
    }

    @Override
    @Transactional
    public boolean sendNotification(CmsNotification notification) {
        notification.setIsRead(0);
        return save(notification);
    }
}