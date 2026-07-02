package com.moyun.portal.component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.moyun.ext.cms.domain.vo.MessageVO;

/**
 * 私信 WebSocket 推送器
 *
 * <p>通过 STOMP 将新消息实时推送给接收者。在线用户即时收到；
 * 离线用户未收到实时推送，但未读数已累加，上线后通过未读数/历史消息可见。</p>
 *
 * @author moyun
 */
@Slf4j
@Component
public class MessageWebSocketSender {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private OnlineUserTracker onlineUserTracker;

    /**
     * 向指定用户推送一条私信消息。
     *
     * <p>消息会发送到 /user/{userId}/queue/message，前端订阅 /user/queue/message 接收。</p>
     *
     * @param userId  接收者用户ID
     * @param message 消息内容
     */
    public void pushToUser(Long userId, MessageVO message) {
        if (userId == null || message == null) {
            return;
        }
        try {
            messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/message", message);
            if (onlineUserTracker.isOnline(userId)) {
                log.debug("私信实时推送成功 userId={}", userId);
            } else {
                log.debug("接收者离线 userId={}，消息以未读数形式在上线后可见", userId);
            }
        } catch (Exception e) {
            log.warn("私信WebSocket推送失败 userId={}", userId, e);
        }
    }
}
