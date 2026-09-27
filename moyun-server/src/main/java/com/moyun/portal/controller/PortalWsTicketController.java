package com.moyun.portal.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.portal.security.ws.WsTicketService;
import com.moyun.portal.util.PortalSecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * WebSocket 握手票据签发（v13.21）
 *
 * <p>路径 {@code POST /portal/ws-ticket}：落在门户链 {@code anyRequest().authenticated()} 上，
 * 必须携带正常登录态（{@code Authorization} 头）——**这正是关键**：用受保护的普通 HTTP 换取一张
 * 一次性短时效票据，再由 WebSocket 握手用票据建连，从而不必把门户 JWT 明文写进 WS URL。</p>
 *
 * <p>票据语义见 {@link WsTicketService}（60 秒、一次性、原子消费）。</p>
 *
 * @author moyun
 */
@Slf4j
@Tag(name = "WebSocket 握手票据", description = "语音流式/消息推送握手用的一次性票据（60s，一次性）")
@RestController
@RequestMapping("/portal/ws-ticket")
public class PortalWsTicketController {

    @Autowired
    private WsTicketService wsTicketService;

    @Operation(summary = "签发 WebSocket 握手票据",
            description = "需登录；返回 ticket 与有效期（秒）。握手时用 ?ticket=xxx，票据只能使用一次。")
    @PostMapping
    public AjaxResult issue() {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error(401, "登录已过期，请重新登录");
        }
        String ticket = wsTicketService.issue(userId);
        return AjaxResult.success(Map.of(
                "ticket", ticket,
                "expiresIn", WsTicketService.TTL_SECONDS));
    }
}
