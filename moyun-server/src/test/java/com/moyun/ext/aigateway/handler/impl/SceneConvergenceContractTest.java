package com.moyun.ext.aigateway.handler.impl;

import com.moyun.ext.aigateway.model.AiExecuteRequest;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * P0-3b 业务收口契约单测（v11.58）
 *
 * <p>锁定 VoiceInterviewHandler 的 task+context 子任务验证路由（2B.5 前仍在）：
 * 业务侧经统一网关调用时，参数契约错误必须在 validate 阶段快速失败（而非进入
 * LLM 调用后解析失败）。resume/question 族契约随 2B.3 配置驱动迁移删除——
 * DefaultSceneExecutor 的输入契约（input/userInput 至少其一非空）由
 * OutputParserWiringTest 覆盖。</p>
 *
 * @author laomao
 * @since 2026-09-11
 */
class SceneConvergenceContractTest {

    private static AiExecuteRequest request(Map<String, Object> input) {
        AiExecuteRequest request = new AiExecuteRequest();
        request.setInput(input != null ? input : new HashMap<>());
        return request;
    }

    // ==================== voice_interview：四子任务契约（v11.58 P0-3c） ====================

    @Test
    void voiceInterview_answerAnalysis_requiresContextAndTranscript() {
        VoiceInterviewHandler handler = new VoiceInterviewHandler();
        // 缺 transcript
        assertThrows(IllegalArgumentException.class,
                () -> handler.validate(request(Map.of("task", "answer_analysis", "context", "面试官人设"))));
        // 缺 context
        assertThrows(IllegalArgumentException.class,
                () -> handler.validate(request(Map.of("task", "answer_analysis", "transcript", "候选人回答"))));
        assertDoesNotThrow(() -> handler.validate(request(Map.of(
                "task", "answer_analysis",
                "context", "面试官人设",
                "transcript", "候选人的语音转写回答"))));
    }

    @Test
    void voiceInterview_candidateAsk_requiresContextAndTranscript() {
        VoiceInterviewHandler handler = new VoiceInterviewHandler();
        assertThrows(IllegalArgumentException.class,
                () -> handler.validate(request(Map.of("task", "candidate_ask", "context", "面试官人设"))));
        assertDoesNotThrow(() -> handler.validate(request(Map.of(
                "task", "candidate_ask",
                "context", "面试官人设",
                "transcript", "候选人想了解团队技术栈"))));
    }

    @Test
    void voiceInterview_knowledgeDesc_requiresContext() {
        VoiceInterviewHandler handler = new VoiceInterviewHandler();
        assertThrows(IllegalArgumentException.class,
                () -> handler.validate(request(Map.of("task", "knowledge_desc"))));
        assertDoesNotThrow(() -> handler.validate(request(Map.of(
                "task", "knowledge_desc",
                "context", "面试岗位：Java开发\n知识点：Spring、MySQL"))));
    }

    @Test
    void voiceInterview_speakText_requiresContext() {
        VoiceInterviewHandler handler = new VoiceInterviewHandler();
        // 缺 context
        assertThrows(IllegalArgumentException.class,
                () -> handler.validate(request(Map.of("task", "speak_text", "transcript", "轮次数据"))));
        // transcript 可选（context 必填）
        assertDoesNotThrow(() -> handler.validate(request(Map.of(
                "task", "speak_text", "context", "面试官人设"))));
    }

    @Test
    void voiceInterview_selfIntro_requiresTranscript() {
        VoiceInterviewHandler handler = new VoiceInterviewHandler();
        // 缺 transcript
        assertThrows(IllegalArgumentException.class,
                () -> handler.validate(request(Map.of("task", "self_intro", "context", "Java开发"))));
        // context（岗位）可空
        assertDoesNotThrow(() -> handler.validate(request(Map.of("task", "self_intro", "transcript", "面试官好..."))));
    }

    @Test
    void voiceInterview_unknownTask_rejected() {
        VoiceInterviewHandler handler = new VoiceInterviewHandler();
        assertThrows(IllegalArgumentException.class,
                () -> handler.validate(request(Map.of("task", "hacked_task", "context", "x"))));
    }

    @Test
    void voiceInterview_defaultMode_requiresQuestion() {
        VoiceInterviewHandler handler = new VoiceInterviewHandler();
        assertThrows(IllegalArgumentException.class,
                () -> handler.validate(request(Map.of("position", "Java开发"))));
        assertDoesNotThrow(() -> handler.validate(request(Map.of("question", "谈谈 JVM 内存模型"))));
    }
}
