package com.moyun.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class TokenConfigValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TokenConfigValidator.class);

    @Value("${token.secret:}")
    private String tokenSecret;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Override
    public void run(ApplicationArguments args) {
        if (tokenSecret == null || tokenSecret.trim().isEmpty()) {
            String message = "JWT Token secret is not configured! ";
            if ("prod".equalsIgnoreCase(activeProfile) || "production".equalsIgnoreCase(activeProfile)) {
                throw new IllegalStateException(
                    message +
                    "Please set the TOKEN_SECRET environment variable or configure it in application.yaml. " +
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
        } else if (tokenSecret.length() < 64) {
            String message = "JWT Token secret is too short! " +
                "It should be at least 64 characters long for HS512 algorithm. " +
                "Current length: " + tokenSecret.length();
            if ("prod".equalsIgnoreCase(activeProfile) || "production".equalsIgnoreCase(activeProfile)) {
                throw new IllegalStateException(message);
            } else {
                log.warn(message);
            }
        }
    }
}
