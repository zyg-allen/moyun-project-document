package com.moyun.portal.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.http.server.ServerHttpRequest;

import com.moyun.portal.handler.PortalWebSocketAuthInterceptor;

import java.security.Principal;
import java.util.Map;

/**
 * 门户 WebSocket（STOMP）配置
 *
 * <p>端点：/ws-message（前端连接时携带 ?token=xxx 进行握手鉴权）。</p>
 * <p>消息代理：/user、/topic；应用前缀：/app。</p>
 * <p>用户级推送：服务端通过 SimpMessagingTemplate.convertAndSendToUser(userId, "/queue/message", ...)
 * 推送到 /user/{userId}/queue/message，前端订阅 /user/queue/message。</p>
 *
 * @author moyun
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class PortalWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private PortalWebSocketAuthInterceptor authInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-message")
                .setAllowedOriginPatterns("*")
                .addInterceptors(authInterceptor)
                // 将握手阶段解析出的 userId 作为 Principal 名称，
                // 使 convertAndSendToUser(userId.toString(), ...) 能正确路由到对应用户的会话
                .setHandshakeHandler(new UserHandshakeHandler());
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 简单内存代理：/user 用于点对点推送，/topic 用于广播
        registry.enableSimpleBroker("/user", "/topic");
        // 客户端发送消息到服务端的前缀
        registry.setApplicationDestinationPrefixes("/app");
    }

    /**
     * 自定义握手处理器：以握手 attributes 中的 userId 作为 Principal 名称。
     * userId 由 {@link PortalWebSocketAuthInterceptor} 在握手前写入 attributes。
     */
    private static class UserHandshakeHandler extends DefaultHandshakeHandler {
        @Override
        protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
                                          Map<String, Object> attributes) {
            Object userId = attributes.get(PortalWebSocketAuthInterceptor.USER_ID_ATTR);
            final String name = userId == null ? "" : userId.toString();
            // Principal 为函数式接口，getName() 返回 userId 字符串
            return () -> name;
        }
    }
}
