package com.moyun.ext.aigateway.support;

import com.moyun.ext.aigateway.support.IntentClassifier.IntentResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 意图判断器单元测试（v14.72 P2-3 规则库扩充）
 *
 * <p>覆盖：通用对话控制、业务域路由（建议场景）、泛化规则顺序
 * （具体规则优先于 HOW_TO）、场景特定意图（voice_interview/default_chat）、
 * 兜底语义（过短/纯标点低置信追问，正常自由文本不误追问）。</p>
 */
class IntentClassifierTest {

    private final IntentClassifier classifier = new IntentClassifier();

    // ==================== 通用对话控制 ====================

    @Test
    void greeting_chinese_shouldHit() {
        IntentResult r = classifier.classify("你好，我想咨询点事情", "default_chat");
        assertEquals("GREETING", r.getIntent());
        assertTrue(r.getConfidence() >= IntentClassifier.CLARIFICATION_THRESHOLD);
        assertNull(r.getSuggestedScene());
    }

    @Test
    void greeting_english_caseInsensitive() {
        assertEquals("GREETING", classifier.classify("Hello there", "default_chat").getIntent());
        assertEquals("GREETING", classifier.classify("HI", "default_chat").getIntent());
    }

    @Test
    void next_and_finish_shouldHit() {
        assertEquals("NEXT", classifier.classify("下一题", "voice_interview").getIntent());
        assertEquals("NEXT", classifier.classify("继续", "question_generate").getIntent());
        assertEquals("FINISH", classifier.classify("就到这里吧，结束了", "default_chat").getIntent());
    }

    // ==================== 业务域路由 ====================

    @Test
    void resumeOptimize_shouldSuggestScene() {
        IntentResult r = classifier.classify("帮我优化简历里项目经历部分", "default_chat");
        assertEquals("RESUME_OPTIMIZE", r.getIntent());
        assertEquals("resume_optimize", r.getSuggestedScene());
    }

    @Test
    void resumeParse_shouldSuggestScene() {
        IntentResult r = classifier.classify("解析一下我的简历", "default_chat");
        assertEquals("RESUME_PARSE", r.getIntent());
        assertEquals("resume_parse", r.getSuggestedScene());
    }

    @Test
    void questionGenerate_shouldSuggestScene() {
        IntentResult r = classifier.classify("给我出5道Java面试题", "default_chat");
        assertEquals("QUESTION_GENERATE", r.getIntent());
        assertEquals("question_generate", r.getSuggestedScene());
    }

    @Test
    void financeAnalysis_shouldSuggestScene() {
        IntentResult r = classifier.classify("分析一下我这个月的收支情况", "default_chat");
        assertEquals("FINANCE_ANALYSIS", r.getIntent());
        assertEquals("finance_analysis", r.getSuggestedScene());
    }

    @Test
    void specificRule_shouldWinOverGenericHowTo() {
        // 含"怎么写"+"简历"：简历咨询规则在前，不应被泛化 HOW_TO 抢先命中
        IntentResult r = classifier.classify("简历里的自我评价怎么写比较好", "default_chat");
        assertEquals("RESUME_HELP", r.getIntent());
    }

    // ==================== 场景特定意图 ====================

    @Test
    void voiceInterview_confused() {
        IntentResult r = classifier.classify("不太清楚", "voice_interview");
        assertEquals("CONFUSED", r.getIntent());
    }

    @Test
    void voiceInterview_completeAnswer() {
        String longAnswer = "这个问题的答案比较长。".repeat(10);
        IntentResult r = classifier.classify(longAnswer, "voice_interview");
        assertEquals("COMPLETE_ANSWER", r.getIntent());
    }

    @Test
    void defaultChat_shortConfirm() {
        assertEquals("CONFIRM", classifier.classify("是的，就这样", "default_chat").getIntent());
    }

    // ==================== 兜底语义（P2-3 核心） ====================

    @Test
    void normalFreeText_shouldStayInScene_notTriggerClarification() {
        // 正常长度自由文本：维持场景且置信度不低于追问阈值（旧版恒 UNKNOWN 0.3 误追问）
        IntentResult r = classifier.classify("我在准备后端岗位的校招，想了解Redis持久化机制的取舍", "default_chat");
        assertTrue(r.getConfidence() >= IntentClassifier.CLARIFICATION_THRESHOLD);
        assertNull(r.getSuggestedScene());
    }

    @Test
    void tooShortInput_shouldTriggerClarification() {
        IntentResult r = classifier.classify("嗯", "default_chat");
        assertEquals("UNKNOWN", r.getIntent());
        assertTrue(r.getConfidence() < IntentClassifier.CLARIFICATION_THRESHOLD);
        assertNotNull(r.getClarificationQuestion());
    }

    @Test
    void punctuationOnly_shouldTriggerClarification() {
        IntentResult r = classifier.classify("？？？ 。。。", "default_chat");
        assertEquals("UNKNOWN", r.getIntent());
        assertTrue(r.getConfidence() < IntentClassifier.CLARIFICATION_THRESHOLD);
    }

    @Test
    void blankOrNull_shouldBeUnknown() {
        assertEquals("UNKNOWN", classifier.classify(null, "default_chat").getIntent());
        assertEquals("UNKNOWN", classifier.classify("  ", "default_chat").getIntent());
    }

    @Test
    void multilineInput_shouldMatch() {
        // DOTALL：跨行文本规则仍可命中
        assertEquals("GREETING", classifier.classify("老师你好\n想请教一个问题", "default_chat").getIntent());
    }
}
