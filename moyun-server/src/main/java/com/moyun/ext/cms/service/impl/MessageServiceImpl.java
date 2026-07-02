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
import com.moyun.util.bean.PageUtils;
import com.moyun.util.string.StringUtils;

import java.time.LocalDateTime;

/**
 * 私信 Service 实现
 *
 * @author moyun
 */
@Service
public class MessageServiceImpl implements IMessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageServiceImpl.class);

    /** 最后消息内容预览最大长度（与表字段 VARCHAR(500) 对齐） */
    private static final int PREVIEW_MAX_LENGTH = 500;

    @Autowired
    private PortalMessageSessionMapper sessionMapper;

    @Autowired
    private PortalMessageMapper messageMapper;

    @Autowired
    private PortalUserMapper userMapper;

    @Autowired
    private MessageWebSocketSender messageWebSocketSender;

    // ========================================================================
    // 会话列表
    // ========================================================================
    @Override
    public Page<MessageSessionVO> listSessions(Long userId, PageDomain query) {
        Page<MessageSessionVO> page = PageUtils.buildPage(query.getPageNum(), query.getPageSize());
        return sessionMapper.selectMySessions(page, userId);
    }

    // ========================================================================
    // 历史消息
    // ========================================================================
    @Override
    public Page<MessageVO> listHistory(Long sessionId, Long userId, PageDomain query) {
        PortalMessageSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new ServiceException("会话不存在");
        }
        if (!userId.equals(session.getUserA()) && !userId.equals(session.getUserB())) {
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
    public MessageVO sendMessage(Long senderId, Long receiverId, String content, String msgType) {
        if (senderId == null || receiverId == null) {
            throw new ServiceException("发送者/接收者不能为空");
        }
        if (senderId.equals(receiverId)) {
            throw new ServiceException("不能给自己发私信");
        }
        if (StringUtils.isEmpty(content)) {
            throw new ServiceException("消息内容不能为空");
        }
        String type = StringUtils.isEmpty(msgType) ? "text" : msgType;

        // 1) 计算会话 userA/userB（小ID为A）
        Long userA = Math.min(senderId, receiverId);
        Long userB = Math.max(senderId, receiverId);

        // 2) 查询或创建会话
        PortalMessageSession session = createOrGetSession(userA, userB);

        // 3) 写入消息
        LocalDateTime now = LocalDateTime.now();
        PortalMessage message = new PortalMessage();
        message.setSessionId(session.getId());
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
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
        if (receiverId.equals(session.getUserA())) {
            uw.setSql("unread_a = unread_a + 1");
        } else {
            uw.setSql("unread_b = unread_b + 1");
        }
        sessionMapper.update(null, uw);

        // 6) 通过 WebSocket 推送给接收者（在线则实时推，离线则等上线拉取，未读数已累加）
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
    public boolean markSessionRead(Long sessionId, Long userId) {
        PortalMessageSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            return false;
        }
        if (!userId.equals(session.getUserA()) && !userId.equals(session.getUserB())) {
            // 非会话成员，无权操作
            return false;
        }
        LocalDateTime now = LocalDateTime.now();

        // 当前用户未读清零
        LambdaUpdateWrapper<PortalMessageSession> uw = Wrappers.lambdaUpdate();
        uw.eq(PortalMessageSession::getId, sessionId)
          .set(PortalMessageSession::getUpdateTime, now);
        if (userId.equals(session.getUserA())) {
            uw.set(PortalMessageSession::getUnreadA, 0);
        } else {
            uw.set(PortalMessageSession::getUnreadB, 0);
        }
        sessionMapper.update(null, uw);

        // 当前用户为接收者且未读的消息标记为已读
        LambdaUpdateWrapper<PortalMessage> muw = Wrappers.lambdaUpdate();
        muw.eq(PortalMessage::getSessionId, sessionId)
           .eq(PortalMessage::getReceiverId, userId)
           .eq(PortalMessage::getIsRead, 0)
           .set(PortalMessage::getIsRead, 1);
        messageMapper.update(null, muw);
        return true;
    }

    // ========================================================================
    // 总未读数
    // ========================================================================
    @Override
    public Integer getUnreadCount(Long userId) {
        Integer count = messageMapper.selectUnreadCount(userId);
        return count == null ? 0 : count;
    }

    // ========================================================================
    // 创建或获取会话
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalMessageSession createOrGetSession(Long userA, Long userB) {
        // 保证 userA 为较小 ID，user_b 为较大 ID
        Long a = Math.min(userA, userB);
        Long b = Math.max(userA, userB);

        PortalMessageSession session = sessionMapper.selectByUsers(a, b);
        if (session != null) {
            return session;
        }
        LocalDateTime now = LocalDateTime.now();
        PortalMessageSession entity = new PortalMessageSession();
        entity.setUserA(a);
        entity.setUserB(b);
        entity.setUnreadA(0);
        entity.setUnreadB(0);
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        // INSERT IGNORE 避免并发重复创建；插入后再查询拿真实 id
        sessionMapper.insertIgnore(entity);
        return sessionMapper.selectByUsers(a, b);
    }

    // ========================================================================
    // 私有辅助
    // ========================================================================
    private MessageVO buildMessageVO(PortalMessage m) {
        MessageVO vo = new MessageVO();
        vo.setId(m.getId());
        vo.setSessionId(m.getSessionId());
        vo.setSenderId(m.getSenderId());
        vo.setReceiverId(m.getReceiverId());
        vo.setContent(m.getContent());
        vo.setMsgType(m.getMsgType());
        vo.setIsRead(m.getIsRead());
        vo.setCreateTime(m.getCreateTime());
        PortalUser sender = userMapper.selectById(m.getSenderId());
        if (sender != null) {
            vo.setSenderNickname(sender.getNickname());
            vo.setSenderAvatar(sender.getAvatar());
        }
        return vo;
    }
}
