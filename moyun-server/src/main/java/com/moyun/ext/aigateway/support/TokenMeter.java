package com.moyun.ext.aigateway.support;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.Tokenizer;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import dev.langchain4j.model.output.TokenUsage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 网关侧 Token 计量：**真实 usage 优先，缺失则本地分词估算并显式标记**
 *
 * <h3>为什么必须有"估算兜底"</h3>
 * <p>非流式调用服务端会回传 usage；**流式调用不会** —— langchain4j {@code 1.0.0-beta3} 的
 * {@code OpenAiStreamingChatModel} 既不下发 {@code stream_options: {"include_usage": true}}，
 * builder 也没有该选项（已用 {@code javap} 查看 builder 方法 + 检索该 jar 全部 class 常量池确认
 * 无 {@code stream_options}/{@code include_usage} 字样）。因此 OpenAI 兼容端点默认不回 usage，
 * 流式回调里的 {@code ChatResponse.tokenUsage()} 恒为 {@code null}。</p>
 *
 * <p>原实现只在 {@code tokenUsage != null} 时才 {@code tokenCostGuard.consume(...)}，
 * 于是<b>流式请求的 Token 全部漏计</b>：场景日配额（成本熔断）被绕过，
 * 执行日志与成本报表里流式场景恒为 0（会话流式是语音面试主干，正是高消耗场景）。</p>
 *
 * <h3>本类规则</h3>
 * <ol>
 *   <li>服务端 usage 可用（任一字段非空）→ 用真实值，{@code estimated=false}；</li>
 *   <li>不可用 → 用 {@link OpenAiTokenizer}（jtokkit，已是 langchain4j-open-ai 的编译期依赖）
 *       对**提示词消息 + 完整输出**本地分词估算，{@code estimated=true}；</li>
 *   <li>分词器不可用的极端情况 → 退化为"CJK 1 字/token、其余 4 字符/token"的粗估并告警；</li>
 *   <li>估算值**必须**带标记（落库 {@code ai_execute_log.token_estimated} + 响应 metadata
 *       + 日志提示），绝不与真实值混为一谈。</li>
 * </ol>
 *
 * @author moyun
 * @since 2026-09-27
 */
@Slf4j
@Component
public class TokenMeter {

    /**
     * jtokkit 分词器；构造失败时为 null（降级为按字符粗估）。
     *
     * <p><b>为什么用 GPT-4 的编码做代理</b>：langchain4j 的 {@code OpenAiTokenizer} 在 beta3 已无无参构造，
     * 只能按模型名解析编码表；而本项目实际模型是通义千问等 OpenAI 兼容端点，没有对应枚举。
     * 因此统一采用 {@code cl100k_base}（GPT-4 系列编码）作为**代理分词器**——
     * 现代 BPE 编码对中英文的切分粒度接近，用于配额与成本量级判断足够；数值本身始终标 {@code estimated}。</p>
     */
    private final Tokenizer tokenizer = initTokenizer();

    /**
     * 计量一次调用（优先真实 usage）
     *
     * @param usage      服务端回传的 usage，可为 {@code null}
     * @param messages   送入模型的消息列表（估算输入用），可为 {@code null}
     * @param completion 模型完整输出文本（估算输出用），可为 {@code null}
     */
    public Metered meter(TokenUsage usage, List<ChatMessage> messages, String completion) {
        if (usage != null) {
            Integer in = usage.inputTokenCount();
            Integer out = usage.outputTokenCount();
            Integer total = usage.totalTokenCount();
            if (in != null || out != null || total != null) {
                int i = in != null ? in : 0;
                int o = out != null ? out : 0;
                return new Metered(i, o, total != null ? total : i + o, false);
            }
        }
        int in = estimateMessages(messages);
        int out = estimateText(completion);
        return new Metered(in, out, in + out, true);
    }

    /**
     * 仅按文本估算（同步路径的兜底：网关只拿得到请求 input 与响应摘要，
     * 拿不到 Handler 内部渲染后的完整提示词 —— 属**偏低估**的保守估算）。
     */
    public Metered meterTexts(String inputText, String outputText) {
        int in = estimateText(inputText);
        int out = estimateText(outputText);
        return new Metered(in, out, in + out, true);
    }

    // ==================== 内部实现 ====================

    private int estimateMessages(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }
        if (tokenizer != null) {
            try {
                return tokenizer.estimateTokenCountInMessages(messages);
            } catch (Exception e) {
                log.debug("[aigateway:token] estimateTokenCountInMessages 失败，退化按文本估算: {}", e.getMessage());
            }
        }
        StringBuilder sb = new StringBuilder();
        for (ChatMessage m : messages) {
            if (m != null) {
                sb.append(m.toString()).append('\n');
            }
        }
        return estimateText(sb.toString());
    }

    private int estimateText(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        if (tokenizer != null) {
            try {
                return tokenizer.estimateTokenCountInText(text);
            } catch (Exception e) {
                log.debug("[aigateway:token] estimateTokenCountInText 失败，退化按字符粗估: {}", e.getMessage());
            }
        }
        return roughEstimate(text);
    }

    /** 粗估：CJK 约 1 字/token，其余约 4 字符/token */
    private int roughEstimate(String text) {
        int cjk = 0;
        int other = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 0x4E00 && c <= 0x9FFF) {
                cjk++;
            } else {
                other++;
            }
        }
        return (int) Math.ceil(cjk + other / 4.0);
    }

    private static Tokenizer initTokenizer() {
        try {
            return new OpenAiTokenizer(OpenAiChatModelName.GPT_4_O);
        } catch (Throwable t) {
            log.warn("[aigateway:token] 本地分词器不可用，Token 估算降级为按字符粗估: {}", t.getMessage());
            return null;
        }
    }

    /**
     * 计量结果
     *
     * @param inputTokens  输入 Token
     * @param outputTokens 输出 Token
     * @param totalTokens  合计
     * @param estimated    {@code true} = 本地估算（服务端未回传 usage），{@code false} = 服务端真实值
     */
    public record Metered(int inputTokens, int outputTokens, int totalTokens, boolean estimated) {
    }
}
