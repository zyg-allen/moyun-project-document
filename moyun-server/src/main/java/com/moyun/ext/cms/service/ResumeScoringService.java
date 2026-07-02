package com.moyun.ext.cms.service;

import com.moyun.ext.cms.domain.vo.UserResumeVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 简历规则评分服务（面试空间第2期）
 * <p>
 * 基于规则引擎的评分，不做 AI 建议。评分维度：
 * <ul>
 *   <li>基本信息（姓名/电话/邮箱）—— 20 分</li>
 *   <li>求职意向 —— 10 分</li>
 *   <li>教育经历 —— 15 分</li>
 *   <li>工作经历 —— 25 分</li>
 *   <li>项目经历 —— 20 分</li>
 *   <li>技能列表 —— 5 分</li>
 *   <li>自我介绍 —— 5 分</li>
 * </ul>
 * 满分 100 分。
 *
 * @author moyun
 */
@Service
public class ResumeScoringService {

    private static final int MAX_BASIC = 20;
    private static final int MAX_INTENTION = 10;
    private static final int MAX_EDUCATION = 15;
    private static final int MAX_WORK = 25;
    private static final int MAX_PROJECT = 20;
    private static final int MAX_SKILL = 5;
    private static final int MAX_INTRO = 5;

    /**
     * 对简历进行评分
     *
     * @return 评分明细列表（含总分汇总在最后一项）
     */
    public List<UserResumeVO.ScoreItem> score(UserResumeVO vo) {
        List<UserResumeVO.ScoreItem> items = new ArrayList<>();

        items.add(scoreBasic(vo));
        items.add(scoreIntention(vo));
        items.add(scoreEducation(vo));
        items.add(scoreWork(vo));
        items.add(scoreProject(vo));
        items.add(scoreSkill(vo));
        items.add(scoreIntro(vo));

        return items;
    }

    private UserResumeVO.ScoreItem scoreBasic(UserResumeVO vo) {
        UserResumeVO.ScoreItem item = new UserResumeVO.ScoreItem();
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

    private UserResumeVO.ScoreItem scoreIntention(UserResumeVO vo) {
        UserResumeVO.ScoreItem item = new UserResumeVO.ScoreItem();
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

    private UserResumeVO.ScoreItem scoreEducation(UserResumeVO vo) {
        UserResumeVO.ScoreItem item = new UserResumeVO.ScoreItem();
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

    private UserResumeVO.ScoreItem scoreWork(UserResumeVO vo) {
        UserResumeVO.ScoreItem item = new UserResumeVO.ScoreItem();
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

    private UserResumeVO.ScoreItem scoreProject(UserResumeVO vo) {
        UserResumeVO.ScoreItem item = new UserResumeVO.ScoreItem();
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

    private UserResumeVO.ScoreItem scoreSkill(UserResumeVO vo) {
        UserResumeVO.ScoreItem item = new UserResumeVO.ScoreItem();
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

    private UserResumeVO.ScoreItem scoreIntro(UserResumeVO vo) {
        UserResumeVO.ScoreItem item = new UserResumeVO.ScoreItem();
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

    /**
     * 计算总分
     */
    public int total(List<UserResumeVO.ScoreItem> items) {
        int sum = 0;
        for (UserResumeVO.ScoreItem it : items) {
            if (it.getScore() != null) sum += it.getScore();
        }
        return sum;
    }

    private static boolean isNotBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
