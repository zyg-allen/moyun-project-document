package com.moyun.portal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * 认证信息安全配置（证件号加密口令等）
 *
 * <p>配置项：{@code moyun.security.cert-no-encrypt-key}</p>
 *
 * <p><b>安全策略：不提供默认口令</b>。历史实现默认值为常量
 * {@code "moyun-cert-default-key"}，会在漏配时静默用公开口令加密证件号（等于未加密），
 * 且运维无法从任何日志察觉。现改为空默认，由
 * {@code ConfigWiringValidator} 在启动期统一断言（prod 下缺失或仍为开发默认口令即阻断启动）。</p>
 *
 * <p>注意：口令变更会导致已加密证件号无法解密（可继续用脱敏值展示），
 * 生产环境请通过环境变量 {@code MOYUN_SECURITY_CERT_NO_ENCRYPT_KEY} 注入并妥善保管，切勿提交到仓库。</p>
 *
 * @author moyun
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "moyun.security")
public class CertSecurityProperties {

    /** 证件号 AES-GCM 加密口令（任意字符串，内部 SHA-256 派生密钥）；空值由 ConfigWiringValidator 拦截 */
    private String certNoEncryptKey = "";
}
