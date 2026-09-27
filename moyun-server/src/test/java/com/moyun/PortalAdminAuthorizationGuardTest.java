package com.moyun;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * 结构守卫：{@code /portal/admin/**}（后台管理接口）必须"两层都关"
 *
 * <h3>报告 §6.4 原判断（附录 P 已订正）</h3>
 * <p>原文称"{@code PortalSecurityConfig:191} 的 {@code permitAll} + 门户过滤器跳过该前缀
 * → 保护完全依赖各 Controller 的 {@code @PreAuthorize}，新增接口漏注解即匿名后台"。
 * <b>实测不成立</b>：门户链的 {@code permitAll} 是**死规则**——两条链都匹配
 * {@code /portal/admin/**}，而 {@code FilterChainProxy} 里**核心链排在前面**（实测链顺序
 * 由注册顺序决定，门户配置类上标的 {@code @Order(1)} 不生效），所以该前缀由核心链处理：
 * 先由 {@code JwtAuthenticationTokenFilter} 解析 admin token，再经
 * {@code anyRequest().authenticated()} 拒绝匿名请求。</p>
 *
 * <p>但原判断指出的**风险是真的、只是层级不同**：链归属当时是"偶然"的（靠注册顺序），
 * 一旦顺序被改动，门户链的 {@code permitAll} 就会**静默变成匿名放行后台接口**。
 * 因此 v13.6 做了两件事，并由本守卫锁死：</p>
 * <ol>
 *   <li>两条链的 {@code @Bean} 方法显式声明 {@code @Order}（核心 1 / 门户 2）→ 链归属不再靠偶然；</li>
 *   <li>门户链的 {@code /portal/admin/**} 由 {@code permitAll} 改为 {@code authenticated()}
 *       → **无论哪条链生效都 fail-closed**（顺序被改只会"拒绝所有人"这种响亮的失败，
 *       不会变成"匿名可进"这种无声的漏洞）。</li>
 * </ol>
 *
 * <p>第 2 层是方法级权限：本守卫用运行期 {@code RequestMappingHandlerMapping} 枚举
 * {@code /portal/admin/**} 下的**真实端点**，逐个断言 {@code @PreAuthorize}
 * （方法级或所在类级）存在——新增后台接口漏注解会被直接拦下。</p>
 *
 * @author moyun
 * @see AsyncSelfInvocationGuardTest 同族守卫（结构约束靠测试而非靠人记）
 * @see ExecutorGovernanceGuardTest 同族守卫（执行器收口）
 */
@SpringBootTest
@AutoConfigureMockMvc
class PortalAdminAuthorizationGuardTest {

    private static final String ADMIN_PREFIX = "/portal/admin/";
    private static final String CORE_JWT_FILTER = "com.moyun.core.security.filter.JwtAuthenticationTokenFilter";
    private static final String PORTAL_JWT_FILTER =
            "com.moyun.portal.security.filter.PortalJwtAuthenticationTokenFilter";

    /** 后台接口的真实端点数（低于此值说明守卫没扫到东西，"零违规"是假绿） */
    private static final int MIN_EXPECTED_ADMIN_ENDPOINTS = 40;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("链归属：/portal/admin/** 必须命中核心链（admin token），而不是门户链")
    void adminPrefixIsOwnedByCoreChain() {
        List<String> filters = filtersOfWinningChain(ADMIN_PREFIX + "categories/list");
        assertTrue(filters.contains(CORE_JWT_FILTER),
                "处理 /portal/admin/** 的链里没有核心 JwtAuthenticationTokenFilter（admin token 不会被解析）：" + filters);
        assertFalse(filters.contains(PORTAL_JWT_FILTER),
                "处理 /portal/admin/** 的链是门户链——门户过滤器会跳过该前缀，导致无认证上下文（权限注解全部 403）"
                        + "且门户链规则一旦是 permitAll 即匿名可进后台：" + filters);
    }

    @Test
    @DisplayName("未被误伤：/portal/** 普通路径仍由门户链处理")
    void portalPathsStillOwnedByPortalChain() {
        List<String> filters = filtersOfWinningChain("/portal/books");
        assertTrue(filters.contains(PORTAL_JWT_FILTER),
                "/portal/books 落在了非门户链上，门户鉴权被绕过：" + filters);
    }

    @Test
    @DisplayName("匿名访问后台接口必须被拒绝（链级 fail-closed，不依赖 @PreAuthorize）")
    void anonymousRequestToAdminEndpointIsRejected() throws Exception {
        MvcResult result = mockMvc.perform(get(ADMIN_PREFIX + "categories/list")).andReturn();
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(body.contains("401") || body.contains("认证失败"),
                "匿名访问 /portal/admin/categories/list 未被拒绝，响应体=" + body);
    }

    @Test
    @DisplayName("结构守卫：/portal/admin/** 下每个端点都必须有 @PreAuthorize（方法级或类级）")
    void everyAdminEndpointHasPreAuthorize() {
        List<String> violations = new ArrayList<>();
        handlerMapping.getHandlerMethods().forEach((info, handlerMethod) -> {
            for (String pattern : info.getPatternValues()) {
                if (!pattern.startsWith(ADMIN_PREFIX)) {
                    continue;
                }
                boolean annotated =
                        AnnotatedElementUtils.hasAnnotation(handlerMethod.getMethod(), PreAuthorize.class)
                                || AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), PreAuthorize.class);
                if (!annotated) {
                    violations.add(pattern + " -> " + handlerMethod.getShortLogMessage());
                }
            }
        });
        assertTrue(violations.isEmpty(),
                "后台接口缺少 @PreAuthorize（链级只保证'已登录的 admin'，不区分权限；"
                        + "漏注解即任意 admin——含低权限账号——可调用）：\n  "
                        + String.join("\n  ", violations));
    }

    @Test
    @DisplayName("守卫自检：确实扫到了后台端点（防止'零违规'其实是零扫描）")
    void guardActuallyScansAdminEndpoints() {
        long count = handlerMapping.getHandlerMethods().keySet().stream()
                .flatMap(info -> info.getPatternValues().stream())
                .filter(pattern -> pattern.startsWith(ADMIN_PREFIX))
                .count();
        assertTrue(count >= MIN_EXPECTED_ADMIN_ENDPOINTS,
                "扫描到的 /portal/admin/** 端点数=" + count + "，低于预期下限 "
                        + MIN_EXPECTED_ADMIN_ENDPOINTS + "——守卫装置可能已失效");
    }

    // ==================== 内部 ====================

    /** 按 FilterChainProxy 的链顺序取第一个匹配该 URI 的链，返回其过滤器全限定类名 */
    private List<String> filtersOfWinningChain(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        for (SecurityFilterChain chain : springSecurityFilterChain.getFilterChains()) {
            if (chain.matches(request)) {
                return chain.getFilters().stream().map(f -> f.getClass().getName()).toList();
            }
        }
        assertNotNull(null, "没有任何安全链匹配 " + uri);
        return List.of();
    }
}
