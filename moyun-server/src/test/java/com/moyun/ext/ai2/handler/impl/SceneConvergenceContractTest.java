package com.moyun.ext.ai2.handler.impl;

import com.moyun.ext.ai2.model.AiExecuteRequest;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * P0-3b 业务收口契约单测（v11.58）
 *
 * <p>锁定 Handler 的 task+context 子任务验证路由：业务侧（简历解析/优化/出题关键词）
 * 经统一网关调用时，参数契约错误必须在 validate 阶段快速失败（而非进入 LLM 调用后解析失败）。</p>
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

    // ==================== resume_optimize：双子任务契约 ====================

    @Test
    void resumeOptimize_subTask_requiresContext() {
        ResumeOptimizeHandler handler = new ResumeOptimizeHandler();
        assertThrows(IllegalArgumentException.class,
                () -> handler.validate(request(Map.of("task", "advice"))));
    }

    @Test
    void resumeOptimize_subTask_allSupportedTasks() {
        ResumeOptimizeHandler handler = new ResumeOptimizeHandler();
        for (String task : new String[]{"advice", "job_match", "field_assist", "draft_empty", "deep_optimize"}) {
            assertDoesNotThrow(() ->
                    handler.validate(request(Map.of("task", task, "context", "业务数据"))));
        }
    }

    @Test
    void resumeOptimize_unknownTask_rejected() {
        ResumeOptimizeHandler handler = new ResumeOptimizeHandler();
        assertThrows(IllegalArgumentException.class,
                () -> handler.validate(request(Map.of("task", "hacked_task", "context", "x"))));
    }

    @Test
    void resumeOptimize_genericMode_requiresResumeText() {
        ResumeOptimizeHandler handler = new ResumeOptimizeHandler();
        // 通用模式（task 缺省）：resumeText 必填
        assertThrows(IllegalArgumentException.class,
                () -> handler.validate(request(Map.of("targetPosition", "Java开发"))));
        assertDoesNotThrow(() ->
                handler.validate(request(Map.of("resumeText", "简历内容"))));
    }

    // ==================== question_generate：jd_keywords 子任务契约 ====================

    @Test
    void questionGenerate_jdKeywords_requiresContext() {
        QuestionGenerateHandler handler = new QuestionGenerateHandler();
        assertThrows(IllegalArgumentException.class,
                () -> handler.validate(request(Map.of("task", "jd_keywords"))));
        assertDoesNotThrow(() ->
                handler.validate(request(Map.of("task", "jd_keywords", "context", "岗位职责..."))));
    }

    @Test
    void questionGenerate_defaultMode_requiresPosition() {
        QuestionGenerateHandler handler = new QuestionGenerateHandler();
        assertThrows(IllegalArgumentException.class,
                () -> handler.validate(request(Map.of("skills", "Java,MySQL"))));
        assertDoesNotThrow(() ->
                handler.validate(request(Map.of("position", "Java开发", "count", 5))));
    }

    // ==================== resume_parse：text 必填 ====================

    @Test
    void resumeParse_requiresText() {
        ResumeParseHandler handler = new ResumeParseHandler();
        assertThrows(IllegalArgumentException.class,
                () -> handler.validate(request(Map.of("other", "x"))));
        assertDoesNotThrow(() ->
                handler.validate(request(Map.of("text", "张三的简历..."))));
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
