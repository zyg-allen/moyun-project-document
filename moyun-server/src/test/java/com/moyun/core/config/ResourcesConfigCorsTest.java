package com.moyun.core.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CORS 源白名单收口（v13.17，报告 §6.4）
 *
 * <h3>修复前</h3>
 * <p>未配置 {@code CORS_ALLOWED_ORIGINS} 时，代码用 {@code addAllowedOriginPattern} 无条件放行
 * {@code http://192.168.*}、{@code http://10.*} 等内网 pattern，并配 {@code allowCredentials(true)}。
 * 但 Spring 的 origin pattern 里 {@code *} 是**通配符**，{@code http://192.168.*} 会匹配
 * <b>{@code http://192.168.evil.com}</b>（可注册域名）——等于把"带 Cookie 的跨域"开放给攻击者域名；
 * 而且内网放行没有"仅开发环境"约束，生产漏配即生效。</p>
 *
 * <h3>修复后</h3>
 * <ul>
 *   <li>基础配置只用 {@code addAllowedOrigin}（**精确匹配**）：环境变量优先；非生产未配置时仅本机回环；
 *       生产未配置时 **fail-closed 不放行任何跨域源**；</li>
 *   <li>局域网设备改为在 {@code getCorsConfiguration} 里按当前 Origin **精确放行一次**，且要求 host 是
 *       **私网 IP 字面量**（172.16/12、192.168/16、10/8 或回环）——任何域名一律拒绝；</li>
 *   <li>该便利还需显式开关 {@code CORS_ALLOW_LAN_DEV_ORIGINS=true} 且非生产 profile。</li>
 * </ul>
 *
 * @author moyun
 */
class ResourcesConfigCorsTest {

    @Test
    @DisplayName("生产未配置环境变量 → fail-closed：不放行任何跨域源（且仍禁止 '*'）")
    void prodWithoutEnvAllowsNothing() {
        CorsConfiguration config = ResourcesConfig.buildCorsConfiguration(null, true);

        assertTrue(config.getAllowedOrigins() == null || config.getAllowedOrigins().isEmpty(),
                "生产未配置 CORS_ALLOWED_ORIGINS 时必须不放行任何源");
        assertNull(config.checkOrigin("https://portal.example.com"), "任意外部源都不得被放行");
        assertFalse(Boolean.TRUE.equals(config.getAllowCredentials()) && "*".equals(config.getAllowedOrigins()),
                "allowCredentials=true 时绝不能配 '*'");
    }

    @Test
    @DisplayName("非生产未配置环境变量 → 仅本机回环（含端口），不再无条件放行内网 pattern")
    void devWithoutEnvAllowsOnlyLoopback() {
        CorsConfiguration config = ResourcesConfig.buildCorsConfiguration(null, false);

        assertEquals("http://localhost", config.checkOrigin("http://localhost"));
        assertEquals("http://localhost:5173", config.checkOrigin("http://localhost:5173"),
                "vite dev server 的带端口回环必须放行（回环 pattern 的 host 段固定，不可伪造）");
        assertEquals("http://127.0.0.1:8080", config.checkOrigin("http://127.0.0.1:8080"));
        // 关键回归：修复前 http://192.168.* pattern 会放行下面这个可注册域名
        assertNull(config.checkOrigin("http://192.168.evil.com"),
                "内网 pattern 曾匹配 http://192.168.evil.com（带凭据跨域泄漏）——不得再出现");
        assertNull(config.checkOrigin("http://localhost.evil.com"),
                "回环 pattern 不得被相似域名绕过");
        assertNull(config.checkOrigin("http://192.168.1.5:5173"),
                "局域网源不再由基础配置无条件放行（需显式开关 + 精确放行）");
    }

    @Test
    @DisplayName("配置了环境变量 → 精确放行（不是 pattern，不做通配）")
    void envOriginsAreExact() {
        CorsConfiguration config = ResourcesConfig.buildCorsConfiguration(
                "https://portal.moyun.com, https://admin.moyun.com", true);

        assertEquals("https://portal.moyun.com", config.checkOrigin("https://portal.moyun.com"));
        assertEquals("https://admin.moyun.com", config.checkOrigin("https://admin.moyun.com"));
        assertNull(config.checkOrigin("https://evil.moyun.com"), "精确匹配不得被相似域名绕过");
        assertNull(config.checkOrigin("https://portal.moyun.com.evil.com"), "后缀拼接不得绕过");
    }

    @Test
    @DisplayName("局域网精确放行判定：只认私网 IP 字面量/回环，域名与公网 IP 一律拒绝")
    void privateNetworkOriginJudgement() {
        assertTrue(ResourcesConfig.isPrivateNetworkOrigin("http://192.168.1.5:5173"));
        assertTrue(ResourcesConfig.isPrivateNetworkOrigin("https://10.0.0.8"));
        assertTrue(ResourcesConfig.isPrivateNetworkOrigin("http://172.16.3.4:8080"));
        assertTrue(ResourcesConfig.isPrivateNetworkOrigin("http://localhost:5173"));
        assertTrue(ResourcesConfig.isPrivateNetworkOrigin("http://127.0.0.1:5173"));

        assertFalse(ResourcesConfig.isPrivateNetworkOrigin("http://192.168.evil.com"),
                "可注册域名（即使前缀像内网）必须拒绝");
        assertFalse(ResourcesConfig.isPrivateNetworkOrigin("http://172.15.0.1"), "172.15 不在 172.16/12 内");
        assertFalse(ResourcesConfig.isPrivateNetworkOrigin("http://172.32.0.1"), "172.32 不在 172.16/12 内");
        assertFalse(ResourcesConfig.isPrivateNetworkOrigin("http://8.8.8.8"), "公网 IP 必须拒绝");
        assertFalse(ResourcesConfig.isPrivateNetworkOrigin("http://999.168.1.1"), "非法 IP 段必须拒绝");
        assertFalse(ResourcesConfig.isPrivateNetworkOrigin("not-a-url"));
        assertFalse(ResourcesConfig.isPrivateNetworkOrigin(null));
    }
}
