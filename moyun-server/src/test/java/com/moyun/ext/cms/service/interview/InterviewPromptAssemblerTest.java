package com.moyun.ext.cms.service.interview;

import com.moyun.portal.domain.entity.PortalVoiceInterviewQA;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 提示词组装器单测：占位符渲染 / 缺失值 / 历史窗口截断 / 任务指令
 */
class InterviewPromptAssemblerTest {

    private final InterviewPromptAssembler assembler = new InterviewPromptAssembler();

    @Test
    void renderPlaceholders() {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("position", "Java后端");
        placeholders.put("difficulty", "中等");
        String prompt = "岗位{{position}}难度{{difficulty}}简历{{resumeDigest}}";
        String rendered = assembler.renderSystemPrompt(prompt, placeholders);
        assertEquals("岗位Java后端难度中等简历无", rendered);
    }

    @Test
    void renderNullPromptReturnsEmpty() {
        assertEquals("", assembler.renderSystemPrompt(null, Map.of()));
        assertEquals("", assembler.renderSystemPrompt("  ", Map.of()));
    }

    @Test
    void renderKeepsTextWithoutPlaceholders() {
        assertEquals("固定文案", assembler.renderSystemPrompt("固定文案", Map.of()));
    }

    @Test
    void buildHistoryConvertsQaPairs() {
        List<PortalVoiceInterviewQA> qaList = new ArrayList<>();
        qaList.add(qa(1L, "第一题是什么？", "我的回答一"));
        qaList.add(qa(2L, "追问细节？", "补充回答"));

        List<ChatMessage> messages = assembler.buildHistory(qaList, 10);
        assertEquals(4, messages.size());
        assertTrue(messages.get(0) instanceof AiMessage);
        assertTrue(messages.get(1) instanceof UserMessage);
        assertTrue(messages.get(2) instanceof AiMessage);
        assertEquals("追问细节？", ((AiMessage) messages.get(2)).text());
    }

    @Test
    void buildHistorySkipsUnanswered() {
        List<PortalVoiceInterviewQA> qaList = new ArrayList<>();
        qaList.add(qa(1L, "第一题", "回答"));
        qaList.add(qa(2L, "第二题", null));       // 未作答（当前题）
        qaList.add(qa(3L, "第三题", "  "));         // 空白回答

        List<ChatMessage> messages = assembler.buildHistory(qaList, 10);
        assertEquals(2, messages.size());
    }

    @Test
    void buildHistoryWindowTruncates() {
        List<PortalVoiceInterviewQA> qaList = new ArrayList<>();
        for (long i = 1; i <= 10; i++) {
            qaList.add(qa(i, "问题" + i, "回答" + i));
        }
        // 只保留最近 3 轮
        List<ChatMessage> messages = assembler.buildHistory(qaList, 3);
        assertEquals(6, messages.size());
        assertEquals("问题8", ((AiMessage) messages.get(0)).text());
        assertEquals("问题10", ((AiMessage) messages.get(4)).text());
    }

    @Test
    void buildHistoryTruncatesLongContent() {
        String longAnswer = "长".repeat(1000);
        List<PortalVoiceInterviewQA> qaList = List.of(qa(1L, "问题", longAnswer));
        List<ChatMessage> messages = assembler.buildHistory(qaList, 10);
        String userText = ((UserMessage) messages.get(1)).singleText();
        assertTrue(userText.length() < 700, "超长回答应被截断，实际长度=" + userText.length());
    }

    @Test
    void buildTaskDirectiveContainsCoreSections() {
        InterviewPromptAssembler.TaskContext ctx = new InterviewPromptAssembler.TaskContext()
                .setQuestionTitle("请介绍 Redis 持久化")
                .setQuestionAnalysis("考察 RDB/AOF 原理")
                .setTranscript("RDB 是快照")
                .setRoundsDone(2).setTotalPlanned(5).setFollowupDepth(0).setFollowupUsed(1);
        String directive = assembler.buildTaskDirective(ctx);
        assertTrue(directive.contains("Redis 持久化"));
        assertTrue(directive.contains("RDB 是快照"));
        assertTrue(directive.contains("nextAction"));
        assertTrue(directive.contains("2/5"));
        assertTrue(directive.contains("```json"));
        assertTrue(directive.contains("打分参考"));
    }

    private PortalVoiceInterviewQA qa(Long id, String question, String answer) {
        PortalVoiceInterviewQA qa = new PortalVoiceInterviewQA();
        qa.setId(id);
        qa.setQuestion(question);
        qa.setUserAnswer(answer);
        return qa;
    }
}
