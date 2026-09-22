package com.moyun.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TokenConfigValidator implements ApplicationRunner {

    @Value("${token.secret:}")
    private String tokenSecret;

    @Value("${token.admin.secret:${token.secret:}}")
    private String adminSecret;

    @Value("${token.portal.secret:${token.secret:}}")
    private String portalSecret;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Override
    public void run(ApplicationArguments args) {
        validateSecret(tokenSecret, "token.secret");
        validateSecret(adminSecret, "token.admin.secret");
        validateSecret(portalSecret, "token.portal.secret");

        // 密钥分离检查：管理端与门户端密钥相同时（即未分别配置专用密钥，回退共用 token.secret），
        // 存在门户 Token 伪造管理端请求的提权风险，log.warn 提示尽快分离
        if (isNotEmpty(adminSecret) && adminSecret.equals(portalSecret)) {
            log.warn("Admin and Portal JWT secrets are identical (token.admin.secret == token.portal.secret)! "
                + "A portal token could be forged as an admin request. "
                + "Please set separate strong secrets via TOKEN_ADMIN_SECRET and TOKEN_PORTAL_SECRET environment variables.");
        }
    }

    private void validateSecret(String secret, String configKey) {
        if (secret == null || secret.trim().isEmpty()) {
            String message = "JWT Token secret is not configured! (" + configKey + ") ";
            if (isProd()) {
                throw new IllegalStateException(
                    message +
                    "Please set the corresponding TOKEN_*_SECRET environment variable or configure it in application.yaml. " +
                    "The secret must be at least 64 characters long for HS512 algorithm."
                );
            } else {
                log.warn(
                    message +
                    "Using development-only fallback secret. " +
                    "This is NOT safe for production! " +
                    "Please set TOKEN_SECRET environment variable in production."
                );
            }
        } else if (secret.length() < 64) {
            String message = "JWT Token secret is too short! (" + configKey + ") " +
                "It should be at least 64 characters long for HS512 algorithm. " +
                "Current length: " + secret.length();
            if (isProd()) {
                throw new IllegalStateException(message);
            } else {
                log.warn(message);
            }
        }
    }

    private boolean isProd() {
        return "prod".equalsIgnoreCase(activeProfile) || "production".equalsIgnoreCase(activeProfile);
    }

    private boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
