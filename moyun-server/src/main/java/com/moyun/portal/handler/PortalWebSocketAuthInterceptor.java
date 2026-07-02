package com.moyun.portal.handler;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import com.moyun.common.constant.Constants;
import com.moyun.portal.domain.model.PortalLoginUser;
import com.moyun.portal.security.auth.PortalTokenService;
import com.moyun.util.string.StringUtils;

import java.util.Map;

/**
 * WebSocket 握手鉴权拦截器
 *
 * <p>从握手请求的 token 参数（?token=xxx）或 Authorization 请求头解析门户 token，
 * 调用 {@link PortalTokenService} 解析出门户用户ID，存入握手 attributes（key: userId），
 * 供后续 STOMP 会话使用（如绑定 Principal、在线状态跟踪）。</p>
 *
 * <p>注意：WebSocket 握手不走 Spring Security 过滤器链的鉴权逻辑，需在此处单独校验。
 * token 无效或缺失时拒绝握手（返回 401）。</p>
 *
 * @author moyun
 */
@Slf4j
@Component
public class PortalWebSocketAuthInterceptor implements HandshakeInterceptor {

    /** 握手 attributes 中存放门户用户ID 的 key */
    public static final String USER_ID_ATTR = "userId";

    @Autowired
    @Qualifier("portalTokenService")
    private PortalTokenService portalTokenService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        Long userId = resolveUserId(request);
        if (userId == null) {
            log.warn("WebSocket握手失败：token无效或缺失");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        attributes.put(USER_ID_ATTR, userId);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }

    private Long resolveUserId(ServerHttpRequest request) {
        String token = extractToken(request);
        if (StringUtils.isEmpty(token)) {
            return null;
        }
        PortalLoginUser loginUser = portalTokenService.getLoginUserByToken(token);
        return loginUser == null ? null : loginUser.getId();
    }

    /**
     * 优先从 query 参数 token 获取，回退到 Authorization 请求头
     */
    private String extractToken(ServerHttpRequest request) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return null;
        }
        HttpServletRequest http = servletRequest.getServletRequest();
        String token = http.getParameter("token");
        if (StringUtils.isEmpty(token)) {
            token = http.getHeader("Authorization");
        }
        return stripBearer(token);
    }

    private String stripBearer(String token) {
        if (StringUtils.isNotEmpty(token) && token.startsWith(Constants.TOKEN_PREFIX)) {
            token = token.replace(Constants.TOKEN_PREFIX, "");
        }
        return token;
    }
}
