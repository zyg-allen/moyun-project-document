package com.moyun.ext.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.core.base.page.PageDomain;
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
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

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

    /** 构建分析/追问共用的上下文系统提示词：岗位 + 简历 + 累积画像 + 风格 */
    private String buildContextualSystemPrompt(PortalVoiceInterview interview) {
        Map<String, Object> cfg = readInterviewConfig(interview);
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位资深技术面试官，正在对候选人进行「").append(interview.getPosition())
          .append("」岗位的").append(difficultyText(interview.getDifficulty()))
          .append("模拟面试。\n");
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
        if ("stress".equals(style)) {
            sb.append("风格要求：压力面。语气锐利，敢于质疑和打断，追问到底。\n");
        } else if ("friendly".equals(style)) {
            sb.append("风格要求：亲和。语气友好，循循善诱，但要挖出真实深度。\n");
        } else {
            sb.append("风格要求：专业标准。语气平稳，逻辑严密。\n");
        }
        return sb.toString();
    }

    private String difficultyText(String difficulty) {
        if ("easy".equals(difficulty)) {
            return "基础";
        }
        if ("hard".equals(difficulty)) {
            return "高难度";
        }
        return "中等";
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

        // 出题（V10.3：resumeId 非空时启用简历深挖配比——简历项目2题 + 画像/岗位2题 + 兜底补满）
        List<PortalInterviewQuestion> questions;
        Map<Integer, Map<String, Object>> questionSnapshots = null;
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

        // 创建会话
        PortalVoiceInterview interview = new PortalVoiceInterview();
        interview.setUserId(userId);
        interview.setPosition(position);
        interview.setScene(scene);
        interview.setResumeId(config.getResumeId());
        interview.setStatus("in_progress");
        interview.setStyle(style);
        interview.setDifficulty(difficulty);
        interview.setTotalQa(questions.size());
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
        PortalInterviewQuestion firstQ = questions.get(0);
        PortalVoiceInterviewQA firstQa = new PortalVoiceInterviewQA();
        firstQa.setInterviewId(interview.getId());
        firstQa.setQuestionId(firstQ.getId());
        firstQa.setQuestionIdx(0);
        firstQa.setQuestion(firstQ.getTitle());
        firstQa.setHintUsed(0);
        firstQa.setTranscriptionEdited(0);
        firstQa.setCreateTime(LocalDateTime.now());
        // 首问话术
        firstQa.setSpeakText(buildGreetText(style, position, firstQ.getTitle()));
        qaMapper.insert(firstQa);

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

        // 异步处理 SSE 事件流
        sseExecutor.execute(() -> {
            try {
                // ① 回查原题目，计算规则分
                PortalInterviewQuestion question = qa.getQuestionId() == null
                        ? null : questionMapper.selectById(qa.getQuestionId());
                ScoreResult sr = scoreAnswer(question, transcript);

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
                // 事件1：规则分（立即返回）
                Map<String, Object> scoreData = new LinkedHashMap<>();
                scoreData.put("score", sr.score);
                scoreData.put("dimensions", sr.dimensions);
                emitter.send(SseEmitter.event().name("score").data(toJson(scoreData)));

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
        if (question == null) {
            throw new ServiceException("题目不存在，无法生成提示");
        }

        HintVO hint = hintEngine.generateHint(question, nextLevel);
        qa.setHintUsed(nextLevel);
        qaMapper.updateById(qa);

        VoiceInterviewVO vo = assembleVO(interview, qa);
        vo.setCurrentQa(toQaVO(qa));
        return vo;
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

        VoiceInterviewQAWrapper next = advanceToNextQuestion(interview);
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
        String greeting = "你好，欢迎参加";
        if (StringUtils.isNotEmpty(position)) {
            greeting += position + "岗位的";
        }
        greeting += "语音面试。我是你的面试官。";
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
