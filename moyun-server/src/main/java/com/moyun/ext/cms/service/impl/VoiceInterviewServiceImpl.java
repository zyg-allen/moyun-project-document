package com.moyun.ext.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.core.base.page.PageDomain;
import com.moyun.ext.ai.entity.Agent;
import com.moyun.ext.cms.domain.vo.HintVO;
import com.moyun.ext.cms.domain.vo.VoiceInterviewQaVO;
import com.moyun.ext.cms.domain.vo.VoiceInterviewReportVO;
import com.moyun.ext.cms.domain.vo.VoiceInterviewVO;
import com.moyun.ext.cms.domain.vo.VoiceStartConfig;

import com.moyun.ext.cms.service.IVoiceInterviewService;
import com.moyun.ext.ai.dto.AiSceneBinding;
import com.moyun.ext.cms.service.IPortalInterviewConfigService;
import com.moyun.portal.domain.entity.PortalInterviewConfig;
import com.moyun.ext.cms.service.interview.InterviewPhase;
import com.moyun.ext.cms.service.interview.ScoringEngine;
import com.moyun.ext.cms.service.interview.InterviewAgentClient;
import com.moyun.ext.cms.service.interview.InterviewTurnResult;
import com.moyun.portal.domain.entity.PortalInterviewQuestion;
import com.moyun.portal.domain.entity.PortalUserResume;
import com.moyun.portal.domain.entity.PortalVoiceInterview;
import com.moyun.portal.domain.entity.PortalVoiceInterviewQA;
import com.moyun.portal.mapper.PortalInterviewQuestionMapper;
import com.moyun.portal.mapper.PortalUserResumeMapper;
import com.moyun.portal.mapper.PortalVoiceInterviewMapper;
import com.moyun.portal.mapper.PortalVoiceInterviewQAMapper;
import com.moyun.util.bean.PageUtils;
import com.moyun.util.string.StringUtils;
import com.moyun.ext.cms.service.interview.AnswerScoringEngine;
import com.moyun.ext.cms.service.interview.InterviewChatMemoryService;
import com.moyun.ext.ai.service.AgentService;
import com.moyun.ext.ai.service.chat.RagRetrievalService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.rag.content.Content;
import com.moyun.ext.aiapp.support.PromptInjectionGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/**
 * 语音面试官 Service 实现（V3：统一 AI 入口 · 纯 agent 自由面试）
 *
 * <p>核心链路：
 * <ul>
 *   <li>{@link #start}：创建会话 + agent 同步生成开场白首问 + 滑窗记忆初始化</li>
 *   <li>{@link #submitAnswer}：回答入滑窗 → agent 流式输出面试官话术（delta 打字机）→
 *       预创建下一题 QA / 问满标记 finished</li>
 *   <li>{@link #requestHint}：agent 基于滑窗上下文生成一句思考引导</li>
 *   <li>{@link #finish}：收口会话 → 异步批量 LLM 分析 → 聚合报告落库</li>
 * </ul>
 * 无实时评分、无规则决策链路，深度分析统一留到结束批量报告。
 *
 * @author moyun
 */
@Service
public class VoiceInterviewServiceImpl implements IVoiceInterviewService {

    private static final Logger log = LoggerFactory.getLogger(VoiceInterviewServiceImpl.class);

    /** 主问题目数量 */
    private static final int QUESTION_COUNT = 5;

    /** 时长制：sys_config 面试时长键（分钟，缺省 20） */
    private static final String CONFIG_KEY_DURATION = "voice.interview.durationMinutes";
    /** 时长制：默认面试时长（分钟） */
    private static final int DEFAULT_DURATION_MINUTES = 20;
    /** 时长制：服务端超时宽限（分钟，倒计时归零后允许收尾作答提交的余量） */
    private static final int DURATION_GRACE_MINUTES = 2;

    /**
     * 口头结束意图检测（严格短语，避免答案中提及"结束"误判）：
     * 命中即视为候选人主动提出结束面试，服务端直接收尾（不走 agent 轮次）。
     */
    private static final Pattern VERBAL_END_PATTERN = Pattern.compile(
            "结束(这场|本次|这个|一下)?(面试|测试)"
                    + "|(我想|我要|我准备|想|要|能不能|可以|希望)(结束|停止|到此为止)"
                    + "|到此为止|就到这里|今天就到这|面试到此(结束|为止)|结束吧|先结束了");

    /**
     * 简历摘要：面试官上下文的数据底座（项目名 + 技术栈 + 亮点，最多 3 个项目）。
     */
    private String buildResumeDigest(Long resumeId) {
        if (resumeId == null) {
            return null;
        }
        try {
            PortalUserResume resume = userResumeMapper.selectById(resumeId);
            if (resume == null || StringUtils.isEmpty(resume.getProjects())) {
                return null;
            }
            JsonNode arr = objectMapper.readTree(resume.getProjects());
            if (arr == null || !arr.isArray()) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            int projIdx = 0;
            for (JsonNode proj : arr) {
                if (projIdx >= 3) {
                    break;
                }
                String name = proj.path("name").asText("");
                if (StringUtils.isEmpty(name)) {
                    continue;
                }
                sb.append("项目").append(projIdx + 1).append("：").append(name);
                String stack = proj.path("stack").asText("");
                if (StringUtils.isNotEmpty(stack)) {
                    sb.append("（").append(stack).append("）");
                }
                String highlight = proj.path("highlight").asText("");
                if (StringUtils.isNotEmpty(highlight)) {
                    sb.append("——").append(highlight);
                }
                sb.append("\n");
                projIdx++;
            }
            String digest = sb.toString();
            return StringUtils.isEmpty(digest) ? null : digest;
        } catch (Exception e) {
            log.warn("[VoiceInterview] 简历摘要提取失败 resumeId={}：{}", resumeId, e.getMessage());
            return null;
        }
    }
    /** 读取 configJson 中的简历摘要（追问上下文） */
    @SuppressWarnings("unchecked")
    private Map<String, Object> readInterviewConfig(PortalVoiceInterview interview) {
        try {
            if (StringUtils.isEmpty(interview.getConfigJson())) {
                return new LinkedHashMap<>();
            }
            return objectMapper.readValue(interview.getConfigJson(), Map.class);
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    /** SSE 事件发送（异常吞掉记日志，不中断回调链） */
    private void sendEvent(SseEmitter emitter, String name, Object data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (Exception e) {
            log.warn("[VoiceInterview] SSE 发送 {} 事件失败：{}", name, e.getMessage());
        }
    }

    /**
     * LLM 一次调用同时完成：评分校正 + 漏洞识别 + 水平评估 + 针对性追问建议。
     * 收口 AI 网关（voice_interview 场景 task=answer_analysis 子任务），
     * 失败/未启用时返回 null，调用方回退规则评分（保证链路永远可用）。
     */
    private AnswerAnalysis analyzeAnswerByLlm(PortalVoiceInterview interview, String questionTitle,
                                              String questionAnalysis, String transcript, AnswerScoringEngine.ScoreResult ruleScore) {
        if (!aiGlobalSwitch.isEnabled()) {
            return null;
        }
        try {
            Object resumeDigest = readInterviewConfig(interview).get("resumeDigest");
            // V3 纯 agent 面试下 qa.question 存的是面试官整段话术（含开场寒暄/上轮反馈），
            // 超长截断避免寒暄内容占满分析上下文、稀释题目重点
            String question = questionTitle != null && questionTitle.length() > 400
                    ? questionTitle.substring(0, 400) + "…（后略）" : questionTitle;
            String context = "你是一位资深技术面试官，正在对候选人进行「"
                    + (StringUtils.isEmpty(interview.getPosition()) ? "综合" : interview.getPosition())
                    + "」岗位的模拟面试。\n"
                    + (resumeDigest != null && StringUtils.isNotEmpty(String.valueOf(resumeDigest))
                            ? "候选人简历项目：\n" + resumeDigest + "\n" : "")
                    + "\n你刚刚向候选人提出问题：\"" + question + "\"\n"
                    + (StringUtils.isNotEmpty(questionAnalysis) ? "该题考察要点：" + questionAnalysis + "\n" : "");
            // LinkedHashMap 可变 Map（Map.of 不可变曾被网关 sanitizeInputChannel setValue 击穿）
            Map<String, Object> input = new LinkedHashMap<>();
            input.put("task", "answer_analysis");
            input.put("context", context);
            input.put("transcript", transcript);
            JsonNode node = aiSceneJsonClient.executeForJson(SCENE_VOICE_INTERVIEW, input, interview.getUserId());
            if (node == null) {
                return null;
            }
            AnswerAnalysis analysis = parseAnalysis(node, ruleScore);
            // v11.x C1：LLM 分与规则分按权重融合（默认 LLM 70% + 规则 30%）
            if (analysis != null) {
                PortalInterviewConfig ic = loadInterviewConfigQuietly();
                analysis.score = scoringEngine.fuseAnswerScore(
                        analysis.score, ruleScore.score, ic == null ? null : ic.getScoringWeights());
            }
            return analysis;
        } catch (Exception e) {
            log.warn("[VoiceInterview] LLM 分析回答失败，回退规则评分：{}", e.getMessage());
            return null;
        }
    }

    /** 解析网关结构化回答分析结果（Handler 已容错解析 JSON，此处只做字段映射与规则分融合） */
    private AnswerAnalysis parseAnalysis(JsonNode node, AnswerScoringEngine.ScoreResult ruleScore) {
        try {
            AnswerAnalysis a = new AnswerAnalysis();
            a.score = clamp(node.path("score").asInt(ruleScore.score), 0, 100);
            a.feedback = node.path("feedback").asText("");
            if (StringUtils.isEmpty(a.feedback)) {
                a.feedback = ruleScore.feedback;
            }
            // 6 维全量解析（LLM 未输出的维度回退规则分），并按 llmRatio 逐维融合
            Map<String, Integer> dims = new LinkedHashMap<>();
            String[] dimKeys = {"relevance", "professionalism", "fluency", "interactivity", "confidence", "logic"};
            PortalInterviewConfig dimCfg = loadInterviewConfigQuietly();
            String dimWeights = dimCfg == null ? null : dimCfg.getScoringWeights();
            for (String key : dimKeys) {
                int llmVal = node.path("dimensions").path(key).asInt(-1);
                int ruleVal = ruleScore.dimensions.getOrDefault(key, 50);
                int finalVal = llmVal >= 0
                        ? scoringEngine.fuseAnswerScore(llmVal, ruleVal, dimWeights)
                        : ruleVal;
                dims.put(key, clamp(finalVal, 0, 100));
            }
            a.dimensions = dims;
            List<String> flaws = new ArrayList<>();
            for (JsonNode f : node.path("flaws")) {
                String t = f.asText("").trim();
                if (StringUtils.isNotEmpty(t)) {
                    flaws.add(t);
                }
            }
            a.flaws = flaws;
            a.level = node.path("level").asText("");
            a.followupWorth = node.path("followupWorth").asBoolean(false) && !flaws.isEmpty();
            a.followupQuestion = node.path("followupQuestion").asText("");
            a.guidance = node.path("guidance").asText("");
            return a;
        } catch (Exception e) {
            return null;
        }
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    /** LLM 分析结果（规则评分的超集：漏洞/水平/追问建议） */
    private static class AnswerAnalysis {
        int score;
        String feedback;
        Map<String, Integer> dimensions;
        List<String> flaws = new ArrayList<>();
        String level = "";
        boolean followupWorth;
        String followupQuestion = "";
        String guidance = "";
    }

    /** 场景代码：语音面试（AI场景配置中心） */
    /** 场景代码统一走 AiSceneEnum 注册表，不再硬编码字符串 */
    private static final String SCENE_VOICE_INTERVIEW = com.moyun.ext.ai.enums.AiSceneEnum.VOICE_INTERVIEW.getCode();

    /** SSE 超时时间（毫秒） */
    private static final long SSE_TIMEOUT = 120_000L;

    @Autowired private PortalVoiceInterviewMapper interviewMapper;
    @Autowired private PortalVoiceInterviewQAMapper qaMapper;
    @Autowired private PortalInterviewQuestionMapper questionMapper;
    @Autowired private PortalUserResumeMapper userResumeMapper;
    @Autowired private ObjectMapper objectMapper;
    /** 面试场景统一走 AiSceneJsonClient（AI 统一网关入口） */
    @Autowired private com.moyun.ext.aiapp.support.AiSceneJsonClient aiSceneJsonClient;
    /** AI 全局运行时开关（sys_config ai.global.enabled，替代 yaml 静态配置） */
    @Autowired private com.moyun.ext.ai.service.AiGlobalSwitch aiGlobalSwitch;
    @Autowired private InterviewAgentClient agentClient;
    /** V3：滑窗记忆服务（面试对话上下文复用统一 AI 会话机制） */
    @Autowired private InterviewChatMemoryService memoryService;
    @Autowired private ScoringEngine scoringEngine;
    @Autowired private com.moyun.ext.cms.service.IWrongQuestionService wrongQuestionService;
    @Autowired private com.moyun.ext.ai.service.WorkflowService aiWorkflowService;
    @Autowired private com.moyun.portal.mapper.PortalUserMapper portalUserMapper;
    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("aiTaskExecutor")
    private org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor aiTaskExecutor;
    @Autowired private IPortalInterviewConfigService interviewConfigService;
    /** V2重构：会话事件日志（start/answer/next/finish/close 全链路追溯） */
    @Autowired private com.moyun.portal.mapper.PortalVoiceInterviewEventMapper eventMapper;
    /** V4：预热 RAG——agent 绑定知识库检索（题库文档作为考察方向供给源） */
    @Autowired private RagRetrievalService ragRetrievalService;
    @Autowired private AgentService agentService;
    /** 时长制：sys_config 读取（voice.interview.durationMinutes） */
    @Autowired private com.moyun.system.service.ISysConfigService sysConfigService;

    /** SSE 异步线程池（避免阻塞请求线程） */
    private final ScheduledExecutorService sseExecutor = Executors.newScheduledThreadPool(2);

    /** 批量分析运行中标记（断链自愈：analysis 卡 1 且无运行任务时轮询接口重触发） */
    private static final java.util.Set<Long> RUNNING_ANALYSIS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    // ========================================================================
    // 开始面试
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public VoiceInterviewVO start(Long userId, VoiceStartConfig config) {

        if (config == null) {
            config = new VoiceStartConfig();
        }

        String position = config.getPosition() == null ? "" : config.getPosition().trim();
        if (position.length() > 64) {
            throw new ServiceException("面试岗位名称不能超过64个字符");
        }
        String difficulty = StringUtils.isNotEmpty(config.getDifficulty()) ? config.getDifficulty() : "medium";
        // 岗位要求 JD（面试官提问方向与深度贴合岗位要求；trim 后空串视为未填）
        String jobRequirements = config.getJobRequirements() == null ? ""
                : config.getJobRequirements().trim();
        if (jobRequirements.length() > 2000) {
            throw new ServiceException("岗位要求不能超过2000个字符");
        }
        int questionCount = config.getQuestionCount() != null && config.getQuestionCount() > 0
                ? config.getQuestionCount() : QUESTION_COUNT;
        // 时长制：sys_config 读取面试时长（缺省 20 分钟），题数仅作软参考不再强制收尾
        int durationMinutes = resolveDurationMinutes();

        // V3 面试官 agent：sys_config 默认配置（voice.interview.defaultAgentId），前端不再选择
        Agent agent = agentClient.resolveAgent(null);
        if (agent == null || !agentClient.isEnabled()) {
            throw new ServiceException("AI 面试官未配置或不可用，请联系管理员（sys_config: voice.interview.defaultAgentId）");
        }

        // 简历摘要（有则作为面试官上下文注入滑窗）
        String resumeDigest = buildResumeDigest(config.getResumeId());

        // V4 预热：RAG 检索题库知识库（agent 绑定；无知识库/失败返回 null 不阻塞开面）
        String kbSnippets = retrieveKbSnippets(agent, position, resumeDigest);

        // 断点续接收口：开始新面试前，遗留的进行中会话自动结束（abandon）并触发异步批量分析（数据不丢）
        closeStaleInterviews(userId);

        // V4 预热：一次调用产出"AI 理解"（画像+考察方向计划）+ 开场白 + 首题（失败降级旧 generateOpening 链路）
        JsonNode warmupPlan = tryWarmup(agent, position, difficulty, questionCount,
                jobRequirements, resumeDigest, kbSnippets);
        String opening;
        if (warmupPlan != null) {
            // opening 场面话 + firstQuestion 首题（固定请自我介绍），合并为滑窗首条 assistant 与首问落库
            String openingText = warmupPlan.path("opening").asText("").trim();
            String firstQuestion = warmupPlan.path("firstQuestion").asText("").trim();
            opening = (openingText + "\n\n" + firstQuestion).trim();
        } else {
            opening = null;
        }

        // 创建会话（V3：岗位/难度/JD/简历 + agent 自由面试，不再有题单/风格/场景/阶段机）
        PortalVoiceInterview interview = new PortalVoiceInterview();
        interview.setUserId(userId);
        interview.setPosition(position);
        interview.setResumeId(config.getResumeId());
        interview.setAgentId(agent.getId());
        interview.setStatus("in_progress");
        interview.setDifficulty(difficulty);
        interview.setTotalQa(questionCount);
        interview.setCurrentIdx(0);
        Map<String, Object> configMap = new LinkedHashMap<>();
        configMap.put("difficulty", difficulty);
        configMap.put("durationMinutes", durationMinutes);
        if (StringUtils.isNotEmpty(jobRequirements)) {
            configMap.put("jobRequirements", jobRequirements);
        }
        if (StringUtils.isNotEmpty(resumeDigest)) {
            configMap.put("resumeDigest", resumeDigest);
        }
        if (warmupPlan != null) {
            // 预热计划落 configJson（断点续接 rebuild 免重算；报告画像复用）
            configMap.put("warmupPlan", objectMapper.valueToTree(warmupPlan));
        }
        interview.setConfigJson(toJson(configMap));
        // 上下文快照（报告三段式第一/二栏数据源）
        Map<String, Object> contextSnapshotMap = new LinkedHashMap<>();
        contextSnapshotMap.put("position", position);
        contextSnapshotMap.put("difficulty", difficulty);
        contextSnapshotMap.put("jobRequirements", jobRequirements);
        contextSnapshotMap.put("resumeDigest", resumeDigest == null ? "" : resumeDigest);
        interview.setContextSnapshot(toJson(contextSnapshotMap));
        interview.setAnalysisStatus(0);
        interview.setAnalysisProgress(0);
        interview.setCreateTime(LocalDateTime.now());
        interviewMapper.insert(interview);

        // 滑窗初始化：system（agent 人设+本场约束）+ 上下文 user（简历摘要+JD，wrapData 包裹）
        String systemPrompt = buildInterviewerSystemPrompt(interview, agent);
        StringBuilder ctx = new StringBuilder("面试背景信息：\n岗位：" + position + "\n难度：" + difficulty);
        if (StringUtils.isNotEmpty(jobRequirements)) {
            ctx.append("\n岗位要求JD：\n").append(jobRequirements);
        }
        if (StringUtils.isNotEmpty(resumeDigest)) {
            ctx.append("\n简历摘要：\n").append(resumeDigest);
        }
        String contextUserMsg = PromptInjectionGuard.wrapData("候选人资料", ctx.toString());

        // 同步生成开场白+首题（V4：warmup 产物优先；失败降级 generateOpening 一次调用）
        if (opening == null || opening.isEmpty()) {
            opening = generateOpening(interview, agent, systemPrompt, contextUserMsg);
        }
        memoryService.initFirstTurn(interview.getId(), agent.getMaxHistoryTurns(),
                systemPrompt, contextUserMsg, opening);

        // 首题落库（question=开场白+首题全文，报告回放展示）
        PortalVoiceInterviewQA firstQa = new PortalVoiceInterviewQA();
        firstQa.setInterviewId(interview.getId());
        firstQa.setQuestionSource("agent");
        firstQa.setQuestionIdx(0);
        firstQa.setQuestion(opening);
        firstQa.setHintUsed(0);
        firstQa.setTranscriptionEdited(0);
        firstQa.setCreateTime(LocalDateTime.now());
        firstQa.setSpeakText(opening);
        qaMapper.insert(firstQa);

        recordEvent(interview.getId(), "start", Map.of(
                "agentId", agent.getId(),
                "position", position,
                "difficulty", difficulty,
                "questionCount", questionCount,
                "durationMinutes", durationMinutes));

        return assembleVO(interview, firstQa);

    }

    /** V3/V4 面试官系统提示词：agent 人设为主体 + 岗位/难度/题数约束 + 段序约束 + 预热计划渲染 */
    private String buildInterviewerSystemPrompt(PortalVoiceInterview interview, Agent agent) {
        String base = StringUtils.isNotEmpty(agent.getSystemPrompt())
                ? agent.getSystemPrompt()
                : "你是一位经验丰富的面试官，主持一场专业、自然的模拟面试。";
        String difficultyDesc;
        switch (interview.getDifficulty() == null ? "medium" : interview.getDifficulty()) {
            case "easy" -> difficultyDesc = "简单（基础问题为主，节奏友好）";
            case "hard" -> difficultyDesc = "深挖（追问细节与原理，考察深度）";
            default -> difficultyDesc = "适中（常规问题+适度追问）";
        }
        StringBuilder sb = new StringBuilder(base);
        sb.append("\n\n【本场面试约束】\n")
                .append("- 岗位：").append(StringUtils.isEmpty(interview.getPosition()) ? "综合" : interview.getPosition()).append("\n")
                .append("- 难度：").append(difficultyDesc).append("\n")
                .append("- 计划 ").append(interview.getTotalQa() == null ? QUESTION_COUNT : interview.getTotalQa())
                .append(" 个大问题，每个大问题可按回答情况追问 1-2 次，不要机械背题，围绕候选人实际经历展开。\n");
        // 段序约束（三段式：自我介绍 → 深挖 → 核心问答；反问融入对话流，非独立段）
        sb.append("\n【段序约束】\n")
                .append("- 第 1 个问题固定为：请候选人做自我介绍；")
                .append("随后 2-3 问必须从其自我介绍内容中提取深挖点逐一追问，之后再扩展到其他考察方向。\n")
                .append("- 候选人在回答中口头反问时，简短作答后自然回到提问；问满计划题数后，")
                .append("口播一句“你还有什么想了解的吗？”，候选人若无反问或反问完毕即做简短收尾致谢。\n");
        // V4：预热计划（AI 理解）渲染进 system，滑窗常驻保证不跑题
        String planSection = renderWarmupPlanSection(interview);
        if (StringUtils.isNotEmpty(planSection)) {
            sb.append("\n").append(planSection);
        }
        sb.append("\n- 只输出面试官口吻的话，不输出任何分析、评分或格式标记。");
        return sb.toString();
    }

    /** V4：渲染预热计划段（configJson.warmupPlan → 面试理解 + 考察方向，常驻滑窗 system） */
    private String renderWarmupPlanSection(PortalVoiceInterview interview) {
        try {
            if (StringUtils.isEmpty(interview.getConfigJson())) {
                return null;
            }
            JsonNode plan = objectMapper.readTree(interview.getConfigJson()).path("warmupPlan");
            if (plan.isMissingNode() || plan.isNull()) {
                return null;
            }
            StringBuilder sb = new StringBuilder("【面试理解（预热生成，面试全程遵循）】\n");
            JsonNode u = plan.path("understanding");
            String profile = u.path("candidateProfile").asText("");
            if (StringUtils.isNotEmpty(profile)) {
                sb.append("- 候选人画像：").append(profile).append("\n");
            }
            appendPlanList(sb, "优势", u.path("strengths"));
            appendPlanList(sb, "待验证疑点", u.path("concerns"));
            JsonNode areas = plan.path("interviewPlan").path("focusAreas");
            if (areas.isArray() && areas.size() > 0) {
                sb.append("【考察方向】\n");
                int i = 1;
                for (JsonNode a : areas) {
                    String area = a.path("area").asText("");
                    if (StringUtils.isEmpty(area)) {
                        continue;
                    }
                    sb.append(i++).append(". ").append(area);
                    String depth = a.path("depth").asText("");
                    if (StringUtils.isNotEmpty(depth)) {
                        sb.append("（").append(depth).append("）");
                    }
                    String reason = a.path("reason").asText("");
                    if (StringUtils.isNotEmpty(reason)) {
                        sb.append("——").append(reason);
                    }
                    sb.append("\n");
                }
            }
            return sb.length() > "【面试理解（预热生成，面试全程遵循）】\n".length() ? sb.toString() : null;
        } catch (Exception e) {
            log.warn("[VoiceInterview] 预热计划渲染失败 interviewId={}：{}", interview.getId(), e.getMessage());
            return null;
        }
    }

    /** 预热计划列表字段渲染（strengths/concerns） */
    private void appendPlanList(StringBuilder sb, String label, JsonNode arr) {
        if (arr == null || !arr.isArray() || arr.isEmpty()) {
            return;
        }
        List<String> items = new ArrayList<>();
        for (JsonNode n : arr) {
            String t = n.asText("").trim();
            if (StringUtils.isNotEmpty(t)) {
                items.add(t);
            }
        }
        if (!items.isEmpty()) {
            sb.append("- ").append(label).append("：").append(String.join("；", items)).append("\n");
        }
    }

    /** V4：预热 RAG——检索 agent 绑定知识库（题库文档），返回 top-5 片段拼装文本（无/失败返回 null） */
    private String retrieveKbSnippets(Agent agent, String position, String resumeDigest) {
        try {
            if (agent == null || agent.getId() == null
                    || agent.getKnowledgeLibraryIds() == null || agent.getKnowledgeLibraryIds().isEmpty()) {
                return null;
            }
            List<Long> kbIds = agentService.getKnowledgeBaseIds(agent.getId());
            if (kbIds == null || kbIds.isEmpty()) {
                return null;
            }
            String query = (StringUtils.isEmpty(position) ? "技术面试考察方向" : position + " 岗位面试考察方向");
            if (StringUtils.isNotEmpty(resumeDigest)) {
                query += " " + resumeDigest.replace("\n", " ");
            }
            List<Content> contents = ragRetrievalService.retrieveContents(query, kbIds, agent);
            if (contents == null || contents.isEmpty()) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            int idx = 0;
            for (Content c : contents) {
                if (c == null || c.textSegment() == null) {
                    continue;
                }
                String text = c.textSegment().text();
                if (StringUtils.isEmpty(text) || idx >= 5) {
                    continue;
                }
                if (sb.length() > 0) {
                    sb.append("\n---\n");
                }
                sb.append("片段").append(idx + 1).append("：")
                        .append(text.length() > 300 ? text.substring(0, 300) : text);
                idx++;
            }
            return sb.length() == 0 ? null : sb.toString();
        } catch (Exception e) {
            log.warn("[VoiceInterview] 预热 RAG 检索失败（跳过知识库注入）：{}", e.getMessage());
            return null;
        }
    }

    /**
     * V4 预热：场景网关 task=warmup 一次调用产出"AI 理解"+开场白+首题。
     * <p>失败/未启用返回 null，调用方降级 generateOpening 旧链路（不阻塞开面）。</p>
     */
    private JsonNode tryWarmup(Agent agent, String position, String difficulty, int questionCount,
                              String jobRequirements, String resumeDigest, String kbSnippets) {
        if (!aiGlobalSwitch.isEnabled()) {
            return null;
        }
        try {
            String context = "岗位：" + (StringUtils.isEmpty(position) ? "综合" : position)
                    + "\n难度：" + difficulty
                    + "\n计划问题数：" + questionCount;
            Map<String, Object> input = new LinkedHashMap<>();
            input.put("task", "warmup");
            input.put("context", context);
            if (StringUtils.isNotEmpty(agent.getSystemPrompt())) {
                input.put("agentPersona", agent.getSystemPrompt());
            }
            if (StringUtils.isNotEmpty(resumeDigest)) {
                input.put("resumeDigest", resumeDigest);
            }
            if (StringUtils.isNotEmpty(jobRequirements)) {
                input.put("jd", jobRequirements);
            }
            if (StringUtils.isNotEmpty(kbSnippets)) {
                input.put("kbSnippets", kbSnippets);
            }
            JsonNode node = aiSceneJsonClient.executeForJson(SCENE_VOICE_INTERVIEW, input, null);
            if (node == null || StringUtils.isEmpty(node.path("opening").asText(""))
                    || StringUtils.isEmpty(node.path("firstQuestion").asText(""))) {
                return null;
            }
            return node;
        } catch (Exception e) {
            log.warn("[VoiceInterview] 预热 warmup 生成失败（降级 generateOpening 链路）：{}", e.getMessage());
            return null;
        }
    }

    /** V3 同步生成开场白+首题（一次 LLM 调用；失败抛出，由前端进度条期间感知） */
    private String generateOpening(PortalVoiceInterview interview, Agent agent,
                                   String systemPrompt, String contextUserMsg) {
        String instruction = "面试现在开始。请先做简短开场（一两句欢迎与放松提示），"
                + "然后直接提出第一个面试问题（结合候选人资料，不要编号或多余格式）。";
        String raw = agentClient.chat(agent, List.of(
                SystemMessage.from(systemPrompt),
                new UserMessage(contextUserMsg),
                new UserMessage(instruction)));
        if (raw == null || raw.trim().isEmpty()) {
            throw new ServiceException("面试官开场生成失败，请稍后重试");
        }
        return raw.trim();
    }

    // ========================================================================
    // 提交答案（SSE 流式）
    // ========================================================================
    @Override
    public SseEmitter submitAnswer(Long interviewId, Long userId, Long qaId, String transcript, Integer latencyMs, Boolean skip) {
        PortalVoiceInterview interview = mustOwnInterview(interviewId, userId);
        if ("finished".equals(interview.getStatus())) {
            throw new ServiceException("面试已结束，无法继续作答");
        }
        PortalVoiceInterviewQA qa = qaMapper.selectById(qaId);
        if (qa == null || !interviewId.equals(qa.getInterviewId())) {
            throw new ServiceException("问答记录不存在");
        }
        boolean isSkip = Boolean.TRUE.equals(skip);
        if (!isSkip && StringUtils.isEmpty(transcript)) {
            throw new ServiceException("答案不能为空");
        }

        // 原始回答提交即落库（铁律：先存原始，任何后续失败对话不丢；跳过题保持 userAnswer 为空）
        if (!isSkip) {
            qa.setUserAnswer(transcript);
            qa.setAnswerRaw(transcript);
            qa.setAnswerTime(LocalDateTime.now());
            qa.setLatencyMs(latencyMs);
            qaMapper.updateById(qa);
            recordEvent(interviewId, "answer", Map.of("qaId", qaId,
                    "latencyMs", latencyMs == null ? 0 : latencyMs));
        } else {
            recordEvent(interviewId, "skip", Map.of("qaId", qaId));
        }

        // 时长制守卫：超过配置时长+宽限后拒绝继续作答，并自动收口触发报告（数据不丢）
        if (isInterviewTimedOut(interview)) {
            recordEvent(interviewId, "timeout_close", Map.of("qaId", qaId));
            finishQuietly(interview);
            SseEmitter timeoutEmitter = new SseEmitter(SSE_TIMEOUT);
            sendEvent(timeoutEmitter, "delta", toJson(Map.of("t", "本场面试时长已到，感谢你的参与。")));
            sendEvent(timeoutEmitter, "end", toJson(Map.of(
                    "roundDone", countAnsweredRounds(interviewId), "finished", true)));
            timeoutEmitter.complete();
            return timeoutEmitter;
        }

        // 口头结束检测：候选人明确表达结束意图（严格短语），直接收尾（不进 agent 轮次）
        if (!isSkip && matchesVerbalEnd(transcript)) {
            recordEvent(interviewId, "verbal_end", Map.of("qaId", qaId));
            SseEmitter endEmitter = new SseEmitter(SSE_TIMEOUT);
            sendEvent(endEmitter, "delta", toJson(Map.of("t", "好的，本场面试就到这里，感谢你的参与，稍后可查看面试报告。")));
            sendEvent(endEmitter, "end", toJson(Map.of(
                    "roundDone", countAnsweredRounds(interviewId), "finished", true)));
            endEmitter.complete();
            return endEmitter;
        }

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        emitter.onTimeout(() -> log.warn(
                "[VoiceInterview] SSE 请求超时（{}ms）interviewId={} qaId={}", SSE_TIMEOUT, interviewId, qaId));
        emitter.onError(t -> log.warn(
                "[VoiceInterview] SSE 连接异常收尾 interviewId={} qaId={}：{}", interviewId, qaId, t.getMessage()));

        // V3：滑窗记忆 + 面试官流式话术（评分/分析统一留到结束批量报告）
        sseExecutor.execute(() -> runAgentTurn(emitter, interview, qa, isSkip ? "" : transcript, isSkip));
        return emitter;
    }

    /**
     * V3 面试官流式轮次：
     * 候选人回答（或跳过标记）入滑窗 → LLM 基于完整上下文流式输出面试官话术（delta 打字机）→
     * 话术入滑窗 + 落库 → 预创建下一题 QA 并随 end 下发 nextQaId/nextQuestion。
     * 时长制：问满题数不再收尾，结束仅由用户主动（按钮/口头）或倒计时归零触发。
     * 无实时评分/无规则决策，深度分析全部留到结束批量报告。
     */
    private void runAgentTurn(SseEmitter emitter, PortalVoiceInterview interview,
                              PortalVoiceInterviewQA qa, String transcript, boolean skip) {
        try {
            Agent agent = agentClient.resolveAgent(interview.getAgentId());
            if (agent == null || !agentClient.isEnabled()) {
                sendEvent(emitter, "error", "AI 面试官不可用，请联系管理员配置");
                emitter.complete();
                return;
            }
            // 回答入滑窗（面试官话术即基于完整上下文，彻底消除重复提问；跳过注入标记）
            MessageWindowChatMemory memory = memoryService.getMemory(interview.getId(), agent.getMaxHistoryTurns());
            memory.add(new UserMessage(skip ? "（候选人表示跳过本题）" : transcript));

            List<ChatMessage> messages = new ArrayList<>(memory.messages());
            // 每轮唯一动态指令：话术风格约束 + 轮次进度（提示收尾时机）
            messages.add(new UserMessage(buildTurnDirective(interview, skip)));

            AtomicBoolean finished = new AtomicBoolean(false);
            agentClient.chatStream(agent, messages,
                    // onToken：面试官话术增量实时下发（前端打字机 + 分句 TTS）
                    token -> {
                        if (token == null || token.isEmpty()) {
                            return;
                        }
                        Map<String, Object> delta = new LinkedHashMap<>();
                        delta.put("t", token);
                        sendEvent(emitter, "delta", toJson(delta));
                    },
                    // onComplete：话术入滑窗 + 落库 → 创建下一题 → end（nextQaId/finished）
                    full -> {
                        if (!finished.compareAndSet(false, true)) {
                            return;
                        }
                        String speak = full == null ? "" : full.trim();
                        try {
                            if (!speak.isEmpty()) {
                                memory.add(new AiMessage(speak));
                            }
                            qa.setSpeakText(speak);
                            qaMapper.updateById(qa);

                            // end 载荷：轮次进度 + 下一题
                            // 时长制：题数仅作软参考，问满不再收尾——
                            // 结束只能由用户主动（按钮/口头）或倒计时归零触发
                            Map<String, Object> payload = new LinkedHashMap<>();
                            int done = countAnsweredRounds(interview.getId());
                            payload.put("roundDone", done);
                            // 预创建下一题 QA（question=面试官话术全文，报告回放与作答锚点）
                            PortalVoiceInterviewQA nextQa = new PortalVoiceInterviewQA();
                            nextQa.setInterviewId(interview.getId());
                            nextQa.setQuestionSource("agent");
                            nextQa.setQuestionIdx((qa.getQuestionIdx() == null ? 0 : qa.getQuestionIdx()) + 1);
                            nextQa.setQuestion(speak);
                            nextQa.setSpeakText(speak);
                            nextQa.setHintUsed(0);
                            nextQa.setTranscriptionEdited(0);
                            nextQa.setCreateTime(LocalDateTime.now());
                            qaMapper.insert(nextQa);
                            payload.put("nextQaId", nextQa.getId());
                            payload.put("nextQuestion", speak);
                            recordEvent(interview.getId(), "next", Map.of(
                                    "qaId", qa.getId(), "nextQaId", nextQa.getId()));
                            sendEvent(emitter, "end", toJson(payload));
                            emitter.complete();
                        } catch (Exception e) {
                            log.error("[VoiceInterview] 轮次收尾异常 interviewId={}", interview.getId(), e);
                            sendEvent(emitter, "error", "面试官响应处理失败");
                            emitter.complete();
                        }
                    },
                    // onError：直接提示（不再维护规则降级链路）
                    err -> {
                        if (!finished.compareAndSet(false, true)) {
                            return;
                        }
                        log.error("[VoiceInterview] 面试官流式失败 interviewId={} qaId={}", interview.getId(), qa.getId(), err);
                        sendEvent(emitter, "error", "面试官响应失败，请稍后重试");
                        emitter.complete();
                    });
        } catch (Exception e) {
            log.error("[VoiceInterview] SSE 处理异常 interviewId={} qaId={}", interview.getId(), qa.getId(), e);
            sendEvent(emitter, "error", e.getMessage());
            emitter.complete();
        }
    }

    /**
     * 每轮任务指令：只约束话术形态，不参与出题决策（面试官自主推进）。
     * 时长制：以剩余时长提示收尾节奏（临近结束提示自然收口），题数仅作进度展示。
     */
    private String buildTurnDirective(PortalVoiceInterview interview, boolean skip) {
        int done = countAnsweredRounds(interview.getId());
        String skipNote = skip ? "候选人刚刚选择跳过本题（未作答），请简短带过、不做追问，自然转入下一个方向。"
                : "请以面试官身份回应候选人的回答：先一两句简要反馈，再自然提出你的下一个问题或针对性追问。";
        // 时长制：剩余时长感知（结束由候选人主动提出或倒计时归零，不由题数决定）
        long remainMin = remainMinutesOf(interview);
        String timeNote;
        if (remainMin <= 0) {
            timeNote = "（本场面试时间已到，请以面试官身份做简短收尾致谢。）";
        } else if (remainMin <= 2) {
            timeNote = "（本场面试临近结束，请在当前话题自然收口，不再展开新的考察方向。）";
        } else {
            timeNote = "（本场面试剩余约 " + remainMin + " 分钟，可自主把握提问节奏与深度。）";
        }
        return skipNote
                + "只输出面试官会说的话，不要任何分析、标记或多余格式。"
                + "（本场已问 " + done + " 个大问题）" + timeNote;
    }

    /** 时长制：本场剩余分钟数（负值表示已超时；配置缺失按 sys_config 当前值） */
    private long remainMinutesOf(PortalVoiceInterview interview) {
        int duration = durationOf(interview);
        if (interview.getCreateTime() == null) {
            return duration;
        }
        return java.time.Duration.between(LocalDateTime.now(),
                interview.getCreateTime().plusMinutes(duration)).toMinutes();
    }

    /** 时长制：是否已超配置时长+宽限（服务端守卫，前端倒计时失灵时兜底收口） */
    private boolean isInterviewTimedOut(PortalVoiceInterview interview) {
        if (interview.getCreateTime() == null) {
            return false;
        }
        return LocalDateTime.now()
                .isAfter(interview.getCreateTime().plusMinutes(durationOf(interview) + DURATION_GRACE_MINUTES));
    }

    /** 时长制：读 sys_config 面试时长（分钟，缺省 20，范围 5-120） */
    private int resolveDurationMinutes() {
        try {
            String value = sysConfigService.selectConfigByKey(CONFIG_KEY_DURATION);
            if (value != null && !value.isBlank()) {
                return Math.max(5, Math.min(120, Integer.parseInt(value.trim())));
            }
        } catch (Exception e) {
            log.warn("[VoiceInterview] 读取面试时长配置失败，使用默认 {} 分钟：{}", DEFAULT_DURATION_MINUTES, e.getMessage());
        }
        return DEFAULT_DURATION_MINUTES;
    }

    /** 时长制：本场时长（configJson 优先，旧会话回退 sys_config 当前值） */
    private int durationOf(PortalVoiceInterview interview) {
        String v = readConfigKey(interview, "durationMinutes");
        if (!v.isEmpty()) {
            try {
                return Math.max(5, Math.min(120, Integer.parseInt(v)));
            } catch (NumberFormatException ignored) {
            }
        }
        return resolveDurationMinutes();
    }

    /** 口头结束意图检测（严格短语匹配，避免答案内容误判） */
    private boolean matchesVerbalEnd(String transcript) {
        return transcript != null && VERBAL_END_PATTERN.matcher(transcript).find();
    }

    /**
     * 静默收口（服务端守卫路径）——与 finish() 同逻辑但不返回报告：
     * 收口状态 + 释放滑窗 + 触发异步批量分析；已结束的幂等跳过。
     */
    private void finishQuietly(PortalVoiceInterview interview) {
        try {
            if ("finished".equals(interview.getStatus())) {
                return;
            }
            interview.setStatus("finished");
            interview.setClosedReason("timeout");
            if (interview.getAnalysisStatus() == null || interview.getAnalysisStatus() == 0) {
                interview.setAnalysisStatus(1);
            }
            interview.setAnalysisProgress(0);
            interviewMapper.updateById(interview);
            recordEvent(interview.getId(), "finish", Map.of("closedReason", "timeout"));
            memoryService.clear(interview.getId());
            triggerBatchAnalysis(interview.getId());
        } catch (Exception e) {
            log.error("[VoiceInterview] 超时收口失败 interviewId={}：{}", interview.getId(), e.getMessage(), e);
        }
    }

    /** 已答轮数（有回答的 QA 计数） */
    private int countAnsweredRounds(Long interviewId) {
        Long n = qaMapper.selectCount(Wrappers.<PortalVoiceInterviewQA>lambdaQuery()
                .eq(PortalVoiceInterviewQA::getInterviewId, interviewId)
                .isNotNull(PortalVoiceInterviewQA::getUserAnswer));
        return n == null ? 0 : n.intValue();
    }

    /** V2：记录会话事件（只增不改，链路追溯/断点恢复依据） */
    private void recordEvent(Long interviewId, String eventType, Map<String, Object> data) {
        try {
            com.moyun.portal.domain.entity.PortalVoiceInterviewEvent event =
                    new com.moyun.portal.domain.entity.PortalVoiceInterviewEvent();
            event.setInterviewId(interviewId);
            event.setEventType(eventType);
            event.setEventData(data == null ? null : toJson(data));
            event.setCreateTime(LocalDateTime.now());
            eventMapper.insert(event);
        } catch (Exception e) {
            log.warn("[VoiceInterview] 事件记录失败 interviewId={} type={}：{}", interviewId, eventType, e.getMessage());
        }
    }

    /** 面试配置（默认配置）宽容加载：失败/无配置返回 null */
    private PortalInterviewConfig loadInterviewConfigQuietly() {
        try {
            return interviewConfigService.getDefaultConfig();
        } catch (Exception e) {
            log.warn("[VoiceInterview] 加载默认面试配置失败：{}", e.getMessage());
            return null;
        }
    }

    // ========================================================================
    // 请求提示（V3：面试官 agent 基于滑窗上下文生成一句引导）
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public VoiceInterviewVO requestHint(Long interviewId, Long userId, Long qaId) {
        PortalVoiceInterview interview = mustOwnInterview(interviewId, userId);
        PortalVoiceInterviewQA qa = qaMapper.selectById(qaId);
        if (qa == null || !interviewId.equals(qa.getInterviewId())) {
            throw new ServiceException("问答记录不存在");
        }

        int nextUsed = (qa.getHintUsed() == null ? 0 : qa.getHintUsed()) + 1;
        if (nextUsed > 3) {
            throw new ServiceException("提示次数已用完");
        }

        // agent 滑窗提示：基于完整对话上下文给一句思考引导（不泄露答案）
        String text = null;
        Agent agent = interview.getAgentId() == null ? null : agentClient.resolveAgent(interview.getAgentId());
        if (agent != null && agentClient.isEnabled()) {
            try {
                List<ChatMessage> messages = new ArrayList<>(
                        memoryService.getMemory(interview.getId(), agent.getMaxHistoryTurns()).messages());
                messages.add(new UserMessage("候选人请求思考提示。请以面试官身份给一句简短的思考引导"
                        + "（提示回答方向或组织思路，不直接给出答案），40字以内，只输出这句话。"));
                text = agentClient.chat(agent, messages);
            } catch (Exception e) {
                log.warn("[VoiceInterview] agent 提示生成失败 interviewId={}：{}", interviewId, e.getMessage());
            }
        }
        if (StringUtils.isEmpty(text)) {
            text = "别着急，可以从你熟悉的相关项目经历入手，按「背景→做法→结果」的思路组织回答。";
        }
        HintVO hint = HintVO.of(nextUsed, "思考提示");
        hint.setKeywords(new ArrayList<>());
        hint.setSpeakText(text.trim());
        qa.setHintUsed(nextUsed);
        qaMapper.updateById(qa);

        VoiceInterviewVO vo = assembleVO(interview, qa);
        vo.setCurrentQa(toQaVO(qa));
        vo.setHint(hint);
        return vo;
    }

    // ========================================================================
    // 结束面试 + 报告
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public VoiceInterviewReportVO finish(Long interviewId, Long userId) {
        PortalVoiceInterview interview = mustOwnInterview(interviewId, userId);
        if ("finished".equals(interview.getStatus())) {
            // 幂等：已结束直接返回报告
            return parseReport(interview);
        }

        // ========== V2：同步段只收口会话状态（<200ms），批量分析转异步 ==========
        interview.setStatus("finished");
        interview.setClosedReason("user");
        if (interview.getAnalysisStatus() == null || interview.getAnalysisStatus() == 0) {
            interview.setAnalysisStatus(1);
        }
        interview.setAnalysisProgress(0);
        if (!InterviewPhase.isLegacy(interview.getPhase())) {
            interview.setPhase(InterviewPhase.FINISHED.code());
        }
        interviewMapper.updateById(interview);
        recordEvent(interviewId, "finish", Map.of("closedReason", "user"));
        // V3：结束释放滑窗（对话上下文已固化到 DB QA，报告批量分析用）
        memoryService.clear(interviewId);

        // 异步批量分析：逐题补 LLM 分析 → 聚合报告 → 进度更新（前端轮询 analysis 接口）
        triggerBatchAnalysis(interviewId);

        // 立即返回报告骨架（前端展示分析进度条，analysis_status=2 后重新拉取完整报告）
        VoiceInterviewReportVO skeleton = new VoiceInterviewReportVO();
        skeleton.setInterviewId(interviewId);
        skeleton.setTotalScore(interview.getScore());
        skeleton.setSummary("报告生成中…");
        return skeleton;
    }

    /**
     * V2：触发异步批量分析（幂等：analysis_status 已为 2 的不重跑；进行中不重复触发）。
     * P0 竞态修复：finish()/start() 均为 @Transactional，事务内直接提交异步任务会
     * 先于事务提交执行——异步线程读到旧值 analysisStatus=0 后直接跳过，报告永远不生成。
     * 此处注册事务提交后回调（afterCommit）再触发；无事务上下文（静默收口/自愈路径）直接触发。
     */
    private void triggerBatchAnalysis(Long interviewId) {
        if (org.springframework.transaction.support.TransactionSynchronizationManager
                .isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager
                    .registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            submitBatchAnalysis(interviewId);
                        }
                    });
        } else {
            submitBatchAnalysis(interviewId);
        }
    }

    /** 异步批量分析提交（线程池饱和时同步降级聚合，保证报告必有） */
    private void submitBatchAnalysis(Long interviewId) {
        try {
            aiTaskExecutor.execute(() -> {
                try {
                    PortalVoiceInterview fresh = interviewMapper.selectById(interviewId);
                    if (fresh == null || fresh.getAnalysisStatus() == null
                            || fresh.getAnalysisStatus() != 1) {
                        return; // 已完成/已在别处触发
                    }
                    runBatchAnalysis(fresh);
                } catch (Exception ex) {
                    log.error("[VoiceInterview] 批量分析异常 interviewId={}：{}", interviewId, ex.getMessage(), ex);
                    // 失败兜底：直接规则聚合出报告（保证报告必有）
                    try {
                        PortalVoiceInterview fresh = interviewMapper.selectById(interviewId);
                        if (fresh != null && fresh.getAnalysisStatus() != null
                                && fresh.getAnalysisStatus() == 1) {
                            aggregateAndStoreReport(fresh);
                        }
                    } catch (Exception ignore) {
                    }
                }
            });
        } catch (Exception rejected) {
            log.warn("[VoiceInterview] 批量分析提交失败（线程池饱和），同步降级聚合 interviewId={}：{}", interviewId, rejected.getMessage());
            try {
                PortalVoiceInterview fresh = interviewMapper.selectById(interviewId);
                if (fresh != null && Integer.valueOf(1).equals(fresh.getAnalysisStatus())) {
                    aggregateAndStoreReport(fresh);
                }
            } catch (Exception ignore) {
            }
        }
    }

    /** V2：批量分析主流程——逐题补 LLM 深度分析（带进度）→ 聚合报告落库 */
    private void runBatchAnalysis(PortalVoiceInterview interview) {
        Long interviewId = interview.getId();
        // 运行中标记防重入（自愈重触发与正常运行并发时只跑一个）
        if (!RUNNING_ANALYSIS.add(interviewId)) {
            return;
        }
        try {
        List<PortalVoiceInterviewQA> qaList = listQaByInterview(interviewId);
        // 待补分析的已作答题目（异步单题分析未覆盖或失败的）
        List<PortalVoiceInterviewQA> pending = new ArrayList<>();
        for (PortalVoiceInterviewQA qa : qaList) {
            if (StringUtils.isNotEmpty(qa.getUserAnswer())
                    && (qa.getAnalysisStatus() == null || qa.getAnalysisStatus() != 2)) {
                pending.add(qa);
            }
        }
        int total = pending.size();
        int done = 0;
        for (PortalVoiceInterviewQA qa : pending) {
            try {
                qa.setAnalysisStatus(1);
                qaMapper.updateById(qa);
                AnswerScoringEngine.ScoreResult sr = new AnswerScoringEngine.ScoreResult(
                        qa.getScore() == null ? 50 : qa.getScore(),
                        qa.getAiFeedback() == null ? "" : qa.getAiFeedback(),
                        qa.getRuleDimensionsJson() == null ? new LinkedHashMap<>()
                                : objectMapper.readValue(qa.getRuleDimensionsJson(),
                                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Integer>>() {}));
                AnswerAnalysis analysis = analyzeAnswerByLlm(interview, qa.getQuestion(), null,
                        qa.getUserAnswer(), sr);
                if (analysis != null) {
                    // LLM 分回写主分/点评/维度——V3 无实时评分，score 恒空会导致
                    // 聚合报告跳过全部题目（报告分数断链），此处收口保证报告必有分数与逐题点评
                    qa.setScore(analysis.score);
                    qa.setAiFeedback(analysis.feedback);
                    qa.setRuleDimensionsJson(toJson(analysis.dimensions));
                    qa.setScoreDraft(analysis.score);
                    qa.setLlmScoreJson(toJson(Map.of(
                            "scores", analysis.dimensions,
                            "total", analysis.score,
                            "comment", analysis.feedback == null ? "" : analysis.feedback)));
                    qa.setLlmAnalysisJson(toJson(Map.of(
                            "flaws", analysis.flaws,
                            "level", analysis.level,
                            "guidance", analysis.guidance == null ? "" : analysis.guidance,
                            "redFlags", java.util.Collections.emptyList())));
                } else {
                    // LLM 失败兜底：规则分写主分，防聚合跳过（V3 规则引擎已删，ScoreResult 为默认值）
                    qa.setScore(sr.score);
                    qa.setAiFeedback(sr.feedback);
                }
                qa.setAnalysisStatus(2);
                qaMapper.updateById(qa);
            } catch (Exception ex) {
                log.warn("[VoiceInterview] 批量分析单题失败 interviewId={} qaId={}：{}", interviewId, qa.getId(), ex.getMessage());
                qa.setAnalysisStatus(2);
                qaMapper.updateById(qa);
            }
            done++;
            updateAnalysisProgress(interviewId, (int) Math.round(done * 80.0 / Math.max(1, total)));
        }
        // 聚合报告（规则分与草稿分融合）+ 错题本 + 场景工作流
        aggregateAndStoreReport(interview);
        } finally {
            RUNNING_ANALYSIS.remove(interviewId);
        }
    }

    /** V2：更新分析进度（条件更新，仅分析中状态才推进，防越界覆盖） */
    private void updateAnalysisProgress(Long interviewId, int progress) {
        try {
            PortalVoiceInterview fresh = interviewMapper.selectById(interviewId);
            if (fresh != null && Integer.valueOf(1).equals(fresh.getAnalysisStatus())) {
                fresh.setAnalysisProgress(Math.max(fresh.getAnalysisProgress() == null ? 0 : fresh.getAnalysisProgress(), progress));
                interviewMapper.updateById(fresh);
            }
        } catch (Exception e) {
            log.warn("[VoiceInterview] 进度更新失败 interviewId={}：{}", interviewId, e.getMessage());
        }
    }

    /** V2：聚合报告并落库（原 finish 聚合段抽取；分数融合 score_draft；进度 100；幂等防重） */
    private void aggregateAndStoreReport(PortalVoiceInterview interview) {
        Long interviewId = interview.getId();
        // 幂等防重：已完成不再重算（并发触发保护）
        PortalVoiceInterview fresh = interviewMapper.selectById(interviewId);
        if (fresh == null || Integer.valueOf(2).equals(fresh.getAnalysisStatus())) {
            return;
        }
        if (StringUtils.isNotEmpty(fresh.getReport()) && Integer.valueOf(1).equals(fresh.getAnalysisStatus())) {
            // 已有报告（旧链路聚合过）仅推进进度
            fresh.setAnalysisStatus(2);
            fresh.setAnalysisProgress(100);
            interviewMapper.updateById(fresh);
            return;
        }

        List<PortalVoiceInterviewQA> qaList = listQaByInterview(interviewId);
        int answered = 0;
        long sum = 0;
        List<VoiceInterviewReportVO.QuestionReview> reviews = new ArrayList<>();
        Map<String, Integer> dimSums = new LinkedHashMap<>();
        // 维度 key 与逐题六维对齐（原 coverage/length/structure 旧 key 报告级断链）
        dimSums.put("relevance", 0);
        dimSums.put("professionalism", 0);
        dimSums.put("fluency", 0);
        dimSums.put("interactivity", 0);
        dimSums.put("confidence", 0);
        dimSums.put("logic", 0);
        int dimCount = 0;

        List<String> highlights = new ArrayList<>();
        List<String> weakPoints = new ArrayList<>();

        for (PortalVoiceInterviewQA qa : qaList) {
            // 分数融合——异步草稿分（LLM）优先按 70/30 融合，无草稿保持规则分
            int effectiveScore;
            if (qa.getScore() == null) {
                continue;
            }
            if (qa.getScoreDraft() != null) {
                effectiveScore = (int) Math.round(qa.getScoreDraft() * 0.7 + qa.getScore() * 0.3);
                qa.setScore(effectiveScore);
                qaMapper.updateById(qa);
            } else {
                effectiveScore = qa.getScore();
            }
            answered++;
            sum += effectiveScore;

            VoiceInterviewReportVO.QuestionReview review = new VoiceInterviewReportVO.QuestionReview();
            review.setQuestionIdx(qa.getQuestionIdx());
            review.setQuestion(qa.getQuestion());
            review.setScore(effectiveScore);
            review.setFeedback(qa.getAiFeedback());
            // 补原始作答与问答ID（前端折叠展示/加入错题本）
            review.setUserAnswer(qa.getUserAnswer());
            review.setQaId(qa.getId());
            reviews.add(review);

            if (effectiveScore >= 80) {
                highlights.add(qa.getQuestion());
            } else if (effectiveScore < 60) {
                weakPoints.add(qa.getQuestion());
            }

            // 累加维度分
            if (StringUtils.isNotEmpty(qa.getRuleDimensionsJson())) {
                try {
                    Map<String, Integer> dims = objectMapper.readValue(
                            qa.getRuleDimensionsJson(),
                            new com.fasterxml.jackson.core.type.TypeReference<Map<String, Integer>>() {});
                    for (Map.Entry<String, Integer> e : dims.entrySet()) {
                        dimSums.merge(e.getKey(), e.getValue(), Integer::sum);
                    }
                    dimCount++;
                } catch (Exception ignored) {
                }
            }
        }

        int avg = answered > 0 ? (int) Math.round((double) sum / answered) : 0;
        // v11.x C1：自我介绍分与技术均分按权重融合（默认 intro 20% + tech 80%）
        VoiceInterviewReportVO.IntroScoreView introScoreView = parseIntroScoreView(interview.getIntroScoreJson());
        PortalInterviewConfig finishConfig = loadInterviewConfigQuietly();
        int totalScore = introScoreView != null && introScoreView.getTotal() != null
                ? scoringEngine.fuseTotalScore(introScoreView.getTotal(), avg,
                        finishConfig == null ? null : finishConfig.getScoringWeights())
                : avg;
        fresh.setScore(totalScore);

        // 聚合逐轮 LLM 深度分析 → 心态趋势 / 可疑信号汇总 / 流畅度均分
        List<String> sentimentTrend = new ArrayList<>();
        List<String> allRedFlags = new ArrayList<>();
        int fluencySum = 0;
        int fluencyCount = 0;
        for (PortalVoiceInterviewQA qa : qaList) {
            if (StringUtils.isEmpty(qa.getLlmAnalysisJson())) {
                continue;
            }
            try {
                InterviewTurnResult turn = objectMapper.readValue(
                        qa.getLlmAnalysisJson(), InterviewTurnResult.class);
                if (turn.getSentiment() != null && StringUtils.isNotEmpty(turn.getSentiment().getState())) {
                    sentimentTrend.add(turn.getSentiment().getState());
                }
                for (String flag : turn.getRedFlags()) {
                    if (StringUtils.isNotEmpty(flag) && !allRedFlags.contains(flag) && allRedFlags.size() < 10) {
                        allRedFlags.add(flag);
                    }
                }
                if (turn.getFluencyAssessment() != null && turn.getFluencyAssessment().getScore() != null) {
                    fluencySum += turn.getFluencyAssessment().getScore();
                    fluencyCount++;
                }
            } catch (Exception ignored) {
            }
        }

        // 平均维度分
        Map<String, Integer> avgDimensions = new LinkedHashMap<>();
        if (dimCount > 0) {
            for (Map.Entry<String, Integer> e : dimSums.entrySet()) {
                avgDimensions.put(e.getKey(), e.getValue() / dimCount);
            }
        }

        // LLM 画像增强——薄弱点优先用追问中识别的真实漏洞，summary 融合水平评估
        Map<String, Object> cfg = readInterviewConfig(interview);
        List<String> profileGaps = (List<String>) cfg.get("profileGaps");
        if (profileGaps != null && !profileGaps.isEmpty()) {
            for (String gap : profileGaps) {
                if (!weakPoints.contains(gap)) {
                    weakPoints.add(gap);
                }
            }
        }
        String levelEstimate = String.valueOf(cfg.getOrDefault("levelEstimate", ""));

        VoiceInterviewReportVO report = new VoiceInterviewReportVO();
        report.setInterviewId(interviewId);
        report.setTotalScore(totalScore);
        report.setDimensions(avgDimensions);
        report.setHighlights(highlights);
        report.setWeakPoints(weakPoints);
        report.setQuestionReviews(reviews);
        String summary = buildSummary(interview.getTotalQa(), answered, avg);
        if (StringUtils.isNotEmpty(levelEstimate) && !"null".equals(levelEstimate)) {
            String levelText = "junior".equals(levelEstimate) ? "初级（基础需夯实）"
                    : "senior".equals(levelEstimate) ? "高级（具备体系化思维）" : "中级（框架完整，深度待补）";
            summary = summary + " 综合水平画像：" + levelText + "。";
        }
        report.setSummary(summary);
        report.setSuggestion(buildSuggestion(avg, weakPoints));
        // v11.x C2：自我介绍独立评分 + 针对性改进建议
        report.setIntroScore(introScoreView);
        report.setImprovementSuggestions(buildImprovementSuggestions(weakPoints, introScoreView));
        // 相关知识点生成移除（题库 tags 聚合对 agent 自由面试无参考意义，前端 Tab 已删）
        // LLM 深度分析聚合结果（Agent 模式产出；旧数据字段为空，前端按缺失隐藏）
        report.setSentimentTrend(sentimentTrend);
        report.setRedFlags(allRedFlags);
        if (fluencyCount > 0) {
            report.setFluencyAvg((int) Math.round((double) fluencySum / fluencyCount));
        }

        // v11.x C3：低分主问题自动入错题本（<60 分且来自题库，幂等累加 wrong_count）
        recordWrongQuestionsQuietly(interview, qaList);

        // V2：报告三段式——第一栏面试者简介（简历提取 + 口头自我介绍）
        report.setCandidate(buildCandidateProfile(interview, qaList));
        // V2：第二栏岗位信息（岗位 + JD + 匹配度）
        report.setJobInfo(buildJobInfo(interview, totalScore));

        // 整场 LLM 复盘——agent 直连通道增强报告（总评/匹配度/结构化亮点薄弱点/建议/
        // 逐题评分回填/六维），任何失败保留上方规则兜底（链路永远可用）
        updateAnalysisProgress(interviewId, 85);
        enhanceReportByAgent(interview, report, qaList, answered, avg);

        // 复盘后总分以报告为准（复盘回填逐题分会重算总分）
        fresh.setScore(report.getTotalScore());
        fresh.setSummary(report.getSummary());
        fresh.setReport(toJson(report));
        fresh.setAnalysisStatus(2);
        fresh.setAnalysisProgress(100);
        interviewMapper.updateById(fresh);

        // v11.x D4：场景绑定工作流时异步触发（报告归档/学习计划等），不阻塞主流程
        triggerSceneWorkflowAsync(fresh, report);
    }

    /**
     * V2：报告三段式第一栏——面试者简介（简历提取 + 面试口头自我介绍）。
     * <p>key：name 姓名 / skills 技能 / resumeSelfIntro 简历自我介绍 /
     * interviewSelfIntro 面试口头自我介绍 / aiScore 简历AI评分；未选简历时为空 Map，前端隐藏。
     */
    private Map<String, String> buildCandidateProfile(PortalVoiceInterview interview, List<PortalVoiceInterviewQA> qaList) {
        Map<String, String> candidate = new LinkedHashMap<>();
        try {
            if (interview.getResumeId() != null) {
                PortalUserResume resume = userResumeMapper.selectById(interview.getResumeId());
                if (resume != null) {
                    if (StringUtils.isNotEmpty(resume.getName())) {
                        candidate.put("name", resume.getName());
                    }
                    if (StringUtils.isNotEmpty(resume.getSkills())) {
                        // 技能 JSON 格式化（"Java·了解 / Python·了解"），不再透出原始 JSON
                        candidate.put("skills", formatSkills(resume.getSkills()));
                    }
                    if (StringUtils.isNotEmpty(resume.getSelfIntro())) {
                        candidate.put("resumeSelfIntro", resume.getSelfIntro());
                    }
                    if (resume.getScore() != null) {
                        candidate.put("aiScore", String.valueOf(resume.getScore()));
                    }
                }
            }
            // 面试口头自我介绍：第一道主问（V4 段序约束——第 1 问固定为自我介绍，
            // V3 首问 questionIdx=0，旧数据兼容 idx=1）
            for (PortalVoiceInterviewQA qa : qaList) {
                if (qa.getParentQaId() == null && qa.getQuestionIdx() != null && qa.getQuestionIdx() <= 1
                        && StringUtils.isNotEmpty(qa.getUserAnswer())) {
                    candidate.put("interviewSelfIntro", qa.getUserAnswer());
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("[VoiceInterview] 面试者简介构建失败 interviewId={}：{}", interview.getId(), e.getMessage());
        }
        return candidate;
    }

    /**
     * V2：报告三段式第二栏——岗位信息（岗位 + 岗位要求 JD + 匹配度）。
     * <p>key：position 岗位 / jobRequirements 岗位要求JD / matchRate 岗位匹配度(%)；
     * JD 优先取 configJson（start 时已存），回退 contextSnapshot。
     */
    private Map<String, String> buildJobInfo(PortalVoiceInterview interview, int totalScore) {
        Map<String, String> jobInfo = new LinkedHashMap<>();
        try {
            if (StringUtils.isNotEmpty(interview.getPosition())) {
                jobInfo.put("position", interview.getPosition());
            }
            Object jd = readInterviewConfig(interview).get("jobRequirements");
            if (jd == null && StringUtils.isNotEmpty(interview.getContextSnapshot())) {
                try {
                    Map<String, Object> snapshot = objectMapper.readValue(interview.getContextSnapshot(),
                            new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                    jd = snapshot.get("jobRequirements");
                } catch (Exception ignored) {
                }
            }
            if (jd != null && StringUtils.isNotEmpty(String.valueOf(jd))) {
                jobInfo.put("jobRequirements", String.valueOf(jd));
            }
            // 岗位匹配度：以综合得分近似（0-100）
            jobInfo.put("matchRate", String.valueOf(Math.max(0, Math.min(100, totalScore))));
        } catch (Exception e) {
            log.warn("[VoiceInterview] 岗位信息构建失败 interviewId={}：{}", interview.getId(), e.getMessage());
        }
        return jobInfo;
    }

    /** V2：报告分析状态查询（前端进度条轮询） */
    @Override
    public Map<String, Object> getAnalysisStatus(Long interviewId, Long userId) {
        PortalVoiceInterview interview = mustOwnInterview(interviewId, userId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("analysisStatus", interview.getAnalysisStatus() == null ? 0 : interview.getAnalysisStatus());
        result.put("analysisProgress", interview.getAnalysisProgress() == null ? 0 : interview.getAnalysisProgress());
        result.put("status", interview.getStatus());
        // 断链自愈：已结束且分析中，但本进程无运行任务（服务重启/任务丢失）→ 重触发，
        // 保证前端轮询永远能等到 analysisStatus=2（历史页进度轮询的数据一致性兜底）
        if ("finished".equals(interview.getStatus())
                && Integer.valueOf(1).equals(interview.getAnalysisStatus())
                && !RUNNING_ANALYSIS.contains(interviewId)) {
            log.warn("[VoiceInterview] 检测到中断的分析任务，自愈重触发 interviewId={}", interviewId);
            triggerBatchAnalysis(interviewId);
        }
        return result;
    }

    /**
     * 重新生成报告——重置分析状态后复用异步批量分析链路（逐题补分析 + 聚合 + 整场 LLM 复盘）。
     * <p>重置要点：主表必须清 report/summary（aggregateAndStoreReport 对报告非空仅推进进度直接返回）；
     * QA 必须清 scoreDraft（否则分数二次融合失真）与 analysisStatus（runBatchAnalysis 只处理未完成题）。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public VoiceInterviewReportVO regenerateReport(Long interviewId, Long userId) {
        PortalVoiceInterview interview = mustOwnInterview(interviewId, userId);
        if (!"finished".equals(interview.getStatus())) {
            throw new ServiceException("面试尚未结束，无法生成报告");
        }
        if (RUNNING_ANALYSIS.contains(interviewId)) {
            throw new ServiceException("报告正在生成中，请稍候");
        }
        // 1. 主表重置（report/summary 必须清空，否则聚合幂等分支直接 return）
        interview.setAnalysisStatus(1);
        interview.setAnalysisProgress(0);
        interview.setReport(null);
        interview.setSummary(null);
        interviewMapper.updateById(interview);
        // 2. QA 重置：已作答题清分析状态与草稿分（保留 score/aiFeedback 作为 LLM 参考输入与兜底）
        List<PortalVoiceInterviewQA> qaList = listQaByInterview(interviewId);
        for (PortalVoiceInterviewQA qa : qaList) {
            if (StringUtils.isEmpty(qa.getUserAnswer())) {
                continue;
            }
            qa.setAnalysisStatus(null);
            qa.setScoreDraft(null);
            qaMapper.updateById(qa);
        }
        recordEvent(interviewId, "regenerate_report", Map.of());
        // 3. 事务提交后触发异步分析（与 finish 同口径）
        triggerBatchAnalysis(interviewId);
        // 4. 返回骨架（前端轮询 analysis 接口直至 analysisStatus=2）
        VoiceInterviewReportVO skeleton = new VoiceInterviewReportVO();
        skeleton.setInterviewId(interviewId);
        skeleton.setTotalScore(interview.getScore());
        skeleton.setSummary("报告生成中…");
        return skeleton;
    }

    /** 解析自我介绍评分 JSON → 报告视图（旧会话/无自我介绍返回 null） */
    private VoiceInterviewReportVO.IntroScoreView parseIntroScoreView(String introScoreJson) {
        if (StringUtils.isEmpty(introScoreJson)) {
            return null;
        }
        try {
            return objectMapper.readValue(introScoreJson, VoiceInterviewReportVO.IntroScoreView.class);
        } catch (Exception e) {
            log.warn("[VoiceInterview] 自我介绍评分 JSON 解析失败：{}", e.getMessage());
            return null;
        }
    }

    @Override
    public String createShareToken(Long interviewId, Long userId, Integer expireDays) {
        PortalVoiceInterview interview = mustOwnInterview(interviewId, userId);
        if (!"finished".equals(interview.getStatus()) || StringUtils.isEmpty(interview.getReport())) {
            throw new com.moyun.common.exception.system.ServiceException("面试尚未结束或报告未生成，无法分享");
        }
        int days = expireDays == null ? 7 : Math.max(1, Math.min(30, expireDays));
        interview.setShareToken(java.util.UUID.randomUUID().toString().replace("-", ""));
        interview.setShareExpireTime(java.time.LocalDateTime.now().plusDays(days));
        interview.setShareCount(0);
        interviewMapper.updateById(interview);
        return interview.getShareToken();
    }

    @Override
    public VoiceInterviewReportVO getSharedReport(String shareToken) {
        if (StringUtils.isEmpty(shareToken) || shareToken.length() < 16 || shareToken.length() > 64) {
            return null;
        }
        PortalVoiceInterview interview = interviewMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PortalVoiceInterview>()
                        .eq(PortalVoiceInterview::getShareToken, shareToken)
                        .eq(PortalVoiceInterview::getStatus, "finished"));
        if (interview == null || StringUtils.isEmpty(interview.getReport())) {
            return null;
        }
        if (interview.getShareExpireTime() != null
                && interview.getShareExpireTime().isBefore(java.time.LocalDateTime.now())) {
            return null; // 已过期
        }
        // 访问计数（尽力而为，失败不影响展示）
        try {
            interview.setShareCount((interview.getShareCount() == null ? 0 : interview.getShareCount()) + 1);
            interviewMapper.updateById(interview);
        } catch (Exception ignored) {
        }
        return parseReport(interview);
    }
    /**
     * 整场 LLM 复盘——规则聚合完成后，走 agent 直连通道（与主对话同链路，实测可用）
     * 基于简历 + 岗位 + 全部问答对生成结构化报告增强：总评/岗位匹配度/结构化亮点薄弱点/
     * 可执行建议/逐题评分回填/六维。任何失败保留规则兜底（链路永远可用）。
     * <p>不注入 agent.systemPrompt——报告分析是代码级任务，与面试官 persona 无关。</p>
     */
    private void enhanceReportByAgent(PortalVoiceInterview interview, VoiceInterviewReportVO report,
                                      List<PortalVoiceInterviewQA> qaList, int answeredCount, int ruleAvg) {
        try {
            // 热回退开关（缺省开启）
            try {
                String sw = sysConfigService.selectConfigByKey("voice.interview.reportLlm.enabled");
                if (sw != null && "false".equalsIgnoreCase(sw.trim())) {
                    return;
                }
            } catch (Exception ignored) {
            }
            Agent agent = agentClient.resolveAgent(interview.getAgentId());
            if (agent == null || !agentClient.isEnabled()) {
                return;
            }
            // ---------- 组装输入：岗位 + JD + 简历摘要 + 全部问答对 ----------
            String position = StringUtils.isEmpty(interview.getPosition()) ? "综合" : interview.getPosition();
            String jobRequirements = readConfigKey(interview, "jobRequirements");
            String resumeDigest = interview.getResumeId() == null ? null
                    : buildResumeDigest(interview.getResumeId());
            StringBuilder input = new StringBuilder();
            input.append("目标岗位：").append(position).append('\n');
            if (StringUtils.isNotEmpty(jobRequirements)) {
                input.append("岗位要求：").append(truncateText(jobRequirements, 600)).append('\n');
            }
            if (StringUtils.isNotEmpty(resumeDigest)) {
                input.append("候选人简历摘要：\n").append(truncateText(resumeDigest, 800)).append('\n');
            }
            input.append("整场对话记录（含每题初评分，供参考）：\n");
            for (PortalVoiceInterviewQA qa : qaList) {
                if (StringUtils.isEmpty(qa.getUserAnswer())) {
                    continue;
                }
                input.append("【第").append(qa.getQuestionIdx()).append("题】")
                        .append(truncateText(qa.getQuestion(), 150)).append('\n')
                        .append("候选人回答：").append(truncateText(qa.getUserAnswer(), 400)).append('\n')
                        .append("初评分：").append(qa.getScore() == null ? 50 : qa.getScore()).append('\n');
            }
            input.append("请输出整场面试复盘报告 JSON。");

            String systemPrompt = "你是一位资深技术面试官，面试已结束，请基于候选人简历、目标岗位与整场对话记录，"
                    + "输出结构化复盘报告 JSON。字段要求：\n"
                    + "1. overallComment：3-5 句整场总评，结合岗位要求评价整体表现，指出最突出的特点。\n"
                    + "2. jobMatch：{rate: 0-100 整数匹配度, reason: 1-2 句依据（对照岗位要求与实际作答）}。\n"
                    + "3. highlights：2-4 条真实亮点数组，每条 {title: 短标题≤12字, detail: 引用作答中的具体内容说明}。\n"
                    + "4. weakPoints：2-4 条薄弱点数组，每条 {title: 短标题≤12字, detail: 具体不足与影响，禁止复述问题原文}。\n"
                    + "5. suggestions：3-5 条可执行改进建议字符串数组，结合简历与岗位，每条不超过 60 字。\n"
                    + "6. perQuestion：每道主问题一条 {questionIdx: 题号, score: 0-100 整数（评分要有区分度："
                    + "优秀≥80、合格60-79、不合格<60）, comment: 1-2 句针对性点评≤80字}。\n"
                    + "7. dimensions：{relevance 切题度, professionalism 专业深度, fluency 表达流畅, "
                    + "interactivity 互动质量, confidence 自信度, logic 逻辑结构}，0-100 整数。\n"
                    + "只输出 JSON 对象，不要输出任何其他文本。";

            List<ChatMessage> messages = new ArrayList<>();
            messages.add(SystemMessage.from(systemPrompt));
            messages.add(new UserMessage(input.toString()));

            String raw = agentClient.chat(agent, messages);
            JsonNode node = extractJsonObject(raw);
            if (node == null) {
                // 降级重试一次：追加严格约束（对齐 chatJson 模式）
                messages.add(new UserMessage("你上一条输出无法解析为 JSON。请重新输出，且只输出一个合法的 JSON 对象，"
                        + "以 { 开头、以 } 结尾，不要包含任何解释、Markdown 代码块或其他文本。"));
                raw = agentClient.chat(agent, messages);
                node = extractJsonObject(raw);
            }
            if (node == null) {
                log.warn("[VoiceInterview] 整场复盘解析失败，保留规则兜底 interviewId={}", interview.getId());
                return;
            }

            // ---------- 总评 / 匹配度 ----------
            String overallComment = node.path("overallComment").asText("");
            if (StringUtils.isNotEmpty(overallComment)) {
                report.setOverallComment(overallComment);
            }
            JsonNode jobMatchNode = node.path("jobMatch");
            if (jobMatchNode.has("rate")) {
                VoiceInterviewReportVO.JobMatchView jobMatch = new VoiceInterviewReportVO.JobMatchView();
                jobMatch.setRate(clamp(jobMatchNode.path("rate").asInt(ruleAvg), 0, 100));
                jobMatch.setReason(jobMatchNode.path("reason").asText(""));
                report.setJobMatch(jobMatch);
                if (report.getJobInfo() != null) {
                    report.getJobInfo().put("matchRate", String.valueOf(jobMatch.getRate()));
                }
            }

            // ---------- 结构化亮点 / 薄弱点（双写兼容旧字段） ----------
            List<String> highlightTitles = parsePointViews(node.path("highlights"), report::setHighlightViews);
            if (!highlightTitles.isEmpty()) {
                report.setHighlights(highlightTitles);
            }
            List<String> weakTitles = parsePointViews(node.path("weakPoints"), report::setWeakPointViews);
            if (!weakTitles.isEmpty()) {
                report.setWeakPoints(weakTitles);
            }

            // ---------- 可执行建议（非空覆盖模板文案） ----------
            List<String> suggestions = new ArrayList<>();
            for (JsonNode s : node.path("suggestions")) {
                String t = s.asText("").trim();
                if (StringUtils.isNotEmpty(t)) {
                    suggestions.add(t);
                }
            }
            if (!suggestions.isEmpty()) {
                report.setImprovementSuggestions(suggestions);
            }

            // ---------- 六维 ----------
            JsonNode dimsNode = node.path("dimensions");
            if (dimsNode.isObject()) {
                Map<String, Integer> dims = new LinkedHashMap<>();
                String[] dimKeys = {"relevance", "professionalism", "fluency", "interactivity", "confidence", "logic"};
                for (String key : dimKeys) {
                    int v = dimsNode.path(key).asInt(-1);
                    if (v >= 0) {
                        dims.put(key, clamp(v, 0, 100));
                    }
                }
                if (dims.size() == dimKeys.length) {
                    report.setDimensions(dims);
                }
            }

            // ---------- 逐题评分回填（覆盖 reviews + QA 表） ----------
            Map<Integer, PortalVoiceInterviewQA> mainQaByIdx = new LinkedHashMap<>();
            for (PortalVoiceInterviewQA qa : qaList) {
                if (qa.getParentQaId() == null && qa.getQuestionIdx() != null
                        && !mainQaByIdx.containsKey(qa.getQuestionIdx())) {
                    mainQaByIdx.put(qa.getQuestionIdx(), qa);
                }
            }
            long scoreSum = 0;
            int scoreCount = 0;
            for (JsonNode pq : node.path("perQuestion")) {
                int idx = pq.path("questionIdx").asInt(-1);
                int score = clamp(pq.path("score").asInt(-1), 0, 100);
                String comment = pq.path("comment").asText("");
                if (idx < 0 || score < 0) {
                    continue;
                }
                for (VoiceInterviewReportVO.QuestionReview review : report.getQuestionReviews()) {
                    if (review.getQuestionIdx() != null && review.getQuestionIdx() == idx) {
                        review.setScore(score);
                        if (StringUtils.isNotEmpty(comment)) {
                            review.setFeedback(comment);
                        }
                        break;
                    }
                }
                PortalVoiceInterviewQA qa = mainQaByIdx.get(idx);
                if (qa != null) {
                    qa.setScore(score);
                    if (StringUtils.isNotEmpty(comment)) {
                        qa.setAiFeedback(comment);
                    }
                    qa.setScoreDraft(score);
                    qaMapper.updateById(qa);
                }
                scoreSum += score;
                scoreCount++;
            }

            // ---------- 重算总分（复盘逐题分优先；与自我介绍分按权重融合，同规则链路口径） ----------
            if (scoreCount > 0) {
                int llmAvg = (int) Math.round((double) scoreSum / scoreCount);
                VoiceInterviewReportVO.IntroScoreView introScoreView = report.getIntroScore();
                PortalInterviewConfig finishConfig = loadInterviewConfigQuietly();
                int newTotal = introScoreView != null && introScoreView.getTotal() != null
                        ? scoringEngine.fuseTotalScore(introScoreView.getTotal(), llmAvg,
                                finishConfig == null ? null : finishConfig.getScoringWeights())
                        : llmAvg;
                report.setTotalScore(newTotal);
                // 概要文案同步复盘结论（避免模板文案与逐题分脱节）
                report.setSummary("本次面试共 " + report.getQuestionReviews().size() + " 题。"
                        + (overallComment.isEmpty() ? "" : overallComment));
            }
            updateAnalysisProgress(interview.getId(), 92);
            log.info("[VoiceInterview] 整场复盘完成 interviewId={}：总分 {}，亮点 {} 条，薄弱点 {} 条",
                    interview.getId(), report.getTotalScore(),
                    report.getHighlightViews() == null ? 0 : report.getHighlightViews().size(),
                    report.getWeakPointViews() == null ? 0 : report.getWeakPointViews().size());
        } catch (Exception e) {
            log.warn("[VoiceInterview] 整场复盘异常（保留规则兜底）interviewId={}：{}", interview.getId(), e.getMessage());
        }
    }

    /** 解析亮点/薄弱点数组 → 结构化视图（setter 注入）+ 返回标题列表（旧字段双写） */
    private List<String> parsePointViews(JsonNode arr,
                                         java.util.function.Consumer<List<VoiceInterviewReportVO.PointView>> setter) {
        List<String> titles = new ArrayList<>();
        if (arr == null || !arr.isArray()) {
            return titles;
        }
        List<VoiceInterviewReportVO.PointView> views = new ArrayList<>();
        for (JsonNode item : arr) {
            if (views.size() >= 4) {
                break;
            }
            String title = item.path("title").asText("").trim();
            if (StringUtils.isEmpty(title)) {
                continue;
            }
            VoiceInterviewReportVO.PointView view = new VoiceInterviewReportVO.PointView();
            view.setTitle(title);
            view.setDetail(item.path("detail").asText("").trim());
            views.add(view);
            titles.add(title);
        }
        if (!views.isEmpty()) {
            setter.accept(views);
        }
        return titles;
    }

    /** 容错提取 JSON 对象主体（剥 Markdown 围栏/前后杂文本；对齐 ai2 Handler 解析口径） */
    private JsonNode extractJsonObject(String raw) {
        if (StringUtils.isEmpty(raw)) {
            return null;
        }
        String text = raw.trim();
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            try {
                return objectMapper.readTree(text.substring(start, end + 1));
            } catch (Exception ignored) {
            }
        }
        try {
            return objectMapper.readTree(text);
        } catch (Exception e) {
            return null;
        }
    }

    /** 文本截断（超长加省略号） */
    private String truncateText(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        String flat = text.replaceAll("\\s+", " ").trim();
        return flat.length() > maxLen ? flat.substring(0, maxLen) + "…" : flat;
    }

    /**
     * 技能 JSON 格式化——{"Java":{"level":"了解"}} 或数组 → "Java·了解 / Python·了解"；
     * 解析失败原样返回（兜底存量/异构数据）。
     */
    private String formatSkills(String rawSkills) {
        if (StringUtils.isEmpty(rawSkills) || !rawSkills.trim().startsWith("{")) {
            return rawSkills;
        }
        try {
            JsonNode node = objectMapper.readTree(rawSkills);
            if (!node.isObject()) {
                return rawSkills;
            }
            List<String> parts = new ArrayList<>();
            java.util.Iterator<Map.Entry<String, JsonNode>> it = node.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> entry = it.next();
                String name = entry.getKey();
                String level = entry.getValue().isContainerNode()
                        ? entry.getValue().path("level").asText("") : entry.getValue().asText("");
                parts.add(StringUtils.isEmpty(level) ? name : name + "·" + level);
            }
            return String.join(" / ", parts);
        } catch (Exception e) {
            return rawSkills;
        }
    }

    private List<String> buildImprovementSuggestions(List<String> weakPoints,
                                                      VoiceInterviewReportVO.IntroScoreView introScore) {
        List<String> suggestions = new ArrayList<>();
        if (weakPoints != null) {
            for (String wp : weakPoints) {
                if (suggestions.size() >= 3) {
                    break;
                }
                suggestions.add("针对薄弱点「" + wp + "」做专项复习，可结合错题本巩固。");
            }
        }
        if (introScore != null && introScore.getWeaknesses() != null) {
            for (String wk : introScore.getWeaknesses()) {
                if (StringUtils.isNotEmpty(wk) && suggestions.size() < 5) {
                    suggestions.add("自我介绍改进：" + wk);
                }
            }
        }
        if (suggestions.isEmpty()) {
            suggestions.add("整体表现均衡，建议挑战更高难度的面试场景以突破上限。");
        }
        return suggestions;
    }

    /** 低分主问题自动入错题本（<60 分、题库来源、非追问；失败不影响报告生成） */
    private void recordWrongQuestionsQuietly(PortalVoiceInterview interview, List<PortalVoiceInterviewQA> qaList) {
        for (PortalVoiceInterviewQA qa : qaList) {
            if (qa.getScore() == null || qa.getScore() >= 60
                    || qa.getQuestionId() == null
                    || qa.getParentQaId() != null
                    || "self_intro".equals(qa.getQuestionSource())) {
                continue;
            }
            try {
                wrongQuestionService.recordWrongQuestion(interview.getUserId(), qa.getQuestionId(), qa.getId());
            } catch (Exception e) {
                log.warn("[VoiceInterview] 错题入库失败 qaId={}：{}", qa.getId(), e.getMessage());
            }
        }
    }

    // ========================================================================
    // 管理端（Admin 复盘：不校验用户归属）
    // ========================================================================

    @Override
    public Page<PortalVoiceInterview> adminList(String username, String position, String status,
                                                  Integer pageNum, Integer pageSize) {
        Page<PortalVoiceInterview> page = new Page<>(
                pageNum == null || pageNum < 1 ? 1 : pageNum,
                pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100));
        LambdaQueryWrapper<PortalVoiceInterview> qw = Wrappers.<PortalVoiceInterview>lambdaQuery()
                .eq(PortalVoiceInterview::getDelFlag, "0")
                .eq(StringUtils.isNotEmpty(position), PortalVoiceInterview::getPosition, position)
                .eq(StringUtils.isNotEmpty(status), PortalVoiceInterview::getStatus, status)
                .orderByDesc(PortalVoiceInterview::getCreateTime);
        // username 筛选：先按 portal_user.username like 反查 userId 集合
        if (StringUtils.isNotEmpty(username)) {
            List<Long> userIds = portalUserMapper.selectList(
                            Wrappers.<com.moyun.portal.domain.entity.PortalUser>lambdaQuery()
                                    .select(com.moyun.portal.domain.entity.PortalUser::getId)
                                    .like(com.moyun.portal.domain.entity.PortalUser::getUsername, username))
                    .stream().map(com.moyun.portal.domain.entity.PortalUser::getId)
                    .collect(java.util.stream.Collectors.toList());
            if (userIds.isEmpty()) {
                return new Page<>(page.getCurrent(), page.getSize());
            }
            qw.in(PortalVoiceInterview::getUserId, userIds);
        }
        return interviewMapper.selectPage(page, qw);
    }

    @Override
    public VoiceInterviewVO adminGetDetail(Long interviewId) {
        PortalVoiceInterview interview = interviewMapper.selectById(interviewId);
        if (interview == null || "2".equals(interview.getDelFlag())) {
            throw new ServiceException("面试会话不存在");
        }
        List<PortalVoiceInterviewQA> qaList = listQaByInterview(interviewId);
        VoiceInterviewVO vo = toVO(interview);
        List<VoiceInterviewQaVO> qaVOList = new ArrayList<>();
        for (PortalVoiceInterviewQA qa : qaList) {
            qaVOList.add(toQaVO(qa));
        }
        vo.setQaList(qaVOList);
        vo.setCurrentQa(qaVOList.isEmpty() ? null : qaVOList.get(qaVOList.size() - 1));
        return vo;
    }

    @Override
    public boolean adminDelete(Long interviewId) {
        PortalVoiceInterview interview = interviewMapper.selectById(interviewId);
        if (interview == null || "2".equals(interview.getDelFlag())) {
            throw new ServiceException("面试会话不存在");
        }
        return interviewMapper.deleteById(interviewId) > 0;
    }

    /** v11.x D4：场景绑定 workflowId 时异步执行工作流（输入=面试报告上下文），失败仅记日志 */
    private void triggerSceneWorkflowAsync(PortalVoiceInterview interview, VoiceInterviewReportVO report) {
        try {
            AiSceneBinding sceneBinding = agentClient.resolveScene(SCENE_VOICE_INTERVIEW);
            Long workflowId = sceneBinding == null ? null : sceneBinding.getWorkflowId();
            if (workflowId == null) {
                return;
            }
            Map<String, Object> input = new LinkedHashMap<>();
            input.put("interviewId", interview.getId());
            input.put("userId", interview.getUserId());
            input.put("position", interview.getPosition());
            input.put("scene", interview.getScene());
            input.put("totalScore", report.getTotalScore());
            input.put("summary", report.getSummary());
            input.put("weakPoints", report.getWeakPoints());
            input.put("improvementSuggestions", report.getImprovementSuggestions());
            input.put("trigger", "interview_finished");
            aiTaskExecutor.execute(() -> {
                try {
                    aiWorkflowService.execute(workflowId, input);
                    log.info("[VoiceInterview] 场景工作流已触发 interviewId={} workflowId={}",
                            interview.getId(), workflowId);
                } catch (Exception e) {
                    log.warn("[VoiceInterview] 场景工作流执行失败 interviewId={} workflowId={}：{}",
                            interview.getId(), workflowId, e.getMessage());
                }
            });
        } catch (Exception e) {
            log.warn("[VoiceInterview] 场景工作流触发跳过：{}", e.getMessage());
        }
    }

    // ========================================================================
    // 列表 / 详情
    // ========================================================================
    @Override
    public Page<PortalVoiceInterview> listMy(Long userId, PageDomain pageDomain) {
        if (userId == null) {
            throw new ServiceException("请登录后操作");
        }
        Page<PortalVoiceInterview> page = PageUtils.buildPage(pageDomain);
        LambdaQueryWrapper<PortalVoiceInterview> qw = Wrappers.<PortalVoiceInterview>lambdaQuery()
                .eq(PortalVoiceInterview::getUserId, userId)
                .eq(PortalVoiceInterview::getDelFlag, "0")
                .orderByDesc(PortalVoiceInterview::getCreateTime);
        return interviewMapper.selectPage(page, qw);
    }

    @Override
    public Map<String, Object> getActiveInterview(Long userId) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (userId == null) {
            return result;
        }
        PortalVoiceInterview interview = interviewMapper.selectOne(Wrappers.<PortalVoiceInterview>lambdaQuery()
                .eq(PortalVoiceInterview::getUserId, userId)
                .eq(PortalVoiceInterview::getStatus, "in_progress")
                .eq(PortalVoiceInterview::getDelFlag, "0")
                .orderByDesc(PortalVoiceInterview::getId)
                .last("LIMIT 1"));
        if (interview == null) {
            return result;
        }
        // 已答题数（提交过回答的 QA）
        Long answered = qaMapper.selectCount(Wrappers.<PortalVoiceInterviewQA>lambdaQuery()
                .eq(PortalVoiceInterviewQA::getInterviewId, interview.getId())
                .isNotNull(PortalVoiceInterviewQA::getUserAnswer));
        // 中断前已用时长锚点：最后作答时间 > 当前题创建时间 > 会话开始时间
        PortalVoiceInterviewQA lastQa = qaMapper.selectOne(Wrappers.<PortalVoiceInterviewQA>lambdaQuery()
                .eq(PortalVoiceInterviewQA::getInterviewId, interview.getId())
                .orderByDesc(PortalVoiceInterviewQA::getId)
                .last("LIMIT 1"));
        java.time.LocalDateTime anchor = interview.getCreateTime();
        if (lastQa != null) {
            anchor = lastQa.getAnswerTime() != null ? lastQa.getAnswerTime()
                    : (lastQa.getCreateTime() != null ? lastQa.getCreateTime() : anchor);
        }
        long elapsedSec = anchor == null || interview.getCreateTime() == null ? 0
                : Math.max(0, java.time.temporal.ChronoUnit.SECONDS.between(interview.getCreateTime(), anchor));

        result.put("interviewId", interview.getId());
        result.put("position", interview.getPosition());
        result.put("scene", interview.getScene());
        result.put("startTime", interview.getCreateTime());
        result.put("answered", answered == null ? 0 : answered.intValue());
        result.put("totalQa", interview.getTotalQa());
        result.put("elapsedSec", elapsedSec);
        return result;
    }

    @Override
    public VoiceInterviewVO resumeInterview(Long interviewId, Long userId) {
        PortalVoiceInterview interview = mustOwnInterview(interviewId, userId);
        if (!"in_progress".equals(interview.getStatus())) {
            throw new ServiceException("该面试已结束，无法继续");
        }
        // 断点续接：记录恢复事件（全链路可追溯）
        recordEvent(interviewId, "resume", Map.of(
                "currentIdx", interview.getCurrentIdx() == null ? 0 : interview.getCurrentIdx(),
                "phase", interview.getPhase() == null ? "" : interview.getPhase()));
        // V3：滑窗仍在则直接复用；Redis 已过期则按 DB 问答逐对重建（保证续接后上下文连贯）
        rebuildMemoryIfNeeded(interview);
        // 返回恢复快照：getDetail 含 qaList（历史问答）+ currentQa（待答题）
        return getDetail(interviewId, userId);
    }

    /** V3 断点续接滑窗重建：system+上下文按 start 规则重注入，DB QA 逐对追加 */
    private void rebuildMemoryIfNeeded(PortalVoiceInterview interview) {
        try {
            Agent agent = agentClient.resolveAgent(interview.getAgentId());
            if (agent == null) {
                return;
            }
            MessageWindowChatMemory memory = memoryService.getMemory(interview.getId(), agent.getMaxHistoryTurns());
            if (!memory.messages().isEmpty()) {
                return;
            }
            // system + 上下文 user（与 start 同源：contextSnapshot）
            String systemPrompt = buildInterviewerSystemPrompt(interview, agent);
            String jobRequirements = readConfigKey(interview, "jobRequirements");
            String resumeDigest = readConfigKey(interview, "resumeDigest");
            StringBuilder ctx = new StringBuilder("面试背景信息：\n岗位："
                    + (interview.getPosition() == null ? "" : interview.getPosition())
                    + "\n难度：" + (interview.getDifficulty() == null ? "medium" : interview.getDifficulty()));
            if (StringUtils.isNotEmpty(jobRequirements)) {
                ctx.append("\n岗位要求JD：\n").append(jobRequirements);
            }
            if (StringUtils.isNotEmpty(resumeDigest)) {
                ctx.append("\n简历摘要：\n").append(resumeDigest);
            }
            String contextUserMsg = PromptInjectionGuard.wrapData("候选人资料", ctx.toString());

            // DB QA 逐对重建（首题 QA 的 speakText 即开场白 assistant 消息）
            List<ChatMessage> qaPairs = new ArrayList<>();
            for (PortalVoiceInterviewQA qa : listQaByInterview(interview.getId())) {
                if (StringUtils.isNotEmpty(qa.getSpeakText())) {
                    qaPairs.add(new AiMessage(qa.getSpeakText()));
                }
                if (StringUtils.isNotEmpty(qa.getUserAnswer())) {
                    qaPairs.add(new UserMessage(qa.getUserAnswer()));
                }
            }
            memoryService.rebuildFromDb(interview.getId(), agent.getMaxHistoryTurns(),
                    systemPrompt, contextUserMsg, qaPairs);
        } catch (Exception e) {
            log.warn("[VoiceInterview] 滑窗重建失败 interviewId={}：{}", interview.getId(), e.getMessage());
        }
    }

    /** 从 configJson 读字符串键（resume 重建上下文用） */
    private String readConfigKey(PortalVoiceInterview interview, String key) {
        try {
            if (StringUtils.isEmpty(interview.getConfigJson())) {
                return "";
            }
            JsonNode node = objectMapper.readTree(interview.getConfigJson());
            return node.path(key).asText("");
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 断点续接收口：开始新面试前，遗留的进行中会话自动结束（closed_reason=abandon）
     * 并触发异步批量分析——数据不丢，报告保留在历史记录可查看。
     */
    private void closeStaleInterviews(Long userId) {
        try {
            List<PortalVoiceInterview> staleList = interviewMapper.selectList(Wrappers.<PortalVoiceInterview>lambdaQuery()
                    .eq(PortalVoiceInterview::getUserId, userId)
                    .eq(PortalVoiceInterview::getStatus, "in_progress")
                    .eq(PortalVoiceInterview::getDelFlag, "0"));
            for (PortalVoiceInterview stale : staleList) {
                stale.setStatus("finished");
                stale.setClosedReason("abandon");
                interviewMapper.updateById(stale);
                recordEvent(stale.getId(), "close", Map.of("reason", "abandon"));
                try {
                    triggerBatchAnalysis(stale.getId());
                } catch (Exception ex) {
                    log.warn("[VoiceInterview] 遗留会话批量分析触发失败 interviewId={}：{}", stale.getId(), ex.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("[VoiceInterview] 收口遗留进行中会话失败 userId={}：{}", userId, e.getMessage());
        }
    }

    @Override
    public VoiceInterviewVO getDetail(Long interviewId, Long userId) {
        PortalVoiceInterview interview = mustOwnInterview(interviewId, userId);
        List<PortalVoiceInterviewQA> qaList = listQaByInterview(interviewId);
        VoiceInterviewVO vo = toVO(interview);
        List<VoiceInterviewQaVO> qaVOList = new ArrayList<>();
        for (PortalVoiceInterviewQA qa : qaList) {
            qaVOList.add(toQaVO(qa));
        }
        vo.setQaList(qaVOList);
        // 当前 QA = currentIdx 对应的最后一条
        VoiceInterviewQaVO currentQa = qaVOList.isEmpty() ? null
                : qaVOList.get(qaVOList.size() - 1);
        vo.setCurrentQa(currentQa);
        return vo;
    }

    @Override
    public VoiceInterviewVO getDetailByQaId(Long qaId, Long userId) {
        PortalVoiceInterviewQA qa = qaMapper.selectById(qaId);
        if (qa == null || "2".equals(qa.getDelFlag())) {
            return null;
        }
        // 校验归属：通过 interviewId 反查面试主表，确认属于当前用户
        PortalVoiceInterview interview = mustOwnInterview(qa.getInterviewId(), userId);
        VoiceInterviewVO vo = toVO(interview);
        vo.setCurrentQa(toQaVO(qa));
        return vo;
    }

    // ========================================================================
    // 报告辅助
    // ========================================================================

    private String buildSummary(int total, int answered, int avg) {
        StringBuilder sb = new StringBuilder();
        sb.append("本次面试共 ").append(total).append(" 题，作答 ").append(answered).append(" 题。");
        if (avg >= 80) sb.append("整体表现优秀，知识点掌握扎实。");
        else if (avg >= 60) sb.append("整体表现良好，部分知识点需加强。");
        else sb.append("整体表现一般，建议针对薄弱点深入复习。");
        return sb.toString();
    }

    private String buildSuggestion(int avg, List<String> weakPoints) {
        if (weakPoints.isEmpty()) {
            return "继续保持，挑战更高难度的题目。";
        }
        return "建议重点复习以下薄弱知识点：" + String.join("、", weakPoints) + "。可通过错题本针对性练习。";
    }

    private VoiceInterviewReportVO parseReport(PortalVoiceInterview interview) {
        if (StringUtils.isEmpty(interview.getReport())) {
            VoiceInterviewReportVO report = new VoiceInterviewReportVO();
            report.setInterviewId(interview.getId());
            report.setTotalScore(interview.getScore());
            report.setSummary(interview.getSummary());
            return report;
        }
        try {
            return objectMapper.readValue(interview.getReport(), VoiceInterviewReportVO.class);
        } catch (Exception e) {
            VoiceInterviewReportVO report = new VoiceInterviewReportVO();
            report.setInterviewId(interview.getId());
            report.setTotalScore(interview.getScore());
            report.setSummary(interview.getSummary());
            return report;
        }
    }

    // ========================================================================
    // VO 组装
    // ========================================================================

    private VoiceInterviewVO assembleVO(PortalVoiceInterview interview, PortalVoiceInterviewQA currentQa) {
        VoiceInterviewVO vo = toVO(interview);
        if (currentQa != null) {
            vo.setCurrentQa(toQaVO(currentQa));
            vo.setGreetText(currentQa.getSpeakText());
        }
        return vo;
    }

    private VoiceInterviewVO toVO(PortalVoiceInterview interview) {
        VoiceInterviewVO vo = new VoiceInterviewVO();
        vo.setId(interview.getId());
        vo.setUserId(interview.getUserId());
        vo.setPosition(interview.getPosition());
        vo.setResumeId(interview.getResumeId());
        // agent 绑定信息（agentName 供前端顶栏展示）
        vo.setAgentId(interview.getAgentId());
        if (interview.getAgentId() != null) {
            vo.setAgentName(agentClient.agentName(interview.getAgentId()));
        }
        vo.setStatus(interview.getStatus());
        // v11.x 状态机阶段（NULL=旧流程）
        vo.setPhase(interview.getPhase());
        if (interview.getPhase() != null) {
            InterviewPhase p = InterviewPhase.fromCode(interview.getPhase());
            vo.setPhaseLabel(p == null ? null : p.getLabel());
        }
        vo.setDifficulty(interview.getDifficulty());
        vo.setTotalQa(interview.getTotalQa());
        vo.setCurrentIdx(interview.getCurrentIdx());
        vo.setScore(interview.getScore());
        vo.setSummary(interview.getSummary());
        vo.setConfigJson(interview.getConfigJson());
        vo.setCreateTime(interview.getCreateTime());
        // 报告生成状态（历史页进度展示与轮询）+ 本场时长（前端全场倒计时）
        vo.setAnalysisStatus(interview.getAnalysisStatus());
        vo.setAnalysisProgress(interview.getAnalysisProgress());
        vo.setDurationMinutes(durationOf(interview));
        return vo;
    }

    private VoiceInterviewQaVO toQaVO(PortalVoiceInterviewQA qa) {
        VoiceInterviewQaVO vo = new VoiceInterviewQaVO();
        vo.setId(qa.getId());
        vo.setInterviewId(qa.getInterviewId());
        vo.setQuestionId(qa.getQuestionId());
        vo.setQuestionSource(qa.getQuestionSource());
        vo.setQuestionIdx(qa.getQuestionIdx());
        vo.setParentQaId(qa.getParentQaId());
        vo.setQuestion(qa.getQuestion());
        vo.setUserAnswer(qa.getUserAnswer());
        vo.setTranscriptionEdited(qa.getTranscriptionEdited());
        vo.setAiFeedback(qa.getAiFeedback());
        vo.setSpeakText(qa.getSpeakText());
        vo.setScore(qa.getScore());
        vo.setRuleDimensionsJson(qa.getRuleDimensionsJson());
        vo.setHintUsed(qa.getHintUsed());
        vo.setLatencyMs(qa.getLatencyMs());
        vo.setNextAction(qa.getNextAction());
        vo.setCreateTime(qa.getCreateTime());
        return vo;
    }

    // ========================================================================
    // 辅助方法
    // ========================================================================

    private PortalVoiceInterview mustOwnInterview(Long interviewId, Long userId) {
        PortalVoiceInterview interview = interviewMapper.selectById(interviewId);
        if (interview == null || "2".equals(interview.getDelFlag())) {
            throw new ServiceException("面试记录不存在");
        }
        if (!userId.equals(interview.getUserId())) {
            throw new ServiceException("无权操作他人面试记录");
        }
        return interview;
    }

    private List<PortalVoiceInterviewQA> listQaByInterview(Long interviewId) {
        LambdaQueryWrapper<PortalVoiceInterviewQA> qw = Wrappers.<PortalVoiceInterviewQA>lambdaQuery()
                .eq(PortalVoiceInterviewQA::getInterviewId, interviewId)
                .eq(PortalVoiceInterviewQA::getDelFlag, "0")
                .orderByAsc(PortalVoiceInterviewQA::getId);
        return qaMapper.selectList(qw);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("[VoiceInterview] JSON 序列化失败：{}", e.getMessage());
            return null;
        }
    }

}
