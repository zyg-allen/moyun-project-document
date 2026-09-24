package com.moyun.ext.aigateway.support;

/**
 * Token 预算器（AI统一网关整改 阶段三 3.3 输入截断）
 *
 * <p>提示词渲染后的输入 token 估算与超限截断，配置驱动
 * （ai_scene_config.max_input_tokens / truncate_strategy），网关侧统一实施。
 * 估算口径：CJK 字符按 1 token/字，非 CJK 按 4 字符/token——无分词器的
 * 轻量启发式，刻意保守（宁可高估早截，不低估漏截）。</p>
 *
 * @author moyun
 * @since 2026-09-24
 */
public final class TokenBudgeter {

    /** 非 CJK 字符的 token 折算比（4 字符 ≈ 1 token） */
    private static final int NON_CJK_CHARS_PER_TOKEN = 4;

    /** 截断标记（模型可见，提示中段/尾部内容被省略） */
    private static final String TRUNCATION_MARK = "\n……[内容超长，已按场景配置截断]……\n";

    private TokenBudgeter() {
    }

    /**
     * 估算文本 token 数（CJK 1:1，非 CJK 4:1；null/空返回 0）
     */
    public static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int cjk = 0;
        int nonCjk = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (isCjk(c)) {
                cjk++;
            } else {
                nonCjk++;
            }
        }
        return cjk + (nonCjk + NON_CJK_CHARS_PER_TOKEN - 1) / NON_CJK_CHARS_PER_TOKEN;
    }

    /**
     * 按上限截断文本（估算不超限原样返回）。
     *
     * @param text    提示词渲染后的完整文本
     * @param maxTokens 输入 token 上限（null/&lt;=0 不截断）
     * @param strategy 截断策略：head=保头部 / tail=保尾部 / head_tail(默认)=保两端去中间
     */
    public static String truncate(String text, Integer maxTokens, String strategy) {
        if (text == null || maxTokens == null || maxTokens <= 0) {
            return text;
        }
        int budget = maxTokens - estimateTokens(TRUNCATION_MARK);
        if (estimateTokens(text) <= maxTokens || budget <= 0) {
            return text;
        }
        // 按 token 预算换算字符预算（估算口径下 CJK:非CJK=1:4，用保守的 1:1 折算字符数，
        // 保证任何构成都不会超限——英文文本会提前截断，可接受：截断本就是防护动作）
        return switch (strategy == null ? "head_tail" : strategy) {
            case "head" -> text.substring(0, charBudget(text, budget)) + TRUNCATION_MARK;
            case "tail" -> TRUNCATION_MARK + text.substring(text.length() - charBudget(text, budget));
            default -> {
                int half = charBudget(text, budget) / 2;
                yield text.substring(0, half) + TRUNCATION_MARK
                        + text.substring(text.length() - half);
            }
        };
    }

    /** 在 token 预算内取可保留的字符数（按文本实际构成折算，保守取小） */
    private static int charBudget(String text, int tokenBudget) {
        int cjk = 0;
        int nonCjk = 0;
        for (int i = 0; i < text.length(); i++) {
            if (isCjk(text.charAt(i))) {
                cjk++;
            } else {
                nonCjk++;
            }
        }
        // 全 CJK：1 字符/token；混合/全英文：按实际密度折算（c + n/4 = budget → 字符数）
        int byCjk = tokenBudget;
        double density = text.isEmpty() ? 1.0 : (double) (cjk + nonCjk) / Math.max(1, estimateTokens(text));
        int byDensity = (int) (tokenBudget * density);
        return Math.max(1, Math.min(Math.min(byCjk, byDensity), text.length()));
    }

    private static boolean isCjk(char c) {
        return c >= 0x4E00 && c <= 0x9FFF      // CJK 统一表意
                || c >= 0x3400 && c <= 0x4DBF  // 扩展A
                || c >= 0xF900 && c <= 0xFAFF  // 兼容表意
                || c >= 0x3000 && c <= 0x303F  // CJK 标点
                || c >= 0xFF00 && c <= 0xFFEF; // 全角字符
    }
}
