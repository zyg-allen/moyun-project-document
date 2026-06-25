package com.moyun.system.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.system.domain.entity.SysNotification;
import com.moyun.system.mapper.SysNotificationMapper;
import com.moyun.system.mapper.SysNotificationReadMapper;
import com.moyun.system.service.ISysNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 系统通知 服务实现
 * 通过 user_type 区分门户用户(portal)和系统用户(sys)
 *
 * @author moyun
 */
@Service
public class SysNotificationServiceImpl extends ServiceImpl<SysNotificationMapper, SysNotification>
        implements ISysNotificationService {

    /** 用户类型常量：门户用户 */
    private static final String USER_TYPE_PORTAL = "portal";

    @Autowired
    private SysNotificationMapper sysNotificationMapper;

    @Autowired
    private SysNotificationReadMapper sysNotificationReadMapper;

    // ==================== 后台管理 ====================

    @Override
    public Page<SysNotification> selectNotificationPage(Page<SysNotification> page, SysNotification query) {
        return sysNotificationMapper.selectNotificationPage(page, query);
    }

    @Override
    public List<SysNotification> selectNotificationList(SysNotification query) {
        return sysNotificationMapper.selectNotificationList(query);
    }

    @Override
    public SysNotification selectNotificationById(Long id) {
        return sysNotificationMapper.selectNotificationById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertNotification(SysNotification notification) {
        if (notification.getCreateTime() == null) {
            notification.setCreateTime(LocalDateTime.now());
        }
        // 个人通知必须有 user_id 和 user_type
        if ("user".equals(notification.getScope())) {
            if (notification.getUserId() == null) {
                throw new ServiceException("个人通知必须指定接收用户ID");
            }
            // user_type 默认为 portal（兼容旧调用方）
            if (notification.getUserType() == null || notification.getUserType().isEmpty()) {
                notification.setUserType(USER_TYPE_PORTAL);
            }
        }
        // 广播通知强制 user_id = null，user_type = null
        if ("all".equals(notification.getScope())) {
            notification.setUserId(null);
            notification.setUserType(null);
        }
        if (notification.getStatus() == null) {
            notification.setStatus("0");
        }
        return sysNotificationMapper.insertNotification(notification);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateNotification(SysNotification notification) {
        notification.setUpdateTime(LocalDateTime.now());
        return sysNotificationMapper.updateNotification(notification);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteNotificationByIds(Long[] ids) {
        return sysNotificationMapper.deleteNotificationByIds(ids);
    }

    /**
     * 群发系统通知（scope=all，全局广播，单条记录）
     * 替代原 CmsNotificationServiceImpl.sendSystemNotification 的逐条 insert
     * 广播通知只存一条主体记录，已读状态由 sys_notification_read 按需记录
     * 广播通知对门户用户和系统用户都可见
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int sendBroadcastNotification(SysNotification notification) {
        notification.setScope("all");
        notification.setUserId(null);
        notification.setUserType(null);
        notification.setType(notification.getType() != null ? notification.getType() : "system");
        if (notification.getStatus() == null) {
            notification.setStatus("0");
        }
        if (notification.getCreateTime() == null) {
            notification.setCreateTime(LocalDateTime.now());
        }
        return sysNotificationMapper.insertNotification(notification);
    }

    // ==================== 用户通知查询（门户 + 系统） ====================

    @Override
    public Page<SysNotification> selectUserNotifications(Page<SysNotification> page, Long userId, String userType) {
        if (userId == null) {
            throw new ServiceException("用户ID不能为空");
        }
        if (userType == null || userType.isEmpty()) {
            userType = USER_TYPE_PORTAL;
        }
        return sysNotificationMapper.selectAllByUserId(page, userId, userType);
    }

    @Override
    public int countUnread(Long userId, String userType) {
        if (userId == null) {
            return 0;
        }
        if (userType == null || userType.isEmpty()) {
            userType = USER_TYPE_PORTAL;
        }
        return sysNotificationMapper.countUnreadByUserId(userId, userType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int markAsRead(Long notificationId, Long userId, String userType) {
        if (notificationId == null || userId == null) {
            throw new ServiceException("通知ID和用户ID不能为空");
        }
        if (userType == null || userType.isEmpty()) {
            userType = USER_TYPE_PORTAL;
        }
        return sysNotificationReadMapper.markAsRead(notificationId, userId, userType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int markAllAsRead(Long userId, String userType) {
        if (userId == null) {
            throw new ServiceException("用户ID不能为空");
        }
        if (userType == null || userType.isEmpty()) {
            userType = USER_TYPE_PORTAL;
        }
        return sysNotificationReadMapper.markAllAsRead(userId, userType);
    }
}
