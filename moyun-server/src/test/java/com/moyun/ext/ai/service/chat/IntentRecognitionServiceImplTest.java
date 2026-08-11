package com.moyun.ext.ai.service.chat;

import com.moyun.ext.ai.entity.Agent;
import com.moyun.ext.ai.entity.ModelConfig;
import com.moyun.ext.ai.service.ModelConfigService;
import com.moyun.ext.ai.service.chat.IntentRecognitionService.IntentResult;
import com.moyun.ext.ai.service.chat.IntentRecognitionService.IntentType;
import com.moyun.ext.ai.service.impl.chat.IntentRecognitionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;

/**
 * {@link IntentRecognitionService} 单元测试
 *
 * <p>测试策略：
 * <ul>
 *   <li>规则匹配路径（GREETING / END / TOOL / QA）无需 LLM，直接断言</li>
 *   <li>LLM fallback 路径通过 mock {@link ModelConfigService} 返回 null 触发，
 *       验证降级为 KNOWLEDGE_QA 的正确性</li>
 *   <li>{@link #needsMoreInfo} 和 {@link #generateFollowUp} 为纯逻辑，直接断言</li>
 * </ul>
 *
 * @author moyun
 */
@DisplayName("意图识别服务 IntentRecognitionService")
@ExtendWith(MockitoExtension.class)
class IntentRecognitionServiceImplTest {

    @Mock
    private ModelConfigService modelConfigService;

    @InjectMocks
    private IntentRecognitionServiceImpl service;

    private Agent agent;

    @BeforeEach
    void setUp() {
        agent = new Agent();
        // LLM 不可用 → 走 fallback 路径
        lenient().when(modelConfigService.getDefaultChatConfig()).thenReturn(null);
        lenient().when(modelConfigService.createChatModel(anyLong())).thenReturn(null);
    }

    // ====================================================================
    // recognize - 规则匹配路径
    // ====================================================================

    @Nested
    @DisplayName("recognize 规则匹配")
    class RecognizeRuleBased {

        @Test
        @DisplayName("问候语 → GREETING (置信度 0.95)")
        void shouldRecognizeGreeting() {
            IntentResult result = service.recognize("你好", agent);
            assertThat(result.getIntent()).isEqualTo(IntentType.GREETING);
            assertThat(result.getConfidence()).isEqualTo(0.95);
        }

        @Test
        @DisplayName("英文 hello → GREETING")
        void shouldRecognizeEnglishGreeting() {
            IntentResult result = service.recognize("hello there", agent);
            assertThat(result.getIntent()).isEqualTo(IntentType.GREETING);
        }

        @Test
        @DisplayName("结束语 → END_CONVERSATION")
        void shouldRecognizeEndConversation() {
            IntentResult result = service.recognize("再见", agent);
            assertThat(result.getIntent()).isEqualTo(IntentType.END_CONVERSATION);
            assertThat(result.getConfidence()).isEqualTo(0.9);
        }

        @Test
        @DisplayName("thanks → END_CONVERSATION")
        void shouldRecognizeThanksAsEnd() {
            IntentResult result = service.recognize("thanks", agent);
            assertThat(result.getIntent()).isEqualTo(IntentType.END_CONVERSATION);
        }

        @Test
        @DisplayName("查询天气 → TOOL_CALL (含 slots)")
        void shouldRecognizeToolCallWeather() {
            IntentResult result = service.recognize("查询北京天气", agent);
            assertThat(result.getIntent()).isEqualTo(IntentType.TOOL_CALL);
            assertThat(result.getConfidence()).isEqualTo(0.8);
            assertThat(result.getSlots()).containsEntry("tool", "weather");
            assertThat(result.getSlots()).containsEntry("location", "北京");
        }

        @Test
        @DisplayName("查询时间 → TOOL_CALL")
        void shouldRecognizeToolCallTime() {
            IntentResult result = service.recognize("现在几点时间", agent);
            assertThat(result.getIntent()).isEqualTo(IntentType.TOOL_CALL);
            assertThat(result.getSlots()).containsEntry("tool", "time");
        }

        @Test
        @DisplayName("翻译 → TOOL_CALL")
        void shouldRecognizeToolCallTranslate() {
            IntentResult result = service.recognize("请翻译这段内容", agent);
            assertThat(result.getIntent()).isEqualTo(IntentType.TOOL_CALL);
            assertThat(result.getSlots()).containsEntry("tool", "translate");
        }

        @Test
        @DisplayName("问号结尾 → KNOWLEDGE_QA")
        void shouldRecognizeQuestionMarkAsQA() {
            IntentResult result = service.recognize("什么是 RAG？", agent);
            assertThat(result.getIntent()).isEqualTo(IntentType.KNOWLEDGE_QA);
            assertThat(result.getConfidence()).isEqualTo(0.85);
            assertThat(result.getSlots()).containsKey("question");
        }

        @Test
        @DisplayName("含 '怎么' → KNOWLEDGE_QA")
        void shouldRecognizeHowAsQA() {
            IntentResult result = service.recognize("怎么配置知识库", agent);
            assertThat(result.getIntent()).isEqualTo(IntentType.KNOWLEDGE_QA);
        }

        @Test
        @DisplayName("含 '什么是' → KNOWLEDGE_QA")
        void shouldRecognizeWhatIsAsQA() {
            IntentResult result = service.recognize("什么是向量数据库", agent);
            assertThat(result.getIntent()).isEqualTo(IntentType.KNOWLEDGE_QA);
        }

        @Test
        @DisplayName("含 '如何' → KNOWLEDGE_QA")
        void shouldRecognizeHowToAsQA() {
            IntentResult result = service.recognize("如何部署大模型", agent);
            assertThat(result.getIntent()).isEqualTo(IntentType.KNOWLEDGE_QA);
        }

        @Test
        @DisplayName("null → UNKNOWN")
        void shouldReturnUnknownForNull() {
            IntentResult result = service.recognize(null, agent);
            assertThat(result.getIntent()).isEqualTo(IntentType.UNKNOWN);
            assertThat(result.getConfidence()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("空字符串 → UNKNOWN")
        void shouldReturnUnknownForEmpty() {
            IntentResult result = service.recognize("", agent);
            assertThat(result.getIntent()).isEqualTo(IntentType.UNKNOWN);
        }

        @Test
        @DisplayName("纯空格 → UNKNOWN")
        void shouldReturnUnknownForBlank() {
            IntentResult result = service.recognize("   ", agent);
            assertThat(result.getIntent()).isEqualTo(IntentType.UNKNOWN);
        }
    }

    // ====================================================================
    // recognize - LLM fallback 路径
    // ====================================================================

    @Nested
    @DisplayName("recognize LLM fallback")
    class RecognizeLlmFallback {

        @Test
        @DisplayName("LLM 不可用时 → 降级为 KNOWLEDGE_QA (置信度 0.6)")
        void shouldFallbackToKnowledgeQAWhenLlmUnavailable() {
            // 不匹配任何规则的消息，会进入 LLM 路径
            IntentResult result = service.recognize("帮我分析一下数据", agent);
            assertThat(result.getIntent()).isEqualTo(IntentType.KNOWLEDGE_QA);
            assertThat(result.getConfidence()).isEqualTo(0.6);
            assertThat(result.getSlots()).containsKey("question");
        }
    }

    // ====================================================================
    // recognizeWithContext
    // ====================================================================

    @Nested
    @DisplayName("recognizeWithContext 带上下文识别")
    class RecognizeWithContext {

        @Test
        @DisplayName("当前实现等价于 recognize")
        void shouldDelegateToRecognize() {
            IntentResult result = service.recognizeWithContext("你好", "之前的对话上下文", agent);
            assertThat(result.getIntent()).isEqualTo(IntentType.GREETING);
        }
    }

    // ====================================================================
    // needsMoreInfo
    // ====================================================================

    @Nested
    @DisplayName("needsMoreInfo 判断是否需要追问")
    class NeedsMoreInfo {

        @Test
        @DisplayName("missingSlots 为空数组 → false")
        void shouldReturnFalseWhenNoMissingSlots() {
            IntentResult result = IntentResult.greeting("你好", 0.95);
            assertThat(service.needsMoreInfo(result)).isFalse();
        }

        @Test
        @DisplayName("missingSlots 含元素 → true")
        void shouldReturnTrueWhenHasMissingSlots() {
            IntentResult result = new IntentResult(
                    IntentType.TASK_EXECUTION, 0.8, java.util.Map.of(),
                    new String[]{"location", "date"}, "test");
            assertThat(service.needsMoreInfo(result)).isTrue();
        }

        @Test
        @DisplayName("missingSlots 为 null → false")
        void shouldReturnFalseWhenMissingSlotsNull() {
            IntentResult result = new IntentResult(
                    IntentType.UNKNOWN, 0.0, java.util.Map.of(),
                    null, "test");
            assertThat(service.needsMoreInfo(result)).isFalse();
        }
    }

    // ====================================================================
    // generateFollowUp
    // ====================================================================

    @Nested
    @DisplayName("generateFollowUp 生成追问")
    class GenerateFollowUp {

        @Test
        @DisplayName("location 槽位 → 追问城市")
        void shouldAskForLocation() {
            IntentResult result = new IntentResult(
                    IntentType.TASK_EXECUTION, 0.8, java.util.Map.of(),
                    new String[]{"location"}, "test");
            String followUp = service.generateFollowUp(result);
            assertThat(followUp)
                    .isNotNull()
                    .contains("请提供更多信息")
                    .contains("请问您想查询哪个城市？");
        }

        @Test
        @DisplayName("date 槽位 → 追问日期")
        void shouldAskForDate() {
            IntentResult result = new IntentResult(
                    IntentType.TASK_EXECUTION, 0.8, java.util.Map.of(),
                    new String[]{"date"}, "test");
            String followUp = service.generateFollowUp(result);
            assertThat(followUp).contains("请问是哪一天？");
        }

        @Test
        @DisplayName("time 槽位 → 追问时间")
        void shouldAskForTime() {
            IntentResult result = new IntentResult(
                    IntentType.TASK_EXECUTION, 0.8, java.util.Map.of(),
                    new String[]{"time"}, "test");
            String followUp = service.generateFollowUp(result);
            assertThat(followUp).contains("请问具体时间是？");
        }

        @Test
        @DisplayName("target 槽位 → 追问具体内容")
        void shouldAskForTarget() {
            IntentResult result = new IntentResult(
                    IntentType.TASK_EXECUTION, 0.8, java.util.Map.of(),
                    new String[]{"target"}, "test");
            String followUp = service.generateFollowUp(result);
            assertThat(followUp).contains("请问具体是什么？");
        }

        @Test
        @DisplayName("未知槽位 → 通用追问")
        void shouldAskForUnknownSlot() {
            IntentResult result = new IntentResult(
                    IntentType.TASK_EXECUTION, 0.8, java.util.Map.of(),
                    new String[]{"customField"}, "test");
            String followUp = service.generateFollowUp(result);
            assertThat(followUp).contains("请提供customField信息");
        }

        @Test
        @DisplayName("无 missingSlots → 返回 null")
        void shouldReturnNullWhenNoMissingSlots() {
            IntentResult result = IntentResult.greeting("你好", 0.95);
            assertThat(service.generateFollowUp(result)).isNull();
        }

        @Test
        @DisplayName("missingSlots 为 null → 返回 null")
        void shouldReturnNullWhenMissingSlotsIsNull() {
            IntentResult result = new IntentResult(
                    IntentType.UNKNOWN, 0.0, java.util.Map.of(),
                    null, "test");
            assertThat(service.generateFollowUp(result)).isNull();
        }

        @Test
        @DisplayName("多个槽位 → 拼接多个追问")
        void shouldConcatenateMultipleSlots() {
            IntentResult result = new IntentResult(
                    IntentType.TASK_EXECUTION, 0.8, java.util.Map.of(),
                    new String[]{"location", "date"}, "test");
            String followUp = service.generateFollowUp(result);
            assertThat(followUp)
                    .contains("请问您想查询哪个城市？")
                    .contains("请问是哪一天？");
        }
    }
}
