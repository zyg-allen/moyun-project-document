package com.moyun.ext.ai.service.chat;

import com.moyun.ext.ai.config.RagConfig;
import com.moyun.ext.ai.service.ModelConfigService;
import com.moyun.ext.ai.service.QueryExpansionService;
import com.moyun.ext.ai.service.TokenUsageService;
import com.moyun.ext.ai.service.impl.chat.RagRetrievalServiceImpl;
import com.moyun.ext.ai.store.VectorStoreExtension;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * {@link RagRetrievalService} 单元测试
 *
 * <p>测试策略：
 * <ul>
 *   <li>{@link #getContentRerankScores} / {@link #clearContentRerankScores} — 纯 ThreadLocal 操作</li>
 *   <li>{@link #rerankContents} — rerankModel 为 null 时走 {@code rerankByRules} 路径，
 *       使用 mock 的 {@link ContentScoringService} 控制评分，验证排序与 topK 截断</li>
 *   <li>{@link #mergeAdjacentSegments} — 纯逻辑，基于 Content metadata 的 fileName/knowledgeBaseId 分组合并</li>
 * </ul>
 *
 * <p>未覆盖：retrieveContents（依赖 EmbeddingStore，集成测试范畴）
 *
 * @author moyun
 */
@DisplayName("RAG 检索服务 RagRetrievalService")
@ExtendWith(MockitoExtension.class)
class RagRetrievalServiceImplTest {

    @Mock
    private VectorStoreExtension embeddingStore;

    @Mock
    private RagConfig ragConfig;

    @Mock
    private ModelConfigService modelConfigService;

    @Mock
    private QueryExpansionService queryExpansionService;

    @Mock
    private TokenUsageService tokenUsageService;

    @Mock
    private ContentScoringService contentScoringService;

    @Mock
    private QueryRewritingService queryRewritingService;

    @InjectMocks
    private RagRetrievalServiceImpl service;

    @BeforeEach
    void setUp() {
        // mock ContentScoringService 的两个方法（rerankByRules 路径依赖）
        // 默认返回空关键词和 0 分，具体测试可覆盖
        lenient().when(contentScoringService.extractKeywords(anyString())).thenReturn(new String[]{"test"});
        lenient().when(contentScoringService.calculateRelevanceScore(anyString(), any(), anyString()))
                .thenReturn(0.0);
    }

    // ====================================================================
    // getContentRerankScores / clearContentRerankScores（ThreadLocal）
    // ====================================================================

    @Nested
    @DisplayName("重排分数 ThreadLocal 管理")
    class RerankScoresThreadLocal {

        @Test
        @DisplayName("初始状态 → 返回空 map（非 null）")
        void shouldReturnEmptyMapInitially() {
            // 清空确保初始状态
            service.clearContentRerankScores();
            Map<Content, Double> scores = service.getContentRerankScores();
            assertThat(scores).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("clearContentRerankScores → 清空 map")
        void shouldClearScoresMap() {
            // 先通过 rerankContents 填充一些分数
            when(contentScoringService.calculateRelevanceScore(anyString(), any(), anyString()))
                    .thenReturn(5.0);
            List<Content> contents = List.of(
                    createContent("内容1", "file1.pdf", "1"),
                    createContent("内容2", "file2.pdf", "1")
            );
            service.rerankContents(contents, "查询", 2);

            // 验证分数已填充
            assertThat(service.getContentRerankScores()).isNotEmpty();

            // 清空
            service.clearContentRerankScores();
            assertThat(service.getContentRerankScores()).isEmpty();
        }

        @Test
        @DisplayName("ThreadLocal 隔离：不同线程独立 map")
        void shouldIsolateByThread() throws Exception {
            when(contentScoringService.calculateRelevanceScore(anyString(), any(), anyString()))
                    .thenReturn(3.0);
            List<Content> contents = List.of(createContent("主线程内容", "main.pdf", "1"));
            service.rerankContents(contents, "查询", 1);

            // 主线程应有分数
            assertThat(service.getContentRerankScores()).hasSize(1);

            // 在另一线程验证隔离
            Thread t = new Thread(() -> {
                // 子线程应有独立的空 map
                assertThat(service.getContentRerankScores()).isEmpty();
            });
            t.start();
            t.join();
        }
    }

    // ====================================================================
    // rerankContents（rerankByRules 路径）
    // ====================================================================

    @Nested
    @DisplayName("rerankContents 规则重排")
    class RerankByRules {

        @Test
        @DisplayName("按分数降序排序")
        void shouldSortByScoreDescending() {
            // 模拟 3 个内容，分数分别为 2.5 / 5.0 / 3.0
            // （低分须 ≥2.0：rerankByRules 质量过滤阈值 max(2.0, topScore*0.3)，1.0 会被过滤）
            Content c1 = createContent("低分内容", "f1.pdf", "1");
            Content c2 = createContent("高分内容", "f2.pdf", "1");
            Content c3 = createContent("中分内容", "f3.pdf", "1");

            when(contentScoringService.calculateRelevanceScore(anyString(), any(), anyString()))
                    .thenReturn(2.5, 5.0, 3.0);

            List<Content> reranked = service.rerankContents(
                    List.of(c1, c2, c3), "查询", 3);

            assertThat(reranked).hasSize(3);
            // 降序：c2 (5.0) → c3 (3.0) → c1 (2.5)
            assertThat(reranked.get(0)).isSameAs(c2);
            assertThat(reranked.get(1)).isSameAs(c3);
            assertThat(reranked.get(2)).isSameAs(c1);

            // 验证分数已记录到 ThreadLocal
            Map<Content, Double> scores = service.getContentRerankScores();
            assertThat(scores).containsEntry(c2, 5.0);
            assertThat(scores).containsEntry(c3, 3.0);
            assertThat(scores).containsEntry(c1, 2.5);
        }

        @Test
        @DisplayName("质量过滤：低于阈值（max(2.0, topScore*0.3)）的低分文本被剔除")
        void shouldFilterLowScoreText() {
            Content high = createContent("高分内容", "f1.pdf", "1");
            Content low = createContent("低分内容", "f2.pdf", "1");

            when(contentScoringService.calculateRelevanceScore(anyString(), any(), anyString()))
                    .thenReturn(5.0, 1.0);

            List<Content> reranked = service.rerankContents(
                    List.of(high, low), "查询", 2);

            // 低分 1.0 < 阈值 max(2.0, 5.0*0.3)=2.0，被过滤
            assertThat(reranked).containsExactly(high);
        }

        @Test
        @DisplayName("topK 截断：只返回前 K 个")
        void shouldTruncateToTopK() {
            Content c1 = createContent("内容1", "f1.pdf", "1");
            Content c2 = createContent("内容2", "f2.pdf", "1");
            Content c3 = createContent("内容3", "f3.pdf", "1");
            Content c4 = createContent("内容4", "f4.pdf", "1");
            Content c5 = createContent("内容5", "f5.pdf", "1");

            // 分数递减
            when(contentScoringService.calculateRelevanceScore(anyString(), any(), anyString()))
                    .thenReturn(5.0, 4.0, 3.0, 2.0, 1.0);

            List<Content> reranked = service.rerankContents(
                    List.of(c1, c2, c3, c4, c5), "查询", 2);

            assertThat(reranked).hasSize(2);
            // 取前 2 个（分数最高的）
            assertThat(reranked).containsExactly(c1, c2);
        }

        @Test
        @DisplayName("空列表 → 返回空列表")
        void shouldReturnEmptyForEmptyList() {
            List<Content> reranked = service.rerankContents(new ArrayList<>(), "查询", 5);
            assertThat(reranked).isEmpty();
        }
    }

    // ====================================================================
    // mergeAdjacentSegments
    // ====================================================================

    @Nested
    @DisplayName("mergeAdjacentSegments 相邻分片合并")
    class MergeAdjacentSegments {

        @Test
        @DisplayName("单个内容 → 原样返回")
        void shouldReturnSingleContentAsIs() {
            Content c = createContent("唯一内容", "file.pdf", "1");
            List<Content> merged = service.mergeAdjacentSegments(List.of(c));
            assertThat(merged).hasSize(1);
        }

        @Test
        @DisplayName("空列表 → 原样返回")
        void shouldReturnEmptyForEmptyList() {
            List<Content> merged = service.mergeAdjacentSegments(new ArrayList<>());
            assertThat(merged).isEmpty();
        }

        @Test
        @DisplayName("不同文件的内容 → 不合并（保持独立）")
        void shouldNotMergeDifferentFiles() {
            Content c1 = createContent("文件1内容A", "file1.pdf", "1");
            Content c2 = createContent("文件2内容B", "file2.pdf", "1");
            List<Content> merged = service.mergeAdjacentSegments(List.of(c1, c2));
            // 不同文件不合并，应保持 2 个
            assertThat(merged).hasSize(2);
        }

        @Test
        @DisplayName("无 metadata 的内容 → 不参与合并")
        void shouldSkipContentWithoutMetadata() {
            // 创建无 metadata 的 Content（仅文本）
            Content c1 = Content.from(TextSegment.from("无元数据内容"));
            Content c2 = createContent("有元数据内容", "file.pdf", "1");
            List<Content> merged = service.mergeAdjacentSegments(List.of(c1, c2));
            // 无 metadata 的内容应保留，不参与合并逻辑
            assertThat(merged).isNotEmpty();
        }
    }

    // ====================================================================
    // 辅助方法
    // ====================================================================

    /** 创建带 fileName/knowledgeBaseId metadata 的 Content */
    private static Content createContent(String text, String fileName, String knowledgeBaseId) {
        Metadata metadata = new Metadata();
        metadata.put("fileName", fileName);
        metadata.put("knowledgeBaseId", knowledgeBaseId);
        return Content.from(TextSegment.from(text, metadata));
    }
}
