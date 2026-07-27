package com.moyun.ext.cms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.ext.cms.domain.vo.UserResumeVO;
import com.moyun.ext.cms.domain.vo.UserResumeVO.ScoreItem;
import com.moyun.ext.cms.domain.vo.UserResumeVO.SubScoreItem;
import com.moyun.portal.domain.entity.PortalInterviewPosition;
import com.moyun.ext.cms.service.IPortalInterviewPositionService;
import com.moyun.util.string.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 简历规则评分服务（面试空间第2期 / v5.9 阶段2：岗位匹配度增强）
 * <p>
 * 基于规则引擎的评分，不做 AI 建议（AI 建议见 ResumeAiAdviceService）。评分维度：
 * <ul>
 *   <li>基本信息（姓名/电话/邮箱）—— 20 分</li>
 *   <li>求职意向 —— 10 分</li>
 *   <li>教育经历 —— 15 分</li>
 *   <li>工作经历 —— 25 分</li>
 *   <li>项目经历 —— 20 分</li>
 *   <li>技能列表 —— 5 分</li>
 *   <li>自我介绍 —— 5 分</li>
 *   <li>岗位匹配度（v5.9 新增）—— 15 分（命中岗位必备技能比例）</li>
 * </ul>
 * 总满分 115 分（兼容历史 100 分展示，前端按 maxScore 归一化展示）。
 *
 * @author moyun
 */
@Service
public class ResumeScoringService {

    private static final Logger log = LoggerFactory.getLogger(ResumeScoringService.class);

    private static final int MAX_BASIC = 20;
    private static final int MAX_INTENTION = 10;
    private static final int MAX_EDUCATION = 15;
    private static final int MAX_WORK = 25;
    private static final int MAX_PROJECT = 20;
    private static final int MAX_SKILL = 5;
    private static final int MAX_INTRO = 5;
    /** v5.9 阶段2：岗位匹配度满分 */
    private static final int MAX_POSITION_MATCH = 15;

    @Autowired
    private IPortalInterviewPositionService positionService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 对简历进行评分（不含岗位匹配度，兼容旧调用）
     *
     * @return 评分明细列表
     */
    public List<ScoreItem> score(UserResumeVO vo) {
        return score(vo, null);
    }

    /**
     * 对简历进行评分（v5.9 阶段2：含岗位匹配度）
     *
     * @param vo           简历 VO
     * @param targetPosition 目标岗位名称（如 "Java后端工程师"），可为空；为空时岗位匹配度维度得 0 分
     * @return 评分明细列表
     */
    public List<ScoreItem> score(UserResumeVO vo, String targetPosition) {
        List<ScoreItem> items = new ArrayList<>();

        items.add(scoreBasic(vo));
        items.add(scoreIntention(vo));
        items.add(scoreEducation(vo));
        items.add(scoreWork(vo));
        items.add(scoreProject(vo));
        items.add(scoreSkill(vo));
        items.add(scoreIntro(vo));
        items.add(scorePositionMatch(vo, targetPosition));

        return items;
    }

    private ScoreItem scoreBasic(UserResumeVO vo) {
        ScoreItem item = new ScoreItem();
        item.setItem("基本信息");
        item.setMaxScore(MAX_BASIC);
        int score = 0;
        StringBuilder msg = new StringBuilder();
        if (isNotBlank(vo.getName())) score += 6;
        else msg.append("姓名缺失；");
        if (isNotBlank(vo.getPhone())) score += 5;
        else msg.append("电话缺失；");
        if (isNotBlank(vo.getEmail())) score += 5;
        else msg.append("邮箱缺失；");
        if (isNotBlank(vo.getGender())) score += 2;
        if (vo.getBirthDate() != null) score += 2;
        if (msg.length() == 0) msg.append("基本信息完整");
        item.setScore(score);
        item.setMessage(msg.toString());
        return item;
    }

    private ScoreItem scoreIntention(UserResumeVO vo) {
        ScoreItem item = new ScoreItem();
        item.setItem("求职意向");
        item.setMaxScore(MAX_INTENTION);
        int score = 0;
        StringBuilder msg = new StringBuilder();
        UserResumeVO.JobIntention ji = vo.getJobIntention();
        if (ji == null) {
            msg.append("未填写求职意向");
        } else {
            if (isNotBlank(ji.getPosition())) { score += 4; } else msg.append("期望职位缺失；");
            if (isNotBlank(ji.getCity())) { score += 3; } else msg.append("期望城市缺失；");
            if (ji.getSalaryMin() != null && ji.getSalaryMax() != null) { score += 2; } else msg.append("期望薪资缺失；");
            if (isNotBlank(ji.getJobType())) { score += 1; }
        }
        if (msg.length() == 0) msg.append("求职意向完整");
        item.setScore(score);
        item.setMessage(msg.toString());
        return item;
    }

    private ScoreItem scoreEducation(UserResumeVO vo) {
        ScoreItem item = new ScoreItem();
        item.setItem("教育经历");
        item.setMaxScore(MAX_EDUCATION);
        int score = 0;
        StringBuilder msg = new StringBuilder();
        List<UserResumeVO.EducationItem> list = vo.getEducations();
        int size = list == null ? 0 : list.size();
        if (size == 0) {
            msg.append("未填写教育经历");
        } else {
            // 每条 5 分，最多 15 分；并校验完整度
            for (UserResumeVO.EducationItem e : list) {
                int per = 0;
                if (isNotBlank(e.getSchool())) per++;
                if (isNotBlank(e.getMajor())) per++;
                if (isNotBlank(e.getDegree())) per++;
                if (isNotBlank(e.getStartDate()) && isNotBlank(e.getEndDate())) per++;
                score += per; // 每条最多 4 分
            }
            score = Math.min(score, MAX_EDUCATION);
            msg.append("共 ").append(size).append(" 条教育经历");
        }
        item.setScore(score);
        item.setMessage(msg.toString());
        return item;
    }

    private ScoreItem scoreWork(UserResumeVO vo) {
        ScoreItem item = new ScoreItem();
        item.setItem("工作经历");
        item.setMaxScore(MAX_WORK);
        int score = 0;
        StringBuilder msg = new StringBuilder();
        List<UserResumeVO.WorkItem> list = vo.getWorks();
        int size = list == null ? 0 : list.size();
        if (size == 0) {
            msg.append("未填写工作经历");
        } else {
            for (UserResumeVO.WorkItem w : list) {
                int per = 0;
                if (isNotBlank(w.getCompany())) per += 2;
                if (isNotBlank(w.getPosition())) per += 2;
                if (isNotBlank(w.getStartDate()) && isNotBlank(w.getEndDate())) per += 2;
                if (isNotBlank(w.getDescription())) per += 2;
                score += per; // 每条最多 8 分
            }
            score = Math.min(score, MAX_WORK);
            msg.append("共 ").append(size).append(" 条工作经历");
        }
        item.setScore(score);
        item.setMessage(msg.toString());
        return item;
    }

    private ScoreItem scoreProject(UserResumeVO vo) {
        ScoreItem item = new ScoreItem();
        item.setItem("项目经历");
        item.setMaxScore(MAX_PROJECT);
        int score = 0;
        StringBuilder msg = new StringBuilder();
        List<UserResumeVO.ProjectItem> list = vo.getProjects();
        int size = list == null ? 0 : list.size();
        if (size == 0) {
            msg.append("未填写项目经历");
        } else {
            for (UserResumeVO.ProjectItem p : list) {
                int per = 0;
                if (isNotBlank(p.getName())) per += 2;
                if (isNotBlank(p.getRole())) per += 1;
                if (isNotBlank(p.getStartDate()) && isNotBlank(p.getEndDate())) per += 1;
                if (isNotBlank(p.getDescription())) per += 3;
                score += per; // 每条最多 7 分
            }
            score = Math.min(score, MAX_PROJECT);
            msg.append("共 ").append(size).append(" 个项目");
        }
        item.setScore(score);
        item.setMessage(msg.toString());
        return item;
    }

    private ScoreItem scoreSkill(UserResumeVO vo) {
        ScoreItem item = new ScoreItem();
        item.setItem("技能列表");
        item.setMaxScore(MAX_SKILL);
        int score = 0;
        StringBuilder msg = new StringBuilder();
        List<UserResumeVO.SkillItem> list = vo.getSkills();
        int size = list == null ? 0 : list.size();
        if (size == 0) {
            msg.append("未填写技能");
        } else {
            // 1 个 1 分，2 个 3 分，3 个及以上 5 分
            if (size == 1) score = 1;
            else if (size == 2) score = 3;
            else score = 5;
            msg.append("共 ").append(size).append(" 项技能");
        }
        item.setScore(score);
        item.setMessage(msg.toString());
        return item;
    }

    private ScoreItem scoreIntro(UserResumeVO vo) {
        ScoreItem item = new ScoreItem();
        item.setItem("自我介绍");
        item.setMaxScore(MAX_INTRO);
        int score = 0;
        StringBuilder msg = new StringBuilder();
        String intro = vo.getSelfIntro();
        if (isNotBlank(intro)) {
            int len = intro.length();
            if (len >= 50) score = 5;
            else if (len >= 20) score = 3;
            else score = 1;
            msg.append("自我介绍 ").append(len).append(" 字");
        } else {
            msg.append("未填写自我介绍");
        }
        item.setScore(score);
        item.setMessage(msg.toString());
        return item;
    }

    // ========================================================================
    // v5.9 阶段2：岗位匹配度评分
    // ========================================================================
    /**
     * 岗位匹配度评分（满分 15 分）
     * <p>
     * 评分逻辑：
     * 1. 通过 targetPosition 反查岗位字典（支持精确 + 模糊兜底）
     * 2. 解析岗位必备技能 required_skills JSON 数组
     * 3. 计算简历技能列表与必备技能的命中率（技能名归一化：去空格、忽略大小写、模糊包含）
     * 4. 得分 = round(命中率 × 15)，并生成每个必备技能的子项明细（已掌握/缺失）
     * <p>
     * 命中策略：技能名 contains 匹配（如简历"Spring Boot"命中岗位"Spring"）
     *
     * @param vo             简历 VO
     * @param targetPosition 目标岗位名称，可为空
     * @return 评分项（含 subItems 子项明细）
     */
    private ScoreItem scorePositionMatch(UserResumeVO vo, String targetPosition) {
        ScoreItem item = new ScoreItem();
        item.setItem("岗位匹配度");
        item.setMaxScore(MAX_POSITION_MATCH);

        // 1. 无目标岗位 → 0 分
        if (StringUtils.isEmpty(targetPosition)) {
            item.setScore(0);
            item.setMessage("未设置目标岗位，无法评估匹配度（建议在档案页选择目标岗位）");
            return item;
        }

        // 2. 反查岗位字典（精确 + 模糊兜底）
        PortalInterviewPosition position = null;
        try {
            position = positionService.findByName(targetPosition);
        } catch (Exception e) {
            log.warn("[ResumeScore] 岗位字典查询失败 position={}: {}", targetPosition, e.getMessage());
        }
        if (position == null) {
            item.setScore(0);
            item.setMessage("目标岗位「" + targetPosition + "」未在岗位字典中找到匹配，无法评估匹配度");
            return item;
        }

        // 3. 解析必备技能 JSON 数组
        List<String> requiredSkills = parseStringArray(position.getRequiredSkills());
        if (requiredSkills.isEmpty()) {
            item.setScore(0);
            item.setMessage("岗位「" + position.getName() + "」未配置必备技能，无法评估匹配度");
            return item;
        }

        // 4. 简历技能列表归一化（去空格、小写）
        Set<String> resumeSkillSet = new HashSet<>();
        List<UserResumeVO.SkillItem> skills = vo.getSkills();
        if (skills != null) {
            for (UserResumeVO.SkillItem s : skills) {
                if (s != null && isNotBlank(s.getName())) {
                    resumeSkillSet.add(normalize(s.getName()));
                }
            }
        }

        // 5. 计算命中率与子项明细
        int hitCount = 0;
        List<SubScoreItem> subItems = new ArrayList<>();
        for (String required : requiredSkills) {
            if (StringUtils.isEmpty(required)) continue;
            SubScoreItem sub = new SubScoreItem();
            sub.setName(required);
            boolean hit = isSkillHit(required, resumeSkillSet);
            sub.setHit(hit);
            if (hit) {
                hitCount++;
                sub.setMessage("已掌握");
            } else {
                sub.setMessage("缺失");
            }
            subItems.add(sub);
        }

        // 6. 计算得分：命中率 × 15，四舍五入
        int score = (int) Math.round((double) hitCount / requiredSkills.size() * MAX_POSITION_MATCH);
        item.setScore(score);

        StringBuilder msg = new StringBuilder();
        msg.append("目标岗位「").append(position.getName()).append("」")
           .append("必备技能 ").append(hitCount).append("/").append(requiredSkills.size())
           .append(" 已掌握");
        if (hitCount < requiredSkills.size()) {
            msg.append("，建议补充缺失技能");
        }
        item.setMessage(msg.toString());
        item.setSubItems(subItems);
        return item;
    }

    /**
     * 技能命中判定：简历技能集合中是否存在某项 contains 归一化后的必备技能名
     * <p>
     * 双向 contains：简历"Spring Boot"包含"spring"（命中岗位"Spring"）；岗位"JavaScript"被简历"JS"包含时也算命中（兼容简写）
     */
    private boolean isSkillHit(String required, Set<String> resumeSkillSet) {
        String req = normalize(required);
        if (req.isEmpty()) return false;
        for (String s : resumeSkillSet) {
            // 简历技能 contains 必备技能，或必备技能 contains 简历技能（兼容简写如 JS↔JavaScript）
            if (s.contains(req) || req.contains(s)) {
                return true;
            }
        }
        return false;
    }

    /** 归一化：去空格、转小写 */
    private String normalize(String s) {
        return s == null ? "" : s.replaceAll("\\s+", "").toLowerCase();
    }

    /** 解析 JSON 字符串数组，失败返回空列表 */
    private List<String> parseStringArray(String json) {
        if (StringUtils.isEmpty(json)) return Collections.emptyList();
        try {
            String[] arr = objectMapper.readValue(json, String[].class);
            return arr == null ? Collections.emptyList() : Arrays.asList(arr);
        } catch (Exception e) {
            log.warn("[ResumeScore] 必备技能 JSON 解析失败: {} → {}", json, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 计算总分
     */
    public int total(List<ScoreItem> items) {
        int sum = 0;
        for (ScoreItem it : items) {
            if (it.getScore() != null) sum += it.getScore();
        }
        return sum;
    }

    private static boolean isNotBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
