package com.moyun.ext.cms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.moyun.core.base.page.PageDomain;
import com.moyun.ext.cms.domain.vo.MessageSessionVO;
import com.moyun.ext.cms.domain.vo.MessageVO;
import com.moyun.portal.domain.entity.PortalMessageSession;

/**
 * 私信 Service
 *
 * @author moyun
 */
public interface IMessageService {

    /**
     * 会话列表（分页）
     *
     * @param userId 当前用户ID
     * @param query  分页参数
     * @return 会话列表
     */
    Page<MessageSessionVO> listSessions(Long userId, PageDomain query);

    /**
     * 历史消息（分页，校验会话归属）
     *
     * @param sessionId 会话ID
     * @param userId    当前用户ID
     * @param query     分页参数
     * @return 历史消息列表
     */
    Page<MessageVO> listHistory(Long sessionId, Long userId, PageDomain query);

    /**
     * 发送消息
     *
     * @param senderId   发送者ID
     * @param receiverId 接收者ID
     * @param content    消息内容
     * @param msgType    消息类型 text/image/file
     * @return 创建的消息 VO
     */
    MessageVO sendMessage(Long senderId, Long receiverId, String content, String msgType);

    /**
     * 标记会话已读（当前用户未读清零，消息置为已读）
     *
     * @param sessionId 会话ID
     * @param userId    当前用户ID
     * @return 是否操作成功
     */
    boolean markSessionRead(Long sessionId, Long userId);

    /**
     * 总未读数
     *
     * @param userId 当前用户ID
     * @return 总未读数
     */
    Integer getUnreadCount(Long userId);

    /**
     * 创建或获取会话（用 INSERT IGNORE 避免重复）
     *
     * @param userA 用户A
     * @param userB 用户B
     * @return 会话对象
     */
    PortalMessageSession createOrGetSession(Long userA, Long userB);

    /**
     * 按对方用户ID获取或创建会话，并返回 VO（含对方信息与未读数）
     * 用于"作者主页发起新私信"场景：尚未有任何消息往来时仍能进入聊天页
     *
     * @param currentUserId 当前用户ID
     * @param peerUserId    对方用户ID
     * @return 会话 VO
     */
    MessageSessionVO getOrCreateSessionVO(Long currentUserId, Long peerUserId);
}
