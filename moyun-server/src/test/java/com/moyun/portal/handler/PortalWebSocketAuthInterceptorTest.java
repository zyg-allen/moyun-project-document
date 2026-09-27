package com.moyun.portal.handler;

import com.moyun.portal.domain.model.PortalLoginUser;
import com.moyun.portal.security.auth.PortalTokenService;
import com.moyun.portal.security.ws.WsTicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.WebSocketHandler;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * WebSocket 握手鉴权（v13.21，报告 §6.4「token 走 URL query」）
 *
 * <h3>修复前</h3>
 * <p>握手从 {@code ?token=<门户JWT>} 取凭证——浏览器 {@code new WebSocket()} 无法自定义请求头，
 * 只能把 JWT 写进 URL，于是门户 JWT 进入 Nginx access log / 浏览器历史 / 代理日志，
 * 而 JWT 在有效期内可重放 → 等价账号接管。</p>
 *
 * <h3>修复后（本测试锁定）</h3>
 * <ul>
 *   <li>{@code ?ticket=} 一次性票据 → 放行，userId 写入握手 attributes；</li>
 *   <li><b>{@code ?token=} 明文 JWT → 一律拒绝</b>（回归锁：老客户端不得"还能用"）；</li>
 *   <li>{@code Authorization: Bearer} → 放行（小程序/原生/服务端客户端）；</li>
 *   <li>三者都没有 → 401 拒绝，且不触碰票据服务。</li>
 * </ul>
 *
 * @author moyun
 */
class PortalWebSocketAuthInterceptorTest {

    private PortalWebSocketAuthInterceptor interceptor;
    private PortalTokenService portalTokenService;
    private WsTicketService wsTicketService;

    @BeforeEach
    void setUp() {
        interceptor = new PortalWebSocketAuthInterceptor();
        portalTokenService = mock(PortalTokenService.class);
        wsTicketService = mock(WsTicketService.class);
        ReflectionTestUtils.setField(interceptor, "portalTokenService", portalTokenService);
        ReflectionTestUtils.setField(interceptor, "wsTicketService", wsTicketService);
    }

    private Map<String, Object> handshake(MockHttpServletRequest request) {
        Map<String, Object> attributes = new HashMap<>();
        MockHttpServletResponse raw = new MockHttpServletResponse();
        boolean ok = interceptor.beforeHandshake(
                new ServletServerHttpRequest(request), new ServletServerHttpResponse(raw),
                mock(WebSocketHandler.class), attributes);
        if (ok) {
            attributes.put("__ok__", true);
        } else {
            attributes.put("__status__", raw.getStatus());
        }
        return attributes;
    }

    @Test
    @DisplayName("一次性票据：放行，userId 写入 attributes，且票据被消费")
    void ticketIsAccepted() {
        when(wsTicketService.consume("t-1")).thenReturn(7L);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws-asr");
        request.setParameter("ticket", "t-1");

        Map<String, Object> attributes = handshake(request);

        assertEquals(Boolean.TRUE, attributes.get("__ok__"));
        assertEquals(7L, attributes.get(PortalWebSocketAuthInterceptor.USER_ID_ATTR));
        verify(wsTicketService).consume("t-1");
    }

    @Test
    @DisplayName("票据无效/已用过（consume 返回 null）→ 401 拒绝")
    void consumedOrExpiredTicketRejected() {
        when(wsTicketService.consume(anyString())).thenReturn(null);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws-asr");
        request.setParameter("ticket", "replayed");

        Map<String, Object> attributes = handshake(request);

        assertNull(attributes.get("__ok__"));
        assertEquals(HttpStatus.UNAUTHORIZED.value(), attributes.get("__status__"));
    }

    @Test
    @DisplayName("回归锁：?token= 明文 JWT 一律拒绝（即使 token 本身有效）")
    void plainTokenInUrlIsRejected() {
        PortalLoginUser loginUser = new PortalLoginUser();
        loginUser.setId(7L);
        when(portalTokenService.getLoginUserByToken("valid-jwt")).thenReturn(loginUser);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws-asr");
        request.setParameter("token", "valid-jwt");

        Map<String, Object> attributes = handshake(request);

        assertNull(attributes.get("__ok__"), "明文 token 走 URL 的写法必须被拒绝");
        assertEquals(HttpStatus.UNAUTHORIZED.value(), attributes.get("__status__"));
        verify(portalTokenService, never()).getLoginUserByToken(anyString());
        verify(wsTicketService, never()).consume(anyString());
    }

    @Test
    @DisplayName("Authorization 头：小程序/原生/服务端客户端仍可建连（含 Bearer 前缀）")
    void authorizationHeaderStillAccepted() {
        PortalLoginUser loginUser = new PortalLoginUser();
        loginUser.setId(8L);
        when(portalTokenService.getLoginUserByToken("abc")).thenReturn(loginUser);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws-asr");
        request.addHeader("Authorization", "Bearer abc");

        Map<String, Object> attributes = handshake(request);

        assertEquals(Boolean.TRUE, attributes.get("__ok__"));
        assertEquals(8L, attributes.get(PortalWebSocketAuthInterceptor.USER_ID_ATTR));
    }

    @Test
    @DisplayName("Authorization 头 token 无效 → 401")
    void invalidAuthorizationRejected() {
        when(portalTokenService.getLoginUserByToken("bad")).thenReturn(null);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws-asr");
        request.addHeader("Authorization", "Bearer bad");

        Map<String, Object> attributes = handshake(request);

        assertNull(attributes.get("__ok__"));
        assertEquals(HttpStatus.UNAUTHORIZED.value(), attributes.get("__status__"));
    }

    @Test
    @DisplayName("无任何凭证 → 401，且不访问票据服务")
    void noCredentialRejected() {
        Map<String, Object> attributes = handshake(new MockHttpServletRequest("GET", "/ws-asr"));

        assertNull(attributes.get("__ok__"));
        assertEquals(HttpStatus.UNAUTHORIZED.value(), attributes.get("__status__"));
        verify(wsTicketService, never()).consume(anyString());
        verify(portalTokenService, never()).getLoginUserByToken(anyString());
    }

    @Test
    @DisplayName("票据优先于 Authorization：两者同时存在时用票据（且不解析 token）")
    void ticketTakesPrecedence() {
        when(wsTicketService.consume("t-2")).thenReturn(9L);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws-asr");
        request.setParameter("ticket", "t-2");
        request.addHeader("Authorization", "Bearer should-not-be-used");

        Map<String, Object> attributes = handshake(request);

        assertEquals(9L, attributes.get(PortalWebSocketAuthInterceptor.USER_ID_ATTR));
        verify(portalTokenService, never()).getLoginUserByToken(anyString());
    }

    @Test
    @DisplayName("非 Servlet 请求（如测试桩）不崩：返回 false 而非异常")
    void nonServletRequestIsRejectedSafely() {
        boolean ok = interceptor.beforeHandshake(
                mock(org.springframework.http.server.ServerHttpRequest.class),
                mock(org.springframework.http.server.ServerHttpResponse.class),
                mock(WebSocketHandler.class), new HashMap<>());
        assertFalse(ok);
    }

    // ==================== Origin 校验（v13.23） ====================

    @Test
    @DisplayName("Origin 伪造：像内网的域名（192.168.evil.com）即使带合法票据也拒绝")
    void spoofedOriginRejectedEvenWithValidTicket() {
        when(wsTicketService.consume("t-3")).thenReturn(7L);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws-asr");
        request.setParameter("ticket", "t-3");
        request.addHeader("Origin", "http://192.168.evil.com");

        Map<String, Object> attributes = handshake(request);

        assertNull(attributes.get("__ok__"), "Origin 不在白名单必须在鉴权前拒绝");
        assertEquals(HttpStatus.UNAUTHORIZED.value(), attributes.get("__status__"));
        verify(wsTicketService, never()).consume(anyString());
    }

    @Test
    @DisplayName("Origin 回环（含端口）：放行（本地开发 vite dev server）")
    void loopbackOriginAccepted() {
        when(wsTicketService.consume("t-4")).thenReturn(7L);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws-asr");
        request.setParameter("ticket", "t-4");
        request.addHeader("Origin", "http://localhost:5173");

        assertEquals(7L, handshake(request).get(PortalWebSocketAuthInterceptor.USER_ID_ATTR));
    }

    @Test
    @DisplayName("无 Origin（小程序/原生/服务端客户端）：放行，仍走票据或 Authorization 鉴权")
    void missingOriginAllowedForNonBrowserClients() {
        when(wsTicketService.consume("t-5")).thenReturn(7L);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws-asr");
        request.setParameter("ticket", "t-5");

        assertEquals(7L, handshake(request).get(PortalWebSocketAuthInterceptor.USER_ID_ATTR),
                "非浏览器客户端没有 Origin 头，不能因此拒绝（鉴权仍由票据/头保证）");
    }

    @Test
    @DisplayName("Origin 无关：未带凭证时即使 Origin 合法也 401（Origin 校验不替代鉴权）")
    void allowedOriginStillRequiresCredential() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws-asr");
        request.addHeader("Origin", "http://localhost:5173");

        Map<String, Object> attributes = handshake(request);

        assertNull(attributes.get("__ok__"));
        assertEquals(HttpStatus.UNAUTHORIZED.value(), attributes.get("__status__"));
    }
}
