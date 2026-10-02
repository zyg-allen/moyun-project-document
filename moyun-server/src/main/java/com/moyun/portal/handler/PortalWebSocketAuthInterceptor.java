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
 * <p>鉴权来源：</p>
 * <ol>
 *   <li><b>{@code ?ticket=xxx}</b>：一次性短时效票据（{@link com.moyun.portal.security.ws.WsTicketService}，
 *       60 秒、原子消费一次）——<b>浏览器唯一可行</b>的方式（{@code new WebSocket()} 不能自定义请求头）；
 *       客户端先走受保护的 {@code POST /portal/ws-ticket} 换取；</li>
 *   <li><b>{@code Authorization: Bearer xxx}</b>：非浏览器客户端（小程序、原生、服务端）可直接用门户 token 建连。</li>
 * </ol>
 *
 * <p><b>安全约束</b>：不接受 {@code ?token=<门户JWT>}——门户 JWT 在有效期内可重放，一旦进 URL 就会
 * 落到 Nginx access log / 浏览器历史 / 代理日志里 → 等于账号被接管。现在遇到该参数**直接拒绝握手**
 * 并告警，防止老客户端"看起来还能用"而把漏洞带回来。</p>
 *
 * <p>校验通过后把门户用户ID 存入握手 attributes（key: {@link #USER_ID_ATTR}）；
 * token/票据无效或缺失时拒绝握手（401）。</p>
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

    @Autowired
    private com.moyun.portal.security.ws.WsTicketService wsTicketService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        Long userId = resolveUserId(request);
        if (userId == null) {
            log.warn("WebSocket握手失败：凭证无效或缺失（请用一次性票据 ?ticket= 或 Authorization 头）");
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
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return null;
        }
        HttpServletRequest http = servletRequest.getServletRequest();

        // ⓪ Origin 校验：WebSocket 不受同源策略保护，必须服务端自判。
        //    口径与 CORS 一致：无 Origin（小程序/原生/服务端）放行；回环/环境变量白名单/（显式开启的）私网 IP 放行；
        //    其余（含 http://192.168.evil.com 这类"像内网"的可注册域名）拒绝。
        String origin = http.getHeader("Origin");
        if (!com.moyun.core.config.ResourcesConfig.isWsOriginAllowed(origin)) {
            log.warn("WebSocket 握手拒绝：Origin 不在白名单内 origin={}", origin);
            return null;
        }

        // ① 一次性票据（浏览器路径）
        String ticket = http.getParameter("ticket");
        if (StringUtils.isNotEmpty(ticket)) {
            Long userId = wsTicketService.consume(ticket);
            if (userId == null) {
                log.warn("WebSocket 握手票据无效/已使用/已过期");
            }
            return userId;
        }

        // ② 显式拒绝历史写法：明文 token 进 URL
        if (StringUtils.isNotEmpty(http.getParameter("token"))) {
            log.warn("WebSocket 握手拒绝 ?token= 明文传参（JWT 会进访问日志/浏览器历史）"
                    + "——请改用 POST /portal/ws-ticket 换取一次性票据");
            return null;
        }

        // ③ Authorization 头（小程序/原生/服务端客户端）
        String token = stripBearer(http.getHeader("Authorization"));
        if (StringUtils.isEmpty(token)) {
            return null;
        }
        PortalLoginUser loginUser = portalTokenService.getLoginUserByToken(token);
        return loginUser == null ? null : loginUser.getId();
    }

    private String stripBearer(String token) {
        if (StringUtils.isNotEmpty(token) && token.startsWith(Constants.TOKEN_PREFIX)) {
            token = token.replace(Constants.TOKEN_PREFIX, "");
        }
        return token;
    }
}
