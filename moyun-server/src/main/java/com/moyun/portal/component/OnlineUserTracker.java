package com.moyun.portal.component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.moyun.portal.handler.PortalWebSocketAuthInterceptor;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 门户 WebSocket 在线用户跟踪器
 *
 * <p>在 STOMP SessionConnect 事件中登记 userId→sessionId，在 SessionDisconnect 事件中移除，
 * 用于判断接收者是否在线（在线则实时推送，离线则等待上线后通过未读数可见）。</p>
 *
 * @author moyun
 */
@Slf4j
@Component
public class OnlineUserTracker {

    private final ConcurrentHashMap<Long, Set<String>> userIdToSessionIds = new ConcurrentHashMap<>();

    @EventListener
    public void onConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Long userId = extractUserId(accessor);
        String sessionId = accessor.getSessionId();
        if (userId != null && sessionId != null) {
            userIdToSessionIds.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
            log.debug("WebSocket上线 userId={} sessionId={} 在线会话数={}", userId, sessionId, userIdToSessionIds.size());
        }
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Long userId = extractUserId(accessor);
        String sessionId = accessor.getSessionId();
        if (userId == null || sessionId == null) {
            return;
        }
        Set<String> sessions = userIdToSessionIds.get(userId);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                userIdToSessionIds.remove(userId);
            }
            log.debug("WebSocket离线 userId={} sessionId={}", userId, sessionId);
        }
    }

    private Long extractUserId(StompHeaderAccessor accessor) {
        Map<String, Object> attrs = accessor.getSessionAttributes();
        if (attrs != null) {
            Object id = attrs.get(PortalWebSocketAuthInterceptor.USER_ID_ATTR);
            if (id instanceof Long) {
                return (Long) id;
            }
        }
        return null;
    }

    /**
     * 判断用户是否在线
     *
     * @param userId 用户ID
     * @return 是否在线
     */
    public boolean isOnline(Long userId) {
        Set<String> sessions = userIdToSessionIds.get(userId);
        return sessions != null && !sessions.isEmpty();
    }
}
