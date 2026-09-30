package com.moyun.ext.cms.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.ext.cms.domain.vo.VoiceInterviewReportVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link InterviewReportFormatter} 单元测试（v13.55 批次 4 四 / 第②步）。
 *
 * <p>这批方法原先埋在 Service 内、只能通过完整报告链路间接覆盖。抽成纯函数后可直接单测，
 * 重点覆盖**上限裁剪**与**空值降级** —— 前者决定报告板块不会爆版，后者决定报告永不空白。</p>
 *
 * @author laomao
 */
class InterviewReportFormatterTest {

    private final ObjectMapper mapper = new ObjectMapper();

    // ==================== parsePointViews ====================

    @Test
    @DisplayName("parsePointViews：结构化视图回填 + 标题列表双写（旧字段兼容）")
    void parsePointViewsDualWrite() throws Exception {
        JsonNode arr = mapper.readTree("""
                [
                  {"title":"亮点A","detail":"细节A"},
                  {"title":"亮点B","detail":"细节B"}
                ]
                """);
        List<List<VoiceInterviewReportVO.PointView>> captured = new ArrayList<>();
        List<String> titles = InterviewReportFormatter.parsePointViews(arr, captured::add);

        assertEquals(List.of("亮点A", "亮点B"), titles, "标题列表用于旧字段双写");
        assertEquals(1, captured.size(), "结构化视图应通过 setter 回填一次");
        assertEquals(2, captured.get(0).size());
        assertEquals("细节A", captured.get(0).get(0).getDetail());
    }

    @Test
    @DisplayName("parsePointViews：上限 4 条裁剪；缺 title 的条目跳过；空集合不触发 setter")
    void parsePointViewsLimitsAndSkips() throws Exception {
        JsonNode arr = mapper.readTree("""
                [
                  {"title":"A"},{"title":"B"},{"title":"C"},
                  {"title":"D"},{"title":"E"},{"title":"F"}
                ]
                """);
        List<List<VoiceInterviewReportVO.PointView>> captured = new ArrayList<>();
        List<String> titles = InterviewReportFormatter.parsePointViews(arr, captured::add);
        assertEquals(4, titles.size(), "上限 4 条（与提示词 2-4 条口径一致）");
        assertEquals(List.of("A", "B", "C", "D"), titles);

        // 缺 title / 空 title 的条目必须跳过
        JsonNode withBlank = mapper.readTree("""
                [{"detail":"无标题"},{"title":"   "},{"title":"有效"}]
                """);
        List<List<VoiceInterviewReportVO.PointView>> cap2 = new ArrayList<>();
        List<String> t2 = InterviewReportFormatter.parsePointViews(withBlank, cap2::add);
        assertEquals(List.of("有效"), t2);

        // 全部无效 → 不触发 setter（避免把空视图写进报告）
        JsonNode allBlank = mapper.readTree("[{\"detail\":\"x\"}]");
        List<List<VoiceInterviewReportVO.PointView>> cap3 = new ArrayList<>();
        assertTrue(InterviewReportFormatter.parsePointViews(allBlank, cap3::add).isEmpty());
        assertTrue(cap3.isEmpty(), "无有效条目时不应回填结构化视图");

        // null / 非数组 → 空列表，且不触发 setter
        List<List<VoiceInterviewReportVO.PointView>> cap4 = new ArrayList<>();
        assertTrue(InterviewReportFormatter.parsePointViews(null, cap4::add).isEmpty());
        assertTrue(InterviewReportFormatter.parsePointViews(mapper.readTree("{}"), cap4::add).isEmpty());
        assertTrue(cap4.isEmpty());
    }

    // ==================== buildImprovementSuggestions ====================

    @Test
    @DisplayName("buildImprovementSuggestions：薄弱点≤3 条 + 自介不足补足到 5 条")
    void buildSuggestionsFromWeakPointsAndIntro() {
        VoiceInterviewReportVO.IntroScoreView intro = new VoiceInterviewReportVO.IntroScoreView();
        intro.setWeaknesses(List.of("结构松散", "缺少数据"));

        List<String> s = InterviewReportFormatter.buildImprovementSuggestions(
                List.of("并发", "JVM", "MySQL", "Redis"), intro);

        // 薄弱点最多 3 条
        assertTrue(s.get(0).contains("并发"));
        assertTrue(s.get(1).contains("JVM"));
        assertTrue(s.get(2).contains("MySQL"));
        assertFalse(s.stream().anyMatch(x -> x.contains("Redis")), "第 4 个薄弱点应被上限裁掉");
        // 自介不足补足到 5 条
        assertEquals(5, s.size());
        assertTrue(s.get(3).contains("自我介绍改进：结构松散"));
        assertTrue(s.get(4).contains("自我介绍改进：缺少数据"));
    }

    @Test
    @DisplayName("buildImprovementSuggestions：两者皆无时给正向文案 —— 保证建议区永不空白")
    void buildSuggestionsNeverEmpty() {
        assertFalse(InterviewReportFormatter.buildImprovementSuggestions(null, null).isEmpty());
        assertFalse(InterviewReportFormatter.buildImprovementSuggestions(List.of(), null).isEmpty());
        assertEquals(1, InterviewReportFormatter.buildImprovementSuggestions(List.of(), null).size());
        // 自介 weaknesses 为空列表同样不炸
        VoiceInterviewReportVO.IntroScoreView intro = new VoiceInterviewReportVO.IntroScoreView();
        intro.setWeaknesses(List.of());
        assertFalse(InterviewReportFormatter.buildImprovementSuggestions(null, intro).isEmpty());
        // 空串不足项应被过滤
        // 注意口径：迁移自原实现，判空用 StringUtils.isNotEmpty（**不 trim**）——
        // 故 ""（真空串）会被过滤，而 "  "（纯空格）算有效项。
        // 这是既有行为（属小瑕疵：会产出「自我介绍改进：  」这类无意义条目），
        // 本步拆分**只搬不改**，故如实断言既有行为，不擅自修改报告产出。
        VoiceInterviewReportVO.IntroScoreView blank = new VoiceInterviewReportVO.IntroScoreView();
        blank.setWeaknesses(java.util.Arrays.asList("", "  ", "有效项"));
        List<String> s = InterviewReportFormatter.buildImprovementSuggestions(null, blank);
        assertEquals(2, s.size(), "真空串被过滤；纯空格按既有口径算有效项");
        assertTrue(s.stream().anyMatch(x -> x.contains("有效项")), "有效项必须被保留");
    }

    // ==================== buildSummary ====================

    @Test
    @DisplayName("buildSummary：三档文案边界（80 / 60）")
    void buildSummaryTiers() {
        assertTrue(InterviewReportFormatter.buildSummary(10, 8, 80).contains("优秀"));
        assertTrue(InterviewReportFormatter.buildSummary(10, 8, 95).contains("优秀"));
        assertTrue(InterviewReportFormatter.buildSummary(10, 8, 79).contains("良好"));
        assertTrue(InterviewReportFormatter.buildSummary(10, 8, 60).contains("良好"));
        assertTrue(InterviewReportFormatter.buildSummary(10, 8, 59).contains("一般"));
        assertTrue(InterviewReportFormatter.buildSummary(10, 8, 0).contains("一般"));

        String s = InterviewReportFormatter.buildSummary(10, 7, 85);
        assertTrue(s.contains("共 10 题"), "应含题目总数");
        assertTrue(s.contains("作答 7 题"), "应含作答题数");
    }

    // ==================== buildSuggestion ====================

    @Test
    @DisplayName("buildSuggestion：有薄弱点列出、无薄弱点给正向文案；null 安全")
    void buildSuggestionWorks() {
        assertTrue(InterviewReportFormatter.buildSuggestion(50, List.of("并发", "JVM"))
                .contains("并发、JVM"), "多个薄弱点应以顿号连接");

        assertEquals("继续保持，挑战更高难度的题目。",
                InterviewReportFormatter.buildSuggestion(90, List.of()));
        assertNotNull(InterviewReportFormatter.buildSuggestion(90, null),
                "weakPoints 为 null 不得 NPE");
        assertEquals("继续保持，挑战更高难度的题目。",
                InterviewReportFormatter.buildSuggestion(90, null));
    }
}
