package com.moyun.ext.cms.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.ext.ai.service.AgentService;
import com.moyun.ext.ai.service.AiGlobalSwitch;
import com.moyun.ext.ai.service.WorkflowService;
import com.moyun.ext.ai.service.chat.RagRetrievalService;
import com.moyun.ext.aigateway.support.AiSceneJsonClient;
import com.moyun.ext.cms.domain.vo.VoiceInterviewReportVO;
import com.moyun.ext.cms.service.IWrongQuestionService;
import com.moyun.ext.cms.service.interview.InterviewAgentClient;
import com.moyun.ext.cms.service.interview.InterviewChatMemoryService;
import com.moyun.ext.cms.service.interview.ScoringEngine;
import com.moyun.portal.domain.entity.PortalVoiceInterview;
import com.moyun.portal.domain.entity.PortalVoiceInterviewQA;
import com.moyun.portal.mapper.PortalUserMapper;
import com.moyun.portal.mapper.PortalUserResumeMapper;
import com.moyun.portal.mapper.PortalVoiceInterviewEventMapper;
import com.moyun.portal.mapper.PortalVoiceInterviewMapper;
import com.moyun.portal.mapper.PortalVoiceInterviewQAMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 报告聚合「契约」集成测试（v13.56 批次 4 四 / 安全网）。
 *
 * <p><b>为什么写它</b>：批次 1（收编）/批次 2（扩字段）/批次 4（拆分）都在动报告链路，
 * 但此前**没有任何测试覆盖"报告最终产出长什么样"** —— 一旦字段静默丢失（如 V1.3 发现的
 * {@code system_prompt_template} 废弃列问题），编译与既有测试都不会报错。
 * 本测试为后续任何重构建立安全网：<b>钉死报告 JSON 的字段契约</b>。</p>
 *
 * <p><b>可隔离的原因</b>：{@code aggregateAndStoreReport}（187 行的报告聚合核心）
 * 只依赖 4 个实例成员（{@code interviewMapper}/{@code qaMapper}/{@code scoringEngine}/
 * {@code objectMapper}），其余增强链（LLM 复盘、错题本、场景工作流）都**自带 try-catch 降级**
 * —— 故可用纯 mock 环境直接驱动规则路径，验证确定性的那部分契约。</p>
 *
 * <p><b>覆盖点</b>：规则兜底报告的必需字段、逐题点评组装、六维聚合、心态/信号/流畅度汇总、
 * 自介分融合、以及<b>幂等防重</b>（已 analysisStatus=2 不重算）。</p>
 *
 * @author laomao
 */
class InterviewReportContractTest {

    private static final Long INTERVIEW_ID = 700001L;
    private static final Long USER_ID = 800001L;

    private final ObjectMapper mapper = new ObjectMapper();
    private VoiceInterviewServiceImpl service;
    private AtomicReference<PortalVoiceInterview> stored;

    @BeforeEach
    void setUp() {
        service = new VoiceInterviewServiceImpl();

        PortalVoiceInterviewMapper interviewMapper = mock(PortalVoiceInterviewMapper.class);
        PortalVoiceInterviewQAMapper qaMapper = mock(PortalVoiceInterviewQAMapper.class);
        PortalVoiceInterviewEventMapper eventMapper = mock(PortalVoiceInterviewEventMapper.class);

        // 关键：捕获"报告落库"那一次 updateById，用于断言最终产出
        stored = new AtomicReference<>();
        when(interviewMapper.selectById(anyLong())).thenAnswer(inv -> {
            PortalVoiceInterview cur = stored.get();
            return cur != null ? cur : baseInterview();
        });
        when(interviewMapper.updateById(any(PortalVoiceInterview.class))).thenAnswer(inv -> {
            PortalVoiceInterview saved = inv.getArgument(0);
            // 只捕获带 report 的那次（报告落库），其余中间更新忽略
            if (saved.getReport() != null) {
                stored.set(saved);
            }
            return 1;
        });
        when(qaMapper.updateById(any(PortalVoiceInterviewQA.class))).thenReturn(1);
        when(qaMapper.selectList(any())).thenReturn(qaList());

        ReflectionTestUtils.setField(service, "interviewMapper", interviewMapper);
        ReflectionTestUtils.setField(service, "qaMapper", qaMapper);
        ReflectionTestUtils.setField(service, "eventMapper", eventMapper);
        ReflectionTestUtils.setField(service, "objectMapper", mapper);
        ReflectionTestUtils.setField(service, "scoringEngine", new ScoringEngine());
        // 以下增强链依赖一律留空（null）→ 其内部 try-catch 会降级，
        // 这样测的就是**确定性的规则兜底路径**，不受 LLM/DB 影响
        ReflectionTestUtils.setField(service, "wrongQuestionService", (IWrongQuestionService) null);
        ReflectionTestUtils.setField(service, "aiWorkflowService", (WorkflowService) null);
        ReflectionTestUtils.setField(service, "userResumeMapper", (PortalUserResumeMapper) null);
        ReflectionTestUtils.setField(service, "agentClient", (InterviewAgentClient) null);
        ReflectionTestUtils.setField(service, "aiSceneJsonClient", (AiSceneJsonClient) null);
        ReflectionTestUtils.setField(service, "aiGlobalSwitch", (AiGlobalSwitch) null);
        ReflectionTestUtils.setField(service, "memoryService", (InterviewChatMemoryService) null);
        ReflectionTestUtils.setField(service, "ragRetrievalService", (RagRetrievalService) null);
        ReflectionTestUtils.setField(service, "agentService", (AgentService) null);
        ReflectionTestUtils.setField(service, "portalUserMapper", (PortalUserMapper) null);
    }

    // ==================== 用例 ====================

    @Test
    @DisplayName("契约：规则兜底报告必需字段齐备（总分/逐题/六维/概要/建议/亮点薄弱点）")
    void ruleBasedReportHasRequiredFields() throws Exception {
        invokeAggregate();

        VoiceInterviewReportVO report = parseStoredReport();
        assertNotNull(report, "报告必须落库（stored.report 不应为 null）");

        // —— 顶层必需字段 ——
        assertEquals(INTERVIEW_ID, report.getInterviewId());
        assertNotNull(report.getTotalScore(), "总分必须有值（不得为 null）");
        assertNotNull(report.getQuestionReviews(), "逐题点评列表不得为 null");
        assertEquals(3, report.getQuestionReviews().size(), "3 道已作答题应全部进逐题点评");
        assertNotNull(report.getDimensions(), "六维必须存在");
        assertEquals(6, report.getDimensions().size(), "六维应完整（缺一即报告雷达图断链）");
        assertTrue(report.getSummary() != null && !report.getSummary().isBlank(), "概要文案不得为空");
        assertNotNull(report.getImprovementSuggestions(), "改进建议不得为 null");
        assertTrue(!report.getImprovementSuggestions().isEmpty(), "改进建议不得为空（报告不出现空板块）");

        // —— 逐题点评字段（前端折叠展示 / 加入错题本依赖）——
        VoiceInterviewReportVO.QuestionReview r0 = report.getQuestionReviews().get(0);
        assertNotNull(r0.getQuestionIdx(), "题号必须有值");
        assertNotNull(r0.getQuestion(), "题面必须有值");
        assertNotNull(r0.getScore(), "逐题得分必须有值");
        assertNotNull(r0.getQaId(), "qaId 必须有值（否则「加入错题本」按钮失效）");
        assertEquals("这是第一题的作答内容", r0.getUserAnswer(), "原始作答应回填（前端展开查看）");
    }

    @Test
    @DisplayName("契约：六维 key 与前端雷达图对齐（旧 key 断链是历史缺陷）")
    void dimensionsKeysAlignWithFrontend() throws Exception {
        invokeAggregate();
        VoiceInterviewReportVO report = parseStoredReport();

        // 前端 DIMENSION_META 的 key 集合（VoiceInterviewPage.vue）
        List<String> expected = List.of(
                "relevance", "professionalism", "fluency", "interactivity", "confidence", "logic");
        for (String k : expected) {
            assertTrue(report.getDimensions().containsKey(k),
                    "六维缺少 " + k + " —— 前端雷达图该维度会断链（历史缺陷：coverage/length/structure 旧 key）");
        }
    }

    @Test
    @DisplayName("契约：心态趋势/可疑信号/流畅度 从逐题分析汇总（前端 deep-review 依赖）")
    void deepReviewAggregatesFromQaAnalysis() throws Exception {
        invokeAggregate();
        VoiceInterviewReportVO report = parseStoredReport();

        assertNotNull(report.getSentimentTrend(), "心态趋势不得为 null");
        assertEquals(3, report.getSentimentTrend().size(), "3 题的 sentiment 应全部汇总");
        assertEquals("nervous", report.getSentimentTrend().get(0));

        assertNotNull(report.getRedFlags(), "可疑信号列表不得为 null");
        assertTrue(report.getRedFlags().contains("回答与简历不符"), "逐题 redFlags 应被汇总去重");

        assertNotNull(report.getFluencyAvg(), "流畅度均分不得为 null");
        assertEquals(70, report.getFluencyAvg(), "(60+70+80)/3 = 70");
    }

    @Test
    @DisplayName("幂等：analysisStatus=2 时直接返回，不重算也不落库（并发触发保护）")
    void aggregateIsIdempotentWhenAlreadyDone() throws Exception {
        // 守卫读的是"自己 selectById 拿到的 fresh"，故探测对象必须是 analysisStatus=2
        PortalVoiceInterview done = baseInterview();
        done.setAnalysisStatus(2);
        stored.set(done);

        invokeAggregate(done);

        // 守卫语义：直接 return ⇒ 不会发生"报告落库"这一次 update
        assertTrue(done.getReport() == null,
                "analysisStatus=2 时应直接返回：不得重算、不得覆盖已有报告（幂等防重被破坏）");
    }

    @Test
    @DisplayName("降级：LLM 复盘不可用时仍产出完整规则报告（链路永不失败）")
    void degradesGracefullyWhenLlmUnavailable() throws Exception {
        // 本测试环境 agentClient / sysConfigService / aiSceneJsonClient 全为 null，
        // enhanceReportByAgent 必然内部抛错并被 catch —— 报告仍须完整产出
        invokeAggregate();

        VoiceInterviewReportVO report = parseStoredReport();
        assertNotNull(report, "LLM 不可用时报告仍必须落库（规则兜底）");
        assertNotNull(report.getTotalScore());
        assertTrue(report.getTotalScore() > 0, "规则均分应 > 0");
        assertEquals(3, report.getQuestionReviews().size(), "逐题点评不因 LLM 不可用而丢失");
    }

    // ==================== 夹具 ====================

    /** 3 道已作答题：分数 85/60/55，含 llm_analysis_json（心态/信号/流畅度）与草稿分 */
    private List<PortalVoiceInterviewQA> qaList() {
        List<PortalVoiceInterviewQA> list = new ArrayList<>();
        list.add(qa(1L, 0, "第一题：请自我介绍", "这是第一题的作答内容", 85, 85,
                "{\"sentiment\":{\"state\":\"nervous\"},\"redFlags\":[\"回答与简历不符\"],"
                        + "\"fluencyAssessment\":{\"score\":60}}"));
        list.add(qa(2L, 1, "第二题：讲讲并发", "这是第二题的作答内容", 60, 60,
                "{\"sentiment\":{\"state\":\"calm\"},\"redFlags\":[],"
                        + "\"fluencyAssessment\":{\"score\":70}}"));
        list.add(qa(3L, 2, "第三题：JVM 调优", "这是第三题的作答内容", 55, 55,
                "{\"sentiment\":{\"state\":\"confident\"},\"redFlags\":[],"
                        + "\"fluencyAssessment\":{\"score\":80}}"));
        return list;
    }

    private PortalVoiceInterviewQA qa(Long id, int idx, String question, String answer,
                                      int score, Integer scoreDraft, String analysisJson) {
        PortalVoiceInterviewQA qa = new PortalVoiceInterviewQA();
        qa.setId(id);
        qa.setInterviewId(INTERVIEW_ID);
        qa.setQuestionIdx(idx);
        qa.setQuestion(question);
        qa.setUserAnswer(answer);
        qa.setScore(score);
        qa.setScoreDraft(scoreDraft);
        qa.setAiFeedback("第 " + idx + " 题点评");
        qa.setLlmAnalysisJson(analysisJson);
        // 六维聚合的唯一来源（缺失会导致 report.dimensions 为空 → 前端雷达图断链）
        qa.setRuleDimensionsJson("{\"relevance\":80,\"professionalism\":70,\"fluency\":60,"
                + "\"interactivity\":75,\"confidence\":65,\"logic\":85}");
        qa.setParentQaId(null);
        qa.setAnalysisStatus(2);
        qa.setCreateTime(LocalDateTime.now());
        return qa;
    }

    private PortalVoiceInterview baseInterview() {
        PortalVoiceInterview it = new PortalVoiceInterview();
        it.setId(INTERVIEW_ID);
        it.setUserId(USER_ID);
        it.setPosition("Java 后端");
        it.setDifficulty("medium");
        it.setStatus("finished");
        it.setAnalysisStatus(1);
        it.setAnalysisProgress(0);
        it.setScore(70);
        it.setResumeId(null);
        it.setAgentId(null);
        // totalQa 被 buildSummary 使用（null 会 NPE）；currentIdx 供后续步骤使用
        it.setTotalQa(3);
        it.setCurrentIdx(2);
        // configJson 含 levelEstimate，覆盖"预热画像 → 概要文案追加定级"这一分支
        it.setConfigJson("{\"levelEstimate\":\"mid\"}");
        return it;
    }

    /** 默认夹具驱动聚合 */
    private void invokeAggregate() throws Exception {
        invokeAggregate(baseInterview());
    }

    /**
     * 反射调用私有的报告聚合方法（契约测试需直取最终产出，无公开入口）。
     *
     * <p>注意：{@code selectById} 由 {@code stored} 兜底返回，故当需要特定
     * {@code analysisStatus} 时，必须**先 set 进 stored**，让守卫看到同一对象。</p>
     */
    private void invokeAggregate(PortalVoiceInterview interview) throws Exception {
        Method m = VoiceInterviewServiceImpl.class
                .getDeclaredMethod("aggregateAndStoreReport", PortalVoiceInterview.class);
        m.setAccessible(true);
        if (stored.get() == null) {
            stored.set(interview);
        }
        // 清空"报告落库"捕获位：用一个不含 report 的副本占位，
        // 这样若发生落库，断言处必然看到非 null；未落库则为 null
        PortalVoiceInterview probe = interview;
        if (probe.getReport() != null) {
            probe = baseInterview();
            probe.setAnalysisStatus(interview.getAnalysisStatus());
        }
        m.invoke(service, probe);
    }

    /** 解析落库的报告 JSON */
    private VoiceInterviewReportVO parseStoredReport() throws Exception {
        PortalVoiceInterview saved = stored.get();
        assertNotNull(saved, "报告未落库：interviewMapper.updateById 从未收到带 report 的对象");
        assertNotNull(saved.getReport(), "报告 JSON 为空");
        JsonNode node = mapper.readTree(saved.getReport());
        return mapper.treeToValue(node, VoiceInterviewReportVO.class);
    }
}
