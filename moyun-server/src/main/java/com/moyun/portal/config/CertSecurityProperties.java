package com.moyun.portal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * 认证信息安全配置（证件号加密口令等）
 *
 * <p>配置项：{@code moyun.security.cert-no-encrypt-key}</p>
 *
 * <p>注意：口令变更会导致已加密证件号无法解密（可继续用脱敏值展示），
 * 生产环境请通过环境变量注入并妥善保管，切勿提交到仓库。</p>
 *
 * @author moyun
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "moyun.security")
public class CertSecurityProperties {

    /** 证件号 AES-GCM 加密口令（任意字符串，内部 SHA-256 派生密钥） */
    private String certNoEncryptKey = "moyun-cert-default-key";
}
