package com.moyun.ext.ai.service.chat;

import com.moyun.ext.ai.entity.ModelConfig;
import com.moyun.ext.ai.service.ModelConfigService;
import com.moyun.ext.ai.service.impl.chat.ConversationSummaryServiceImpl;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * {@link ConversationSummaryService} 单元测试
 *
 * <p>测试策略：
 * <ul>
 *   <li>纯逻辑方法（needsSummary / estimateTokenCount）直接断言</li>
 *   <li>LLM 依赖方法（generateSummary / compressHistory / updateSummary）
 *       通过 mock {@link ModelConfigService#getDefaultChatConfig()} 返回 null，
 *       触发 fallback 路径，验证降级行为的正确性</li>
 * </ul>
 *
 * @author moyun
 */
@DisplayName("对话摘要服务 ConversationSummaryService")
@ExtendWith(MockitoExtension.class)
class ConversationSummaryServiceImplTest {

    @Mock
    private ModelConfigService modelConfigService;

    @InjectMocks
    private ConversationSummaryServiceImpl service;

    /** 默认 mock 策略：getDefaultChatConfig 返回 null → 触发所有 LLM 方法的 fallback 路径 */
    @BeforeEach
    void setUp() {
        // lenient 避免未使用的 stubbing 报错（部分测试不需要此 mock）
        lenient().when(modelConfigService.getDefaultChatConfig()).thenReturn(null);
        lenient().when(modelConfigService.createChatModel(anyLong())).thenReturn(null);
    }

    // ====================================================================
    // needsSummary
    // ====================================================================

    @Nested
    @DisplayName("needsSummary 判断是否需要摘要")
    class NeedsSummary {

        @Test
        @DisplayName("消息数超过阈值 → 需要")
        void shouldReturnTrueWhenExceedsThreshold() {
            List<ChatMessage> messages = buildMessages(15);
            assertThat(service.needsSummary(messages, 10)).isTrue();
        }

        @Test
        @DisplayName("消息数等于阈值 → 不需要")
        void shouldReturnFalseWhenEqualThreshold() {
            List<ChatMessage> messages = buildMessages(10);
            assertThat(service.needsSummary(messages, 10)).isFalse();
        }

        @Test
        @DisplayName("消息数低于阈值 → 不需要")
        void shouldReturnFalseWhenBelowThreshold() {
            List<ChatMessage> messages = buildMessages(5);
            assertThat(service.needsSummary(messages, 10)).isFalse();
        }

        @Test
        @DisplayName("空列表 → 不需要")
        void shouldReturnFalseForEmptyList() {
            assertThat(service.needsSummary(new ArrayList<>(), 10)).isFalse();
        }

        @Test
        @DisplayName("null → 不需要")
        void shouldReturnFalseForNull() {
            assertThat(service.needsSummary(null, 10)).isFalse();
        }
    }

    // ====================================================================
    // estimateTokenCount
    // ====================================================================

    @Nested
    @DisplayName("estimateTokenCount Token 估算")
    class EstimateTokenCount {

        @Test
        @DisplayName("中文约 2 字符/token，估算为 字符数 × 0.5")
        void shouldEstimateByHalfCharCount() {
            List<ChatMessage> messages = List.of(
                    new UserMessage("这是一段测试消息"),       // 8 字符
                    new AiMessage("助手回复内容")               // 6 字符
            );
            int tokenCount = service.estimateTokenCount(messages);
            // (8 + 6) × 0.5 = 7
            assertThat(tokenCount).isEqualTo(7);
        }

        @Test
        @DisplayName("包含 SystemMessage 也计入")
        void shouldIncludeSystemMessage() {
            List<ChatMessage> messages = List.of(
                    new SystemMessage("系统提示"),  // 4 字符
                    new UserMessage("用户问题")      // 4 字符
            );
            assertThat(service.estimateTokenCount(messages)).isEqualTo(4);
        }

        @Test
        @DisplayName("空列表返回 0")
        void shouldReturnZeroForEmptyList() {
            assertThat(service.estimateTokenCount(new ArrayList<>())).isEqualTo(0);
        }

        @Test
        @DisplayName("null 返回 0")
        void shouldReturnZeroForNull() {
            assertThat(service.estimateTokenCount(null)).isEqualTo(0);
        }
    }

    // ====================================================================
    // generateSummary（fallback 路径：LLM 不可用）
    // ====================================================================

    @Nested
    @DisplayName("generateSummary 生成摘要（LLM 不可用时走 fallback）")
    class GenerateSummaryFallback {

        @Test
        @DisplayName("LLM 不可用 → 使用简单摘要（包含前 3 条用户消息）")
        void shouldFallbackToSimpleSummaryWhenLlmUnavailable() {
            List<ChatMessage> messages = List.of(
                    new UserMessage("什么是 RAG？"),
                    new AiMessage("RAG 是检索增强生成"),
                    new UserMessage("如何配置知识库？"),
                    new UserMessage("向量检索怎么实现？"),
                    new UserMessage("这是第 4 个用户消息，不应出现在简单摘要中")
            );
            String summary = service.generateSummary(messages);

            assertThat(summary)
                    .isNotEmpty()
                    .startsWith("对话主题包括：")
                    .contains("什么是 RAG？")
                    .contains("如何配置知识库？")
                    .contains("向量检索怎么实现？")
                    // 简单摘要只取前 3 条用户消息
                    .doesNotContain("这是第 4 个用户消息");
        }

        @Test
        @DisplayName("空消息列表 → 返回空字符串")
        void shouldReturnEmptyForEmptyList() {
            assertThat(service.generateSummary(new ArrayList<>())).isEmpty();
        }

        @Test
        @DisplayName("null → 返回空字符串")
        void shouldReturnEmptyForNull() {
            assertThat(service.generateSummary(null)).isEmpty();
        }

        @Test
        @DisplayName("长用户消息被截断为 50 字符 + ...")
        void shouldTruncateLongUserMessage() {
            String longMessage = "这是一段非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常长的用户消息，"
                    + "用于测试简单摘要中的消息截断逻辑，长度超过 50 字符就应该被截断";
            List<ChatMessage> messages = List.of(new UserMessage(longMessage));
            String summary = service.generateSummary(messages);

            assertThat(summary).contains("...");
            // 原始消息不应完整出现在摘要中
            assertThat(summary.length()).isLessThan(longMessage.length());
        }
    }

    // ====================================================================
    // compressHistory（fallback 路径）
    // ====================================================================

    @Nested
    @DisplayName("compressHistory 压缩历史")
    class CompressHistory {

        @Test
        @DisplayName("消息数 <= keepRecent → 直接返回原列表副本")
        void shouldReturnOriginalWhenWithinKeepRecent() {
            List<ChatMessage> messages = List.of(
                    new UserMessage("问题1"),
                    new AiMessage("回答1"),
                    new UserMessage("问题2")
            );
            List<ChatMessage> compressed = service.compressHistory(messages, 3);

            assertThat(compressed).hasSize(3);
            // 返回的是副本，不是同一引用
            assertThat(compressed).isNotSameAs(messages);
        }

        @Test
        @DisplayName("空列表 → 返回空列表")
        void shouldReturnEmptyForEmptyList() {
            List<ChatMessage> compressed = service.compressHistory(new ArrayList<>(), 3);
            assertThat(compressed).isEmpty();
        }

        @Test
        @DisplayName("null → 返回空列表")
        void shouldReturnEmptyForNull() {
            assertThat(service.compressHistory(null, 3)).isEmpty();
        }

        @Test
        @DisplayName("消息数 > keepRecent → 摘要 + 最近消息")
        void shouldCompressOldMessagesIntoSummary() {
            // 6 条消息，保留最近 2 条
            List<ChatMessage> messages = new ArrayList<>();
            for (int i = 1; i <= 4; i++) {
                messages.add(new UserMessage("历史问题" + i));
                messages.add(new AiMessage("历史回答" + i));
            }
            messages.add(new UserMessage("最近问题1"));
            messages.add(new AiMessage("最近回答1"));

            List<ChatMessage> compressed = service.compressHistory(messages, 2);

            // 应包含：1 个摘要 SystemMessage + 最近 2 条消息
            assertThat(compressed).hasSize(3);
            // 第一条是摘要 SystemMessage
            assertThat(compressed.get(0)).isInstanceOf(SystemMessage.class);
            // 最后 2 条是最近的消息
            assertThat(compressed.get(1)).isInstanceOf(UserMessage.class);
            assertThat(((UserMessage) compressed.get(1)).singleText()).isEqualTo("最近问题1");
            assertThat(compressed.get(2)).isInstanceOf(AiMessage.class);
            assertThat(((AiMessage) compressed.get(2)).text()).isEqualTo("最近回答1");
        }
    }

    // ====================================================================
    // updateSummary（fallback 路径）
    // ====================================================================

    @Nested
    @DisplayName("updateSummary 更新增量摘要")
    class UpdateSummaryFallback {

        @Test
        @DisplayName("LLM 不可用 → 简单拼接已有摘要和新内容")
        void shouldFallbackToSimpleConcatenation() {
            String existing = "已有摘要内容";
            List<ChatMessage> newMessages = List.of(
                    new UserMessage("新的问题"),
                    new AiMessage("新的回答")
            );
            String updated = service.updateSummary(existing, newMessages);

            assertThat(updated)
                    .isNotEmpty()
                    .startsWith(existing)
                    .contains("新的问题");
        }

        @Test
        @DisplayName("新消息为空 → 返回已有摘要")
        void shouldReturnExistingWhenNoNewMessages() {
            String existing = "已有摘要";
            assertThat(service.updateSummary(existing, new ArrayList<>())).isEqualTo(existing);
        }

        @Test
        @DisplayName("新消息为 null → 返回已有摘要")
        void shouldReturnExistingWhenNullNewMessages() {
            String existing = "已有摘要";
            assertThat(service.updateSummary(existing, null)).isEqualTo(existing);
        }
    }

    // ====================================================================
    // 辅助方法
    // ====================================================================

    /** 构造指定数量的 UserMessage 列表 */
    private static List<ChatMessage> buildMessages(int count) {
        List<ChatMessage> messages = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            messages.add(new UserMessage("消息" + i));
        }
        return messages;
    }
}
