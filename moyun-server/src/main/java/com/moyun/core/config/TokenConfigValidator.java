package com.moyun.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TokenConfigValidator implements ApplicationRunner {

    @Value("${token.secret:}")
    private String tokenSecret;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Override
    public void run(ApplicationArguments args) {
        // 判断是否为本地开发环境（仅 local profile 允许使用默认密钥）
        boolean isLocal = "local".equalsIgnoreCase(activeProfile);

        if (tokenSecret == null || tokenSecret.trim().isEmpty()) {
            if (isLocal) {
                log.warn("JWT Token secret 未配置，local 环境将使用内置开发密钥。请勿用于生产！");
            } else {
                throw new IllegalStateException(
                    "JWT Token secret is not configured! " +
                    "Please set the TOKEN_SECRET environment variable. " +
                    "The secret must be at least 64 characters long for HS512 algorithm. " +
                    "(当前 profile: " + activeProfile + ")"
                );
            }
        } else if (tokenSecret.length() < 64) {
            if (isLocal) {
                log.warn("JWT Token secret 长度不足 64 字符（当前 {}），local 环境继续启动。", tokenSecret.length());
            } else {
                throw new IllegalStateException(
                    "JWT Token secret is too short! " +
                    "It should be at least 64 characters long for HS512 algorithm. " +
                    "Current length: " + tokenSecret.length() +
                    "(当前 profile: " + activeProfile + ")"
                );
            }
        }
    }
}
