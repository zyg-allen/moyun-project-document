package com.moyun.ext.cms.service.interview;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * LLM 输出解析器单测：围栏 / 裸 JSON / markdown 包裹 / 纯文本 / 垃圾输入
 */
class InterviewAnalysisParserTest {

    private InterviewAnalysisParser parser;

    @BeforeEach
    void setUp() {
        parser = new InterviewAnalysisParser(new ObjectMapper());
    }

    @Test
    void parseFencedJsonWithReply() {
        String raw = "嗯，你提到了索引优化，但没说清楚具体场景。\n```json\n"
                + "{\"score\":72,\"feedback\":\"回答有框架\",\"flaws\":[\"缺少场景说明\"],"
                + "\"redFlags\":[\"背诵痕迹明显\"],"
                + "\"sentiment\":{\"state\":\"nervous\",\"note\":\"多次停顿\"},"
                + "\"fluencyAssessment\":{\"score\":60,\"comment\":\"口头禅较多\"},"
                + "\"completeness\":{\"covered\":[\"索引原理\"],\"missing\":[\"失效场景\"]},"
                + "\"level\":\"mid\",\"followupWorth\":true,"
                + "\"nextAction\":\"deepen\",\"nextQuestion\":\"你说的是哪种场景？\","
                + "\"candidateId\":3,\"transition\":\"\",\"guidance\":\"\"}\n```";
        InterviewTurnResult r = parser.parse(raw);
        assertNotNull(r);
        assertEquals("嗯，你提到了索引优化，但没说清楚具体场景。", r.getReply());
        assertEquals(72, r.getScore());
        assertEquals(List.of("缺少场景说明"), r.getFlaws());
        assertEquals(List.of("背诵痕迹明显"), r.getRedFlags());
        assertEquals("nervous", r.getSentiment().getState());
        assertEquals(60, r.getFluencyAssessment().getScore());
        assertEquals(List.of("索引原理"), r.getCompleteness().getCovered());
        assertEquals(List.of("失效场景"), r.getCompleteness().getMissing());
        assertEquals("deepen", r.getNextAction());
        assertEquals(3L, r.getCandidateId());
    }

    @Test
    void parseJsonWithoutLanguageTag() {
        String raw = "好的回答。\n```\n{\"score\":85,\"nextAction\":\"change_topic\"}\n```";
        InterviewTurnResult r = parser.parse(raw);
        assertNotNull(r);
        assertEquals(85, r.getScore());
        assertEquals("change_topic", r.getNextAction());
    }

    @Test
    void parseBareJson() {
        String raw = "{\"score\":50,\"feedback\":\"一般\",\"nextAction\":\"wrap_up\"}";
        InterviewTurnResult r = parser.parse(raw);
        assertNotNull(r);
        assertEquals(50, r.getScore());
        assertEquals("wrap_up", r.getNextAction());
    }

    @Test
    void parseMarkdownWrappedJson() {
        String raw = "**点评**：不错\n```json\n{\"score\":90}\n```\n以上。";
        InterviewTurnResult r = parser.parse(raw);
        assertNotNull(r);
        assertEquals(90, r.getScore());
        assertTrue(r.getReply().contains("不错"));
    }

    @Test
    void parseMissingScoreKeepsNull() {
        String raw = "```\n{\"feedback\":\"无分\",\"nextAction\":\"change_topic\"}\n```";
        InterviewTurnResult r = parser.parse(raw);
        assertNotNull(r);
        assertNull(r.getScore());
        assertEquals("change_topic", r.getNextAction());
    }

    @Test
    void parsePlainTextOnlyYieldsReply() {
        String raw = "你的回答整体不错，我们继续下一题。";
        InterviewTurnResult r = parser.parse(raw);
        assertNotNull(r);
        assertEquals(raw, r.getReply());
        assertNull(r.getScore());
        assertEquals("", r.getNextAction());
    }

    @Test
    void parseGarbageReturnsNullOrReply() {
        assertNull(parser.parse(null));
        assertNull(parser.parse("   "));
        InterviewTurnResult r = parser.parse("}}}}{{{{ not json at all");
        assertNotNull(r);
        assertNull(r.getScore());
    }

    @Test
    void scoreClampedTo100() {
        String raw = "```\n{\"score\":150}\n```";
        InterviewTurnResult r = parser.parse(raw);
        assertNotNull(r);
        assertEquals(100, r.getScore());
    }

    @Test
    void brokenJsonInsideFenceStillExtractsReply() {
        String raw = "这是我的回应。\n```json\n{\"score\": 60, ###broken\n```";
        InterviewTurnResult r = parser.parse(raw);
        assertNotNull(r);
        assertEquals("这是我的回应。", r.getReply());
        assertNull(r.getScore());
    }
}
