package com.moyun.util.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * LLM 输出 JSON 提取（全项目唯一实现）
 *
 * <h3>为什么要有唯一实现</h3>
 * <p>LLM 不会老老实实只吐 JSON：常见形态有 markdown 围栏（{@code ```json ... ```} / {@code ``` ... ```}）、
 * 前后夹说明文字（"好的，以下是结果：{...} 希望对你有帮助"）、直接给数组、以及与 JSON 混排的额外花括号。
 * 这段"抠 JSON"的逻辑一旦各写各的，就会出现：</p>
 * <ul>
 *   <li>有的实现只处理"文本以围栏开头"（围栏出现在第二行就失效）；</li>
 *   <li>有的只找 {@code {..}} 不支持数组，或漏了"数组在前、对象在后"的场景；</li>
 *   <li>有的返回 {@code null}、有的返回原文、有的抛异常——调用方各自再兜一层。</li>
 * </ul>
 * <p>全仓曾有 4 处同族实现（含 1 处从未被调用的死代码），本类是它们的<b>能力并集</b>：
 * 围栏（任意位置）+ 对象/数组（谁先出现取谁）+ <b>括号配平扫描</b>（跳过字符串字面量与转义，
 * 因此 {@code {"a":"}"} 这类内含花括号的字符串不会被切错）+ 括号未配平时的"首个左括号 → 最后一个右括号"兜底；
 * <b>完全不闭合（如被 max_tokens 截断）时返回空串</b>——半截 JSON 无法解析，交给调用方走降级分支更安全。</p>
 *
 * <h3>用法</h3>
 * <pre>{@code
 * JsonNode node = LlmJsonExtractor.extractNode(MAPPER, llmRawOutput);   // 失败返回 null，不抛异常
 * String body  = LlmJsonExtractor.extract(llmRawOutput);               // 只要原始 JSON 文本，找不到返回 ""
 * }</pre>
 *
 * @author moyun
 */
public final class LlmJsonExtractor {

    private LlmJsonExtractor() {
    }

    /**
     * 提取 LLM 输出中的 JSON 本体（对象或数组）
     *
     * @param raw 模型原始输出（可为 null）
     * @return JSON 文本；找不到任何 JSON 主体时返回空串（<b>永不返回 null</b>，调用方可直接判空）
     */
    public static String extract(String raw) {
        if (raw == null) {
            return "";
        }
        String text = stripFence(raw.trim());
        if (text.isEmpty()) {
            return "";
        }
        String balanced = firstBalancedJson(text);
        if (balanced != null) {
            return balanced;
        }
        // 兜底：模型把 JSON 截断/多吐了括号，退化到"首个左括号 → 最后一个右括号"
        return sliceByLastBracket(text);
    }

    /**
     * 提取并解析为 {@link JsonNode}
     *
     * @param mapper 调用方的 ObjectMapper（沿用各自配置）
     * @return 解析结果；提取不到或解析失败返回 null（<b>不抛异常</b>，调用方按"解析失败"处理即可）
     */
    public static JsonNode extractNode(ObjectMapper mapper, String raw) {
        String body = extract(raw);
        if (!body.isEmpty()) {
            try {
                JsonNode node = mapper.readTree(body);
                if (node != null && !node.isMissingNode()) {
                    return node;
                }
            } catch (Exception ignored) {
                // 落到整体解析兜底
            }
        }
        // 兜底：整体就是合法 JSON（含纯标量/裸数组等非抠取场景）
        try {
            String trimmed = raw == null ? "" : raw.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            JsonNode node = mapper.readTree(trimmed);
            return node == null || node.isMissingNode() ? null : node;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 仅剥离 markdown 代码围栏（保留围栏内原文，**不做 JSON 抠取**）
     *
     * <p>用于"只要去掉 {@code ```} 包裹、内容本身是自然语言"的场景（如模型返回的文案兜底）。
     * 与 {@link #extract(String)} 的区别：后者会继续切出 {@code {..}} / {@code [..]}，对散文会切错。</p>
     *
     * @param raw 原始文本（可为 null）
     * @return 去围栏后的文本（无围栏则原样，仅去首尾空白）
     */
    public static String stripCodeFence(String raw) {
        if (raw == null) {
            return "";
        }
        String text = raw.trim();
        if (!text.contains("```")) {
            return text;
        }
        return stripFence(text);
    }

    /**
     * 剥离 markdown 代码围栏（围栏可出现在任意位置；无换行、无闭合围栏也能处理）
     */
    private static String stripFence(String text) {
        int fence = text.indexOf("```");
        if (fence < 0) {
            return text;
        }
        int lineEnd = text.indexOf('\n', fence);
        String content = lineEnd > 0 ? text.substring(lineEnd + 1) : "";
        int close = content.indexOf("```");
        if (close >= 0) {
            content = content.substring(0, close);
        }
        String stripped = content.trim();
        // 围栏里没内容（如只有 ```json```）时回退原文，交给后续括号扫描
        return stripped.isEmpty() ? text : stripped;
    }

    /**
     * 括号配平扫描：返回首个完整 JSON 对象/数组文本；找不到完整结构返回 null
     *
     * <p>扫描时跳过字符串字面量（含 {@code \"} 转义），所以 {@code {"a":"}{"}} 这类内容不会破坏配平。</p>
     */
    private static String firstBalancedJson(String text) {
        int objStart = text.indexOf('{');
        int arrStart = text.indexOf('[');
        int begin;
        char open;
        char close;
        if (objStart >= 0 && (arrStart < 0 || objStart < arrStart)) {
            begin = objStart;
            open = '{';
            close = '}';
        } else if (arrStart >= 0) {
            begin = arrStart;
            open = '[';
            close = ']';
        } else {
            return null;
        }

        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = begin; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == open) {
                depth++;
            } else if (c == close) {
                depth--;
                if (depth == 0) {
                    return text.substring(begin, i + 1);
                }
            }
        }
        return null;
    }

    /** 兜底切分：首个左括号 → 最后一个右括号（对象优先，其次数组） */
    private static String sliceByLastBracket(String text) {
        int objStart = text.indexOf('{');
        int objEnd = text.lastIndexOf('}');
        if (objStart >= 0 && objEnd > objStart) {
            return text.substring(objStart, objEnd + 1);
        }
        int arrStart = text.indexOf('[');
        int arrEnd = text.lastIndexOf(']');
        if (arrStart >= 0 && arrEnd > arrStart) {
            return text.substring(arrStart, arrEnd + 1);
        }
        return "";
    }
}
