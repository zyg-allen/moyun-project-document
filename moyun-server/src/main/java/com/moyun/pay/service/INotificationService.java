package com.moyun.pay.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.moyun.pay.domain.entity.PayNotification;

/**
 * 支付通知服务（V11.0）
 *
 * @author moyun
 */
public interface INotificationService {

    /** 发送通知（事务内调用，失败不影响主流程则由调用方决定） */
    void send(Long userId, String notifyType, String refNo, String title, String content);

    /** 我的通知分页 */
    IPage<PayNotification> myNotifications(Long userId, long current, long size);

    /** 未读数 */
    long unreadCount(Long userId);

    /** 标记已读（限本人） */
    void markRead(Long userId, Long notificationId);
}
