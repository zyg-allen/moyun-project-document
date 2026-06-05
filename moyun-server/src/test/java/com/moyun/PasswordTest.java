package com.moyun;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordTest {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // 生成 123456 的加密密码
        String rawPassword = "123456";

        // 生成新的哈希
        String newHash = encoder.encode(rawPassword);
        System.out.println("原始密码: " + rawPassword);
        System.out.println("BCrypt加密密码: " + newHash);

        // 验证新哈希
        boolean matches = encoder.matches(rawPassword, newHash);
        System.out.println("验证结果: " + matches);
    }
}
