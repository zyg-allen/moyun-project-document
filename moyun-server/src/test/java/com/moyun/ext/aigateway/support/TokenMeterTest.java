package com.moyun.ext.aigateway.support;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.TokenUsage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link TokenMeter} 计量语义单测
 *
 * <p>锁定三条规则，防止"静默 0"回归：</p>
 * <ol>
 *   <li>服务端 usage 可用 → 用真实值且 {@code estimated=false}；</li>
 *   <li>usage 缺失（流式常态）→ 本地分词估算，{@code estimated=true}，且**必须 &gt; 0**；</li>
 *   <li>空输入空输出 → 0（不伪造）。</li>
 * </ol>
 *
 * @author moyun
 */
class TokenMeterTest {

    private final TokenMeter meter = new TokenMeter();

    @Test
    @DisplayName("服务端回传 usage → 用真实值，estimated=false")
    void providerUsage_isUsedAsIs() {
        TokenMeter.Metered m = meter.meter(new TokenUsage(120, 80),
                List.of(UserMessage.from("任意提示词")), "任意输出");

        assertEquals(120, m.inputTokens());
        assertEquals(80, m.outputTokens());
        assertEquals(200, m.totalTokens());
        assertFalse(m.estimated(), "服务端真实值不得标记为估算");
    }

    @Test
    @DisplayName("仅回传合计（input/output 为空）→ 合计用真实值，estimated=false")
    void providerUsageTotalOnly() {
        // 注意：langchain4j 的 TokenUsage(Integer) 单参构造是"仅输入"，故这里显式用三参：只有合计
        TokenUsage totalOnly = new TokenUsage(null, null, 300);
        TokenMeter.Metered m = meter.meter(totalOnly, List.of(UserMessage.from("x")), "y");

        assertEquals(0, m.inputTokens());
        assertEquals(0, m.outputTokens());
        assertEquals(300, m.totalTokens());
        assertFalse(m.estimated());
    }

    @Test
    @DisplayName("仅回传输入（单参构造）→ 输入真实值，合计=input，estimated=false")
    void providerUsageInputOnly() {
        TokenMeter.Metered m = meter.meter(new TokenUsage(120),
                List.of(UserMessage.from("x")), "y");

        assertEquals(120, m.inputTokens());
        assertEquals(0, m.outputTokens());
        assertEquals(120, m.totalTokens());
        assertFalse(m.estimated());
    }

    @Test
    @DisplayName("usage 缺失（流式常态）→ 本地分词估算，estimated=true 且 >0")
    void missingUsage_fallsBackToLocalEstimate() {
        TokenMeter.Metered m = meter.meter(null,
                List.of(SystemMessage.from("你是一位资深面试官。"),
                        UserMessage.from("请用一句话介绍你自己，并说明你最擅长的技术方向。")),
                "我是本次的面试官，我们先从你的项目经历聊起。");

        assertTrue(m.estimated(), "本地分词结果必须标记为估算");
        assertTrue(m.inputTokens() > 0, "输入估算必须 >0（否则仍是漏计）");
        assertTrue(m.outputTokens() > 0, "输出估算必须 >0");
        assertEquals(m.inputTokens() + m.outputTokens(), m.totalTokens());
    }

    @Test
    @DisplayName("usage 字段全空对象 → 同样走估算（不当成真实值）")
    void emptyProviderUsage_fallsBackToEstimate() {
        TokenMeter.Metered m = meter.meter(new TokenUsage(null, null),
                List.of(UserMessage.from("你好")), "你好");

        assertTrue(m.estimated());
        assertTrue(m.totalTokens() > 0);
    }

    @Test
    @DisplayName("空输入空输出 → 0，且不抛异常")
    void emptyInputs_zero() {
        TokenMeter.Metered m = meter.meter(null, null, null);
        assertEquals(0, m.totalTokens());
        assertEquals(0, m.inputTokens());
        assertEquals(0, m.outputTokens());

        TokenMeter.Metered blank = meter.meterTexts("", "");
        assertEquals(0, blank.totalTokens());
    }

    @Test
    @DisplayName("meterTexts 估算单调：更长文本得到更多 token")
    void estimateIsMonotonic() {
        TokenMeter.Metered shortText = meter.meterTexts("你好", "好的");
        TokenMeter.Metered longText = meter.meterTexts(
                "你好，请用一句话介绍你自己，并说明你最擅长的技术方向以及最近做过的项目。",
                "我是本次的面试官，我们先从你的项目经历聊起，请重点说明你在其中的职责与产出。");

        assertTrue(longText.totalTokens() > shortText.totalTokens(),
                "更长文本应得到更多 token：" + longText.totalTokens() + " vs " + shortText.totalTokens());
        assertTrue(shortText.estimated() && longText.estimated());
    }
}
