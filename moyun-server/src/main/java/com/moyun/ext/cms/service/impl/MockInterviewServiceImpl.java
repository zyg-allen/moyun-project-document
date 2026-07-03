package com.moyun.ext.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.core.base.page.PageDomain;
import com.moyun.ext.cms.domain.vo.MockInterviewDetailVO;
import com.moyun.ext.cms.service.IMockInterviewService;
import com.moyun.portal.domain.entity.PortalInterviewQuestion;
import com.moyun.portal.domain.entity.PortalMockInterview;
import com.moyun.portal.domain.entity.PortalMockInterviewQA;
import com.moyun.portal.mapper.PortalInterviewQuestionMapper;
import com.moyun.portal.mapper.PortalMockInterviewMapper;
import com.moyun.portal.mapper.PortalMockInterviewQAMapper;
import com.moyun.util.bean.PageUtils;
import com.moyun.util.string.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * AI 模拟面试官 Service 实现（任务 3.10 学习者成长闭环）
 * <p>
 * 规则化评分：从题目 tags + solution 提取关键词，统计答案覆盖率 + 长度奖励，最高 100 分。
 *
 * @author moyun
 */
@Service
public class MockInterviewServiceImpl implements IMockInterviewService {

    /** 每次面试抽取的题目数量 */
    private static final int QUESTION_COUNT = 5;

    /** 关键词提取上限 */
    private static final int MAX_KEYWORDS = 12;

    /** 中文/英文停用词，关键词提取时过滤 */
    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
            "的", "了", "是", "在", "和", "与", "或", "等", "为", "对", "由", "及",
            "一个", "一种", "可以", "通过", "使用", "进行", "实现", "the", "a", "an",
            "is", "are", "to", "of", "in", "on", "for", "and", "or", "with", "by",
            "this", "that", "it", "be", "as", "at", "so", "we", "he", "she", "you"
    ));

    @Autowired private PortalMockInterviewMapper interviewMapper;
    @Autowired private PortalMockInterviewQAMapper qaMapper;
    @Autowired private PortalInterviewQuestionMapper questionMapper;

    // ========================================================================
    // 开始面试
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MockInterviewDetailVO start(Long userId, String position, String scene) {
        if (userId == null) {
            throw new ServiceException("请登录后操作");
        }
        List<PortalInterviewQuestion> questions = pickQuestions(position, scene, QUESTION_COUNT);
        if (questions.isEmpty()) {
            throw new ServiceException("题库中暂无可用题目，请稍后再试");
        }

        // 创建会话
        PortalMockInterview interview = new PortalMockInterview();
        interview.setUserId(userId);
        interview.setPosition(position);
        interview.setScene(scene);
        interview.setStatus("in_progress");
        interview.setTotalQa(questions.size());
        interview.setCreateTime(LocalDateTime.now());
        interviewMapper.insert(interview);

        // 创建问答记录（题目快照）
        for (int i = 0; i < questions.size(); i++) {
            PortalInterviewQuestion q = questions.get(i);
            PortalMockInterviewQA qa = new PortalMockInterviewQA();
            qa.setInterviewId(interview.getId());
            qa.setQuestionId(q.getId());
            qa.setQuestionIdx(i);
            qa.setQuestion(q.getTitle());
            qa.setCreateTime(LocalDateTime.now());
            qaMapper.insert(qa);
        }

        return assembleDetail(interview);
    }

    // ========================================================================
    // 面试详情
    // ========================================================================
    @Override
    public MockInterviewDetailVO getDetail(Long id, Long userId) {
        PortalMockInterview interview = mustOwnInterview(id, userId);
        return assembleDetail(interview);
    }

    // ========================================================================
    // 提交答案 + AI 规则评分
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalMockInterviewQA answer(Long interviewId, Long userId, Integer questionIdx, String answer) {
        PortalMockInterview interview = mustOwnInterview(interviewId, userId);
        if ("finished".equals(interview.getStatus())) {
            throw new ServiceException("面试已结束，无法继续作答");
        }
        if (questionIdx == null || questionIdx < 0 || questionIdx >= interview.getTotalQa()) {
            throw new ServiceException("题目序号无效");
        }
        if (StringUtils.isEmpty(answer)) {
            throw new ServiceException("答案不能为空");
        }

        PortalMockInterviewQA qa = qaMapper.selectByInterviewId(interviewId).stream()
                .filter(x -> questionIdx.equals(x.getQuestionIdx()))
                .findFirst()
                .orElseThrow(() -> new ServiceException("题目不存在"));

        // 回查原题目，获取 tags/solution 作为评分参考要点
        PortalInterviewQuestion question = qa.getQuestionId() == null
                ? null : questionMapper.selectById(qa.getQuestionId());

        ScoreResult sr = scoreAnswer(question, answer);
        qa.setUserAnswer(answer);
        qa.setScore(sr.score);
        qa.setAiFeedback(sr.feedback);
        qaMapper.updateById(qa);
        return qa;
    }

    // ========================================================================
    // 结束面试
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MockInterviewDetailVO finish(Long interviewId, Long userId) {
        PortalMockInterview interview = mustOwnInterview(interviewId, userId);
        if ("finished".equals(interview.getStatus())) {
            // 幂等：已结束直接返回
            return assembleDetail(interview);
        }

        List<PortalMockInterviewQA> qaList = qaMapper.selectByInterviewId(interviewId);
        int answered = 0;
        long sum = 0;
        for (PortalMockInterviewQA qa : qaList) {
            if (qa.getScore() != null) {
                answered++;
                sum += qa.getScore();
            }
        }
        int avg = answered > 0 ? (int) Math.round((double) sum / answered) : 0;
        interview.setScore(avg);
        interview.setSummary(buildSummary(interview.getTotalQa(), answered, avg));
        interview.setStatus("finished");
        interview.setUpdateTime(LocalDateTime.now());
        interviewMapper.updateById(interview);
        return assembleDetail(interview);
    }

    // ========================================================================
    // 我的面试列表
    // ========================================================================
    @Override
    public Page<PortalMockInterview> listMy(Long userId, PageDomain query) {
        if (userId == null) {
            throw new ServiceException("请登录后操作");
        }
        Page<PortalMockInterview> page = PageUtils.buildPage(query);
        LambdaQueryWrapper<PortalMockInterview> qw = Wrappers.<PortalMockInterview>lambdaQuery()
                .eq(PortalMockInterview::getUserId, userId)
                .orderByDesc(PortalMockInterview::getCreateTime);
        return interviewMapper.selectPage(page, qw);
    }

    // ========================================================================
    // 题目抽取：按 scene（tags/description）+ position（tags/companies）匹配，逐步放宽
    // ========================================================================
    private List<PortalInterviewQuestion> pickQuestions(String position, String scene, int count) {
        // 1. 严格：scene + position
        List<PortalInterviewQuestion> qs = queryQuestions(scene, position, count);
        if (qs.size() >= count) return qs;
        // 2. 放宽：仅 scene
        if (StringUtils.isNotEmpty(scene)) {
            qs = queryQuestions(scene, null, count);
            if (qs.size() >= count) return qs;
        }
        // 3. 放宽：仅 position
        if (StringUtils.isNotEmpty(position)) {
            qs = queryQuestions(null, position, count);
            if (qs.size() >= count) return qs;
        }
        // 4. 全量
        return queryQuestions(null, null, count);
    }

    private List<PortalInterviewQuestion> queryQuestions(String scene, String position, int count) {
        LambdaQueryWrapper<PortalInterviewQuestion> qw = Wrappers.<PortalInterviewQuestion>lambdaQuery()
                .eq(PortalInterviewQuestion::getStatus, "active");
        if (StringUtils.isNotEmpty(scene)) {
            // scene 匹配 tags 或 description
            String s = scene.trim();
            qw.and(w -> w.like(PortalInterviewQuestion::getTags, s)
                    .or().like(PortalInterviewQuestion::getDescription, s));
        }
        if (StringUtils.isNotEmpty(position)) {
            // position 匹配 tags 或 companies
            String p = position.trim();
            qw.and(w -> w.like(PortalInterviewQuestion::getTags, p)
                    .or().like(PortalInterviewQuestion::getCompanies, p));
        }
        // 随机抽取，count 为 int 拼接安全
        qw.last("ORDER BY RAND() LIMIT " + Math.max(1, count));
        return questionMapper.selectList(qw);
    }

    // ========================================================================
    // AI 规则评分
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

        // 覆盖率：无关键词时退化为按答案长度给基础分，避免无参考要点的题目被全扣
        double coverage;
        if (keywords.isEmpty()) {
            coverage = answer.length() >= 50 ? 0.6 : 0.2;
        } else {
            coverage = (double) matched / keywords.size();
        }
        // 长度奖励：每 200 字 1 分，封顶 20 分
        double lengthBonus = Math.min(answer.length() / 200.0, 1.0) * 20;
        int score = (int) Math.min(100, Math.round(coverage * 80 + lengthBonus));

        String feedback = buildFeedback(score, matched, keywords.size(), answer.length());
        return new ScoreResult(score, feedback);
    }

    /** 从 tags + solution 提取关键词（去重、过滤停用词） */
    private List<String> extractKeywords(String tags, String solution) {
        Set<String> kw = new LinkedHashSet<>();
        // tags：逗号分隔，质量较高
        if (StringUtils.isNotEmpty(tags)) {
            for (String t : tags.split("[,，]")) {
                String s = t.trim();
                if (isValidKeyword(s)) {
                    kw.add(s);
                }
            }
        }
        // solution：按标点/空白切分，取长度 2-10 的片段
        if (StringUtils.isNotEmpty(solution) && kw.size() < MAX_KEYWORDS) {
            String[] chunks = solution.split("[\\s,，。.、；;：:！!？?\\n\\r\\t/()（）\\[\\]【】\"'`]+");
            for (String c : chunks) {
                String s = c.trim();
                if (isValidKeyword(s) && kw.size() < MAX_KEYWORDS) {
                    kw.add(s);
                }
            }
        }
        return new ArrayList<>(kw);
    }

    private boolean isValidKeyword(String s) {
        if (s == null || s.length() < 2 || s.length() > 10) {
            return false;
        }
        if (STOPWORDS.contains(s)) {
            return false;
        }
        // 纯数字不计
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private String buildFeedback(int score, int matched, int total, int len) {
        StringBuilder sb = new StringBuilder();
        if (score >= 80) {
            sb.append("回答全面，覆盖了核心要点");
        } else if (score >= 60) {
            sb.append("回答较好，但部分关键点未提及");
        } else if (score >= 40) {
            sb.append("回答一般，建议补充更多细节");
        } else {
            sb.append("回答不够充分，建议参考标准答案深入理解");
        }
        sb.append("。关键词覆盖 ").append(matched).append("/").append(total);
        sb.append("，答案长度 ").append(len).append(" 字。");
        return sb.toString();
    }

    private String buildSummary(int total, int answered, int avg) {
        StringBuilder sb = new StringBuilder();
        sb.append("本次模拟面试共 ").append(total).append(" 题，答完 ").append(answered).append(" 题");
        if (answered == 0) {
            sb.append("。建议先完成作答以获得评分。");
            return sb.toString();
        }
        sb.append("，平均得分 ").append(avg).append(" 分。");
        if (avg >= 80) {
            sb.append("整体表现优秀，对核心知识点掌握扎实。");
        } else if (avg >= 60) {
            sb.append("整体表现良好，部分知识点可进一步巩固。");
        } else if (avg >= 40) {
            sb.append("整体表现一般，建议针对薄弱环节重点复习。");
        } else {
            sb.append("整体表现有待提升，建议系统复习相关知识点后再试。");
        }
        return sb.toString();
    }

    // ========================================================================
    // 内部工具
    // ========================================================================

    /** 校验面试归属 */
    private PortalMockInterview mustOwnInterview(Long id, Long userId) {
        if (userId == null) {
            throw new ServiceException("请登录后操作");
        }
        PortalMockInterview interview = interviewMapper.selectById(id);
        if (interview == null) {
            throw new ServiceException("面试记录不存在");
        }
        if (!userId.equals(interview.getUserId())) {
            throw new ServiceException("无权操作该面试记录");
        }
        return interview;
    }

    /** 组装详情 VO（含问答列表与已答题数） */
    private MockInterviewDetailVO assembleDetail(PortalMockInterview interview) {
        MockInterviewDetailVO vo = new MockInterviewDetailVO();
        // 复制会话字段
        vo.setId(interview.getId());
        vo.setUserId(interview.getUserId());
        vo.setPosition(interview.getPosition());
        vo.setScene(interview.getScene());
        vo.setStatus(interview.getStatus());
        vo.setTotalQa(interview.getTotalQa());
        vo.setScore(interview.getScore());
        vo.setSummary(interview.getSummary());
        vo.setCreateTime(interview.getCreateTime());
        vo.setUpdateTime(interview.getUpdateTime());
        // 问答列表
        List<PortalMockInterviewQA> qaList = qaMapper.selectByInterviewId(interview.getId());
        vo.setQaList(qaList);
        int answered = 0;
        for (PortalMockInterviewQA qa : qaList) {
            if (qa.getScore() != null) {
                answered++;
            }
        }
        vo.setAnsweredCount(answered);
        return vo;
    }

    /** 评分结果值对象 */
    private static class ScoreResult {
        final int score;
        final String feedback;
        ScoreResult(int score, String feedback) {
            this.score = score;
            this.feedback = feedback;
        }
    }
}
