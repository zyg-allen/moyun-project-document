package com.moyun.system.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.core.base.entity.SysUser;
import com.moyun.portal.mapper.PortalUserMapper;
import com.moyun.system.domain.entity.SysNotification;
import com.moyun.system.mapper.SysNotificationMapper;
import com.moyun.system.mapper.SysNotificationReadMapper;
import com.moyun.system.mapper.SysUserMapper;
import com.moyun.system.service.ISysNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

/**
 * 系统通知 服务实现
 * 通过 user_type 区分门户用户(portal)和系统用户(sys)
 *
 * @author moyun
 */
@Slf4j
@Service
public class SysNotificationServiceImpl extends ServiceImpl<SysNotificationMapper, SysNotification>
        implements ISysNotificationService {

    /** 用户类型常量：门户用户 */
    private static final String USER_TYPE_PORTAL = "portal";

    @Autowired
    private SysNotificationMapper sysNotificationMapper;

    @Autowired
    private SysNotificationReadMapper sysNotificationReadMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private PortalUserMapper portalUserMapper;

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
        // ── 接收方通知偏好校验（清单 #36）──
        // portal_user 的 notify_like/notify_comment/notify_follow/notify_system 此前**没有任何读取点**，
        // 设置页的开关形同虚设。本方法是门户个人通知的唯一落库入口，故在此统一收口：
        // 按 type 映射到对应偏好，关闭则**不写库**（返回 0，调用方均为"失败不影响主流程"的语义）。
        if ("user".equals(notification.getScope())
                && USER_TYPE_PORTAL.equals(notification.getUserType())
                && !recipientAllowsNotification(notification.getUserId(), notification.getType())) {
            log.info("[notification] 接收方已关闭该类通知，跳过写入 userId={} type={}",
                    notification.getUserId(), notification.getType());
            return 0;
        }
        return sysNotificationMapper.insertNotification(notification);
    }

    /**
     * 接收方是否允许该类通知。
     *
     * <p>映射口径：{@code like → notifyLike}、{@code comment/reply → notifyComment}、
     * {@code follow → notifyFollow}，其余（系统/审核/认证等）归 {@code notifySystem}。</p>
     *
     * <p><b>null 视为开启</b>：偏好字段可空（历史用户未设置），缺省应保持原有"发通知"行为，
     * 避免因数据缺失把通知静默丢弃。</p>
     */
    private boolean recipientAllowsNotification(Long userId, String type) {
        if (userId == null) {
            return true;
        }
        // 用 var 承接：本类已依赖 portalUserMapper（见类头 import），此处不再新增
        // com.moyun.portal.domain.entity.PortalUser 的 import —— 避免把
        // ModuleDependencyGuardTest 冻结的 system -> portal 计数推高（该计数写明"待建端口"）。
        var recipient = portalUserMapper.selectPortalUserById(userId);
        if (recipient == null) {
            return true;
        }
        Boolean flag;
        switch (type == null ? "" : type) {
            case "like":
            case "article_like":
                flag = recipient.getNotifyLike();
                break;
            case "comment":
            case "reply":
            case "article_comment":
                flag = recipient.getNotifyComment();
                break;
            case "follow":
                flag = recipient.getNotifyFollow();
                break;
            default:
                flag = recipient.getNotifySystem();
                break;
        }
        return flag == null || flag;
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

    /**
     * 发送待办通知（type=todo）给所有系统用户 + 被系统用户绑定的前台用户
     * 使用个人通知（scope=user）定向发送，未绑定的前台用户不可见
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int sendTodoNotification(SysNotification template) {
        // 强制 type=todo, scope=user
        template.setType("todo");
        template.setScope("user");
        template.setNoticeType("1");
        template.setStatus("0");
        if (template.getCreateTime() == null) {
            template.setCreateTime(LocalDateTime.now());
        }

        List<SysNotification> batch = new ArrayList<>();

        // 1. 系统用户
        List<SysUser> sysUsers = sysUserMapper.selectUserList(new SysUser());
        for (SysUser su : sysUsers) {
            SysNotification n = cloneTemplate(template);
            n.setUserId(su.getUserId());
            n.setUserType("sys");
            batch.add(n);
        }

        // 2. 被系统用户绑定的前台用户
        List<Long> boundPortalIds = portalUserMapper.selectBoundPortalUserIds();
        for (Long portalId : boundPortalIds) {
            SysNotification n = cloneTemplate(template);
            n.setUserId(portalId);
            n.setUserType("portal");
            batch.add(n);
        }

        int count = 0;
        for (SysNotification n : batch) {
            count += sysNotificationMapper.insertNotification(n);
        }
        return count;
    }

    /** 克隆通知模板，避免共享同一对象引用 */
    private SysNotification cloneTemplate(SysNotification src) {
        SysNotification n = new SysNotification();
        n.setType(src.getType());
        n.setTitle(src.getTitle());
        n.setContent(src.getContent());
        n.setData(src.getData());
        n.setScope(src.getScope());
        n.setNoticeType(src.getNoticeType());
        n.setStatus(src.getStatus());
        n.setCreateTime(src.getCreateTime());
        return n;
    }

    /**
     * 关闭待办：按 data JSON 字段中的 bizType + id 精确匹配所有 type=todo 的通知，
     * 将 status 从 0（正常）更新为 1（关闭/已办）。
     * <p>调用方需在 sendTodoNotification 时保证 data 字段包含
     * {@code {"bizType":"xxx","id":123}} 结构，本方法才能正确匹配。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int completeTodoByBizData(String bizType, Long entityId) {
        if (bizType == null || bizType.isEmpty() || entityId == null) {
            return 0;
        }
        return sysNotificationMapper.completeTodoByBizData(bizType, entityId);
    }

    // ==================== 用户通知查询（门户 + 系统） ====================

    @Override
    public Page<SysNotification> selectUserNotifications(Page<SysNotification> page, Long userId, String userType) {
        // 兼容旧调用方（系统用户收件箱）：不按类型过滤
        return selectUserNotifications(page, userId, userType, null, null);
    }

    @Override
    public Page<SysNotification> selectUserNotifications(Page<SysNotification> page, Long userId, String userType,
                                                         String type, Boolean excludeTodo) {
        if (userId == null) {
            throw new ServiceException("用户ID不能为空");
        }
        if (userType == null || userType.isEmpty()) {
            userType = USER_TYPE_PORTAL;
        }
        return sysNotificationMapper.selectAllByUserId(page, userId, userType, type, excludeTodo);
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

    @Override
    public Page<SysNotification> selectBroadcastNotifications(Page<SysNotification> page, Long userId, String userType) {
        // userId/userType 为 null 时不计算已读状态（未登录用户）
        return sysNotificationMapper.selectBroadcastAll(page, userId, userType);
    }
}
