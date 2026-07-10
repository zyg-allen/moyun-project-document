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
     * @param userId     当前用户ID
     * @param userType   当前用户类型 portal/sys
     * @param query      分页参数
     * @return 会话列表
     */
    Page<MessageSessionVO> listSessions(Long userId, String userType, PageDomain query);

    /**
     * 历史消息（分页，校验会话归属）
     *
     * @param sessionId 会话ID
     * @param userId    当前用户ID
     * @param userType  当前用户类型 portal/sys
     * @param query     分页参数
     * @return 历史消息列表
     */
    Page<MessageVO> listHistory(Long sessionId, Long userId, String userType, PageDomain query);

    /**
     * 发送消息
     *
     * @param senderId     发送者ID
     * @param senderType   发送者类型 portal/sys
     * @param receiverId   接收者ID
     * @param receiverType 接收者类型 portal/sys
     * @param content      消息内容
     * @param msgType      消息类型 text/image/file
     * @return 创建的消息 VO
     */
    MessageVO sendMessage(Long senderId, String senderType, Long receiverId, String receiverType, String content, String msgType);

    /**
     * 标记会话已读（当前用户未读清零，消息置为已读）
     *
     * @param sessionId 会话ID
     * @param userId    当前用户ID
     * @param userType  当前用户类型 portal/sys
     * @return 是否操作成功
     */
    boolean markSessionRead(Long sessionId, Long userId, String userType);

    /**
     * 总未读数
     *
     * @param userId   当前用户ID
     * @param userType 当前用户类型 portal/sys
     * @return 总未读数
     */
    Integer getUnreadCount(Long userId, String userType);

    /**
     * 创建或获取会话（用 INSERT IGNORE 避免重复）
     *
     * @param userA       用户A（较小ID）
     * @param userAType   A方类型
     * @param userB       用户B（较大ID）
     * @param userBType   B方类型
     * @return 会话对象
     */
    PortalMessageSession createOrGetSession(Long userA, String userAType, Long userB, String userBType);

    /**
     * 按对方用户ID获取或创建会话，并返回 VO（含对方信息与未读数）
     * 用于"作者主页发起新私信"场景：尚未有任何消息往来时仍能进入聊天页
     *
     * @param currentUserId   当前用户ID
     * @param currentUserType 当前用户类型 portal/sys
     * @param peerUserId      对方用户ID
     * @param peerUserType    对方用户类型 portal/sys
     * @return 会话 VO
     */
    MessageSessionVO getOrCreateSessionVO(Long currentUserId, String currentUserType, Long peerUserId, String peerUserType);
}
