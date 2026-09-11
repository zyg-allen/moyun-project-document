package com.moyun.ext.cms.service.interview.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.ext.cms.domain.vo.UserProfileSnapshotVO;
import com.moyun.ext.cms.service.IPortalInterviewConfigService;
import com.moyun.ext.cms.service.interview.QuestionPickCommand;
import com.moyun.ext.cms.service.interview.QuestionPickResult;
import com.moyun.ext.cms.service.interview.QuestionPicker;
import com.moyun.ext.cms.service.interview.QuestionWeights;
import com.moyun.ext.cms.service.interview.ResumeContext;
import com.moyun.portal.domain.entity.PortalInterviewConfig;
import com.moyun.portal.domain.entity.PortalInterviewQuestion;
import com.moyun.portal.domain.entity.PortalJobTemplate;
import com.moyun.portal.mapper.PortalInterviewQuestionMapper;
import com.moyun.portal.mapper.PortalJobTemplateMapper;
import com.moyun.util.string.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 智能出题器实现（v11.x）
 *
 * <p>四路题源 + 自动流转降级：
 * <ul>
 *   <li><b>job</b>：job_template_id 匹配按难度递进排序 → 不足用模板 keywords 匹配 tags</li>
 *   <li><b>resume</b>：ResumeContext.keywords 匹配 tags → 不足生成项目锚定题（上限 2）</li>
 *   <li><b>weak</b>：画像 weakTags（failRate 降序）逐个匹配 tags</li>
 *   <li><b>random</b>：随机兜底，所有题源剩余额度最终在此补满</li>
 * </ul>
 * 吸收 VoiceInterviewServiceImpl 原三路召回私有方法，锚定题快照协议保持兼容。</p>
 *
 * @author moyun
 */
@Component
public class QuestionPickerImpl implements QuestionPicker {

    private static final Logger log = LoggerFactory.getLogger(QuestionPickerImpl.class);

    /** 简历锚定题数量上限 */
    private static final int MAX_ANCHOR_QUESTIONS = 2;

    @Autowired
    private PortalInterviewQuestionMapper questionMapper;

    @Autowired
    private PortalJobTemplateMapper jobTemplateMapper;

    @Autowired
    private IPortalInterviewConfigService interviewConfigService;

    @Override
    public QuestionPickResult pick(QuestionPickCommand command) {
        if (command == null || command.getCount() <= 0) {
            throw new ServiceException("出题数量必须大于0");
        }
        int count = command.getCount();
        ResumeContext resumeContext = command.getResumeContext();

        // ── 岗位模板与权重落定（前端 > 面试配置 > 岗位模板 > 默认） ──
        PortalJobTemplate jobTemplate = loadJobTemplate(command.getJobTemplateId());
        QuestionWeights weights = resolveWeights(command, jobTemplate);
        Map<String, Integer> quota = weights.quota(count);
        log.info("[QuestionPicker] 出题权重落定 jobTemplateId={} weights={}/{}/{}/{} quota={}",
                command.getJobTemplateId(), weights.getJob(), weights.getResume(),
                weights.getWeak(), weights.getRandom(), quota);

        List<PortalInterviewQuestion> questions = new ArrayList<>();
        Map<Integer, Map<String, Object>> snapshots = new LinkedHashMap<>();
        Set<Long> pickedIds = new HashSet<>();
        QuestionPickResult result = new QuestionPickResult();

        // ── 题源1：job 岗位核心题（模板绑定题 + keywords 匹配） ──
        int jobWanted = quota.get("job");
        int jobGot = 0;
        if (jobTemplate != null && jobWanted > 0) {
            jobGot = pickFromJobTemplate(jobTemplate, command.getDifficulty(), jobWanted, pickedIds, questions);
            for (int i = 0; i < jobGot; i++) {
                result.recordSource("job");
            }
        }
        int remaining = count - questions.size();

        // ── 题源2：resume 简历深挖题（keywords 匹配 + 项目锚定题），承接 job 未满足额度 ──
        int resumeWanted = quota.get("resume") + Math.max(0, jobWanted - jobGot);
        if (resumeContext != null && resumeContext.hasResume() && resumeWanted > 0 && remaining > 0) {
            resumeWanted = Math.min(resumeWanted, remaining);
            pickFromResume(resumeContext, command, resumeWanted, pickedIds, questions, snapshots, result);
            remaining = count - questions.size();
        }
        int resumeGot = countSource(result, "resume") + countSource(result, "resume_project");

        // ── 题源3：weak 薄弱点题（画像 weakTags 按 failRate 降序），承接 job/resume 未满足额度 ──
        int weakWanted = quota.get("weak") + Math.max(0, resumeWanted - resumeGot);
        if (command.isUseProfile() && command.getSnapshot() != null && weakWanted > 0 && remaining > 0) {
            weakWanted = Math.min(weakWanted, remaining);
            int picked = pickFromWeak(command.getSnapshot(), weakWanted, pickedIds, questions);
            for (int i = 0; i < picked; i++) {
                result.recordSource("weak");
            }
            remaining = count - questions.size();
        }

        // ── 题源4：random 随机兜底（所有剩余额度在此补满） ──
        if (remaining > 0) {
            int picked = pickRandom(remaining, pickedIds, questions);
            for (int i = 0; i < picked; i++) {
                result.recordSource("random");
            }
        }

        if (questions.isEmpty()) {
            throw new ServiceException("题库中暂无可用题目，请稍后再试");
        }
        // 超额裁剪（保留锚定题与快照索引对齐）
        if (questions.size() > count) {
            questions = new ArrayList<>(questions.subList(0, count));
            snapshots.keySet().removeIf(idx -> idx >= count);
        }
        result.setQuestions(questions);
        result.setSnapshots(snapshots);
        log.info("[QuestionPicker] 出题完成 total={} sources={} anchorSnapshots={}",
                questions.size(), result.getSources(), snapshots.size());
        return result;
    }

    // ==================== 权重解析链 ====================

    private QuestionWeights resolveWeights(QuestionPickCommand command, PortalJobTemplate jobTemplate) {
        // 1. 前端传参
        if (command.getWeightsOverride() != null) {
            return command.getWeightsOverride();
        }
        // 2. 面试配置（默认配置）
        try {
            PortalInterviewConfig config = interviewConfigService.getDefaultConfig();
            if (config != null && StringUtils.isNotEmpty(config.getQuestionWeights())) {
                return QuestionWeights.fromJson(config.getQuestionWeights());
            }
        } catch (Exception e) {
            log.warn("[QuestionPicker] 读取面试配置权重失败，降级：{}", e.getMessage());
        }
        // 3. 岗位模板
        if (jobTemplate != null && StringUtils.isNotEmpty(jobTemplate.getWeights())) {
            return QuestionWeights.fromJson(jobTemplate.getWeights());
        }
        // 4. 默认 40/30/20/10
        return QuestionWeights.defaults();
    }

    private PortalJobTemplate loadJobTemplate(Long jobTemplateId) {
        if (jobTemplateId == null) {
            return null;
        }
        try {
            PortalJobTemplate template = jobTemplateMapper.selectById(jobTemplateId);
            if (template != null && "active".equals(template.getStatus())) {
                return template;
            }
        } catch (Exception e) {
            log.warn("[QuestionPicker] 岗位模板加载失败 jobTemplateId={}：{}", jobTemplateId, e.getMessage());
        }
        return null;
    }

    private int countSource(QuestionPickResult result, String source) {
        return result.getSources().getOrDefault(source, 0);
    }

    // ==================== 题源实现 ====================

    /** job 题源：模板绑定题（难度递进）→ 模板 keywords 匹配 tags */
    private int pickFromJobTemplate(PortalJobTemplate template, String difficulty, int quota,
                                    Set<Long> pickedIds, List<PortalInterviewQuestion> questions) {
        int before = questions.size();
        // 1. 模板绑定题：按难度递进排序（easy → medium → hard，未匹配难度排最后）
        List<PortalInterviewQuestion> bound = questionMapper.selectList(
                Wrappers.<PortalInterviewQuestion>lambdaQuery()
                        .eq(PortalInterviewQuestion::getStatus, "published")
                        .eq(PortalInterviewQuestion::getJobTemplateId, template.getId()));
        bound.sort((a, b) -> Integer.compare(difficultyRank(a.getDifficulty()), difficultyRank(b.getDifficulty())));
        for (PortalInterviewQuestion q : bound) {
            if (questions.size() - before >= quota) {
                break;
            }
            if (q.getId() != null && !pickedIds.contains(q.getId())) {
                pickedIds.add(q.getId());
                questions.add(q);
            }
        }
        // 2. 不足：模板 keywords 匹配 tags
        if (questions.size() - before < quota && StringUtils.isNotEmpty(template.getKeywords())) {
            for (String keyword : template.getKeywords().split(",")) {
                if (questions.size() - before >= quota) {
                    break;
                }
                String kw = keyword.trim();
                if (StringUtils.isEmpty(kw)) {
                    continue;
                }
                mergeUnique(queryByTag(kw, quota), pickedIds, questions, before + quota);
            }
        }
        return questions.size() - before;
    }

    /** resume 题源：keywords 匹配 tags → 项目锚定题（上限 2，虚拟实体 + 快照） */
    private void pickFromResume(ResumeContext resumeContext, QuestionPickCommand command, int quota,
                                Set<Long> pickedIds, List<PortalInterviewQuestion> questions,
                                Map<Integer, Map<String, Object>> snapshots, QuestionPickResult result) {
        int before = questions.size();
        // 1. 简历关键词匹配题库 tags
        for (String keyword : resumeContext.keywords()) {
            if (questions.size() - before >= quota) {
                break;
            }
            int matchedBefore = questions.size();
            mergeUnique(queryByTag(keyword, quota), pickedIds, questions, before + quota);
            for (int i = 0; i < questions.size() - matchedBefore; i++) {
                result.recordSource("resume");
            }
        }
        // 2. 不足：生成项目锚定题（虚拟实体，id=null，沿用旧快照协议）
        int anchorCount = 0;
        for (JsonNode project : resumeContext.getProjectNodes()) {
            if (questions.size() - before >= quota || anchorCount >= MAX_ANCHOR_QUESTIONS) {
                break;
            }
            String name = project.path("name").asText(project.path("projectName").asText(""));
            String role = project.path("role").asText(project.path("position").asText(""));
            String description = project.path("description").asText(project.path("content").asText(""));
            if (StringUtils.isEmpty(name)) {
                continue;
            }
            int idx = questions.size();
            String title = StringUtils.isEmpty(role)
                    ? "请详细介绍你在「" + name + "」项目中的核心工作与产出"
                    : "你在「" + name + "」项目中担任 " + role + "，请介绍你负责的核心模块、技术选型理由和最终产出";
            PortalInterviewQuestion anchor = new PortalInterviewQuestion();
            anchor.setTitle(title);
            questions.add(anchor);
            Map<String, Object> snap = new LinkedHashMap<>();
            snap.put("idx", idx);
            snap.put("title", title);
            snap.put("source", "resume_project");
            snap.put("resumeId", resumeContext.getResumeId());
            snap.put("projectName", name);
            if (StringUtils.isNotEmpty(role)) {
                snap.put("projectRole", role);
            }
            if (StringUtils.isNotEmpty(description)) {
                snap.put("projectDesc", abbreviate(description, 400));
            }
            snapshots.put(idx, snap);
            result.recordSource("resume_project");
            anchorCount++;
        }
    }

    /** weak 题源：画像薄弱点（weakTags 已按 failRate 降序）逐个匹配 tags */
    private int pickFromWeak(UserProfileSnapshotVO snapshot, int quota,
                             Set<Long> pickedIds, List<PortalInterviewQuestion> questions) {
        int before = questions.size();
        List<UserProfileSnapshotVO.WeakTagItem> weakTags = snapshot.getWeakTags();
        if (weakTags == null || weakTags.isEmpty()) {
            return 0;
        }
        for (UserProfileSnapshotVO.WeakTagItem tag : weakTags) {
            if (questions.size() - before >= quota) {
                break;
            }
            if (tag == null || StringUtils.isEmpty(tag.getTagName())) {
                continue;
            }
            mergeUnique(queryByTag(tag.getTagName(), quota), pickedIds, questions, before + quota);
        }
        return questions.size() - before;
    }

    /** random 题源：随机兜底（排除已选） */
    private int pickRandom(int need, Set<Long> pickedIds, List<PortalInterviewQuestion> questions) {
        int before = questions.size();
        LambdaQueryWrapper<PortalInterviewQuestion> qw = Wrappers.<PortalInterviewQuestion>lambdaQuery()
                .eq(PortalInterviewQuestion::getStatus, "published");
        if (!pickedIds.isEmpty()) {
            qw.notIn(PortalInterviewQuestion::getId, pickedIds);
        }
        qw.last("ORDER BY RAND() LIMIT " + Math.max(1, need));
        for (PortalInterviewQuestion q : questionMapper.selectList(qw)) {
            if (q != null && q.getId() != null && !pickedIds.contains(q.getId())) {
                pickedIds.add(q.getId());
                questions.add(q);
            }
        }
        return questions.size() - before;
    }

    // ==================== 工具（沿用原私有方法语义） ====================

    private int difficultyRank(String difficulty) {
        if ("easy".equals(difficulty)) {
            return 0;
        }
        if ("medium".equals(difficulty)) {
            return 1;
        }
        if ("hard".equals(difficulty)) {
            return 2;
        }
        return 3;
    }

    private List<PortalInterviewQuestion> queryByTag(String tag, int limit) {
        if (StringUtils.isEmpty(tag) || limit <= 0) {
            return new ArrayList<>();
        }
        return questionMapper.selectList(Wrappers.<PortalInterviewQuestion>lambdaQuery()
                .eq(PortalInterviewQuestion::getStatus, "published")
                .like(PortalInterviewQuestion::getTags, tag.trim())
                .last("ORDER BY RAND() LIMIT " + Math.max(1, limit)));
    }

    private void mergeUnique(List<PortalInterviewQuestion> candidates, Set<Long> pickedIds,
                             List<PortalInterviewQuestion> result, int limit) {
        if (candidates == null || candidates.isEmpty()) {
            return;
        }
        for (PortalInterviewQuestion q : candidates) {
            if (result.size() >= limit) {
                break;
            }
            if (q == null || q.getId() == null || pickedIds.contains(q.getId())) {
                continue;
            }
            pickedIds.add(q.getId());
            result.add(q);
        }
    }

    private String abbreviate(String text, int maxLen) {
        if (text == null) {
            return null;
        }
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "…";
    }
}