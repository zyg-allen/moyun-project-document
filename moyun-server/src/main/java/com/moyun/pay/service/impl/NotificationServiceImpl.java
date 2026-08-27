package com.moyun.pay.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.pay.domain.entity.PayNotification;
import com.moyun.pay.mapper.PayNotificationMapper;
import com.moyun.pay.service.INotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 支付通知服务实现（V11.0）
 *
 * @author moyun
 */
@Service
public class NotificationServiceImpl implements INotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    @Autowired
    private PayNotificationMapper notificationMapper;

    @Override
    public void send(Long userId, String notifyType, String refNo, String title, String content) {
        PayNotification notification = new PayNotification();
        notification.setUserId(userId);
        notification.setNotifyType(notifyType);
        notification.setRefNo(refNo);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setReadFlag(0);
        notification.setCreateTime(LocalDateTime.now());
        notificationMapper.insert(notification);
        log.info("[pay-notify] 通知已发送 userId={} type={} refNo={}", userId, notifyType, refNo);
    }

    @Override
    public IPage<PayNotification> myNotifications(Long userId, long current, long size) {
        Page<PayNotification> page = new Page<>(current, size);
        return notificationMapper.selectPage(page, new LambdaQueryWrapper<PayNotification>()
                .eq(PayNotification::getUserId, userId)
                .orderByDesc(PayNotification::getId));
    }

    @Override
    public long unreadCount(Long userId) {
        Long count = notificationMapper.selectCount(new LambdaQueryWrapper<PayNotification>()
                .eq(PayNotification::getUserId, userId)
                .eq(PayNotification::getReadFlag, 0));
        return count == null ? 0 : count;
    }

    @Override
    public void markRead(Long userId, Long notificationId) {
        PayNotification notification = notificationMapper.selectById(notificationId);
        if (notification == null || !userId.equals(notification.getUserId())) {
            throw new IllegalArgumentException("通知不存在或无权操作");
        }
        notificationMapper.update(null, new LambdaUpdateWrapper<PayNotification>()
                .eq(PayNotification::getId, notificationId)
                .eq(PayNotification::getUserId, userId)
                .set(PayNotification::getReadFlag, 1));
    }
}
