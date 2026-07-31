package com.moyun.system.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.system.domain.entity.SysNotification;

/**
 * 系统通知 服务层
 * 合并原 portal_notification + sys_notice，采用两表结构
 * 通过 user_type 区分门户用户(portal)和系统用户(sys)
 *
 * @author moyun
 */
public interface ISysNotificationService extends IService<SysNotification> {

    // ==================== 后台管理 ====================

    /**
     * 分页查询通知列表（后台管理用）
     */
    Page<SysNotification> selectNotificationPage(Page<SysNotification> page, SysNotification query);

    /**
     * 不分页查询通知列表（导出等）
     */
    List<SysNotification> selectNotificationList(SysNotification query);

    /**
     * 根据ID查询通知
     */
    SysNotification selectNotificationById(Long id);

    /**
     * 新增通知
     */
    int insertNotification(SysNotification notification);

    /**
     * 修改通知
     */
    int updateNotification(SysNotification notification);

    /**
     * 批量删除通知
     */
    int deleteNotificationByIds(Long[] ids);

    /**
     * 群发系统通知（scope=all，全局广播，单条记录）
     * 替代原 CmsNotificationServiceImpl.sendSystemNotification 的逐条 insert
     * 广播通知只存一条主体记录，已读状态由 sys_notification_read 按需记录
     *
     * @param notification 通知内容（scope 强制设为 all，user_id 设为 null）
     * @return 结果
     */
    int sendBroadcastNotification(SysNotification notification);

    /**
     * 发送待办通知（type=todo）给所有系统用户 + 被系统用户绑定的前台用户
     * 使用个人通知（scope=user）定向发送，未绑定的前台用户不可见
     *
     * @param template 通知模板（title/content/data 等，type 强制设为 todo）
     * @return 发送条数
     */
    int sendTodoNotification(SysNotification template);

    // ==================== 用户通知查询（门户 + 系统） ====================

    /**
     * 查询用户的所有通知（个人 + 广播，含已读状态）
     *
     * @param page     分页参数
     * @param userId   用户ID
     * @param userType 用户类型：portal=门户用户 / sys=系统用户
     */
    Page<SysNotification> selectUserNotifications(Page<SysNotification> page, Long userId, String userType);

    /**
     * 统计用户未读通知数
     *
     * @param userId   用户ID
     * @param userType 用户类型：portal=门户用户 / sys=系统用户
     */
    int countUnread(Long userId, String userType);

    /**
     * 标记单条通知已读
     *
     * @param notificationId 通知ID
     * @param userId         用户ID
     * @param userType       用户类型：portal=门户用户 / sys=系统用户
     */
    int markAsRead(Long notificationId, Long userId, String userType);

    /**
     * 标记用户所有未读通知为已读
     *
     * @param userId   用户ID
     * @param userType 用户类型：portal=门户用户 / sys=系统用户
     */
    int markAllAsRead(Long userId, String userType);

    /**
     * 查询公开广播通知（scope=all，未登录用户也可查看）
     *
     * <p>用于门户公开页面（如公告/版本发布），无需登录即可查看广播通知。
     * 已登录用户可返回 isRead 状态，未登录用户 isRead 固定为 false。</p>
     *
     * @param page     分页参数
     * @param userId   用户ID（未登录时传 null，不计算已读状态）
     * @param userType 用户类型：portal=门户用户 / sys=系统用户（未登录时传 null）
     */
    Page<SysNotification> selectBroadcastNotifications(Page<SysNotification> page, Long userId, String userType);
}
