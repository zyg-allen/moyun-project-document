package com.moyun.portal.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.moyun.portal.handler.AsrStreamRelayHandler;
import com.moyun.portal.handler.PortalWebSocketAuthInterceptor;

/**
 * 语音识别实时流式 WebSocket（原生二进制协议）配置 —— V10.1 语音面试官
 *
 * <p>端点：/ws-asr（前端连接时携带 ?token=xxx 握手鉴权，与 /ws-message 相同机制）。
 * 与 {@link PortalWebSocketConfig}（STOMP 消息推送）并存：本端点走原生 WebSocket，
 * 用于 PCM 音频二进制帧双向中继，不适合 STOMP 文本协议。</p>
 *
 * @author moyun
 */
@Configuration
@EnableWebSocket
public class PortalAsrWebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private AsrStreamRelayHandler asrStreamRelayHandler;

    @Autowired
    private PortalWebSocketAuthInterceptor authInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(asrStreamRelayHandler, "/ws-asr")
                .addInterceptors(authInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
