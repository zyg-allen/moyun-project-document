package com.moyun.ext.ai.service.chat;

import com.moyun.ext.ai.entity.Agent;
import com.moyun.ext.ai.entity.ModelConfig;
import com.moyun.ext.ai.service.ModelConfigService;
import com.moyun.ext.ai.service.impl.chat.QueryRewritingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;

/**
 * {@link QueryRewritingService} 单元测试
 *
 * <p>测试策略：
 * <ul>
 *   <li>{@link #shouldRewrite} 为纯逻辑，直接断言各种边界</li>
 *   <li>LLM 依赖方法（rewriteQuery / decomposeQuery / generateHypotheticalDocument）
 *       通过 mock LLM 不可用，验证 fallback 行为</li>
 * </ul>
 *
 * @author moyun
 */
@DisplayName("查询改写服务 QueryRewritingService")
@ExtendWith(MockitoExtension.class)
class QueryRewritingServiceImplTest {

    @Mock
    private ModelConfigService modelConfigService;

    @InjectMocks
    private QueryRewritingServiceImpl service;

    private Agent agent;

    @BeforeEach
    void setUp() {
        agent = new Agent();
        // LLM 不可用 → 触发 fallback
        lenient().when(modelConfigService.getDefaultChatConfig()).thenReturn(null);
        lenient().when(modelConfigService.createChatModel(anyLong())).thenReturn(null);
    }

    // ====================================================================
    // shouldRewrite（纯逻辑）
    // ====================================================================

    @Nested
    @DisplayName("shouldRewrite 判断是否需要改写")
    class ShouldRewrite {

        @Test
        @DisplayName("null → false")
        void shouldReturnFalseForNull() {
            assertThat(service.shouldRewrite(null)).isFalse();
        }

        @Test
        @DisplayName("长度 < 5 → false")
        void shouldReturnFalseForShortQuery() {
            assertThat(service.shouldRewrite("abc")).isFalse();
        }

        @Test
        @DisplayName("纯空格 → false")
        void shouldReturnFalseForBlankQuery() {
            assertThat(service.shouldRewrite("    ")).isFalse();
        }

        @Test
        @DisplayName("短词（< 10 字符，无空格）→ false")
        void shouldReturnFalseForShortSingleWord() {
            // "RAG检索" 6 字符，无空格 → false
            assertThat(service.shouldRewrite("RAG检索")).isFalse();
        }

        @Test
        @DisplayName("含空格的中长查询 → true")
        void shouldReturnTrueForSpacedQuery() {
            assertThat(service.shouldRewrite("RAG 检索 向量数据库")).isTrue();
        }

        @Test
        @DisplayName("长查询（>= 10 字符）→ true")
        void shouldReturnTrueForLongQuery() {
            assertThat(service.shouldRewrite("这是一个比较长的查询语句")).isTrue();
        }
    }

    // ====================================================================
    // rewriteQuery（fallback：LLM 不可用）
    // ====================================================================

    @Nested
    @DisplayName("rewriteQuery 查询改写（fallback）")
    class RewriteQueryFallback {

        @Test
        @DisplayName("短查询 → 直接返回原查询（不改写）")
        void shouldReturnOriginalForShortQuery() {
            String original = "abc";
            String rewritten = service.rewriteQuery(original, agent);
            assertThat(rewritten).isEqualTo(original);
        }

        @Test
        @DisplayName("LLM 不可用 → 返回原查询")
        void shouldReturnOriginalWhenLlmUnavailable() {
            String original = "这是一个比较长的查询语句需要改写";
            String rewritten = service.rewriteQuery(original, agent);
            assertThat(rewritten).isEqualTo(original);
        }

        @Test
        @DisplayName("null 查询 → 返回 null（shouldRewrite 返回 false，直接返回原值）")
        void shouldReturnOriginalForNullQuery() {
            // shouldRewrite(null) = false → 直接 return originalQuery
            String rewritten = service.rewriteQuery(null, agent);
            assertThat(rewritten).isNull();
        }
    }

    // ====================================================================
    // decomposeQuery（fallback）
    // ====================================================================

    @Nested
    @DisplayName("decomposeQuery 查询分解（fallback）")
    class DecomposeQueryFallback {

        @Test
        @DisplayName("短查询（< 50 字符）→ 返回单元素列表")
        void shouldReturnSingleElementForShortQuery() {
            List<String> subQueries = service.decomposeQuery("什么是 RAG 检索", agent);
            assertThat(subQueries).hasSize(1).first().isEqualTo("什么是 RAG 检索");
        }

        @Test
        @DisplayName("长查询但 LLM 不可用 → 返回单元素列表（原查询）")
        void shouldReturnOriginalForLongQueryWhenLlmUnavailable() {
            String longQuery = "这是一个超过 50 字符的长查询语句，需要被分解为多个子查询以便进行多路检索再合并结果，"
                    + "但由于 LLM 不可用，应直接返回原查询作为唯一元素";
            List<String> subQueries = service.decomposeQuery(longQuery, agent);
            assertThat(subQueries).hasSize(1).first().isEqualTo(longQuery);
        }
    }

    // ====================================================================
    // generateHypotheticalDocument（fallback）
    // ====================================================================

    @Nested
    @DisplayName("generateHypotheticalDocument HyDE（fallback）")
    class GenerateHypotheticalDocumentFallback {

        @Test
        @DisplayName("LLM 不可用 → 返回 null")
        void shouldReturnNullWhenLlmUnavailable() {
            String result = service.generateHypotheticalDocument("什么是 RAG", agent);
            assertThat(result).isNull();
        }
    }
}
