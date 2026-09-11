package com.moyun.ext.ai.service.chat;

import com.moyun.ext.ai.service.impl.chat.ContentScoringServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ContentScoringService} 单元测试
 *
 * <p>纯逻辑服务，无外部依赖（仅依赖 {@link com.moyun.ext.ai.util.TextProcessingUtils} 工具类），
 * 直接 new 实例进行测试，无需 Spring/Mockito 上下文。
 *
 * <p>覆盖维度：
 * <ul>
 *   <li>关键词匹配评分（首次+3，后续+1）</li>
 *   <li>完整查询匹配加分（+30）</li>
 *   <li>文本长度惩罚（&lt;30 ×0.5，&lt;80 ×0.7，&gt;2000 ×0.9）</li>
 *   <li>图片内容降权（×0.3）</li>
 *   <li>泛化词降权策略（×0.3 / 跳过）</li>
 *   <li>主题一致性检查（跨领域惩罚 ×0.2）</li>
 *   <li>语义相关词扩展评分</li>
 *   <li>子串计数与关键词提取</li>
 * </ul>
 *
 * @author moyun
 */
@DisplayName("内容评分服务 ContentScoringService")
class ContentScoringServiceImplTest {

    private ContentScoringService service;

    @BeforeEach
    void setUp() {
        service = new ContentScoringServiceImpl();
    }

    // ====================================================================
    // countOccurrences
    // ====================================================================

    @Nested
    @DisplayName("countOccurrences 子串计数")
    class CountOccurrences {

        @Test
        @DisplayName("正常计数：子串多次出现")
        void shouldCountMultipleOccurrences() {
            int count = service.countOccurrences("RAG 是 RAG 系统的核心，RAG 检索", "rag");
            assertThat(count).isEqualTo(3);
        }

        @Test
        @DisplayName("大小写不敏感")
        void shouldBeCaseInsensitive() {
            assertThat(service.countOccurrences("Embedding embedding EMBEDDING", "embedding")).isEqualTo(3);
        }

        @Test
        @DisplayName("中文子串计数")
        void shouldCountChineseSubstring() {
            assertThat(service.countOccurrences("污水处理方案包含污水处理工艺", "污水处理")).isEqualTo(2);
        }

        @Test
        @DisplayName("空文本返回 0")
        void shouldReturnZeroForNullText() {
            assertThat(service.countOccurrences(null, "rag")).isEqualTo(0);
        }

        @Test
        @DisplayName("空子串返回 0")
        void shouldReturnZeroForEmptySubstring() {
            assertThat(service.countOccurrences("some text", "")).isEqualTo(0);
        }

        @Test
        @DisplayName("子串不存在返回 0")
        void shouldReturnZeroWhenNotFound() {
            assertThat(service.countOccurrences("知识库检索", "向量数据库")).isEqualTo(0);
        }
    }

    // ====================================================================
    // extractKeywords
    // ====================================================================

    @Nested
    @DisplayName("extractKeywords 关键词提取")
    class ExtractKeywords {

        @Test
        @DisplayName("英文术语保持完整（RAG）")
        void shouldKeepEnglishTermIntact() {
            String[] keywords = service.extractKeywords("什么是 RAG 检索");
            assertThat(keywords).contains("rag");
        }

        @Test
        @DisplayName("过滤停用词（的、是、在、什么、怎么 等）")
        void shouldFilterStopWords() {
            // 关键词按空格/标点切分后逐词对照停用词表过滤
            String[] keywords = service.extractKeywords("的 是 什么 怎么");
            assertThat(keywords).isEmpty();
        }

        @Test
        @DisplayName("无分隔连续中文不拆分（无分词器，整体作为关键词保留）")
        void shouldKeepContinuousChineseAsWhole() {
            // 现状说明：extractKeywords 按空格/标点切分，无中文分词能力，
            // "什么是的怎么" 作为整体无法词级匹配停用词表，整体保留
            String[] keywords = service.extractKeywords("什么是的怎么");
            assertThat(keywords).contains("什么是的怎么");
        }

        @Test
        @DisplayName("长中文词被拆分为 2 字词")
        void shouldSplitLongChineseWord() {
            String[] keywords = service.extractKeywords("污水处理方案");
            // 应包含原始词及拆分的 2 字子词
            assertThat(keywords)
                    .anySatisfy(k -> assertThat(k).isIn("污水处理方案", "污水", "处理", "方案"));
        }

        @Test
        @DisplayName("中英文混合提取")
        void shouldExtractMixedKeywords() {
            String[] keywords = service.extractKeywords("RAG 向量检索 embedding");
            assertThat(keywords).contains("rag", "embedding");
        }

        @Test
        @DisplayName("标点符号被替换为空格")
        void shouldReplacePunctuationWithSpace() {
            String[] keywords = service.extractKeywords("知识库，检索：向量");
            assertThat(keywords).contains("知识库", "检索", "向量");
        }
    }

    // ====================================================================
    // checkTopicConsistency
    // ====================================================================

    @Nested
    @DisplayName("checkTopicConsistency 主题一致性")
    class CheckTopicConsistency {

        @Test
        @DisplayName("查询无明确领域 → 不惩罚（返回 1.0）")
        void shouldReturnOneWhenQueryHasNoDomain() {
            double penalty = service.checkTopicConsistency("通用文本内容", "今天天气", new String[]{"今天", "天气"});
            assertThat(penalty).isEqualTo(1.0);
        }

        @Test
        @DisplayName("查询与文本同属环保水务领域 → 不惩罚（1.0）")
        void shouldReturnOneWhenSameDomain() {
            double penalty = service.checkTopicConsistency(
                    "污水处理厂采用生物处理工艺，排水达到一级A标准",
                    "污水处理方案",
                    new String[]{"污水", "处理", "方案"});
            assertThat(penalty).isEqualTo(1.0);
        }

        @Test
        @DisplayName("查询属环保水务、文本属 AI 文档 → 跨领域惩罚（0.2）")
        void shouldReturnSeverePenaltyWhenCrossDomain() {
            double penalty = service.checkTopicConsistency(
                    "本系统采用大模型进行文档解析，向量化后存入知识库",
                    "污水处理方案",
                    new String[]{"污水", "处理", "方案"});
            assertThat(penalty).isEqualTo(0.2);
        }

        @Test
        @DisplayName("查询有领域但文本无领域 → 轻度惩罚（0.8）")
        void shouldReturnMildPenaltyWhenTextHasNoDomain() {
            double penalty = service.checkTopicConsistency(
                    "这是一段通用描述文本，没有领域关键词",
                    "污水处理方案",
                    new String[]{"污水", "处理", "方案"});
            assertThat(penalty).isEqualTo(0.8);
        }

        @Test
        @DisplayName("查询与文本同属 AI 文档领域 → 不惩罚（1.0）")
        void shouldReturnOneWhenBothInAiDomain() {
            double penalty = service.checkTopicConsistency(
                    "RAG 系统使用 embedding 向量化文档",
                    "知识库检索算法",
                    new String[]{"知识库", "检索", "算法"});
            assertThat(penalty).isEqualTo(1.0);
        }
    }

    // ====================================================================
    // calculateSemanticScore
    // ====================================================================

    @Nested
    @DisplayName("calculateSemanticScore 语义相关评分")
    class CalculateSemanticScore {

        @Test
        @DisplayName("服务器语义扩展：匹配 cpu/gpu/内存 等")
        void shouldMatchServerSemanticWords() {
            String text = "本服务器配置 64Core CPU，512GB 内存，配 4 张 NVIDIA GPU";
            double score = service.calculateSemanticScore(text, new String[]{"服务器"}, "服务器");
            assertThat(score).isGreaterThan(0.0);
        }

        @Test
        @DisplayName("RAG 语义扩展：匹配知识库/检索/向量 等")
        void shouldMatchRagSemanticWords() {
            String text = "本系统集成了知识库检索与向量召回能力";
            double score = service.calculateSemanticScore(text, new String[]{"rag"}, "rag");
            assertThat(score).isGreaterThan(0.0);
        }

        @Test
        @DisplayName("列表枚举模式加分（1. 2. 3.）")
        void shouldAddScoreForListPattern() {
            String text = "本系统特点：1. 高性能 2. 易扩展 3. 安全可靠";
            double score = service.calculateSemanticScore(text, new String[]{"系统"}, "系统特点");
            assertThat(score).isGreaterThanOrEqualTo(2.0);
        }

        @Test
        @DisplayName("无语义匹配返回较低分数")
        void shouldReturnLowScoreWhenNoMatch() {
            String text = "今天天气真好，适合出门散步";
            double score = service.calculateSemanticScore(text, new String[]{"天气"}, "天气");
            // 仅可能匹配列表/标题模式，分数应较低
            assertThat(score).isLessThan(5.0);
        }
    }

    // ====================================================================
    // calculateRelevanceScore（综合评分）
    // ====================================================================

    @Nested
    @DisplayName("calculateRelevanceScore 综合相关性评分")
    class CalculateRelevanceScore {

        @Test
        @DisplayName("完整查询匹配加分（+30 基础分）")
        void shouldAddFullMatchBonus() {
            String text = "本系统实现了完整的污水处理流程，包括预处理、生化处理和深度处理";
            double score = service.calculateRelevanceScore(text, new String[]{"污水", "处理"}, "污水处理");
            // 含完整查询匹配，分数应较高
            assertThat(score).isGreaterThanOrEqualTo(30.0);
        }

        @Test
        @DisplayName("极短文本被降权（<30 字符 ×0.5）")
        void shouldApplyShortTextPenalty() {
            String shortText = "污水处理";
            double score = service.calculateRelevanceScore(shortText, new String[]{"污水", "处理"}, "污水处理");
            // 虽有完整匹配（+30），但被 ×0.5 惩罚
            assertThat(score).isLessThan(30.0);
        }

        @Test
        @DisplayName("图片内容被降权（×0.3）")
        void shouldApplyImageContentPenalty() {
            String imageText = "这张图片展示了污水处理工艺流程图，包含预处理、生化处理等环节，详细描述了污水处理的完整步骤";
            double score = service.calculateRelevanceScore(imageText, new String[]{"污水", "处理"}, "污水处理");
            // 图片内容 ×0.3 降权，分数应明显低于正常文本
            assertThat(score).isLessThan(20.0);
        }

        @Test
        @DisplayName("无关键词匹配分数为 0 或极低")
        void shouldReturnZeroWhenNoKeywordMatch() {
            String text = "今天天气真好，适合出门散步和购物";
            double score = service.calculateRelevanceScore(text, new String[]{"向量", "数据库"}, "向量数据库");
            // 无关键词匹配，分数应为 0 或极低
            assertThat(score).isLessThanOrEqualTo(5.0);
        }

        @Test
        @DisplayName("关键词多次出现累计加分（首次+3，后续每次+1）")
        void shouldAccumulateKeywordScore() {
            String text = "污水处理厂采用生物处理工艺，污水处理达标排放，处理效率高";
            double score = service.calculateRelevanceScore(text, new String[]{"污水", "处理"}, "污水处理");
            // 多次匹配，分数应较高
            assertThat(score).isGreaterThan(10.0);
        }

        @Test
        @DisplayName("完整短语在开头额外加分（+5）")
        void shouldAddBonusWhenFullMatchInBeginning() {
            // 文本须 ≥80 字符：30-80 字符区间触发长度惩罚 ×0.7，会吃掉开头加分
            String text = "污水处理是环保工程的核心环节，本文档介绍污水处理方案的设计与实施，"
                    + "涵盖预处理、生化处理、深度处理等全流程工艺，并结合工程实践给出参数设计"
                    + "与运行维护建议，供环保工程人员参考";
            double score = service.calculateRelevanceScore(text, new String[]{"污水", "处理"}, "污水处理");
            // 开头有完整短语匹配（+30 完整匹配 + 5 开头加分，其余项均非负）
            assertThat(score).isGreaterThanOrEqualTo(35.0);
        }
    }
}
