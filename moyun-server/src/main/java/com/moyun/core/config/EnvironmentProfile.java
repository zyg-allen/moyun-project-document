package com.moyun.core.config;

import java.util.Locale;
import java.util.Set;

import org.springframework.core.env.Environment;

/**
 * 运行环境判定（全项目唯一事实来源）。
 *
 * <p><b>为什么需要它</b>：生产安全守卫（"生产禁用 mock 支付/代付"、密钥强度校验等）此前各自
 * 用 {@code "prod".equalsIgnoreCase(profile)} 判断。该写法只认 {@code prod}/{@code production}
 * 两个**字面量**，一旦部署时把 profile 写成 {@code prd}、{@code gray}、{@code k8s-prod} 等，
 * 守卫会**静默降级为"非生产"**，从而放开 mock 通道 —— 这是真实缺陷（曾导致"提现扣款却不出金"
 * 可被配置疏漏触发）。</p>
 *
 * <p><b>口径：非生产白名单 + 默认按生产处理（fail-closed）</b>。
 * 只有命中白名单的 profile 才视为非生产；未知、为空、未激活一律按**生产**对待。
 * 这样"配置写错"的后果是"守卫生效"（可用性受影响但资金安全），而不是"守卫失效"（资金受损）。</p>
 *
 * @author moyun
 */
public final class EnvironmentProfile {

    /** 明确属于"非生产"的 profile 名称（小写比较）。其余一律按生产对待。 */
    private static final Set<String> NON_PRODUCTION = Set.of(
            "dev", "develop", "development",
            "local", "localhost",
            "test", "testing", "unit", "unittest",
            "mock", "demo");

    private EnvironmentProfile() {
    }

    /**
     * 是否按生产环境对待。
     *
     * @param env Spring 环境（可为 null —— 此时保守返回 true）
     * @return true 表示按生产处理；无激活 profile 时同样返回 true（fail-closed）
     */
    public static boolean isProduction(Environment env) {
        if (env == null) {
            return true;
        }
        String[] active = env.getActiveProfiles();
        if (active == null || active.length == 0) {
            return true;
        }
        for (String profile : active) {
            if (profile == null || profile.isBlank()) {
                continue;
            }
            if (NON_PRODUCTION.contains(profile.trim().toLowerCase(Locale.ROOT))) {
                return false;
            }
        }
        return true;
    }

    /** 非生产的便捷判断（仅用于日志/提示语；安全守卫请一律使用 {@link #isProduction(Environment)}）。 */
    public static boolean isNonProduction(Environment env) {
        return !isProduction(env);
    }
}
