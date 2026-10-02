package com.moyun.ext.ai.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 出站 URL 守卫的单测（清单 P2：SSRF 防护固化）。
 *
 * <p>覆盖四类拦截：协议白名单、本机/元数据主机名、字面量内网 IP、域名解析到内网地址。</p>
 */
class OutboundUrlGuardTest {

    @Test
    @DisplayName("协议白名单：仅允许 http/https")
    void rejectsNonHttpSchemes() {
        assertThrows(IllegalArgumentException.class, () -> OutboundUrlGuard.assertAllowed("file:///etc/passwd"));
        assertThrows(IllegalArgumentException.class, () -> OutboundUrlGuard.assertAllowed("gopher://example.com/"));
        assertThrows(IllegalArgumentException.class, () -> OutboundUrlGuard.assertAllowed("ftp://example.com/x"));
        assertThrows(IllegalArgumentException.class, () -> OutboundUrlGuard.assertAllowed(null));
        assertThrows(IllegalArgumentException.class, () -> OutboundUrlGuard.assertAllowed("   "));
        assertThrows(IllegalArgumentException.class, () -> OutboundUrlGuard.assertAllowed("http://"));
    }

    @Test
    @DisplayName("本机与云元数据主机名一律拒绝")
    void rejectsLocalhostAndMetadataHosts() {
        assertThrows(IllegalArgumentException.class, () -> OutboundUrlGuard.assertAllowed("http://localhost/admin"));
        assertThrows(IllegalArgumentException.class, () -> OutboundUrlGuard.assertAllowed("http://127.0.0.1:8080/actuator"));
        assertThrows(IllegalArgumentException.class, () -> OutboundUrlGuard.assertAllowed("http://metadata.google.internal/computeMetadata/v1/"));
        assertThrows(IllegalArgumentException.class, () -> OutboundUrlGuard.assertAllowed("http://metadata/"));
        assertThrows(IllegalArgumentException.class, () -> OutboundUrlGuard.assertAllowed("http://db.internal/"));
    }

    @Test
    @DisplayName("云元数据与内网字面量 IP 拒绝（含 169.254.169.254）")
    void rejectsMetadataAndPrivateIps() {
        assertThrows(IllegalArgumentException.class, () -> OutboundUrlGuard.assertAllowed("http://169.254.169.254/latest/meta-data/"));
        assertThrows(IllegalArgumentException.class, () -> OutboundUrlGuard.assertAllowed("http://10.0.0.5/"));
        assertThrows(IllegalArgumentException.class, () -> OutboundUrlGuard.assertAllowed("http://172.16.1.1/"));
        assertThrows(IllegalArgumentException.class, () -> OutboundUrlGuard.assertAllowed("http://192.168.1.1/"));
        assertThrows(IllegalArgumentException.class, () -> OutboundUrlGuard.assertAllowed("http://[::1]/"));
        assertThrows(IllegalArgumentException.class, () -> OutboundUrlGuard.assertAllowed("http://100.64.0.1/"));   // 运营商级 NAT
        assertThrows(IllegalArgumentException.class, () -> OutboundUrlGuard.assertAllowed("http://240.0.0.1/"));    // 保留
    }

    @Test
    @DisplayName("公网地址放行（本测试不依赖外网可达，仅校验守卫判定）")
    void allowsPublicHosts() {
        // 使用公网字面量 IP，避免依赖 DNS 解析结果（CI 可能无外网）
        assertDoesNotThrow(() -> OutboundUrlGuard.assertAllowed("https://1.1.1.1/"));
        assertDoesNotThrow(() -> OutboundUrlGuard.assertAllowed("http://93.184.216.34/path?q=1"));
    }

    @Test
    @DisplayName("特征化：被拦截时的异常消息可直接回显给用户")
    void errorMessagesAreUserReadable() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> OutboundUrlGuard.assertAllowed("http://169.254.169.254/latest/meta-data/"));
        assertTrue(e.getMessage().contains("禁止访问"), "异常消息应说明原因，实际：" + e.getMessage());
    }
}
