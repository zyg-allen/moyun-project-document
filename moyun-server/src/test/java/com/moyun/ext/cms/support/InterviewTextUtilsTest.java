package com.moyun.ext.cms.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.ext.cms.domain.vo.VoiceInterviewReportVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link InterviewTextUtils} 单元测试（v13.54 批次 4 四 / 第①步）。
 *
 * <p>这批方法原先埋在 2943 行的 {@code VoiceInterviewServiceImpl} 里、只能通过
 * 大 Service 间接覆盖。抽成纯函数后可直接单测 —— 这也是"抽纯函数优先"的收益之一：
 * <b>同时提升可测性</b>。</p>
 *
 * <p>重点覆盖**边界与降级**：入参为 null/空、非法 JSON、超限条目 —— 这些在面试链路里
 * 都来自"LLM 输出不可信"这一根因，是最容易出问题的地方。</p>
 *
 * @author laomao
 */
class InterviewTextUtilsTest {

    private final ObjectMapper mapper = new ObjectMapper();

    // ==================== clamp ====================

    @Test
    @DisplayName("clamp：区间内原样返回，越界钳制到边界")
    void clampWorks() {
        assertEquals(50, InterviewTextUtils.clamp(50, 0, 100));
        assertEquals(0, InterviewTextUtils.clamp(-10, 0, 100));
        assertEquals(100, InterviewTextUtils.clamp(999, 0, 100));
        assertEquals(0, InterviewTextUtils.clamp(0, 0, 100));
        assertEquals(100, InterviewTextUtils.clamp(100, 0, 100));
    }

    // ==================== truncateText ====================

    @Test
    @DisplayName("truncateText：压缩空白 + 超长加省略号；null 返回空串（非 null）")
    void truncateTextWorks() {
        // null / 空 → 空串（调用方无需判空，避免 NPE）
        assertEquals("", InterviewTextUtils.truncateText(null, 10));
        assertEquals("", InterviewTextUtils.truncateText("", 10));

        // 空白压缩（换行/制表/多空格 → 单空格）并 trim
        assertEquals("a b c", InterviewTextUtils.truncateText("  a \n\t b   c  ", 100));

        // 超长截断并加省略号
        String longText = "一二三四五六七八九十";
        assertEquals("一二三四五…", InterviewTextUtils.truncateText(longText, 5));

        // 刚好等于上限：不截断、不加省略号
        assertEquals("一二三四五", InterviewTextUtils.truncateText("一二三四五", 5));
    }

    // ==================== formatSkills ====================

    @Test
    @DisplayName("formatSkills：对象/数组/普通文本 三种形态；非法 JSON 原样返回不抛")
    void formatSkillsWorks() {
        // 对象形态：{"Java":{"level":"了解"}}
        assertEquals("Java·了解 / Python·精通",
                InterviewTextUtils.formatSkills(mapper,
                        "{\"Java\":{\"level\":\"了解\"},\"Python\":{\"level\":\"精通\"}}"));

        // 对象形态但值是纯字符串：{"Java":"了解"}
        assertEquals("Java·了解", InterviewTextUtils.formatSkills(mapper, "{\"Java\":\"了解\"}"));

        // 无 level：只出名称
        assertEquals("Java", InterviewTextUtils.formatSkills(mapper, "{\"Java\":{}}"));

        // 非 JSON 文本：原样返回（兜底存量/异构数据）
        assertEquals("Java, Python", InterviewTextUtils.formatSkills(mapper, "Java, Python"));

        // 空 / null：原样返回
        assertNull(InterviewTextUtils.formatSkills(mapper, null));
        assertEquals("", InterviewTextUtils.formatSkills(mapper, ""));

        // 以 { 开头但**非法 JSON**：必须原样返回，绝不抛
        String broken = "{not a json";
        assertEquals(broken, InterviewTextUtils.formatSkills(mapper, broken));
    }

    // ==================== extractJsonObject ====================

    @Test
    @DisplayName("extractJsonObject：容错提取（含 markdown 围栏/前后杂文本）；无法解析返回 null")
    void extractJsonObjectWorks() {
        // 裸 JSON
        JsonNode a = InterviewTextUtils.extractJsonObject(mapper, "{\"a\":1}");
        assertNotNull(a);
        assertEquals(1, a.path("a").asInt());

        // markdown 围栏包裹（LLM 常见输出）
        JsonNode b = InterviewTextUtils.extractJsonObject(mapper, "```json\n{\"b\":2}\n```");
        assertNotNull(b, "应能剥离 markdown 围栏");
        assertEquals(2, b.path("b").asInt());

        // 前后有解释文字
        JsonNode c = InterviewTextUtils.extractJsonObject(mapper, "好的，结果如下：{\"c\":3} 以上。");
        assertNotNull(c, "应能跳过前后杂文本");
        assertEquals(3, c.path("c").asInt());

        // 完全无法解析 → null（调用方据此走降级分支）
        assertNull(InterviewTextUtils.extractJsonObject(mapper, "完全没有 JSON"));
        assertNull(InterviewTextUtils.extractJsonObject(mapper, null));
    }

    // ==================== parsePredictedQuestions ====================

    @Test
    @DisplayName("parsePredictedQuestions：解析字段 + 上限 6 条兜底 + 空条目跳过")
    void parsePredictedQuestionsWorks() throws Exception {
        JsonNode arr = mapper.readTree("""
                [
                  {"question":"Q1","briefAnswer":"A1","analysis":"why1","knowledgePoint":"考点1",
                   "askedThisRound":true,"askedScore":88},
                  {"question":"Q2","briefAnswer":"A2","analysis":"why2","knowledgePoint":"考点2",
                   "askedThisRound":false},
                  {"question":"   "},
                  {"briefAnswer":"无问题文本的条目应被跳过"}
                ]
                """);
        List<VoiceInterviewReportVO.PredictedQuestionView> list =
                InterviewTextUtils.parsePredictedQuestions(arr);
        assertEquals(2, list.size(), "空 question 的条目必须被跳过");

        VoiceInterviewReportVO.PredictedQuestionView q1 = list.get(0);
        assertEquals("Q1", q1.getQuestion());
        assertEquals("A1", q1.getBriefAnswer());
        assertEquals("why1", q1.getAnalysis());
        assertEquals("考点1", q1.getKnowledgePoint());
        assertEquals(Boolean.TRUE, q1.getAskedThisRound());
        assertEquals(88, q1.getAskedScore());

        VoiceInterviewReportVO.PredictedQuestionView q2 = list.get(1);
        assertEquals(Boolean.FALSE, q2.getAskedThisRound());
        assertNull(q2.getAskedScore(), "未问到的条目不应有得分");
    }

    @Test
    @DisplayName("parsePredictedQuestions：上限 6 条（提示词已约束，代码再兜底）")
    void parsePredictedQuestionsCapsAtSix() throws Exception {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < 12; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"question\":\"Q").append(i).append("\"}");
        }
        sb.append(']');
        List<VoiceInterviewReportVO.PredictedQuestionView> list =
                InterviewTextUtils.parsePredictedQuestions(mapper.readTree(sb.toString()));
        assertEquals(InterviewTextUtils.MAX_PREDICTED_QUESTIONS, list.size());
        assertEquals(6, list.size());
    }

    @Test
    @DisplayName("parsePredictedQuestions：缺失/非数组/元素非对象 一律安全降级为空列表（永不为 null）")
    void parsePredictedQuestionsDegradesSafely() throws Exception {
        // 解析失败时前端据此整 tab 隐藏 → 必须返回空列表而非 null，也不得抛异常
        assertNotNull(InterviewTextUtils.parsePredictedQuestions(null));
        assertTrue(InterviewTextUtils.parsePredictedQuestions(null).isEmpty());
        assertTrue(InterviewTextUtils.parsePredictedQuestions(mapper.readTree("{}")).isEmpty());
        assertTrue(InterviewTextUtils.parsePredictedQuestions(mapper.readTree("\"str\"")).isEmpty());
        assertTrue(InterviewTextUtils.parsePredictedQuestions(mapper.readTree("[]")).isEmpty());
        // 数组里混入非对象元素
        assertTrue(InterviewTextUtils.parsePredictedQuestions(mapper.readTree("[1,\"a\",null]")).isEmpty());
    }
}
