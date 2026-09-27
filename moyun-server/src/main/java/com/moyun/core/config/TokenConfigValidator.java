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

        // 密钥分离检查。
        // 背景（已接线）：TokenService 读 token.admin.secret、PortalTokenService 读 token.portal.secret，
        // 两者均回退 token.secret。因此"密钥相同"等价于"未做分离"——此时任一端的合法令牌
        // 都可用同一密钥伪造另一端的签名令牌（实际拦截依赖 Redis key 前缀 login_tokens: /
        // portal_login_tokens: 这一实现细节，属偶然约定，不应作为安全边界）。
        // 故：生产环境把"未分离"视为配置缺陷并阻断启动；非生产仅告警，保留本地零配置体验。
        if (isNotEmpty(adminSecret) && adminSecret.equals(portalSecret)) {
            String message = "Admin and Portal JWT secrets are identical "
                    + "(token.admin.secret == token.portal.secret)! Admin/portal token isolation "
                    + "currently relies only on the Redis key prefix, not on key separation.";
            if (isProd()) {
                throw new IllegalStateException(message
                        + " Please set separate strong secrets via TOKEN_ADMIN_SECRET and TOKEN_PORTAL_SECRET.");
            }
            log.warn("{} Please set separate strong secrets via TOKEN_ADMIN_SECRET and TOKEN_PORTAL_SECRET "
                    + "(non-prod: warning only; production will fail fast).", message);
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
