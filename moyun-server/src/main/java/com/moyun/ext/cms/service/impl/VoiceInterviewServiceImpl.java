package com.moyun.ext.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.core.base.page.PageDomain;
import com.moyun.ext.ai.entity.Agent;
import com.moyun.ext.cms.domain.vo.HintVO;
import com.moyun.ext.cms.domain.vo.UserProfileSnapshotVO;
import com.moyun.ext.cms.domain.vo.VoiceInterviewQaVO;
import com.moyun.ext.cms.domain.vo.VoiceInterviewReportVO;
import com.moyun.ext.cms.domain.vo.VoiceInterviewVO;
import com.moyun.ext.cms.domain.vo.VoiceStartConfig;
import com.moyun.ext.cms.config.AiProperties;
import com.moyun.ext.cms.service.IUserProfileSnapshotService;
import com.moyun.ext.cms.service.IVoiceInterviewService;
import com.moyun.ext.cms.service.LlmClient;
import com.moyun.ext.cms.service.interview.HintEngine;
import com.moyun.ext.cms.service.interview.InterviewAgentClient;
import com.moyun.ext.cms.service.interview.InterviewAnalysisParser;
import com.moyun.ext.cms.service.interview.InterviewDecisionPolicy;
import com.moyun.ext.cms.service.interview.InterviewPromptAssembler;
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
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 语音面试官 Service 实现（V10.1 MVP）
 *
 * <p>核心设计：
 * <ul>
 *   <li>出题：复用 MockInterview 三路召回模式（画像薄弱点 + 岗位必备 + 随机兜底）</li>
 *   <li>评分：规则版（关键词覆盖率 + 长度奖励），与 MockInterview 保持一致</li>
 *   <li>SSE：规则分先出 → LLM 话术（可选）→ data 完整数据 → end</li>
 *   <li>降级：LLM 不可用时用 HintEngine 的 speakText 作为话术</li>
 *   <li>状态机：IDLE → ASKING → LISTENING → ANALYZING → DECIDING → ENDED</li>
 * </ul>
 *
 * @author moyun
 */
@Service
public class VoiceInterviewServiceImpl implements IVoiceInterviewService {

    private static final Logger log = LoggerFactory.getLogger(VoiceInterviewServiceImpl.class);

    /** 主问题目数量 */
    private static final int QUESTION_COUNT = 5;

    /** 关键词提取上限 */
    private static final int MAX_KEYWORDS = 12;

    /** 薄弱点召回上限 */
    private static final int WEAK_TAG_RECALL_LIMIT = 3;

    /** 岗位必备技能召回上限 */
    private static final int REQUIRED_SKILL_RECALL_LIMIT = 2;

    /** V10.3 简历深挖：项目锚定题数量（配比 2+2+1） */
    private static final int RESUME_PROJECT_QUESTION_COUNT = 2;

    /** V10.3 简历深挖：画像题数量（配比 2+2+1） */
    private static final int PROFILE_QUESTION_COUNT_WITH_RESUME = 2;

    // ==================== V10.4 LLM 驱动动态追问体系 ====================

    /** 简历摘要：追问上下文的数据底座（项目名 + 技术栈 + 亮点），从简历 JSON 提取 */
    private String buildResumeDigest(Long resumeId) {
        if (resumeId == null) {
            return null;
        }
        try {
            PortalUserResume resume = userResumeMapper.selectById(resumeId);
            if (resume == null || StringUtils.isEmpty(resume.getProjects())) {
                return null;
            }
            com.fasterxml.jackson.databind.JsonNode arr = objectMapper.readTree(resume.getProjects());
            StringBuilder sb = new StringBuilder();
            int n = Math.min(arr.size(), 3);
            for (int i = 0; i < n; i++) {
                com.fasterxml.jackson.databind.JsonNode proj = arr.get(i);
                String name = proj.path("name").asText("");
                String stack = proj.path("stack").asText("");
                String highlight = proj.path("highlight").asText("");
                if (StringUtils.isEmpty(name)) {
                    continue;
                }
                sb.append("项目").append(i + 1).append("：").append(name);
                if (StringUtils.isNotEmpty(stack)) {
                    sb.append("（").append(stack).append("）");
                }
                if (StringUtils.isNotEmpty(highlight)) {
                    sb.append("——").append(highlight);
                }
                sb.append("\n");
            }
            return sb.length() > 0 ? sb.toString() : null;
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

    /** 构建分析/追问共用的上下文系统提示词：岗位 + 场景 + 简历 + 累积画像 + 风格 + 难度 */
    private String buildContextualSystemPrompt(PortalVoiceInterview interview) {
        Map<String, Object> cfg = readInterviewConfig(interview);
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位资深技术面试官，正在对候选人进行「").append(interview.getPosition())
          .append("」岗位的").append(difficultyText(interview.getDifficulty()))
          .append("模拟面试，面试场景为").append(sceneText(interview.getScene())).append("。\n");
        sb.append("难度要求：").append(difficultyRequirement(interview.getDifficulty())).append("\n");
        sb.append("场景要求：").append(sceneRequirement(interview.getScene())).append("\n");
        Object digest = cfg.get("resumeDigest");
        if (digest != null && StringUtils.isNotEmpty(String.valueOf(digest))) {
            sb.append("候选人简历项目（追问时优先围绕其真实项目深挖细节、质疑数据与亮点）：\n")
              .append(digest).append("\n");
        }
        Object gaps = cfg.get("profileGaps");
        if (gaps instanceof List && !((List<?>) gaps).isEmpty()) {
            sb.append("本场已识别的薄弱点（后续出题与追问需覆盖验证）：")
              .append(String.join("、", ((List<String>) gaps).stream().map(String::valueOf).toArray(String[]::new)))
              .append("\n");
        }
        Object level = cfg.get("levelEstimate");
        if (level != null && StringUtils.isNotEmpty(String.valueOf(level))) {
            sb.append("当前水平评估：").append(level).append("\n");
        }
        String style = interview.getStyle();
        if ("strict".equals(style)) {
            sb.append("风格要求：压力面。语气锐利，敢于质疑和打断，对模糊表述立即追问到底，不轻易给正面肯定。\n");
        } else if ("friendly".equals(style)) {
            sb.append("风格要求：亲和面。语气友好，先肯定再提问，循循善诱，但要挖出真实深度，不给空泛的鼓励。\n");
        } else {
            sb.append("风格要求：专业标准面。语气平稳，逻辑严密，就事论事，不刻意施压也不过度寒暄。\n");
        }
        return sb.toString();
    }

    /** 难度等级文案（拼接进系统提示词） */
    private String difficultyText(String difficulty) {
        if ("easy".equals(difficulty)) {
            return "基础";
        }
        if ("hard".equals(difficulty)) {
            return "高难度";
        }
        return "中等";
    }

    /** 难度评分标准要求（拼接进系统提示词，让不同难度真实产生差异） */
    private String difficultyRequirement(String difficulty) {
        if ("easy".equals(difficulty)) {
            return "考察基础概念与常用实践，候选人答对基础要点即可得分，追问点到为止，不苛求底层原理。";
        }
        if ("hard".equals(difficulty)) {
            return "考察底层原理、性能权衡与复杂场景设计，回答缺少原理依据或取舍分析时应扣分并追问。";
        }
        return "考察原理理解与实践结合，既看概念正确性也看落地细节，追问聚焦关键实现。";
    }

    /** 面试场景文案（拼接进系统提示词与开场白） */
    private String sceneText(String scene) {
        if ("project".equals(scene)) {
            return "项目面";
        }
        if ("hr".equals(scene)) {
            return "HR 面";
        }
        if ("comprehensive".equals(scene)) {
            return "综合面";
        }
        return "技术面";
    }

    /** 场景考察要求（拼接进系统提示词，让不同场景真实产生差异） */
    private String sceneRequirement(String scene) {
        if ("project".equals(scene)) {
            return "围绕候选人简历中的项目经历深挖：项目背景与你的职责、技术选型理由、难点攻克过程、量化成果与数据真实性。";
        }
        if ("hr".equals(scene)) {
            return "考察沟通表达、职业规划、离职动机、团队协作与抗压能力，问题偏向行为面（STAR），弱化纯技术深度。";
        }
        if ("comprehensive".equals(scene)) {
            return "技术、项目、软素质全方位考察，按候选人回答表现动态调节技术与行为问题的配比。";
        }
        return "聚焦技术深度：概念准确性、原理理解、实现细节与工程权衡，必要时追问源码级别依据。";
    }

    // ==================== V11.0 Agent 模式（智能体绑定 + 动态出题 + 决策接管） ====================

    /** 动态出题注入 LLM 的题库候选数量 */
    private static final int AGENT_CANDIDATE_COUNT = 8;

    /** 风格文案（占位符渲染用） */
    private String styleText(String style) {
        if ("strict".equals(style)) {
            return "压力面";
        }
        if ("friendly".equals(style)) {
            return "亲和面";
        }
        return "专业标准面";
    }

    /**
     * 出题模式判定：入参显式指定优先；未指定时 agent 可用 + sys_config 开关（voice.interview.dynamicMode）
     */
    private boolean resolveDynamicMode(Boolean requestFlag, Agent agent) {
        if (agent == null) {
            return false;
        }
        if (requestFlag != null) {
            return requestFlag;
        }
        return agentClient.dynamicModeEnabled();
    }

    /** 会话是否为动态出题模式（configJson.questionMode == "dynamic"；旧数据缺省按 preset） */
    private boolean isDynamicInterview(PortalVoiceInterview interview) {
        return "dynamic".equals(readInterviewConfig(interview).get("questionMode"));
    }

    /** agent.systemPrompt 占位符数据源：interview 主表 + configJson 画像累积区 */
    private Map<String, String> buildPlaceholders(PortalVoiceInterview interview) {
        Map<String, Object> cfg = readInterviewConfig(interview);
        Map<String, String> ph = new LinkedHashMap<>();
        ph.put("position", StringUtils.isNotEmpty(interview.getPosition()) ? interview.getPosition() : "未指定");
        ph.put("scene", sceneText(interview.getScene()));
        ph.put("difficulty", difficultyText(interview.getDifficulty()));
        ph.put("style", styleText(interview.getStyle()));
        Object digest = cfg.get("resumeDigest");
        ph.put("resumeDigest", digest == null || String.valueOf(digest).isEmpty() ? "无" : String.valueOf(digest));
        Object gaps = cfg.get("profileGaps");
        ph.put("profileGaps", gaps instanceof List && !((List<?>) gaps).isEmpty()
                ? joinList((List<?>) gaps) : "暂无");
        Object level = cfg.get("levelEstimate");
        String levelStr = level == null ? "" : String.valueOf(level);
        ph.put("levelEstimate", levelStr.isEmpty() || "null".equals(levelStr)
                ? "待评估" : levelStr);
        return ph;
    }

    @SuppressWarnings("unchecked")
    private String joinList(List<?> list) {
        StringBuilder sb = new StringBuilder();
        for (Object o : list) {
            if (sb.length() > 0) {
                sb.append("、");
            }
            sb.append(o);
        }
        return sb.toString();
    }

    /** Agent 模式系统消息：agent 人设占位符渲染；agent 未配人设时回退 V10.4 上下文提示词 */
    private String buildAgentSystemMessage(PortalVoiceInterview interview, Agent agent) {
        String persona = promptAssembler.renderSystemPrompt(agent.getSystemPrompt(), buildPlaceholders(interview));
        if (StringUtils.isEmpty(persona)) {
            persona = buildContextualSystemPrompt(interview);
        }
        return persona;
    }

    /** 本场已问过的题库题 id（动态候选排除用） */
    private Set<Long> usedQuestionIds(Long interviewId) {
        Set<Long> ids = new HashSet<>();
        for (PortalVoiceInterviewQA qa : listQaByInterview(interviewId)) {
            if (qa.getQuestionId() != null) {
                ids.add(qa.getQuestionId());
            }
        }
        return ids;
    }

    /** 动态出题候选：岗位/场景召回 + 已问排除 */
    private List<PortalInterviewQuestion> pickAgentCandidates(PortalVoiceInterview interview) {
        Set<Long> used = usedQuestionIds(interview.getId());
        List<PortalInterviewQuestion> raw = pickQuestions(interview.getPosition(), interview.getScene(),
                AGENT_CANDIDATE_COUNT + used.size());
        List<PortalInterviewQuestion> result = new ArrayList<>();
        for (PortalInterviewQuestion q : raw) {
            if (result.size() >= AGENT_CANDIDATE_COUNT) {
                break;
            }
            if (q != null && q.getId() != null && !used.contains(q.getId())) {
                result.add(q);
            }
        }
        return result;
    }

    /** 已完成主问轮数（distinct questionIdx 已作答数，基于 DB + 当前未落库主问） */
    private int countRoundsDone(Long interviewId, PortalVoiceInterviewQA currentQa) {
        Set<Integer> idxSet = new HashSet<>();
        for (PortalVoiceInterviewQA qa : listQaByInterview(interviewId)) {
            if (qa.getParentQaId() == null && StringUtils.isNotEmpty(qa.getUserAnswer())) {
                idxSet.add(qa.getQuestionIdx());
            }
        }
        // 当前主问刚作答但尚未落库 → 计入
        if (currentQa != null && currentQa.getParentQaId() == null) {
            idxSet.add(currentQa.getQuestionIdx());
        }
        return idxSet.size();
    }

    /** Agent 轮次消息组装结果：消息列表 + 本轮注入的题库候选（candidateId 回链校验用） */
    private static class AgentTurnMessages {
        final List<ChatMessage> messages;
        final List<PortalInterviewQuestion> candidates;
        AgentTurnMessages(List<ChatMessage> messages, List<PortalInterviewQuestion> candidates) {
            this.messages = messages;
            this.candidates = candidates;
        }
    }

    /** 构建 Agent 多轮消息：system(人设) + 历史 + 任务指令（当前问题/转写/候选/进度） */
    private AgentTurnMessages buildAgentTurnMessages(PortalVoiceInterview interview, Agent agent,
                                                     PortalVoiceInterviewQA qa,
                                                     PortalInterviewQuestion question, String transcript) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(buildAgentSystemMessage(interview, agent)));

        // 历史：当前轮之前的已作答问答对（多轮记忆生效点）
        List<PortalVoiceInterviewQA> history = new ArrayList<>();
        for (PortalVoiceInterviewQA item : listQaByInterview(interview.getId())) {
            if (item.getId() < qa.getId() && StringUtils.isNotEmpty(item.getUserAnswer())) {
                history.add(item);
            }
        }
        int maxTurns = agent.getMaxHistoryTurns() == null || agent.getMaxHistoryTurns() <= 0
                ? 20 : agent.getMaxHistoryTurns();
        messages.addAll(promptAssembler.buildHistory(history, maxTurns));

        // 任务指令
        List<PortalInterviewQuestion> candidates = pickAgentCandidates(interview);
        InterviewPromptAssembler.TaskContext ctx = new InterviewPromptAssembler.TaskContext()
                .setQuestionTitle(question != null ? question.getTitle() : qa.getQuestion())
                .setQuestionAnalysis(question != null ? question.getAnalysis() : null)
                .setTranscript(transcript)
                .setRoundsDone(countRoundsDone(interview.getId(), qa))
                .setTotalPlanned(interview.getTotalQa() == null ? 0 : interview.getTotalQa())
                .setFollowupDepth(followupDepthOf(qa))
                .setFollowupUsed(countFollowupUsed(interview.getId()))
                .setCandidates(candidates);
        messages.add(new UserMessage(promptAssembler.buildTaskDirective(ctx)));
        return new AgentTurnMessages(messages, candidates);
    }

    /** 动态模式首问生成：agent 一次调用产出开场问题；失败返回 null 走兜底 */
    private PortalInterviewQuestion generateFirstQuestionByAgent(PortalVoiceInterview interview, Agent agent) {
        List<PortalInterviewQuestion> candidates = pickAgentCandidates(interview);
        StringBuilder user = new StringBuilder();
        user.append("面试正式开始。请直接提出第一个面试问题：只输出问题本身一句话，");
        user.append("紧密结合候选人简历项目与岗位要求，不要任何解释、编号或多余文字。\n");
        if (!candidates.isEmpty()) {
            user.append("可参考以下题库候选（可直接采用、改写或自拟更贴合的问题）：\n");
            int idx = 1;
            for (PortalInterviewQuestion q : candidates) {
                user.append(idx).append(". ").append(q.getTitle()).append("\n");
                idx++;
            }
        }
        try {
            String raw = agentClient.chat(agent, List.of(
                    SystemMessage.from(buildAgentSystemMessage(interview, agent)),
                    new UserMessage(user.toString())));
            String q = normalizeAgentQuestion(raw);
            if (q != null) {
                // 候选回链：题面高度相似则认定采用题库题
                PortalInterviewQuestion matched = matchCandidate(candidates, q);
                if (matched != null) {
                    return matched;
                }
                PortalInterviewQuestion virtual = new PortalInterviewQuestion();
                virtual.setTitle(q);
                return virtual;
            }
        } catch (Exception e) {
            log.warn("[VoiceInterview] agent 首问生成失败，走题库兜底：{}", e.getMessage());
        }
        return null;
    }

    /** 动态模式下一题生成（forceNext / LLM 未给 nextQuestion 时兜底）：优先题库，无题则通用模板 */
    private PortalInterviewQuestion generateNextQuestion(PortalVoiceInterview interview) {
        List<PortalInterviewQuestion> candidates = pickAgentCandidates(interview);
        if (!candidates.isEmpty()) {
            return candidates.get(0);
        }
        PortalInterviewQuestion virtual = new PortalInterviewQuestion();
        virtual.setTitle("请结合你最近的项目经历，讲讲你解决过的一个有挑战性的技术难题，以及你的取舍过程。");
        return virtual;
    }

    /** 清洗 LLM 输出的问题文本：去围栏/编号/引号/首尾空白，长度异常返回 null */
    private String normalizeAgentQuestion(String raw) {
        if (StringUtils.isEmpty(raw)) {
            return null;
        }
        String q = raw.trim();
        if (q.contains("```")) {
            int st = q.indexOf('\n');
            int en = q.lastIndexOf("```");
            q = (st > 0 && en > st) ? q.substring(st, en) : q.replace("```", "");
        }
        q = q.replaceAll("^[\\d一-十]+[、.．]\\s*", "").trim();
        q = q.replaceAll("^[「『\"']+", "").replaceAll("[」』\"']+$", "").trim();
        if (q.length() < 8 || q.length() > 200) {
            return null;
        }
        return q;
    }

    /** 题面相似匹配：候选题目标题包含于 LLM 输出（或反之）即认定采用 */
    private PortalInterviewQuestion matchCandidate(List<PortalInterviewQuestion> candidates, String question) {
        for (PortalInterviewQuestion c : candidates) {
            if (c.getTitle() != null && question.length() >= 10
                    && (question.contains(c.getTitle()) || c.getTitle().contains(question))) {
                return c;
            }
        }
        return null;
    }

    /** 候选编号回链：candidateId 必须在本轮注入清单内才有效 */
    private Long resolveCandidateId(List<PortalInterviewQuestion> candidates, Long candidateId) {
        if (candidateId == null || candidates == null) {
            return null;
        }
        int idx = candidateId.intValue() - 1;
        if (idx >= 0 && idx < candidates.size()) {
            PortalInterviewQuestion q = candidates.get(idx);
            return q == null ? null : q.getId();
        }
        return null;
    }

    /** SSE 事件发送（异常吞掉记日志，不中断回调链） */
    private void sendEvent(SseEmitter emitter, String name, Object data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (Exception e) {
            log.warn("[VoiceInterview] SSE 发送 {} 事件失败：{}", name, e.getMessage());
        }
    }

    /** 画像累积（Agent 模式）：漏洞与水平写回 configJson */
    private void accumulateProfileFromTurn(PortalVoiceInterview interview, InterviewTurnResult turn) {
        if (turn == null) {
            return;
        }
        try {
            Map<String, Object> cfg = readInterviewConfig(interview);
            List<String> gaps = (List<String>) cfg.get("profileGaps");
            if (gaps == null) {
                gaps = new ArrayList<>();
            }
            for (String flaw : turn.getFlaws()) {
                if (StringUtils.isNotEmpty(flaw) && !gaps.contains(flaw) && gaps.size() < PROFILE_MAX_GAPS) {
                    gaps.add(flaw);
                }
            }
            cfg.put("profileGaps", gaps);
            if (StringUtils.isNotEmpty(turn.getLevel())) {
                cfg.put("levelEstimate", turn.getLevel());
            }
            interview.setConfigJson(toJson(cfg));
            interviewMapper.updateById(interview);
        } catch (Exception e) {
            log.warn("[VoiceInterview] Agent 画像累积写入失败：{}", e.getMessage());
        }
    }


    /**
     * LLM 一次调用同时完成：评分校正 + 漏洞识别 + 水平评估 + 针对性追问建议。
     * 失败/未启用时返回 null，调用方回退规则评分（保证链路永远可用）。
     */
    private AnswerAnalysis analyzeAnswerByLlm(PortalVoiceInterview interview, String questionTitle,
                                              String questionAnalysis, String transcript, ScoreResult ruleScore) {
        if (llmClient == null || !llmClient.isEnabled()) {
            return null;
        }
        try {
            String system = buildContextualSystemPrompt(interview)
                    + "\n你刚刚向候选人提出问题：\"" + questionTitle + "\"\n"
                    + (StringUtils.isNotEmpty(questionAnalysis) ? "该题考察要点：" + questionAnalysis + "\n" : "")
                    + "候选人的语音转写回答如下（可能口语化、有转写噪音）：\n\"" + transcript + "\"\n\n"
                    + "请以严格的技术面试官标准分析该回答，只输出如下 JSON（不要任何其他文字）：\n"
                    + "{\n"
                    + "  \"score\": 0-100的整数,\n"
                    + "  \"dimensions\": {\"relevance\": 0-100, \"professionalism\": 0-100, \"fluency\": 0-100},\n"
                    + "  \"feedback\": \"两到三句中文点评，先肯定再指出问题\",\n"
                    + "  \"flaws\": [\"回答中暴露的具体漏洞或模糊点，每条一句话，最多3条，没有则空数组\"],\n"
                    + "  \"level\": \"junior或mid或senior，对候选人当前真实水平的判断\",\n"
                    + "  \"followupWorth\": true或false，该回答是否存在值得追问的漏洞,\n"
                    + "  \"followupQuestion\": \"若followupWorth为true，给出一句针对漏洞的追问；必须引用候选人回答中的具体表述\",\n"
                    + "  \"guidance\": \"若回答明显跑偏，给出一句引导性提示，否则为空字符串\"\n"
                    + "}\n"
                    + "打分参考：完全跑题<30；浅层正确但无细节50-65；有正确框架和部分细节65-80；深入准确有取舍权衡80+。";
            String resp = llmClient.chat(system, "请分析该回答。");
            if (StringUtils.isEmpty(resp)) {
                return null;
            }
            return parseAnalysis(resp, ruleScore);
        } catch (Exception e) {
            log.warn("[VoiceInterview] LLM 分析回答失败，回退规则评分：{}", e.getMessage());
            return null;
        }
    }

    /** 容错解析 LLM 返回的 JSON（兼容 markdown 代码块包裹） */
    private AnswerAnalysis parseAnalysis(String raw, ScoreResult ruleScore) {
        try {
            String json = raw.trim();
            if (json.startsWith("```")) {
                int st = json.indexOf('{');
                int en = json.lastIndexOf('}');
                if (st >= 0 && en > st) {
                    json = json.substring(st, en + 1);
                }
            }
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(json);
            AnswerAnalysis a = new AnswerAnalysis();
            a.score = clamp(node.path("score").asInt(ruleScore.score), 0, 100);
            a.feedback = node.path("feedback").asText("");
            if (StringUtils.isEmpty(a.feedback)) {
                a.feedback = ruleScore.feedback;
            }
            Map<String, Integer> dims = new LinkedHashMap<>();
            dims.put("relevance", clamp(node.path("dimensions").path("relevance").asInt(ruleScore.dimensions.get("relevance")), 0, 100));
            dims.put("professionalism", clamp(node.path("dimensions").path("professionalism").asInt(ruleScore.dimensions.get("professionalism")), 0, 100));
            dims.put("fluency", clamp(node.path("dimensions").path("fluency").asInt(ruleScore.dimensions.get("fluency")), 0, 100));
            a.dimensions = dims;
            List<String> flaws = new ArrayList<>();
            for (com.fasterxml.jackson.databind.JsonNode f : node.path("flaws")) {
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

    /** 本场已消耗的追问次数（所有题目合计），受 FOLLOWUP_BUDGET 预算约束 */
    private int countFollowupUsed(Long interviewId) {
        return Math.toIntExact(qaMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PortalVoiceInterviewQA>()
                        .eq(PortalVoiceInterviewQA::getInterviewId, interviewId)
                        .isNotNull(PortalVoiceInterviewQA::getParentQaId)));
    }

    /** 从当前 QA 沿 parentQaId 向上回溯，计算追问链深度（原题为 0，追问为 1，追问的追问为 2） */
    private int followupDepthOf(PortalVoiceInterviewQA qa) {
        int depth = 0;
        Long pid = qa.getParentQaId();
        int guard = 0;
        while (pid != null && guard++ < 10) {
            PortalVoiceInterviewQA parent = qaMapper.selectById(pid);
            if (parent == null) {
                break;
            }
            depth++;
            pid = parent.getParentQaId();
        }
        return depth;
    }

    /** LLM 直接生成追问问题（带候选人原话引用），失败回退规则模板 */
    private String generateFollowupQuestion(PortalVoiceInterview interview, String questionTitle,
                                            String transcript, AnswerAnalysis analysis, int depth) {
        if (analysis != null && StringUtils.isNotEmpty(analysis.followupQuestion)) {
            String q = analysis.followupQuestion.trim();
            if (q.length() > 5 && q.length() <= 200) {
                return q;
            }
        }
        // 回退：规则模板（保留原有兜底能力）
        if (depth >= 1) {
            return "你刚才提到的这一点，能再举个你实际项目里的具体例子吗？";
        }
        return "你提到了一些做法，能展开讲讲「" + extractKeyword(transcript, questionTitle) + "」这个点吗？";
    }

    /** 从转写文本提取追问锚点关键词（截取前 12 字，规则兜底用） */
    private String extractKeyword(String transcript, String fallback) {
        if (StringUtils.isEmpty(transcript)) {
            return fallback;
        }
        String t = transcript.replaceAll("[，。？！,.?! \\s]+", " ").trim();
        return t.length() > 12 ? t.substring(0, 12) : t;
    }

    /** 画像累积：把本轮识别的漏洞/水平写回 configJson，供后续追问与报告使用 */
    private void accumulateProfile(PortalVoiceInterview interview, AnswerAnalysis analysis) {
        if (analysis == null) {
            return;
        }
        try {
            Map<String, Object> cfg = readInterviewConfig(interview);
            List<String> gaps = (List<String>) cfg.get("profileGaps");
            if (gaps == null) {
                gaps = new ArrayList<>();
            }
            for (String flaw : analysis.flaws) {
                if (flaw != null && !flaw.isEmpty() && !gaps.contains(flaw) && gaps.size() < PROFILE_MAX_GAPS) {
                    gaps.add(flaw);
                }
            }
            cfg.put("profileGaps", gaps);
            if (StringUtils.isNotEmpty(analysis.level)) {
                cfg.put("levelEstimate", analysis.level);
            }
            interview.setConfigJson(toJson(cfg));
            interviewMapper.updateById(interview);
        } catch (Exception e) {
            log.warn("[VoiceInterview] 画像累积写入失败：{}", e.getMessage());
        }
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

    /** 同一主题允许的最大追问链深度（追问的追问），防死循环 */
    private static final int FOLLOWUP_MAX_DEPTH = 2;

    /** 单场面试的追问总预算（所有题目合计），保持面试节奏 */
    private static final int FOLLOWUP_BUDGET = 4;

    /** 画像中累积记录的最大薄弱点数量 */
    private static final int PROFILE_MAX_GAPS = 8;

    /** SSE 超时时间（毫秒） */
    private static final long SSE_TIMEOUT = 120_000L;

    /** 中文/英文停用词 */
    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
            "的", "了", "是", "在", "和", "与", "或", "等", "为", "对", "由", "及",
            "一个", "一种", "可以", "通过", "使用", "进行", "实现", "the", "a", "an",
            "is", "are", "to", "of", "in", "on", "for", "and", "or", "with", "by"
    ));

    @Autowired private PortalVoiceInterviewMapper interviewMapper;
    @Autowired private PortalVoiceInterviewQAMapper qaMapper;
    @Autowired private PortalInterviewQuestionMapper questionMapper;
    @Autowired private PortalUserResumeMapper userResumeMapper;
    @Autowired private IUserProfileSnapshotService profileSnapshotService;
    @Autowired private HintEngine hintEngine;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private LlmClient llmClient;
    @Autowired private AiProperties aiProperties;
    @Autowired private InterviewAgentClient agentClient;
    @Autowired private InterviewPromptAssembler promptAssembler;
    @Autowired private InterviewAnalysisParser analysisParser;

    /** SSE 异步线程池（避免阻塞请求线程） */
    private final ScheduledExecutorService sseExecutor = Executors.newScheduledThreadPool(2);

    // ========================================================================
    // 开始面试
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public VoiceInterviewVO start(Long userId, VoiceStartConfig config) {
        if (userId == null) {
            throw new ServiceException("请登录后操作");
        }
        if (config == null) {
            config = new VoiceStartConfig();
        }

        String position = config.getPosition();
        String scene = config.getScene();
        
        // 字段长度校验（数据库定义：position varchar(64), scene varchar(64)）
        if (StringUtils.isNotEmpty(position) && position.length() > 64) {
            throw new ServiceException("面试岗位名称不能超过64个字符");
        }
        if (StringUtils.isNotEmpty(scene) && scene.length() > 64) {
            throw new ServiceException("面试场景名称不能超过64个字符");
        }
        
        String style = StringUtils.isNotEmpty(config.getStyle()) ? config.getStyle() : "professional";
        String difficulty = StringUtils.isNotEmpty(config.getDifficulty()) ? config.getDifficulty() : "medium";
        boolean personalized = Boolean.TRUE.equals(config.getPersonalized());
        boolean hintsEnabled = config.getHintsEnabled() == null || config.getHintsEnabled();

        // 构建用户画像快照
        UserProfileSnapshotVO snapshot = null;
        boolean useProfile = personalized;
        try {
            snapshot = profileSnapshotService.buildSnapshot(userId, position, scene);
            if (useProfile && !snapshot.isPersonalized()) {
                useProfile = false;
            }
        } catch (Exception e) {
            log.warn("[VoiceInterview] 用户{}画像构建失败：{}，降级随机抽题", userId, e.getMessage());
            useProfile = false;
        }

        // Agent 解析 + 出题模式判定（V11.0：agent 可用时支持动态出题）
        Agent agent = agentClient.resolveAgent(config.getAgentId());
        boolean dynamic = resolveDynamicMode(config.getDynamicMode(), agent);

        // 出题（V10.3：resumeId 非空时启用简历深挖配比——简历项目2题 + 画像/岗位2题 + 兜底补满）
        // dynamic 模式不预生成题单，由 agent 结合上下文动态出题
        List<PortalInterviewQuestion> questions = new ArrayList<>();
        Map<Integer, Map<String, Object>> questionSnapshots = null;
        if (!dynamic) {
            if (config.getResumeId() != null) {
                ResumePickResult rp = pickQuestionsWithResume(userId, config.getResumeId(), snapshot, useProfile, position, scene);
                questions = rp.questions;
                questionSnapshots = rp.snapshots;
            } else if (useProfile && snapshot != null) {
                questions = pickQuestionsByProfile(snapshot, QUESTION_COUNT);
            } else {
                questions = pickQuestions(position, scene, QUESTION_COUNT);
            }
            if (questions.isEmpty()) {
                throw new ServiceException("题库中暂无可用题目，请稍后再试");
            }
        }

        // 创建会话
        PortalVoiceInterview interview = new PortalVoiceInterview();
        interview.setUserId(userId);
        interview.setPosition(position);
        interview.setScene(scene);
        interview.setResumeId(config.getResumeId());
        // V11.0：绑定面试官智能体（preset 模式也绑定，供分析链路升级；agent 为 null 时走旧逻辑）
        interview.setAgentId(agent == null ? null : agent.getId());
        interview.setStatus("in_progress");
        interview.setStyle(style);
        interview.setDifficulty(difficulty);
        // dynamic 模式 totalQa 为主问预算（不预生成题单）；preset 为实际题单长度
        interview.setTotalQa(dynamic ? QUESTION_COUNT : questions.size());
        interview.setCurrentIdx(0);
        interview.setIsPersonalized(useProfile ? 1 : 0);
        if (snapshot != null) {
            interview.setProfileSnapshot(toJson(snapshot));
        }
        // 配置 JSON
        Map<String, Object> configMap = new LinkedHashMap<>();
        configMap.put("hintsEnabled", hintsEnabled);
        configMap.put("stuckThreshold", config.getStuckThreshold() != null ? config.getStuckThreshold() : 30);
        configMap.put("style", style);
        configMap.put("difficulty", difficulty);
        configMap.put("questionMode", dynamic ? "dynamic" : "preset");
        // V10.4 动态追问上下文底座：简历摘要 + 画像薄弱点累积区（后续每轮 LLM 分析后写入）
        String resumeDigest = buildResumeDigest(config.getResumeId());
        if (StringUtils.isNotEmpty(resumeDigest)) {
            configMap.put("resumeDigest", resumeDigest);
        }
        configMap.put("profileGaps", new ArrayList<String>());
        configMap.put("levelEstimate", "");
        interview.setConfigJson(toJson(configMap));
        interview.setCreateTime(LocalDateTime.now());
        interviewMapper.insert(interview);

        // 创建问答记录（首问）
        PortalInterviewQuestion firstQ;
        String greetText;
        if (dynamic) {
            // V11.0 动态模式：agent 生成首问（失败回退题库随机题），开场白用 agent.welcomeMessage 渲染
            firstQ = generateFirstQuestionByAgent(interview, agent);
            String firstSource = firstQ == null ? null
                    : (firstQ.getId() != null ? "bank" : "llm");
            if (firstQ == null) {
                List<PortalInterviewQuestion> fallback = pickQuestions(position, scene, 1);
                if (fallback.isEmpty()) {
                    throw new ServiceException("题库中暂无可用题目，请稍后再试");
                }
                firstQ = fallback.get(0);
                firstSource = "bank";
            }
            String welcome = StringUtils.isNotEmpty(agent.getWelcomeMessage())
                    ? promptAssembler.renderSystemPrompt(agent.getWelcomeMessage(), buildPlaceholders(interview))
                    : "你好，欢迎参加" + (StringUtils.isNotEmpty(position) ? position + "岗位的" : "")
                        + "模拟面试。我是今天的面试官，放松心态，我们像聊天一样开始。";
            greetText = welcome + " 首先第一个问题：" + firstQ.getTitle();
            PortalVoiceInterviewQA firstQa = insertFirstQa(interview, firstQ, firstSource, greetText);
            // 动态模式无预生成题单，questionIds 不写入（换题时由 agent 生成/题库兜底）
            return assembleVO(interview, firstQa);
        }

        firstQ = questions.get(0);
        greetText = buildGreetText(style, position, firstQ.getTitle());
        String firstSource = firstQ.getId() == null ? "resume_project" : "bank";
        PortalVoiceInterviewQA firstQa = insertFirstQa(interview, firstQ, firstSource, greetText);

        // 缓存题单到会话（通过 configJson 附加 questionIds；锚定题为 null 占位 + 快照，保证索引对齐）
        List<Long> qIds = new ArrayList<>();
        for (PortalInterviewQuestion q : questions) {
            qIds.add(q.getId());
        }
        configMap.put("questionIds", qIds);
        if (questionSnapshots != null && !questionSnapshots.isEmpty()) {
            configMap.put("questionSnapshots", questionSnapshots);
        }
        interview.setConfigJson(toJson(configMap));
        interviewMapper.updateById(interview);

        return assembleVO(interview, firstQa);
    }

    /** 创建首问 QA 记录 */
    private PortalVoiceInterviewQA insertFirstQa(PortalVoiceInterview interview, PortalInterviewQuestion firstQ,
                                                 String questionSource, String greetText) {
        PortalVoiceInterviewQA firstQa = new PortalVoiceInterviewQA();
        firstQa.setInterviewId(interview.getId());
        firstQa.setQuestionId(firstQ.getId());
        firstQa.setQuestionSource(questionSource);
        firstQa.setQuestionIdx(0);
        firstQa.setQuestion(firstQ.getTitle());
        firstQa.setHintUsed(0);
        firstQa.setTranscriptionEdited(0);
        firstQa.setCreateTime(LocalDateTime.now());
        firstQa.setSpeakText(greetText);
        qaMapper.insert(firstQa);
        return firstQa;
    }

    // ========================================================================
    // 提交答案（SSE 双通道流）
    // ========================================================================
    @Override
    public SseEmitter submitAnswer(Long interviewId, Long userId, Long qaId, String transcript, Integer latencyMs) {
        PortalVoiceInterview interview = mustOwnInterview(interviewId, userId);
        if ("finished".equals(interview.getStatus())) {
            throw new ServiceException("面试已结束，无法继续作答");
        }
        PortalVoiceInterviewQA qa = qaMapper.selectById(qaId);
        if (qa == null || !interviewId.equals(qa.getInterviewId())) {
            throw new ServiceException("问答记录不存在");
        }
        if (StringUtils.isEmpty(transcript)) {
            throw new ServiceException("答案不能为空");
        }

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        // 超时/异常优雅收尾：超时后响应已固化，不能再写事件，仅记录日志定位慢环节
        // （Agent 链路含 LLM 流式 + 失败重试，单轮可能接近超时阈值）
        emitter.onTimeout(() -> log.warn(
                "[VoiceInterview] SSE 请求超时（{}ms）interviewId={} qaId={}", SSE_TIMEOUT, interviewId, qaId));
        emitter.onError(t -> log.warn(
                "[VoiceInterview] SSE 连接异常收尾 interviewId={} qaId={}：{}", interviewId, qaId, t.getMessage()));

        // 异步处理 SSE 事件流
        sseExecutor.execute(() -> {
            try {
                // ① 回查原题目，计算规则分
                PortalInterviewQuestion question = qa.getQuestionId() == null
                        ? null : questionMapper.selectById(qa.getQuestionId());
                ScoreResult sr = scoreAnswer(question, transcript);

                // V11.0 Agent 链路：绑定 agent 且 AI 可用 → 流式多轮分析 + 决策接管（失败自动回退旧链路）
                if (interview.getAgentId() != null && agentClient.isEnabled()) {
                    Agent agent = agentClient.resolveAgent(interview.getAgentId());
                    if (agent != null) {
                        // 事件1：规则分先行（打字机 delta 之前到达）
                        Map<String, Object> scoreData = new LinkedHashMap<>();
                        scoreData.put("score", sr.score);
                        scoreData.put("dimensions", sr.dimensions);
                        sendEvent(emitter, "score", toJson(scoreData));
                        runAgentTurnStream(emitter, interview, agent, qa, question, transcript, sr, latencyMs);
                        return;
                    }
                }

                // 旧链路（preset / agent 不可用）
                runLegacyTurn(emitter, interview, qa, question, transcript, sr, latencyMs, false);
            } catch (Exception e) {
                log.error("[VoiceInterview] SSE 处理异常 interviewId={} qaId={}", interviewId, qaId, e);
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                } catch (IOException ignored) {
                }
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * 旧链路（preset 模式 / Agent 不可用降级）：V10.4 单轮 LLM 分析 + DECIDING 矩阵 + 预生成题单推进
     *
     * @param scoreSent 规则分事件是否已发送（Agent 流式失败降级回来时为 true，避免重复发送）
     */
    private void runLegacyTurn(SseEmitter emitter, PortalVoiceInterview interview, PortalVoiceInterviewQA qa,
                               PortalInterviewQuestion question, String transcript,
                               ScoreResult sr, Integer latencyMs, boolean scoreSent) {
        try {
            // V10.4 LLM 深度分析：评分校正 + 漏洞识别 + 水平评估 + 追问建议
                // （LLM 不可用/失败时返回 null，全链路回退规则分，保证可用性）
                AnswerAnalysis analysis = null;
                try {
                    analysis = analyzeAnswerByLlm(interview,
                            question != null ? question.getTitle() : qa.getQuestion(),
                            question != null ? question.getAnalysis() : null,
                            transcript, sr);
                } catch (Exception llmEx) {
                    log.warn("[VoiceInterview] LLM 分析异常（回退规则分）：{}", llmEx.getMessage());
                }
                if (analysis != null) {
                    // LLM 分与规则分加权融合，避免单边极端
                    int fused = (int) Math.round(analysis.score * 0.7 + sr.score * 0.3);
                    sr = new ScoreResult(fused, analysis.feedback, analysis.dimensions);
                    // 画像累积：漏洞与水平写回 configJson，驱动后续追问上下文
                    accumulateProfile(interview, analysis);
                }

                // 保存答案与评分
                qa.setUserAnswer(transcript);
                qa.setScore(sr.score);
                qa.setAiFeedback(sr.feedback);
                qa.setLatencyMs(latencyMs);
                qa.setRuleDimensionsJson(toJson(sr.dimensions));

                // ② 决定下一步动作（DECIDING 矩阵 + LLM 漏洞识别增强）
                String nextAction = decideNextAction(interview, qa, sr.score);
                // LLM 识别到值得追问的漏洞 且 预算/链深允许 → 追问优先
                if (analysis != null && analysis.followupWorth
                        && countFollowupUsed(interview.getId()) < FOLLOWUP_BUDGET
                        && followupDepthOf(qa) < FOLLOWUP_MAX_DEPTH) {
                    nextAction = "followup";
                } else if ("followup".equals(nextAction)
                        && (countFollowupUsed(interview.getId()) >= FOLLOWUP_BUDGET
                            || followupDepthOf(qa) >= FOLLOWUP_MAX_DEPTH)) {
                    // 规则想追问但预算耗尽/链深到顶 → 降级推进
                    nextAction = "next";
                }
                qa.setNextAction(nextAction);

                // ③ 生成话术（LLM 可用时增强，否则规则降级）
                String speakText = generateSpeakText(interview, question, transcript, sr, nextAction);
                qa.setSpeakText(speakText);
                qaMapper.updateById(qa);

                // ④ 发送 SSE 事件
                // 事件1：规则分（立即返回；Agent 降级场景已发过则跳过）
                if (!scoreSent) {
                    Map<String, Object> scoreData = new LinkedHashMap<>();
                    scoreData.put("score", sr.score);
                    scoreData.put("dimensions", sr.dimensions);
                    emitter.send(SseEmitter.event().name("score").data(toJson(scoreData)));
                }

                // 事件2：LLM 话术（如有）
                if (StringUtils.isNotEmpty(speakText)) {
                    emitter.send(SseEmitter.event().name("speak").data(speakText));
                }

                // 事件3：完整数据（含 nextAction + feedback）
                Map<String, Object> fullData = new LinkedHashMap<>();
                fullData.put("qaId", qa.getId());
                fullData.put("score", sr.score);
                fullData.put("feedback", sr.feedback);
                fullData.put("nextAction", nextAction);
                fullData.put("speakText", speakText);

                // 若 nextAction=next，预创建下一题 QA
                if ("next".equals(nextAction)) {
                    VoiceInterviewQAWrapper nextWrapper = advanceToNextQuestion(interview);
                    if (nextWrapper != null) {
                        fullData.put("nextQaId", nextWrapper.qa.getId());
                        fullData.put("nextQuestion", nextWrapper.qa.getQuestion());
                        fullData.put("nextSpeakText", nextWrapper.qa.getSpeakText());
                    } else {
                        // 无下一题，改为 report
                        fullData.put("nextAction", "report");
                        qa.setNextAction("report");
                        qaMapper.updateById(qa);
                    }
                } else if ("followup".equals(nextAction)) {
                    // 追问：创建追问 QA（V10.4 优先用 LLM 针对候选人原话漏洞生成的问题）
                    String followupQuestion = generateFollowupQuestion(interview,
                            question != null ? question.getTitle() : qa.getQuestion(),
                            transcript, analysis, followupDepthOf(qa));
                    PortalVoiceInterviewQA followup = createFollowupQa(interview, qa, question, sr, followupQuestion, analysis);
                    fullData.put("nextQaId", followup.getId());
                    fullData.put("nextQuestion", followup.getQuestion());
                    fullData.put("nextSpeakText", followup.getSpeakText());
                }

                // V10.4：LLM 引导提示（回答跑偏时引导用户回答，而非直接判死）
                if (analysis != null && StringUtils.isNotEmpty(analysis.guidance)) {
                    fullData.put("guidance", analysis.guidance);
                }

                emitter.send(SseEmitter.event().name("data").data(toJson(fullData)));

                // 事件4：结束
                emitter.send(SseEmitter.event().name("end").data("{}"));
                emitter.complete();

        } catch (Exception e) {
            log.error("[VoiceInterview] 旧链路处理异常 interviewId={} qaId={}", interview.getId(), qa.getId(), e);
            sendEvent(emitter, "error", e.getMessage());
            emitter.completeWithError(e);
        }
    }

    // ========================================================================
    // V11.0 Agent 轮次：流式分析 + 决策接管
    // ========================================================================

    /**
     * Agent 流式轮次：score 已先行发送 → delta*N（打字机）→ speak → data → end。
     * 流式失败时尝试同步调用一次，再失败回退旧链路（scoreSent=true）。
     */
    private void runAgentTurnStream(SseEmitter emitter, PortalVoiceInterview interview, Agent agent,
                                    PortalVoiceInterviewQA qa, PortalInterviewQuestion question,
                                    String transcript, ScoreResult sr, Integer latencyMs) {
        AgentTurnMessages tm = buildAgentTurnMessages(interview, agent, qa, question, transcript);
        StringBuilder buffer = new StringBuilder();
        AtomicInteger sentLen = new AtomicInteger(0);
        AtomicBoolean finished = new AtomicBoolean(false);

        agentClient.chatStream(agent, tm.messages,
                // onToken：``` 围栏之前的增量文本实时下发（围栏后的 JSON 分析只入缓冲）
                token -> {
                    if (token == null || token.isEmpty()) {
                        return;
                    }
                    buffer.append(token);
                    int fence = buffer.indexOf("```");
                    int limit = fence >= 0 ? fence : buffer.length();
                    int sent = sentLen.get();
                    if (limit > sent && sentLen.compareAndSet(sent, limit)) {
                        Map<String, Object> delta = new LinkedHashMap<>();
                        delta.put("t", buffer.substring(sent, limit));
                        sendEvent(emitter, "delta", toJson(delta));
                    }
                },
                // onComplete：解析全文 → 落库 + 决策 + 收尾事件
                full -> {
                    if (!finished.compareAndSet(false, true)) {
                        return;
                    }
                    InterviewTurnResult turn = null;
                    try {
                        turn = analysisParser.parse(full);
                    } catch (Exception e) {
                        log.warn("[VoiceInterview] agent 输出解析失败：{}", e.getMessage());
                    }
                    if (turn == null) {
                        runLegacyTurn(emitter, interview, qa, question, transcript, sr, latencyMs, true);
                        return;
                    }
                    try {
                        finishAgentTurn(emitter, interview, agent, qa, question, transcript, sr, latencyMs,
                                turn, tm.candidates);
                    } catch (Exception e) {
                        log.error("[VoiceInterview] agent 轮次收尾异常 interviewId={}", interview.getId(), e);
                        sendEvent(emitter, "error", e.getMessage());
                        emitter.completeWithError(e);
                    }
                },
                // onError：同步重试一次，仍失败回退旧链路
                error -> {
                    if (!finished.compareAndSet(false, true)) {
                        return;
                    }
                    log.warn("[VoiceInterview] agent 流式调用失败，尝试同步重试：{}", error.getMessage());
                    InterviewTurnResult turn = null;
                    try {
                        String raw = agentClient.chat(agent, tm.messages);
                        turn = raw == null ? null : analysisParser.parse(raw);
                    } catch (Exception e) {
                        log.warn("[VoiceInterview] agent 同步重试失败：{}", e.getMessage());
                    }
                    if (turn == null) {
                        runLegacyTurn(emitter, interview, qa, question, transcript, sr, latencyMs, true);
                        return;
                    }
                    try {
                        finishAgentTurn(emitter, interview, agent, qa, question, transcript, sr, latencyMs,
                                turn, tm.candidates);
                    } catch (Exception e) {
                        log.error("[VoiceInterview] agent 轮次收尾异常 interviewId={}", interview.getId(), e);
                        sendEvent(emitter, "error", e.getMessage());
                        emitter.completeWithError(e);
                    }
                });
    }

    /**
     * Agent 轮次收尾：评分融合 + 画像累积 + 持久化 + 决策裁决 + 下一问生成 + SSE 收尾事件
     */
    private void finishAgentTurn(SseEmitter emitter, PortalVoiceInterview interview, Agent agent,
                                 PortalVoiceInterviewQA qa, PortalInterviewQuestion question, String transcript,
                                 ScoreResult sr, Integer latencyMs, InterviewTurnResult turn,
                                 List<PortalInterviewQuestion> candidates) {
        // ① 评分融合（LLM 0.7 + 规则 0.3；LLM 未给分直接用规则分）
        int finalScore = turn.getScore() != null
                ? (int) Math.round(turn.getScore() * 0.7 + sr.score * 0.3) : sr.score;
        Map<String, Integer> dims = turn.getDimensions().isEmpty() ? sr.dimensions : turn.getDimensions();
        String feedback = StringUtils.isNotEmpty(turn.getFeedback()) ? turn.getFeedback() : sr.feedback;

        // ② 画像累积：漏洞/水平写回 configJson（驱动后续轮次上下文）
        accumulateProfileFromTurn(interview, turn);

        // ③ 决策裁决（LLM 建议权 + 策略决定权）
        int followupDepth = followupDepthOf(qa);
        int followupUsed = countFollowupUsed(interview.getId());
        int roundsDone = countRoundsDone(interview.getId(), qa);
        int totalPlanned = interview.getTotalQa() == null ? 0 : interview.getTotalQa();
        InterviewDecisionPolicy.Decision decision = InterviewDecisionPolicy.resolve(
                turn.getNextAction(), followupDepth, followupUsed, roundsDone, totalPlanned);
        if (StringUtils.isNotEmpty(decision.getOverrideReason())) {
            log.info("[VoiceInterview] 决策覆盖 interviewId={}：{}", interview.getId(), decision.getOverrideReason());
        }
        String agentAction = decision.getAction();

        // ④ 持久化本轮 QA
        qa.setUserAnswer(transcript);
        qa.setScore(finalScore);
        qa.setAiFeedback(feedback);
        qa.setLatencyMs(latencyMs);
        qa.setRuleDimensionsJson(toJson(dims));
        qa.setLlmAnalysisJson(toJson(turn));

        Map<String, Object> fullData = new LinkedHashMap<>();
        fullData.put("qaId", qa.getId());
        fullData.put("score", finalScore);
        fullData.put("feedback", feedback);
        fullData.put("agentAction", agentAction);
        fullData.put("analysis", turn);
        if (StringUtils.isNotEmpty(turn.getTransition())) {
            fullData.put("transition", turn.getTransition());
        }
        if (StringUtils.isNotEmpty(turn.getGuidance())) {
            fullData.put("guidance", turn.getGuidance());
        }

        // ⑤ 按裁决生成交互（追问 / 换题 / 收尾）
        String speak = StringUtils.isNotEmpty(turn.getReply()) ? turn.getReply()
                : buildRuleSpeakText(finalScore, mapAgentAction(agentAction));
        switch (agentAction) {
            case InterviewDecisionPolicy.DEEPEN: {
                String followupQuestion = normalizeAgentQuestion(turn.getNextQuestion());
                if (followupQuestion == null) {
                    followupQuestion = turn.getFlaws() != null && !turn.getFlaws().isEmpty()
                            ? "你刚才提到的「" + turn.getFlaws().get(0) + "」这一点，能再展开讲讲具体细节吗？"
                            : "你刚才提到的这一点，能再举个你实际项目里的具体例子吗？";
                }
                PortalVoiceInterviewQA followup = createAgentFollowupQa(interview, qa, followupQuestion, turn);
                qa.setNextAction("followup");
                fullData.put("nextAction", "followup");
                fullData.put("nextQaId", followup.getId());
                fullData.put("nextQuestion", followup.getQuestion());
                fullData.put("nextSpeakText", followup.getSpeakText());
                break;
            }
            case InterviewDecisionPolicy.CHANGE_TOPIC: {
                VoiceInterviewQAWrapper next = isDynamicInterview(interview)
                        ? advanceDynamicNextQuestion(interview, turn, candidates)
                        : advanceToNextQuestion(interview);
                if (next != null) {
                    qa.setNextAction("next");
                    fullData.put("nextAction", "next");
                    fullData.put("nextQaId", next.qa.getId());
                    fullData.put("nextQuestion", next.qa.getQuestion());
                    fullData.put("nextSpeakText", next.qa.getSpeakText());
                } else {
                    qa.setNextAction("report");
                    fullData.put("nextAction", "report");
                }
                break;
            }
            default: {
                // wrap_up：收尾
                qa.setNextAction("report");
                fullData.put("nextAction", "report");
                if (StringUtils.isEmpty(turn.getReply())) {
                    speak = StringUtils.isNotEmpty(turn.getTransition()) ? turn.getTransition()
                            : "本次面试到此结束，我来为你做一个总结。";
                }
            }
        }
        qa.setSpeakText(speak);
        qaMapper.updateById(qa);

        // ⑥ SSE 收尾事件：speak → data → end（score/delta 已先行）
        sendEvent(emitter, "speak", speak);
        sendEvent(emitter, "data", toJson(fullData));
        sendEvent(emitter, "end", "{}");
        emitter.complete();
    }

    /** agent 动作 → 旧 nextAction 话术映射（规则降级话术用） */
    private String mapAgentAction(String agentAction) {
        switch (agentAction) {
            case InterviewDecisionPolicy.DEEPEN: return "followup";
            case InterviewDecisionPolicy.CHANGE_TOPIC: return "next";
            default: return "report";
        }
    }

    /** Agent 追问 QA（questionSource=llm，话术含漏洞点明） */
    private PortalVoiceInterviewQA createAgentFollowupQa(PortalVoiceInterview interview,
                                                         PortalVoiceInterviewQA parentQa,
                                                         String followupQuestion, InterviewTurnResult turn) {
        PortalVoiceInterviewQA followup = new PortalVoiceInterviewQA();
        followup.setInterviewId(interview.getId());
        followup.setQuestionId(parentQa.getQuestionId());
        followup.setQuestionSource("llm");
        followup.setQuestionIdx(parentQa.getQuestionIdx());
        followup.setParentQaId(parentQa.getId());
        followup.setHintUsed(0);
        followup.setTranscriptionEdited(0);
        followup.setCreateTime(LocalDateTime.now());
        followup.setQuestion(followupQuestion);
        String speak = followupQuestion;
        if (turn != null && turn.getFlaws() != null && !turn.getFlaws().isEmpty()) {
            speak = "我注意到你刚才的回答里，" + turn.getFlaws().get(0) + " 这一点说得还比较模糊。" + followupQuestion;
        }
        followup.setSpeakText(speak);
        qaMapper.insert(followup);
        return followup;
    }

    /**
     * 动态模式推进下一主问：currentIdx+1；问题优先级 LLM nextQuestion（含候选回链）→ 题库兜底 → 通用模板
     */
    private VoiceInterviewQAWrapper advanceDynamicNextQuestion(PortalVoiceInterview interview,
                                                               InterviewTurnResult turn,
                                                               List<PortalInterviewQuestion> candidates) {
        int nextIdx = (interview.getCurrentIdx() == null ? 0 : interview.getCurrentIdx()) + 1;
        int total = interview.getTotalQa() == null ? 0 : interview.getTotalQa();
        if (nextIdx >= total) {
            return null;
        }

        Long questionId = null;
        String source = "llm";
        String title = normalizeAgentQuestion(turn == null ? null : turn.getNextQuestion());

        if (title != null && turn.getCandidateId() != null) {
            Long cid = resolveCandidateId(candidates, turn.getCandidateId());
            if (cid != null) {
                questionId = cid;
                source = "bank";
            }
        }
        if (title == null) {
            // LLM 未给题 → 题库兜底
            PortalInterviewQuestion fallback = generateNextQuestion(interview);
            title = fallback.getTitle();
            if (fallback.getId() != null) {
                questionId = fallback.getId();
                source = "bank";
            }
        }

        String transition = turn != null && StringUtils.isNotEmpty(turn.getTransition())
                ? turn.getTransition() : "好，我们换个话题。";
        PortalVoiceInterviewQA qa = new PortalVoiceInterviewQA();
        qa.setInterviewId(interview.getId());
        qa.setQuestionId(questionId);
        qa.setQuestionSource(source);
        qa.setQuestionIdx(nextIdx);
        qa.setQuestion(title);
        qa.setHintUsed(0);
        qa.setTranscriptionEdited(0);
        qa.setCreateTime(LocalDateTime.now());
        qa.setSpeakText(transition + " " + title);
        qaMapper.insert(qa);

        interview.setCurrentIdx(nextIdx);
        interviewMapper.updateById(interview);
        return new VoiceInterviewQAWrapper(qa);
    }

    /**
     * 动态模式强制下一题（forceNext）：LLM 短调用生成，失败题库兜底
     */
    private VoiceInterviewQAWrapper advanceDynamicByForce(PortalVoiceInterview interview, Agent agent) {
        int nextIdx = (interview.getCurrentIdx() == null ? 0 : interview.getCurrentIdx()) + 1;
        int total = interview.getTotalQa() == null ? 0 : interview.getTotalQa();
        if (nextIdx >= total) {
            return null;
        }

        Long questionId = null;
        String source = "llm";
        String title = null;
        if (agent != null) {
            try {
                String user = "候选人跳过了当前问题。请直接提出下一个面试问题：只输出问题本身一句话，"
                        + "结合此前对话换个考察方向，不要任何解释或多余文字。";
                String raw = agentClient.chat(agent, List.of(
                        SystemMessage.from(buildAgentSystemMessage(interview, agent)),
                        new UserMessage(user)));
                title = normalizeAgentQuestion(raw);
            } catch (Exception e) {
                log.warn("[VoiceInterview] agent 强制换题生成失败，题库兜底：{}", e.getMessage());
            }
        }
        if (title == null) {
            PortalInterviewQuestion fallback = generateNextQuestion(interview);
            title = fallback.getTitle();
            if (fallback.getId() != null) {
                questionId = fallback.getId();
                source = "bank";
            }
        }

        PortalVoiceInterviewQA qa = new PortalVoiceInterviewQA();
        qa.setInterviewId(interview.getId());
        qa.setQuestionId(questionId);
        qa.setQuestionSource(source);
        qa.setQuestionIdx(nextIdx);
        qa.setQuestion(title);
        qa.setHintUsed(0);
        qa.setTranscriptionEdited(0);
        qa.setCreateTime(LocalDateTime.now());
        qa.setSpeakText("好的，那我们看下一个问题：" + title);
        qaMapper.insert(qa);

        interview.setCurrentIdx(nextIdx);
        interviewMapper.updateById(interview);
        return new VoiceInterviewQAWrapper(qa);
    }

    // ========================================================================
    // 请求提示
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public VoiceInterviewVO requestHint(Long interviewId, Long userId, Long qaId) {
        PortalVoiceInterview interview = mustOwnInterview(interviewId, userId);
        PortalVoiceInterviewQA qa = qaMapper.selectById(qaId);
        if (qa == null || !interviewId.equals(qa.getInterviewId())) {
            throw new ServiceException("问答记录不存在");
        }

        int nextLevel = (qa.getHintUsed() == null ? 0 : qa.getHintUsed()) + 1;
        if (nextLevel > 3) {
            throw new ServiceException("提示次数已用完");
        }

        PortalInterviewQuestion question = qa.getQuestionId() == null
                ? null : questionMapper.selectById(qa.getQuestionId());

        HintVO hint;
        if (question == null) {
            // V11.0：LLM 生成的题目（questionId=null）→ agent 提示路径；失败复述题目作提示
            hint = generateHintByAgent(interview, qa, nextLevel);
            if (hint == null) {
                hint = HintVO.of(nextLevel, hintLevelTitle(nextLevel));
                hint.setKeywords(new ArrayList<>());
                hint.setStructureHint("可以按「背景 → 你做了什么 → 结果与收获」的思路组织回答。");
                hint.setSpeakText("别着急，可以先从你熟悉的部分讲起，比如这个问题的背景，然后说你的做法，最后讲结果。");
            }
        } else {
            hint = hintEngine.generateHint(question, nextLevel);
        }
        qa.setHintUsed(nextLevel);
        qaMapper.updateById(qa);

        VoiceInterviewVO vo = assembleVO(interview, qa);
        vo.setCurrentQa(toQaVO(qa));
        vo.setHint(hint);
        return vo;
    }

    /** 提示级别标题 */
    private String hintLevelTitle(int level) {
        switch (level) {
            case 1: return "切入点提示";
            case 2: return "结构提示";
            default: return "全量提示";
        }
    }

    /**
     * V11.0：LLM 生成题目的分级提示（agent 路径）
     * level=1 切入点关键词 / level=2 答题结构 / level=3 考察点+完整思路；失败返回 null 走模板
     */
    private HintVO generateHintByAgent(PortalVoiceInterview interview, PortalVoiceInterviewQA qa, int level) {
        if (interview.getAgentId() == null || !agentClient.isEnabled()) {
            return null;
        }
        try {
            Agent agent = agentClient.resolveAgent(interview.getAgentId());
            if (agent == null) {
                return null;
            }
            String levelReq;
            switch (level) {
                case 1:
                    levelReq = "给1-2个切入点关键词，引导思考方向，不透露答案";
                    break;
                case 2:
                    levelReq = "给答题结构提示（如 STAR 框架或要点大纲），引导组织语言";
                    break;
                default:
                    levelReq = "给全部考察点、关键要点与完整答题思路（不给出完整答案原文）";
            }
            String user = "候选人请求第" + level + "级提示。\n【当前问题】" + qa.getQuestion() + "\n"
                    + "【候选人已给出的回答】" + (StringUtils.isEmpty(qa.getUserAnswer()) ? "（尚未作答）" : qa.getUserAnswer()) + "\n"
                    + "【提示要求】" + levelReq + "。只输出如下 JSON：\n"
                    + "{\"keywords\":[\"关键词\"],\"structureHint\":\"结构提示文本\",\"examinePoints\":[\"考察点\"],\"speakText\":\"可直接口播的引导语（40字内）\"}";
            String raw = agentClient.chat(agent, List.of(
                    SystemMessage.from(buildAgentSystemMessage(interview, agent)),
                    new UserMessage(user)));
            if (StringUtils.isEmpty(raw)) {
                return null;
            }
            String json = raw.trim();
            if (json.contains("```")) {
                int st = json.indexOf('{');
                int en = json.lastIndexOf('}');
                if (st >= 0 && en > st) {
                    json = json.substring(st, en + 1);
                }
            }
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(json);
            HintVO hint = HintVO.of(level, hintLevelTitle(level));
            List<String> keywords = new ArrayList<>();
            for (com.fasterxml.jackson.databind.JsonNode k : node.path("keywords")) {
                String t = k.asText("").trim();
                if (!t.isEmpty()) {
                    keywords.add(t);
                }
            }
            hint.setKeywords(keywords);
            hint.setStructureHint(node.path("structureHint").asText(""));
            if (level >= 3) {
                List<String> points = new ArrayList<>();
                for (com.fasterxml.jackson.databind.JsonNode p : node.path("examinePoints")) {
                    String t = p.asText("").trim();
                    if (!t.isEmpty()) {
                        points.add(t);
                    }
                }
                hint.setExaminePoints(points);
            }
            String speak = node.path("speakText").asText("");
            hint.setSpeakText(StringUtils.isNotEmpty(speak) ? speak : hint.getStructureHint());
            return hint;
        } catch (Exception e) {
            log.warn("[VoiceInterview] agent 提示生成失败：{}", e.getMessage());
            return null;
        }
    }

    // ========================================================================
    // 强制下一题
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public VoiceInterviewVO forceNext(Long interviewId, Long userId, String reason) {
        PortalVoiceInterview interview = mustOwnInterview(interviewId, userId);
        if ("finished".equals(interview.getStatus())) {
            throw new ServiceException("面试已结束");
        }

        VoiceInterviewQAWrapper next;
        if (isDynamicInterview(interview)) {
            // V11.0 动态模式：agent 生成下一题（失败题库兜底）
            Agent agent = interview.getAgentId() == null ? null : agentClient.resolveAgent(interview.getAgentId());
            next = advanceDynamicByForce(interview, agent);
        } else {
            next = advanceToNextQuestion(interview);
        }
        if (next == null) {
            // 无下一题，自动结束
            finish(interviewId, userId);
            return getDetail(interviewId, userId);
        }
        return assembleVO(interview, next.qa);
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

        List<PortalVoiceInterviewQA> qaList = listQaByInterview(interviewId);
        int answered = 0;
        long sum = 0;
        List<VoiceInterviewReportVO.QuestionReview> reviews = new ArrayList<>();
        Map<String, Integer> dimSums = new LinkedHashMap<>();
        dimSums.put("coverage", 0);
        dimSums.put("length", 0);
        dimSums.put("structure", 0);
        int dimCount = 0;

        List<String> highlights = new ArrayList<>();
        List<String> weakPoints = new ArrayList<>();

        for (PortalVoiceInterviewQA qa : qaList) {
            if (qa.getScore() != null) {
                answered++;
                sum += qa.getScore();

                VoiceInterviewReportVO.QuestionReview review = new VoiceInterviewReportVO.QuestionReview();
                review.setQuestionIdx(qa.getQuestionIdx());
                review.setQuestion(qa.getQuestion());
                review.setScore(qa.getScore());
                review.setFeedback(qa.getAiFeedback());
                reviews.add(review);

                if (qa.getScore() >= 80) {
                    highlights.add(qa.getQuestion());
                } else if (qa.getScore() < 60) {
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
        }

        int avg = answered > 0 ? (int) Math.round((double) sum / answered) : 0;
        interview.setScore(avg);

        // V11.0：聚合逐轮 LLM 深度分析 → 心态趋势 / 可疑信号汇总 / 流畅度均分
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

        // V10.4：LLM 画像增强——薄弱点优先用追问中识别的真实漏洞，summary 融合水平评估
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
        report.setTotalScore(avg);
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
        // V11.0：LLM 深度分析聚合结果（Agent 模式产出；旧数据字段为空，前端按缺失隐藏）
        report.setSentimentTrend(sentimentTrend);
        report.setRedFlags(allRedFlags);
        if (fluencyCount > 0) {
            report.setFluencyAvg((int) Math.round((double) fluencySum / fluencyCount));
        }

        interview.setSummary(report.getSummary());
        interview.setReport(toJson(report));
        interview.setStatus("finished");
        interviewMapper.updateById(interview);

        return report;
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
    // 出题逻辑（复用 MockInterview 模式）
    // ========================================================================

    private List<PortalInterviewQuestion> pickQuestionsByProfile(UserProfileSnapshotVO snapshot, int count) {
        List<PortalInterviewQuestion> result = new ArrayList<>();
        Set<Long> pickedIds = new HashSet<>();

        // 路径1：薄弱点优先
        List<UserProfileSnapshotVO.WeakTagItem> weakTags = snapshot.getWeakTags();
        if (weakTags != null && !weakTags.isEmpty()) {
            List<UserProfileSnapshotVO.WeakTagItem> sorted = new ArrayList<>(weakTags);
            sorted.sort((a, b) -> {
                double fa = a.getFailRate() == null ? 0 : a.getFailRate();
                double fb = b.getFailRate() == null ? 0 : b.getFailRate();
                return Double.compare(fb, fa);
            });
            int remain = Math.min(sorted.size(), WEAK_TAG_RECALL_LIMIT);
            for (int i = 0; i < remain && result.size() < count; i++) {
                UserProfileSnapshotVO.WeakTagItem tag = sorted.get(i);
                if (StringUtils.isEmpty(tag.getTagName())) continue;
                mergeUnique(queryByTag(tag.getTagName(), count - result.size()), pickedIds, result, count - result.size());
            }
        }

        // 路径2：岗位必备技能
        if (result.size() < count) {
            List<String> requiredSkills = snapshot.getRequiredSkills();
            if (requiredSkills != null && !requiredSkills.isEmpty()) {
                int remain = Math.min(requiredSkills.size(), REQUIRED_SKILL_RECALL_LIMIT);
                for (int i = 0; i < remain && result.size() < count; i++) {
                    String skill = requiredSkills.get(i);
                    if (StringUtils.isEmpty(skill)) continue;
                    mergeUnique(queryByTag(skill, count - result.size()), pickedIds, result, count - result.size());
                }
            }
        }

        // 路径3：随机兜底
        if (result.size() < count) {
            LambdaQueryWrapper<PortalInterviewQuestion> qw = Wrappers.<PortalInterviewQuestion>lambdaQuery()
                    .eq(PortalInterviewQuestion::getStatus, "published");
            if (!pickedIds.isEmpty()) {
                qw.notIn(PortalInterviewQuestion::getId, pickedIds);
            }
            qw.last("ORDER BY RAND() LIMIT " + Math.max(1, count - result.size()));
            result.addAll(questionMapper.selectList(qw));
        }
        return result.size() > count ? new ArrayList<>(result.subList(0, count)) : result;
    }

    // ========================================================================
    // 简历深挖出题（V10.3）：项目 2 题 + 画像 2 题 + 兜底 1 题
    // ========================================================================

    /** 简历出题结果：questions 题单（锚定题为虚拟实体，id=null）；snapshots 索引→题面快照 */
    private static class ResumePickResult {
        final List<PortalInterviewQuestion> questions;
        final Map<Integer, Map<String, Object>> snapshots;
        ResumePickResult(List<PortalInterviewQuestion> questions, Map<Integer, Map<String, Object>> snapshots) {
            this.questions = questions;
            this.snapshots = snapshots;
        }
    }

    /** 简历项目经历条目（projects JSON 数组元素） */
    private static class ResumeProjectItem {
        String name;
        String role;
        String description;
    }

    private ResumePickResult pickQuestionsWithResume(Long userId, Long resumeId, UserProfileSnapshotVO snapshot,
                                                     boolean useProfile, String position, String scene) {
        List<PortalInterviewQuestion> questions = new ArrayList<>();
        Map<Integer, Map<String, Object>> snapshots = new LinkedHashMap<>();
        Set<Long> pickedIds = new HashSet<>();

        // ── 路径1：简历项目深挖 2 题（锚定题，不入题库）
        List<ResumeProjectItem> projects = loadResumeProjects(userId, resumeId);
        int anchorCount = 0;
        for (ResumeProjectItem project : projects) {
            if (anchorCount >= RESUME_PROJECT_QUESTION_COUNT) break;
            if (project == null || StringUtils.isEmpty(project.name)) continue;
            int idx = questions.size();
            String title = StringUtils.isEmpty(project.role)
                    ? "请详细介绍你在「" + project.name + "」项目中的核心工作与产出"
                    : "你在「" + project.name + "」项目中担任 " + project.role + "，请介绍你负责的核心模块、技术选型理由和最终产出";
            PortalInterviewQuestion anchor = new PortalInterviewQuestion();
            anchor.setTitle(title);
            questions.add(anchor);
            Map<String, Object> snap = new LinkedHashMap<>();
            snap.put("idx", idx);
            snap.put("title", title);
            snap.put("source", "resume_project");
            snap.put("resumeId", resumeId);
            snap.put("projectName", project.name);
            if (StringUtils.isNotEmpty(project.role)) snap.put("projectRole", project.role);
            if (StringUtils.isNotEmpty(project.description)) snap.put("projectDesc", abbreviate(project.description, 400));
            snapshots.put(idx, snap);
            anchorCount++;
        }

        // ── 路径2：画像出题（薄弱点/岗位技能优先，跳过已选）
        List<PortalInterviewQuestion> profileQuestions = new ArrayList<>();
        if (useProfile && snapshot != null) {
            List<PortalInterviewQuestion> byProfile = pickQuestionsByProfile(snapshot, QUESTION_COUNT);
            for (PortalInterviewQuestion q : byProfile) {
                if (profileQuestions.size() >= PROFILE_QUESTION_COUNT_WITH_RESUME) break;
                if (q != null && q.getId() != null && !pickedIds.contains(q.getId())) {
                    profileQuestions.add(q);
                    pickedIds.add(q.getId());
                }
            }
        }
        // 画像不足时从题库补齐
        if (profileQuestions.size() < PROFILE_QUESTION_COUNT_WITH_RESUME) {
            List<PortalInterviewQuestion> fallback = pickQuestions(position, scene,
                    PROFILE_QUESTION_COUNT_WITH_RESUME - profileQuestions.size());
            for (PortalInterviewQuestion q : fallback) {
                if (q != null && q.getId() != null && !pickedIds.contains(q.getId())) {
                    profileQuestions.add(q);
                    pickedIds.add(q.getId());
                }
            }
        }
        questions.addAll(profileQuestions);

        // ── 路径3：兜底补满（锚定题无 id，不参与去重）
        if (questions.size() < QUESTION_COUNT) {
            LambdaQueryWrapper<PortalInterviewQuestion> qw = Wrappers.<PortalInterviewQuestion>lambdaQuery()
                    .eq(PortalInterviewQuestion::getStatus, "published");
            if (!pickedIds.isEmpty()) {
                qw.notIn(PortalInterviewQuestion::getId, pickedIds);
            }
            qw.last("ORDER BY RAND() LIMIT " + Math.max(1, QUESTION_COUNT - questions.size()));
            questions.addAll(questionMapper.selectList(qw));
        }
        if (questions.size() > QUESTION_COUNT) {
            // 优先裁掉末尾兜底题，保留锚定题与画像题
            questions = new ArrayList<>(questions.subList(0, QUESTION_COUNT));
            snapshots.keySet().removeIf(idx -> idx >= QUESTION_COUNT);
        }
        return new ResumePickResult(questions, snapshots);
    }

    /** 解析简历 projects JSON 为项目条目（兼容数组与 [{name,role,description}] 结构） */
    private List<ResumeProjectItem> loadResumeProjects(Long userId, Long resumeId) {
        List<ResumeProjectItem> items = new ArrayList<>();
        try {
            PortalUserResume resume = userResumeMapper.selectById(resumeId);
            if (resume == null || !userId.equals(resume.getUserId())) {
                log.warn("[VoiceInterview] 简历不存在或不属于当前用户 resumeId={}", resumeId);
                return items;
            }
            String projectsJson = resume.getProjects();
            if (StringUtils.isEmpty(projectsJson)) {
                return items;
            }
            com.fasterxml.jackson.databind.JsonNode arr = objectMapper.readTree(projectsJson);
            if (arr == null || !arr.isArray()) {
                return items;
            }
            for (com.fasterxml.jackson.databind.JsonNode node : arr) {
                if (node == null || !node.isObject()) continue;
                ResumeProjectItem item = new ResumeProjectItem();
                item.name = node.path("name").asText(node.path("projectName").asText(null));
                item.role = node.path("role").asText(node.path("position").asText(null));
                item.description = node.path("description").asText(node.path("content").asText(null));
                if (StringUtils.isNotEmpty(item.name)) {
                    items.add(item);
                }
            }
        } catch (Exception e) {
            log.warn("[VoiceInterview] 解析简历项目失败 resumeId={}：{}", resumeId, e.getMessage());
        }
        return items;
    }

    /** 从 configJson 取指定索引的锚定题快照标题（供推进逻辑恢复题面） */
    private String extractQuestionSnapshotTitle(String configJson, int idx) {
        if (StringUtils.isEmpty(configJson)) return null;
        try {
            Map<String, Object> config = objectMapper.readValue(configJson, Map.class);
            Object snaps = config.get("questionSnapshots");
            if (snaps instanceof Map) {
                Object snap = ((Map<String, Object>) snaps).get(String.valueOf(idx));
                if (snap instanceof Map) {
                    Object title = ((Map<String, Object>) snap).get("title");
                    return title == null ? null : title.toString();
                }
            }
        } catch (Exception e) {
            log.warn("[VoiceInterview] 解析 questionSnapshots 失败 idx={}：{}", idx, e.getMessage());
        }
        return null;
    }

    private String abbreviate(String text, int maxLen) {
        if (text == null) return null;
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "…";
    }

    private List<PortalInterviewQuestion> pickQuestions(String position, String scene, int count) {
        List<PortalInterviewQuestion> qs = queryQuestions(scene, position, count);
        if (qs.size() >= count) return qs;
        if (StringUtils.isNotEmpty(scene)) {
            qs = queryQuestions(scene, null, count);
            if (qs.size() >= count) return qs;
        }
        if (StringUtils.isNotEmpty(position)) {
            qs = queryQuestions(null, position, count);
            if (qs.size() >= count) return qs;
        }
        return queryQuestions(null, null, count);
    }

    private List<PortalInterviewQuestion> queryQuestions(String scene, String position, int count) {
        LambdaQueryWrapper<PortalInterviewQuestion> qw = Wrappers.<PortalInterviewQuestion>lambdaQuery()
                .eq(PortalInterviewQuestion::getStatus, "published");
        if (StringUtils.isNotEmpty(scene)) {
            qw.and(w -> w.like(PortalInterviewQuestion::getTags, scene)
                    .or().like(PortalInterviewQuestion::getDescription, scene));
        }
        if (StringUtils.isNotEmpty(position)) {
            qw.and(w -> w.like(PortalInterviewQuestion::getTags, position)
                    .or().like(PortalInterviewQuestion::getCompanies, position));
        }
        qw.last("ORDER BY RAND() LIMIT " + Math.max(1, count));
        return questionMapper.selectList(qw);
    }

    private List<PortalInterviewQuestion> queryByTag(String tag, int limit) {
        if (StringUtils.isEmpty(tag) || limit <= 0) return new ArrayList<>();
        LambdaQueryWrapper<PortalInterviewQuestion> qw = Wrappers.<PortalInterviewQuestion>lambdaQuery()
                .eq(PortalInterviewQuestion::getStatus, "published")
                .like(PortalInterviewQuestion::getTags, tag.trim())
                .last("ORDER BY RAND() LIMIT " + Math.max(1, limit));
        return questionMapper.selectList(qw);
    }

    private void mergeUnique(List<PortalInterviewQuestion> candidates, Set<Long> pickedIds,
                             List<PortalInterviewQuestion> result, int need) {
        if (candidates == null || candidates.isEmpty() || need <= 0) return;
        for (PortalInterviewQuestion q : candidates) {
            if (result.size() >= need) break;
            if (q == null || q.getId() == null || pickedIds.contains(q.getId())) continue;
            pickedIds.add(q.getId());
            result.add(q);
        }
    }

    // ========================================================================
    // 评分逻辑（复用 MockInterview 模式）
    // ========================================================================

    private ScoreResult scoreAnswer(PortalInterviewQuestion question, String answer) {
        List<String> keywords = question == null
                ? new ArrayList<>()
                : extractKeywords(question.getTags(), question.getSolution());
        String lowerAnswer = answer == null ? "" : answer.toLowerCase();
        int matched = 0;
        for (String kw : keywords) {
            if (lowerAnswer.contains(kw.toLowerCase())) {
                matched++;
            }
        }

        double coverage;
        if (keywords.isEmpty()) {
            coverage = answer.length() >= 50 ? 0.6 : 0.2;
        } else {
            coverage = (double) matched / keywords.size();
        }
        double lengthBonus = Math.min(answer.length() / 200.0, 1.0) * 20;
        int score = (int) Math.min(100, Math.round(coverage * 80 + lengthBonus));

        // 维度分（6 维，对齐前端雷达图：relevance/professionalism/fluency/interactivity/confidence/logic）
        Map<String, Integer> dimensions = new LinkedHashMap<>();
        int coverageScore = (int) Math.round(coverage * 100);
        int lengthScore = (int) Math.min(100, answer.length() / 2);
        int structureScore = matched >= 2 ? 70 : 40;
        dimensions.put("relevance", coverageScore);                                   // 回答相关性 = 关键词覆盖率
        dimensions.put("professionalism", structureScore);                             // 专业度 = 结构化命中
        dimensions.put("fluency", lengthScore);                                       // 表达流畅度 = 答案长度
        dimensions.put("interactivity", matched >= 1 ? 65 : 40);                      // 面试互动性 = 是否命中关键词
        dimensions.put("confidence", Math.min(100, 50 + (int) lengthBonus));          // 自信度 = 长度奖励基线
        dimensions.put("logic", matched >= 2 ? 75 : (matched >= 1 ? 55 : 35));         // 逻辑清晰 = 关键词结构化程度

        String feedback = buildFeedback(score, matched, keywords.size(), answer.length());
        return new ScoreResult(score, feedback, dimensions);
    }

    private List<String> extractKeywords(String tags, String solution) {
        Set<String> kw = new LinkedHashSet<>();
        if (StringUtils.isNotEmpty(tags)) {
            for (String t : tags.split("[,，]")) {
                String s = t.trim();
                if (isValidKeyword(s)) kw.add(s);
            }
        }
        if (StringUtils.isNotEmpty(solution) && kw.size() < MAX_KEYWORDS) {
            String[] chunks = solution.split("[\\s,，。.、；;：:！!？?\\n\\r\\t/()（）\\[\\]【】\"'`]+");
            for (String c : chunks) {
                String s = c.trim();
                if (isValidKeyword(s) && kw.size() < MAX_KEYWORDS) kw.add(s);
            }
        }
        return new ArrayList<>(kw);
    }

    private boolean isValidKeyword(String s) {
        if (s == null || s.length() < 2 || s.length() > 10) return false;
        if (STOPWORDS.contains(s)) return false;
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) return true;
        }
        return false;
    }

    private String buildFeedback(int score, int matched, int total, int len) {
        StringBuilder sb = new StringBuilder();
        if (score >= 80) sb.append("回答全面，覆盖了核心要点");
        else if (score >= 60) sb.append("回答较好，但部分关键点未提及");
        else if (score >= 40) sb.append("回答一般，建议补充更多细节");
        else sb.append("回答不够充分，建议参考标准答案深入理解");
        sb.append("。关键词覆盖 ").append(matched).append("/").append(total);
        sb.append("，答案长度 ").append(len).append(" 字。");
        return sb.toString();
    }

    // ========================================================================
    // DECIDING 矩阵：决定下一步动作
    // ========================================================================

    private String decideNextAction(PortalVoiceInterview interview, PortalVoiceInterviewQA qa, int score) {
        int currentIdx = interview.getCurrentIdx() == null ? 0 : interview.getCurrentIdx();
        int total = interview.getTotalQa() == null ? 0 : interview.getTotalQa();

        // 分数低且未追问过 → 追问
        if (score < 50 && qa.getParentQaId() == null && currentIdx < total - 1) {
            return "followup";
        }
        // 还有下一题 → next
        if (currentIdx < total - 1) {
            return "next";
        }
        // 最后一题 → report
        return "report";
    }

    /** 推进到下一题 */
    private VoiceInterviewQAWrapper advanceToNextQuestion(PortalVoiceInterview interview) {
        int nextIdx = (interview.getCurrentIdx() == null ? 0 : interview.getCurrentIdx()) + 1;
        int total = interview.getTotalQa() == null ? 0 : interview.getTotalQa();
        if (nextIdx >= total) {
            return null;
        }

        // 从 configJson 读取题单
        List<Long> qIds = extractQuestionIds(interview.getConfigJson());
        if (nextIdx >= qIds.size()) {
            return null;
        }
        Long qId = qIds.get(nextIdx);
        String questionTitle;
        Long resolvedQuestionId;
        if (qId != null) {
            PortalInterviewQuestion q = questionMapper.selectById(qId);
            if (q == null) {
                return null;
            }
            questionTitle = q.getTitle();
            resolvedQuestionId = q.getId();
        } else {
            // 简历锚定题：从快照恢复（题库中不存在该题）
            String snapshotTitle = extractQuestionSnapshotTitle(interview.getConfigJson(), nextIdx);
            if (StringUtils.isEmpty(snapshotTitle)) {
                return null;
            }
            questionTitle = snapshotTitle;
            resolvedQuestionId = null;
        }

        PortalVoiceInterviewQA qa = new PortalVoiceInterviewQA();
        qa.setInterviewId(interview.getId());
        qa.setQuestionId(resolvedQuestionId);
        qa.setQuestionIdx(nextIdx);
        qa.setQuestion(questionTitle);
        qa.setHintUsed(0);
        qa.setTranscriptionEdited(0);
        qa.setCreateTime(LocalDateTime.now());
        qa.setSpeakText(buildQuestionIntro(interview.getStyle(), questionTitle, nextIdx + 1));
        qaMapper.insert(qa);

        interview.setCurrentIdx(nextIdx);
        interviewMapper.updateById(interview);

        return new VoiceInterviewQAWrapper(qa);
    }

    /** 创建追问 QA（V10.4：question 由 LLM 针对候选人原话漏洞生成，analysis 提供漏洞上下文） */
    private PortalVoiceInterviewQA createFollowupQa(PortalVoiceInterview interview,
                                                     PortalVoiceInterviewQA parentQa,
                                                     PortalInterviewQuestion question,
                                                     ScoreResult sr,
                                                     String followupQuestion,
                                                     AnswerAnalysis analysis) {
        PortalVoiceInterviewQA followup = new PortalVoiceInterviewQA();
        followup.setInterviewId(interview.getId());
        followup.setQuestionId(parentQa.getQuestionId());
        followup.setQuestionIdx(parentQa.getQuestionIdx());
        followup.setParentQaId(parentQa.getId());
        followup.setHintUsed(0);
        followup.setTranscriptionEdited(0);
        followup.setCreateTime(LocalDateTime.now());

        followup.setQuestion(followupQuestion);
        // 播报话术：有漏洞时点明漏洞再追问，无则直接问
        String speak;
        if (analysis != null && !analysis.flaws.isEmpty()) {
            speak = "我注意到你刚才的回答里，" + analysis.flaws.get(0) + " 这一点说得还比较模糊。" + followupQuestion;
        } else {
            speak = followupQuestion;
        }
        followup.setSpeakText(speak);
        qaMapper.insert(followup);
        return followup;
    }

    // ========================================================================
    // 话术生成
    // ========================================================================

    private String generateSpeakText(PortalVoiceInterview interview, PortalInterviewQuestion question,
                                     String answer, ScoreResult sr, String nextAction) {
        // LLM 可用时增强（V10.4：系统提示词携带岗位/简历/画像完整上下文）
        if (aiProperties.isEnabled() && llmClient.isEnabled()) {
            try {
                String systemPrompt = buildContextualSystemPrompt(interview);
                StringBuilder userMsg = new StringBuilder();
                userMsg.append("题目：").append(question == null ? "未知" : question.getTitle()).append("\n");
                userMsg.append("考生回答：").append(answer).append("\n");
                userMsg.append("得分：").append(sr.score).append("/100\n");
                userMsg.append("关键词覆盖：").append(sr.dimensions.get("relevance")).append("%\n");
                userMsg.append("请作为面试官给出简短回应（50字以内），");
                switch (nextAction) {
                    case "followup":
                        userMsg.append("并引导考生补充回答。");
                        break;
                    case "next":
                        userMsg.append("肯定回答并过渡到下一题。");
                        break;
                    case "report":
                        userMsg.append("总结本次面试表现。");
                        break;
                    default:
                        userMsg.append("给出鼓励性反馈。");
                }
                String llmText = llmClient.chat(systemPrompt, userMsg.toString());
                if (StringUtils.isNotEmpty(llmText)) {
                    return llmText;
                }
            } catch (Exception e) {
                log.warn("[VoiceInterview] LLM 话术生成失败，降级规则话术：{}", e.getMessage());
            }
        }
        // 规则降级
        return buildRuleSpeakText(sr.score, nextAction);
    }

    private String buildInterviewerPrompt(String style) {
        String base = "你是一名资深技术面试官，正在面试候选人。请根据考生回答给出简短、专业的回应。";
        switch (style == null ? "professional" : style) {
            case "friendly":
                return base + "语气亲和、鼓励，让候选人放松。";
            case "strict":
                return base + "语气严格、直接，指出不足并要求补充。";
            default:
                return base + "语气专业、客观。";
        }
    }

    private String buildRuleSpeakText(int score, String nextAction) {
        switch (nextAction) {
            case "followup":
                return "你的回答覆盖了部分要点，能否再补充一些细节？";
            case "next":
                return score >= 70 ? "回答不错，我们来看下一题。" : "好的，我们继续下一题。";
            case "report":
                return "本次面试到此结束，我来为你做一个总结。";
            default:
                return "请继续。";
        }
    }

    private String buildGreetText(String style, String position, String firstQuestion) {
        // 场景信息通过会话记录读取，开场白同步体现，让候选人有真实代入感
        String greeting = "你好，欢迎参加";
        if (StringUtils.isNotEmpty(position)) {
            greeting += position + "岗位的";
        }
        greeting += "语音模拟面试。我是你的面试官。";
        greeting += "第一题：" + firstQuestion;
        return greeting;
    }

    private String buildQuestionIntro(String style, String question, int idx) {
        return "第" + idx + "题：" + question;
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
        vo.setScene(interview.getScene());
        vo.setResumeId(interview.getResumeId());
        // V11.0：agent 绑定信息（agentName 供前端顶栏展示；questionMode 从 configJson 读取）
        vo.setAgentId(interview.getAgentId());
        if (interview.getAgentId() != null) {
            vo.setAgentName(agentClient.agentName(interview.getAgentId()));
        }
        vo.setQuestionMode(String.valueOf(readInterviewConfig(interview).getOrDefault("questionMode", "preset")));
        vo.setStatus(interview.getStatus());
        vo.setStyle(interview.getStyle());
        vo.setDifficulty(interview.getDifficulty());
        vo.setTotalQa(interview.getTotalQa());
        vo.setCurrentIdx(interview.getCurrentIdx());
        vo.setScore(interview.getScore());
        vo.setSummary(interview.getSummary());
        vo.setConfigJson(interview.getConfigJson());
        vo.setIsPersonalized(interview.getIsPersonalized());
        vo.setCreateTime(interview.getCreateTime());
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

    @SuppressWarnings("unchecked")
    private List<Long> extractQuestionIds(String configJson) {
        if (StringUtils.isEmpty(configJson)) return new ArrayList<>();
        try {
            Map<String, Object> config = objectMapper.readValue(configJson, Map.class);
            Object ids = config.get("questionIds");
            if (ids instanceof List) {
                List<Long> result = new ArrayList<>();
                for (Object o : (List<Object>) ids) {
                    if (o == null) {
                        // 简历锚定题占位：保持索引对齐，不允许跳过
                        result.add(null);
                    } else if (o instanceof Number) {
                        result.add(((Number) o).longValue());
                    }
                }
                return result;
            }
        } catch (Exception e) {
            log.warn("[VoiceInterview] 解析 questionIds 失败：{}", e.getMessage());
        }
        return new ArrayList<>();
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("[VoiceInterview] JSON 序列化失败：{}", e.getMessage());
            return null;
        }
    }

    // ========================================================================
    // 内部类
    // ========================================================================

    private static class ScoreResult {
        final int score;
        final String feedback;
        final Map<String, Integer> dimensions;
        ScoreResult(int score, String feedback, Map<String, Integer> dimensions) {
            this.score = score;
            this.feedback = feedback;
            this.dimensions = dimensions;
        }
    }

    /** 追问决策结果 */
    private static class FollowupDecision {
        /** null = 不追问，推进下一题 */
        String question;
        String intent;
        String targetGap;
    }

    private static class VoiceInterviewQAWrapper {
        final PortalVoiceInterviewQA qa;
        VoiceInterviewQAWrapper(PortalVoiceInterviewQA qa) {
            this.qa = qa;
        }
    }
}
