package com.moyun.pay.controller;

import com.moyun.core.base.model.LoginUser;
import com.moyun.pay.domain.entity.PayOrder;
import com.moyun.pay.config.PayProperties;
import com.moyun.pay.gateway.IPayGateway;
import com.moyun.portal.domain.model.PortalLoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 收银台端点越权防护（v13.17，§6.4）
 *
 * <h3>修复前</h3>
 * <ul>
 *   <li>{@code GET /pay/status/{payNo}}：只校验"已登录"，{@code payNo} 可枚举/猜测
 *       → 任何登录用户都能读到**他人订单**的金额、支付链接（{@code codeUrl}）、过期时间；</li>
 *   <li>{@code POST /pay/mock/{payNo}}：只校验"已登录 + mock 已开启"
 *       → 可把**他人订单**置为支付成功，触发与真实回调一致的后续链路（发卡/记账/打赏到账）。</li>
 * </ul>
 * <p>现两个端点统一走 {@code ownOrderOrNull(payNo, userId)}：查不到与不属于自己返回同一个 403
 * （不泄露"订单是否存在"）。</p>
 *
 * @author moyun
 */
class PortalPayControllerOwnershipTest {

    private static final Long OWNER = 7L;
    private static final Long ATTACKER = 8L;

    private PortalPayController controller;
    private IPayGateway payGateway;
    private PayProperties payProperties;

    @BeforeEach
    void setUp() {
        controller = new PortalPayController();
        payGateway = mock(IPayGateway.class);
        payProperties = mock(PayProperties.class);
        PayProperties.Wechat wechat = mock(PayProperties.Wechat.class);
        when(payProperties.getWechat()).thenReturn(wechat);
        when(wechat.isMockEnabled()).thenReturn(true);
        ReflectionTestUtils.setField(controller, "payGateway", payGateway);
        ReflectionTestUtils.setField(controller, "payProperties", payProperties);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(Long userId) {
        PortalLoginUser loginUser = mock(PortalLoginUser.class);
        when(loginUser.getId()).thenReturn(userId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, List.of()));
    }

    private PayOrder orderOf(Long userId) {
        PayOrder order = new PayOrder();
        order.setPayNo("PAY-1");
        order.setUserId(userId);
        order.setAmount(new BigDecimal("99.00"));
        order.setStatus("pending");
        order.setCodeUrl("weixin://wxpay/bizpayurl?pr=secret");
        return order;
    }

    @Test
    @DisplayName("状态轮询：他人订单 → 403，且不返回金额/支付链接（IDOR 修复）")
    void statusRejectsOtherUsersOrder() {
        loginAs(ATTACKER);
        when(payGateway.queryStatus("PAY-1")).thenReturn(orderOf(OWNER));

        Object result = controller.status("PAY-1");

        assertEquals(403, codeOf(result));
        String body = result.toString();
        assertNotNull(body);
        // 关键：不得泄露他人订单的金额与支付链接
        org.junit.jupiter.api.Assertions.assertFalse(body.contains("99.00"), "不得回显他人订单金额");
        org.junit.jupiter.api.Assertions.assertFalse(body.contains("wxpayurl"), "不得回显他人订单支付链接");
    }

    @Test
    @DisplayName("状态轮询：本人订单 → 正常返回")
    void statusAllowsOwnOrder() {
        loginAs(OWNER);
        when(payGateway.queryStatus("PAY-1")).thenReturn(orderOf(OWNER));

        Object result = controller.status("PAY-1");

        assertEquals(200, codeOf(result));
    }

    @Test
    @DisplayName("状态轮询：订单不存在 → 403（与「不属于自己」同一个响应，不泄露存在性）")
    void statusOnMissingOrder() {
        loginAs(OWNER);
        when(payGateway.queryStatus("NOPE")).thenReturn(null);

        assertEquals(403, codeOf(controller.status("NOPE")));
    }

    @Test
    @DisplayName("mock 支付：他人订单 → 403 且绝不调用 mockPaySuccess（修复前可把他人订单刷成已支付）")
    void mockPayRejectsOtherUsersOrder() {
        loginAs(ATTACKER);
        when(payGateway.queryStatus("PAY-1")).thenReturn(orderOf(OWNER));

        Object result = controller.mockPay("PAY-1");

        assertEquals(403, codeOf(result));
        verify(payGateway, never()).mockPaySuccess(anyString());
    }

    @Test
    @DisplayName("mock 支付：本人订单 + mock 开启 → 正常触发")
    void mockPayAllowsOwnOrder() {
        loginAs(OWNER);
        when(payGateway.queryStatus("PAY-1")).thenReturn(orderOf(OWNER));
        when(payGateway.mockPaySuccess("PAY-1")).thenReturn(java.util.Map.of("status", "paid"));

        assertEquals(200, codeOf(controller.mockPay("PAY-1")));
        verify(payGateway).mockPaySuccess("PAY-1");
    }

    @Test
    @DisplayName("未登录（无门户登录态）→ 401，不触碰订单查询")
    void unauthenticatedRejected() {
        SecurityContextHolder.clearContext();

        assertEquals(401, codeOf(controller.status("PAY-1")));
        assertEquals(401, codeOf(controller.mockPay("PAY-1")));
        verify(payGateway, never()).queryStatus(anyString());
        verify(payGateway, never()).mockPaySuccess(anyString());
    }

    /** 从 AjaxResult 里取 code（AjaxResult 是 Map，用 get("code")） */
    private static int codeOf(Object ajaxResult) {
        Object code = ((java.util.Map<?, ?>) ajaxResult).get("code");
        assertNotNull(code, "响应应包含 code");
        return ((Number) code).intValue();
    }

    /** 占位断言：确保 LoginUser 工具类未被误用（后台/门户登录态必须分离） */
    @Test
    @DisplayName("后台登录态不构成门户登录（PortalSecurityUtils 与 SecurityUtils 相互独立）")
    void adminLoginDoesNotGrantPortalIdentity() {
        LoginUser admin = mock(LoginUser.class);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, List.of()));
        assertNull(com.moyun.portal.util.PortalSecurityUtils.getUserId(), "后台 principal 不应被识别为门户用户");
    }
}
