package com.moyun.core.security.principal;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录主体解析器（模块中立，v13.11）
 *
 * <p>从 Spring Security 上下文取主体，仅当其实现了 {@link PrincipalProvider} 时返回信息 ——
 * 因此 {@code core} 无需认识任何一个业务模块的主体类型。</p>
 *
 * @author moyun
 * @since 2026-09-27
 */
public final class PrincipalResolver {

    private PrincipalResolver() {
    }

    /**
     * 解析当前主体。
     *
     * @return 主体视图；未登录 / 匿名 / 非本体系主体时返回 {@code null}
     */
    public static PrincipalInfo resolve() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null) {
                return null;
            }
            Object principal = authentication.getPrincipal();
            if (principal instanceof PrincipalProvider provider) {
                return provider.toPrincipalInfo();
            }
        } catch (Exception ignored) {
            // 横切路径（日志/限流）不因主体解析失败而中断业务
        }
        return null;
    }
}
