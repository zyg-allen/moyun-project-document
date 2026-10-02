package com.moyun.pay.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.moyun.pay.domain.entity.PayNotification;

/**
 * 支付通知服务
 *
 * @author moyun
 */
public interface INotificationService {

    /** 发送通知（事务内调用，失败不影响主流程则由调用方决定） */
    void send(Long userId, String notifyType, String refNo, String title, String content);

    /** 发送通知（带端维度，事务内调用） */
    void send(Long userId, String notifyType, String refNo, String title, String content, String platformCode);

    /** 我的通知分页 */
    IPage<PayNotification> myNotifications(Long userId, long current, long size);

    /** 未读数 */
    long unreadCount(Long userId);

    /** 标记已读（限本人） */
    void markRead(Long userId, Long notificationId);

    /**
     * 把该用户**全部**未读支付通知标记为已读（服务端一条 UPDATE 完成）。
     *
     * <p>为什么需要：门户「全部已读」原先只能对**已加载的那一页**逐条调用 markRead，
     * 未加载的仍为未读 ⇒ 角标清完又回来（客户端还会直接清零角标，等于对用户撒谎）。
     * 后端既有的 markAllAsRead 已作为死接口移除，故在此补一个语义明确的服务端批量方法。</p>
     *
     * @param userId 接收用户
     * @return 本次影响（置为已读）的条数
     */
    int markAllRead(Long userId);
}
