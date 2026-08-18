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
import com.moyun.portal.domain.entity.PortalVoiceInterview;
import com.moyun.portal.domain.entity.PortalVoiceInterviewQA;
import com.moyun.portal.mapper.PortalInterviewQuestionMapper;
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

        // 出题
        List<PortalInterviewQuestion> questions = useProfile && snapshot != null
                ? pickQuestionsByProfile(snapshot, QUESTION_COUNT)
                : pickQuestions(position, scene, QUESTION_COUNT);
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

        // 缓存题单到会话（通过 configJson 附加 questionIds）
        List<Long> qIds = new ArrayList<>();
        for (PortalInterviewQuestion q : questions) {
            qIds.add(q.getId());
        }
        configMap.put("questionIds", qIds);
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

                // 保存答案与评分
                qa.setUserAnswer(transcript);
                qa.setScore(sr.score);
                qa.setAiFeedback(sr.feedback);
                qa.setLatencyMs(latencyMs);
                qa.setRuleDimensionsJson(toJson(sr.dimensions));

                // ② 决定下一步动作（DECIDING 矩阵）
                String nextAction = decideNextAction(interview, qa, sr.score);
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
                    // 追问：创建追问 QA
                    PortalVoiceInterviewQA followup = createFollowupQa(interview, qa, question, sr);
                    fullData.put("nextQaId", followup.getId());
                    fullData.put("nextQuestion", followup.getQuestion());
                    fullData.put("nextSpeakText", followup.getSpeakText());
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

        VoiceInterviewReportVO report = new VoiceInterviewReportVO();
        report.setInterviewId(interviewId);
        report.setTotalScore(avg);
        report.setDimensions(avgDimensions);
        report.setHighlights(highlights);
        report.setWeakPoints(weakPoints);
        report.setQuestionReviews(reviews);
        report.setSummary(buildSummary(interview.getTotalQa(), answered, avg));
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
        PortalInterviewQuestion q = questionMapper.selectById(qIds.get(nextIdx));
        if (q == null) {
            return null;
        }

        PortalVoiceInterviewQA qa = new PortalVoiceInterviewQA();
        qa.setInterviewId(interview.getId());
        qa.setQuestionId(q.getId());
        qa.setQuestionIdx(nextIdx);
        qa.setQuestion(q.getTitle());
        qa.setHintUsed(0);
        qa.setTranscriptionEdited(0);
        qa.setCreateTime(LocalDateTime.now());
        qa.setSpeakText(buildQuestionIntro(interview.getStyle(), q.getTitle(), nextIdx + 1));
        qaMapper.insert(qa);

        interview.setCurrentIdx(nextIdx);
        interviewMapper.updateById(interview);

        return new VoiceInterviewQAWrapper(qa);
    }

    /** 创建追问 QA */
    private PortalVoiceInterviewQA createFollowupQa(PortalVoiceInterview interview,
                                                     PortalVoiceInterviewQA parentQa,
                                                     PortalInterviewQuestion question,
                                                     ScoreResult sr) {
        PortalVoiceInterviewQA followup = new PortalVoiceInterviewQA();
        followup.setInterviewId(interview.getId());
        followup.setQuestionId(parentQa.getQuestionId());
        followup.setQuestionIdx(parentQa.getQuestionIdx());
        followup.setParentQaId(parentQa.getId());
        followup.setHintUsed(0);
        followup.setTranscriptionEdited(0);
        followup.setCreateTime(LocalDateTime.now());

        // 追问话术：基于评分反馈引导补充（对齐 6 维：relevance 回答相关性）
        String followupQuestion = "你的回答覆盖了部分要点，但还有补充空间。"
                + (sr.dimensions.get("relevance") < 50 ? "建议围绕核心概念再展开说明。" : "能否举一个具体例子说明？");
        followup.setQuestion(followupQuestion);
        followup.setSpeakText(followupQuestion);
        qaMapper.insert(followup);
        return followup;
    }

    // ========================================================================
    // 话术生成
    // ========================================================================

    private String generateSpeakText(PortalVoiceInterview interview, PortalInterviewQuestion question,
                                     String answer, ScoreResult sr, String nextAction) {
        // LLM 可用时增强
        if (aiProperties.isEnabled() && llmClient.isEnabled()) {
            try {
                String systemPrompt = buildInterviewerPrompt(interview.getStyle());
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
                    if (o instanceof Number) {
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

    private static class VoiceInterviewQAWrapper {
        final PortalVoiceInterviewQA qa;
        VoiceInterviewQAWrapper(PortalVoiceInterviewQA qa) {
            this.qa = qa;
        }
    }
}
