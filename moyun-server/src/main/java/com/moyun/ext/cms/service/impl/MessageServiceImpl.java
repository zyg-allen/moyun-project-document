package com.moyun.ext.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moyun.common.exception.system.ServiceException;
import com.moyun.core.base.page.PageDomain;
import com.moyun.ext.cms.domain.vo.MessageSessionVO;
import com.moyun.ext.cms.domain.vo.MessageVO;
import com.moyun.ext.cms.service.IMessageService;
import com.moyun.portal.component.MessageWebSocketSender;
import com.moyun.portal.domain.entity.PortalMessage;
import com.moyun.portal.domain.entity.PortalMessageSession;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.mapper.PortalMessageMapper;
import com.moyun.portal.mapper.PortalMessageSessionMapper;
import com.moyun.portal.mapper.PortalUserMapper;
import com.moyun.system.mapper.SysUserMapper;
import com.moyun.core.base.entity.SysUser;
import com.moyun.util.bean.PageUtils;
import com.moyun.util.string.StringUtils;

import java.time.LocalDateTime;

/**
 * 私信 Service 实现
 *
 * <p>支持跨用户体系（portal 用户 ↔ sys 管理员）：
 * 会话表 user_a_type/user_b_type 标识双方所属体系，
 * 查询时必须同时按 userId + userType 过滤，避免不同体系同 ID 撞会话。
 * user_a/user_b 仍按数值"小ID为A"归一化，类型跟随各自一方。</p>
 *
 * @author moyun
 */
@Service
public class MessageServiceImpl implements IMessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageServiceImpl.class);

    /** 最后消息内容预览最大长度（与表字段 VARCHAR(500) 对齐） */
    private static final int PREVIEW_MAX_LENGTH = 500;

    /** 门户用户类型标识 */
    private static final String TYPE_PORTAL = "portal";
    /** 系统用户（管理员）类型标识 */
    private static final String TYPE_SYS = "sys";

    @Autowired
    private PortalMessageSessionMapper sessionMapper;

    @Autowired
    private PortalMessageMapper messageMapper;

    @Autowired
    private PortalUserMapper userMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private MessageWebSocketSender messageWebSocketSender;

    // ========================================================================
    // 会话列表
    // ========================================================================
    @Override
    public Page<MessageSessionVO> listSessions(Long userId, String userType, PageDomain query) {
        Page<MessageSessionVO> page = PageUtils.buildPage(query.getPageNum(), query.getPageSize());
        return sessionMapper.selectMySessions(page, userId, userType);
    }

    // ========================================================================
    // 历史消息
    // ========================================================================
    @Override
    public Page<MessageVO> listHistory(Long sessionId, Long userId, String userType, PageDomain query) {
        PortalMessageSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new ServiceException("会话不存在");
        }
        if (!isMember(session, userId, userType)) {
            throw new ServiceException("无权查看该会话");
        }
        Page<MessageVO> page = PageUtils.buildPage(query.getPageNum(), query.getPageSize());
        return messageMapper.selectBySession(page, sessionId);
    }

    // ========================================================================
    // 发送消息
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MessageVO sendMessage(Long senderId, String senderType, Long receiverId, String receiverType, String content, String msgType) {
        if (senderId == null || receiverId == null) {
            throw new ServiceException("发送者/接收者不能为空");
        }
        if (senderId.equals(receiverId) && StringUtils.equals(senderType, receiverType)) {
            throw new ServiceException("不能给自己发私信");
        }
        if (StringUtils.isEmpty(content)) {
            throw new ServiceException("消息内容不能为空");
        }
        String type = StringUtils.isEmpty(msgType) ? "text" : msgType;
        String st = StringUtils.isEmpty(senderType) ? TYPE_PORTAL : senderType;
        String rt = StringUtils.isEmpty(receiverType) ? TYPE_PORTAL : receiverType;

        // 1) 计算会话 userA/userB（小ID为A），类型跟随各自一方
        Long[] idsTypes = normalizePair(senderId, st, receiverId, rt);
        Long userA = idsTypes[0];
        String userAType = st;
        Long userB = idsTypes[1];
        String userBType = rt;
        // 若 id 顺序因归一化被交换，类型也同步交换，保证类型与 id 一一对应
        if (!senderId.equals(userA)) {
            userAType = rt;
            userBType = st;
        }

        // 2) 查询或创建会话
        PortalMessageSession session = createOrGetSession(userA, userAType, userB, userBType);

        // 3) 写入消息
        LocalDateTime now = LocalDateTime.now();
        PortalMessage message = new PortalMessage();
        message.setSessionId(session.getId());
        message.setSenderId(senderId);
        message.setSenderType(st);
        message.setReceiverId(receiverId);
        message.setReceiverType(rt);
        message.setContent(content);
        message.setMsgType(type);
        message.setIsRead(0);
        message.setCreateTime(now);
        messageMapper.insert(message);

        // 4) 更新会话 last_message_id/content/time
        // 5) 接收者未读数 +1
        String preview = content.length() > PREVIEW_MAX_LENGTH
                ? content.substring(0, PREVIEW_MAX_LENGTH) : content;
        LambdaUpdateWrapper<PortalMessageSession> uw = Wrappers.lambdaUpdate();
        uw.eq(PortalMessageSession::getId, session.getId())
          .set(PortalMessageSession::getLastMessageId, message.getId())
          .set(PortalMessageSession::getLastMessageContent, preview)
          .set(PortalMessageSession::getLastMessageTime, now)
          .set(PortalMessageSession::getUpdateTime, now);
        if (isReceiverSideA(session, receiverId, rt)) {
            uw.setSql("unread_a = unread_a + 1");
        } else {
            uw.setSql("unread_b = unread_b + 1");
        }
        sessionMapper.update(null, uw);

        // 6) 通过 WebSocket 推送给接收者
        // 注：sys 管理员不通过 portal token 订阅 WS，推送对其无实时通道，
        //     但未读数已累加，管理员侧通过头部铃铛/私信中心轮询感知。
        MessageVO vo = buildMessageVO(message);
        try {
            messageWebSocketSender.pushToUser(receiverId, vo);
        } catch (Exception e) {
            log.warn("私信WebSocket推送失败 receiverId={}", receiverId, e);
        }
        return vo;
    }

    // ========================================================================
    // 标记会话已读
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markSessionRead(Long sessionId, Long userId, String userType) {
        PortalMessageSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            return false;
        }
        if (!isMember(session, userId, userType)) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();

        // 当前用户未读清零
        LambdaUpdateWrapper<PortalMessageSession> uw = Wrappers.lambdaUpdate();
        uw.eq(PortalMessageSession::getId, sessionId)
          .set(PortalMessageSession::getUpdateTime, now);
        if (isReceiverSideA(session, userId, userType)) {
            uw.set(PortalMessageSession::getUnreadA, 0);
        } else {
            uw.set(PortalMessageSession::getUnreadB, 0);
        }
        sessionMapper.update(null, uw);

        // 当前用户为接收者且未读的消息标记为已读
        LambdaUpdateWrapper<PortalMessage> muw = Wrappers.lambdaUpdate();
        muw.eq(PortalMessage::getSessionId, sessionId)
           .eq(PortalMessage::getReceiverId, userId)
           .eq(PortalMessage::getReceiverType, userType)
           .eq(PortalMessage::getIsRead, 0)
           .set(PortalMessage::getIsRead, 1);
        messageMapper.update(null, muw);
        return true;
    }

    // ========================================================================
    // 总未读数
    // ========================================================================
    @Override
    public Integer getUnreadCount(Long userId, String userType) {
        Integer count = messageMapper.selectUnreadCount(userId, userType);
        return count == null ? 0 : count;
    }

    // ========================================================================
    // 创建或获取会话
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalMessageSession createOrGetSession(Long userA, String userAType, Long userB, String userBType) {
        // 保证 userA 为较小 ID，user_b 为较大 ID，类型跟随
        Long a = Math.min(userA, userB);
        Long b = Math.max(userA, userB);
        String aType = (a.equals(userA)) ? userAType : userBType;
        String bType = (b.equals(userB)) ? userBType : userAType;

        PortalMessageSession session = sessionMapper.selectByUsers(a, aType, b, bType);
        if (session != null) {
            return session;
        }
        LocalDateTime now = LocalDateTime.now();
        PortalMessageSession entity = new PortalMessageSession();
        entity.setUserA(a);
        entity.setUserAType(aType);
        entity.setUserB(b);
        entity.setUserBType(bType);
        entity.setUnreadA(0);
        entity.setUnreadB(0);
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        // INSERT IGNORE 避免并发重复创建；插入后再查询拿真实 id
        sessionMapper.insertIgnore(entity);
        return sessionMapper.selectByUsers(a, aType, b, bType);
    }

    // ========================================================================
    // 按对方用户ID获取或创建会话 VO
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MessageSessionVO getOrCreateSessionVO(Long currentUserId, String currentUserType, Long peerUserId, String peerUserType) {
        if (currentUserId == null || peerUserId == null) {
            throw new ServiceException("用户ID不能为空");
        }
        if (currentUserId.equals(peerUserId) && StringUtils.equals(currentUserType, peerUserType)) {
            throw new ServiceException("不能给自己发私信");
        }
        PortalMessageSession session = createOrGetSession(currentUserId, currentUserType, peerUserId, peerUserType);
        return sessionMapper.selectSessionVOById(session.getId(), currentUserId, currentUserType);
    }

    // ========================================================================
    // 私有辅助
    // ========================================================================

    /** 规整双方 ID：返回 [userA(较小), userB(较大)]，类型由调用方按是否交换判定 */
    private Long[] normalizePair(Long id1, String type1, Long id2, String type2) {
        if (id1 <= id2) {
            return new Long[]{id1, id2};
        }
        return new Long[]{id2, id1};
    }

    /** 当前用户是否为该会话成员 */
    private boolean isMember(PortalMessageSession session, Long userId, String userType) {
        if (session.getUserA().equals(userId) && StringUtils.equals(session.getUserAType(), userType)) {
            return true;
        }
        return session.getUserB().equals(userId) && StringUtils.equals(session.getUserBType(), userType);
    }

    /** 当前用户是否处于会话 A 方 */
    private boolean isReceiverSideA(PortalMessageSession session, Long userId, String userType) {
        return session.getUserA().equals(userId) && StringUtils.equals(session.getUserAType(), userType);
    }

    private MessageVO buildMessageVO(PortalMessage m) {
        MessageVO vo = new MessageVO();
        vo.setId(m.getId());
        vo.setSessionId(m.getSessionId());
        vo.setSenderId(m.getSenderId());
        vo.setSenderType(m.getSenderType());
        vo.setReceiverId(m.getReceiverId());
        vo.setReceiverType(m.getReceiverType());
        vo.setContent(m.getContent());
        vo.setMsgType(m.getMsgType());
        vo.setIsRead(m.getIsRead());
        vo.setCreateTime(m.getCreateTime());
        // 发送者信息：portal 用户走 PortalUserMapper，sys 管理员走 SysUserMapper
        if (TYPE_SYS.equals(m.getSenderType())) {
            SysUser sysUser = sysUserMapper.selectById(m.getSenderId());
            if (sysUser != null) {
                vo.setSenderNickname(sysUser.getNickName());
                vo.setSenderAvatar(sysUser.getAvatar());
            }
        } else {
            PortalUser sender = userMapper.selectById(m.getSenderId());
            if (sender != null) {
                vo.setSenderNickname(sender.getNickname());
                vo.setSenderAvatar(sender.getAvatar());
            }
        }
        return vo;
    }
}
