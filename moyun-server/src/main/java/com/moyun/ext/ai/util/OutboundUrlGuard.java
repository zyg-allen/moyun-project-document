package com.moyun.ext.ai.util;

import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.URI;
import java.util.Locale;

/**
 * 出站 URL 安全守卫（清单 P2）。
 *
 * <p>背景：AI 工作流的 HTTP / Webhook 节点允许用户配置任意 URL，而实现里直接用
 * {@code new RestTemplate().exchange(url, ...)} 抓取 —— 既无协议限制，也无内网与云元数据地址拦截，
 * 存在典型 <b>SSRF</b> 风险（可探测内网、读取 {@code 169.254.169.254} 元数据、绕过边界访问本机服务）。</p>
 *
 * <p>本工具在真正发请求前做四道校验：</p>
 * <ol>
 *   <li>协议只允许 {@code http} / {@code https}；</li>
 *   <li>主机名不得是本机别名或云元数据域名（localhost / *.internal / metadata 等）；</li>
 *   <li>主机若是字面量 IP，必须不是内网/保留地址；</li>
 *   <li>主机若是域名，<b>解析后</b>逐个检查实际 IP（同一条 DNS 记录里只要有内网地址即拒绝）。</li>
 * </ol>
 *
 * <p>说明：DNS 解析与真正连接之间存在 TOCTOU 窗口，彻底防护需自定义 DNS/连接工厂；
 * 调用方同时应关闭重定向跟随（见各执行器），避免"公网 URL 302 到内网"绕过本校验。</p>
 *
 * @author moyun
 */
@Slf4j
public final class OutboundUrlGuard {

    private OutboundUrlGuard() {
    }

    /** 被禁用的主机名（小写比较；后缀匹配用 endsWith 单独处理） */
    private static final String[] BLOCKED_HOSTS = {
            "localhost", "ip6-localhost", "ip6-loopback", "metadata", "metadata.google.internal"
    };

    /**
     * 校验出站 URL 是否允许访问。
     *
     * @param rawUrl 待校验 URL（可含变量替换后的查询串）
     * @throws IllegalArgumentException 校验不通过时抛出，消息可直接回显给用户
     */
    public static void assertAllowed(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("URL 为空");
        }
        URI uri;
        try {
            uri = URI.create(rawUrl.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("URL 格式不合法");
        }
        String scheme = uri.getScheme();
        if (scheme == null || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
            throw new IllegalArgumentException("仅支持 http/https 协议");
        }
        String host = uri.getHost();
        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("URL 缺少主机名");
        }
        String lowerHost = host.toLowerCase(Locale.ROOT);

        for (String blocked : BLOCKED_HOSTS) {
            if (lowerHost.equals(blocked)) {
                throw new IllegalArgumentException("禁止访问本机或元数据地址：" + host);
            }
        }
        if (lowerHost.endsWith(".internal") || lowerHost.endsWith(".local") || lowerHost.endsWith(".localhost")) {
            throw new IllegalArgumentException("禁止访问内网域名：" + host);
        }

        // 字面量 IP：直接判定
        if (isIpLiteral(lowerHost)) {
            if (isBlockedAddress(lowerHost)) {
                throw new IllegalArgumentException("禁止访问内网/保留地址：" + host);
            }
            return;
        }

        // 域名：解析后逐个检查（防止域名指向内网）
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            if (addresses.length == 0) {
                throw new IllegalArgumentException("域名无法解析：" + host);
            }
            for (InetAddress addr : addresses) {
                if (isBlockedAddress(addr)) {
                    log.warn("[出站URL守卫] 域名 {} 解析到受限地址 {}，已拒绝", host, addr.getHostAddress());
                    throw new IllegalArgumentException("禁止访问内网/保留地址：" + host);
                }
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("域名解析失败：" + host);
        }
    }

    /** 是否形如字面量 IP（含 IPv6 的冒号形式） */
    private static boolean isIpLiteral(String host) {
        return host.contains(":") || host.matches("^\\d{1,3}(\\.\\d{1,3}){3}$");
    }

    private static boolean isBlockedAddress(String ip) {
        try {
            return isBlockedAddress(InetAddress.getByName(ip));
        } catch (Exception e) {
            // 解析不了的字面量：保守拒绝
            return true;
        }
    }

    /** 内网 / 保留 / 链路本地 / 组播 / 回环 / 未指定地址判定 */
    private static boolean isBlockedAddress(InetAddress addr) {
        if (addr.isAnyLocalAddress() || addr.isLoopbackAddress() || addr.isLinkLocalAddress()
                || addr.isSiteLocalAddress() || addr.isMulticastAddress()) {
            return true;
        }
        byte[] b = addr.getAddress();
        if (b.length == 4) {
            int first = b[0] & 0xFF;
            int second = b[1] & 0xFF;
            // 100.64.0.0/10（运营商级 NAT）、192.0.0.0/24、198.18.0.0/15、240.0.0.0/4（保留）
            if (first == 100 && second >= 64 && second <= 127) {
                return true;
            }
            if (first == 192 && second == 0) {
                return true;
            }
            if (first == 198 && (second == 18 || second == 19)) {
                return true;
            }
            if (first >= 240) {
                return true;
            }
        }
        if (b.length == 16) {
            // IPv6 唯一本地地址 fc00::/7
            int first = b[0] & 0xFF;
            if ((first & 0xFE) == 0xFC) {
                return true;
            }
        }
        return false;
    }
}
