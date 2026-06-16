package com.moyun.ext.cms.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.util.string.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moyun.ext.cms.domain.query.InterviewCommentQuery;
import com.moyun.ext.cms.domain.query.InterviewCompanyQuery;
import com.moyun.ext.cms.domain.query.InterviewExperienceQuery;
import com.moyun.ext.cms.domain.query.InterviewQuestionQuery;
import com.moyun.ext.cms.domain.query.InterviewResumeTemplateQuery;
import com.moyun.ext.cms.domain.vo.InterviewBookmarkVO;
import com.moyun.ext.cms.domain.vo.InterviewCategoryVO;
import com.moyun.ext.cms.domain.vo.InterviewCommentVO;
import com.moyun.ext.cms.domain.vo.InterviewCompanyVO;
import com.moyun.ext.cms.domain.vo.InterviewExperienceVO;
import com.moyun.ext.cms.domain.vo.InterviewHomeDataVO;
import com.moyun.ext.cms.domain.vo.InterviewQuestionDetailVO;
import com.moyun.ext.cms.domain.vo.InterviewQuestionVO;
import com.moyun.ext.cms.domain.vo.InterviewResumeTemplateVO;
import com.moyun.ext.cms.domain.vo.InterviewSubmissionVO;
import com.moyun.ext.cms.service.IPortalInterviewService;
import com.moyun.portal.domain.entity.PortalInterviewAttempt;
import com.moyun.portal.domain.entity.PortalInterviewBookmark;
import com.moyun.portal.domain.entity.PortalInterviewCategory;
import com.moyun.portal.domain.entity.PortalInterviewComment;
import com.moyun.portal.domain.entity.PortalInterviewCommentLike;
import com.moyun.portal.domain.entity.PortalInterviewCompany;
import com.moyun.portal.domain.entity.PortalInterviewExperience;
import com.moyun.portal.domain.entity.PortalInterviewExperienceLike;
import com.moyun.portal.domain.entity.PortalInterviewQuestion;
import com.moyun.portal.domain.entity.PortalInterviewQuestionLike;
import com.moyun.portal.domain.entity.PortalInterviewResumeTemplate;
import com.moyun.portal.domain.entity.PortalInterviewSubmission;
import com.moyun.portal.mapper.PortalInterviewAttemptMapper;
import com.moyun.portal.mapper.PortalInterviewBookmarkMapper;
import com.moyun.portal.mapper.PortalInterviewCategoryMapper;
import com.moyun.portal.mapper.PortalInterviewCommentLikeMapper;
import com.moyun.portal.mapper.PortalInterviewCommentMapper;
import com.moyun.portal.mapper.PortalInterviewCompanyMapper;
import com.moyun.portal.mapper.PortalInterviewExperienceLikeMapper;
import com.moyun.portal.mapper.PortalInterviewExperienceMapper;
import com.moyun.portal.mapper.PortalInterviewQuestionLikeMapper;
import com.moyun.portal.mapper.PortalInterviewQuestionMapper;
import com.moyun.portal.mapper.PortalInterviewResumeTemplateMapper;
import com.moyun.portal.mapper.PortalInterviewSubmissionMapper;
import com.moyun.portal.service.IPortalTagService;

/**
 * 面试模块 Service 实现
 *
 * @author moyun
 */
@Service
public class PortalInterviewServiceImpl implements IPortalInterviewService {

    @Autowired private PortalInterviewQuestionMapper questionMapper;
    @Autowired private PortalInterviewCategoryMapper categoryMapper;
    @Autowired private PortalInterviewExperienceMapper experienceMapper;
    @Autowired private PortalInterviewResumeTemplateMapper resumeTemplateMapper;
    @Autowired private PortalInterviewSubmissionMapper submissionMapper;
    @Autowired private PortalInterviewBookmarkMapper bookmarkMapper;
    @Autowired private PortalInterviewQuestionLikeMapper questionLikeMapper;
    @Autowired private PortalInterviewAttemptMapper attemptMapper;
    @Autowired private PortalInterviewExperienceLikeMapper experienceLikeMapper;
    @Autowired private PortalInterviewCommentMapper commentMapper;
    @Autowired private PortalInterviewCommentLikeMapper commentLikeMapper;
    @Autowired private PortalInterviewCompanyMapper companyMapper;
    @Autowired private IPortalTagService portalTagService;

    // ========================================================================
    // 首页聚合
    // ========================================================================
    @Override
    public InterviewHomeDataVO getHomeData(Long currentUserId) {
        InterviewHomeDataVO vo = new InterviewHomeDataVO();

        // 分类列表
        List<PortalInterviewCategory> categoryEntities = categoryMapper.selectList(
                Wrappers.<PortalInterviewCategory>lambdaQuery().eq(PortalInterviewCategory::getStatus, "active").orderByAsc(PortalInterviewCategory::getSort));
        vo.setCategories(categoryEntities.stream().map(this::toCategoryVO).collect(Collectors.toList()));

        // 热门题目（按点赞数 + 提交数排序）
        Page<InterviewQuestionVO> hotQPage = new Page<>(1, 10);
        Page<InterviewQuestionVO> hotQResult = selectQuestionPage(hotQPage, new InterviewQuestionQuery(), currentUserId);
        vo.setHotQuestions(hotQResult.getRecords());

        // 热门面经（按浏览数 + 点赞数）
        InterviewExperienceQuery expQuery = new InterviewExperienceQuery();
        expQuery.setStatus("published");
        Page<InterviewExperienceVO> hotExpPage = new Page<>(1, 5);
        Page<InterviewExperienceVO> hotExpResult = selectExperiencePage(hotExpPage, expQuery, currentUserId);
        vo.setHotExperiences(hotExpResult.getRecords());

        // 简历模板
        InterviewResumeTemplateQuery resumeQuery = new InterviewResumeTemplateQuery();
        resumeQuery.setStatus("active");
        Page<InterviewResumeTemplateVO> resumePage = new Page<>(1, 8);
        Page<InterviewResumeTemplateVO> resumeResult = selectResumeTemplatePage(resumePage, resumeQuery, currentUserId);
        vo.setResumeTemplates(resumeResult.getRecords());

        // 热门公司
        List<PortalInterviewCompany> hotCompanies = companyMapper.selectHotCompanies(10);
        vo.setHotCompanies(hotCompanies.stream().map(this::toCompanyVO).collect(Collectors.toList()));

        // 平台统计
        vo.setTotalQuestionCount((long) questionMapper.selectCount(Wrappers.<PortalInterviewQuestion>lambdaQuery().eq(PortalInterviewQuestion::getStatus, "active")));
        vo.setTotalSubmissionCount(submissionMapper.selectCount(null) == null ? 0L : submissionMapper.selectCount(null).longValue());

        return vo;
    }

    // ========================================================================
    // 分类管理
    // ========================================================================
    @Override
    public List<InterviewCategoryVO> selectCategoryList() {
        List<PortalInterviewCategory> list = categoryMapper.selectList(Wrappers.<PortalInterviewCategory>lambdaQuery().orderByAsc(PortalInterviewCategory::getSort));
        return list.stream().map(this::toCategoryVO).collect(Collectors.toList());
    }

    @Override
    public InterviewCategoryVO selectCategoryById(Long id) {
        PortalInterviewCategory entity = categoryMapper.selectById(id);
        return entity == null ? null : toCategoryVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertCategory(PortalInterviewCategory category) {
        category.setCreateTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());
        if (category.getStatus() == null) category.setStatus("active");
        return categoryMapper.insert(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateCategory(PortalInterviewCategory category) {
        category.setUpdateTime(LocalDateTime.now());
        return categoryMapper.updateById(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteCategoryByIds(Long[] ids) {
        return categoryMapper.deleteBatchIds(Arrays.asList(ids));
    }

    // ========================================================================
    // 题目管理
    // ========================================================================
    @Override
    public Page<InterviewQuestionVO> selectQuestionPage(Page<InterviewQuestionVO> page, InterviewQuestionQuery query, Long currentUserId) {
        LambdaQueryWrapper<PortalInterviewQuestion> qw = Wrappers.lambdaQuery();
        qw.eq(PortalInterviewQuestion::getStatus, query.getStatus() == null ? "active" : query.getStatus());
        if (query.getCategoryId() != null) qw.eq(PortalInterviewQuestion::getCategoryId, query.getCategoryId());
        if (StringUtils.isNotEmpty(query.getDifficulty())) qw.eq(PortalInterviewQuestion::getDifficulty, query.getDifficulty());
        if (StringUtils.isNotEmpty(query.getKeyword())) {
            qw.like(PortalInterviewQuestion::getTitle, query.getKeyword()).or().like(PortalInterviewQuestion::getDescription, query.getKeyword());
        }
        qw.orderByAsc(PortalInterviewQuestion::getSort).orderByDesc(PortalInterviewQuestion::getCreateTime);

        Page<PortalInterviewQuestion> entityPage = new Page<>(page.getCurrent(), page.getSize());
        questionMapper.selectPage(entityPage, qw);

        List<InterviewQuestionVO> vos = entityPage.getRecords().stream().map(entity -> toQuestionVO(entity, currentUserId)).collect(Collectors.toList());
        page.setRecords(vos);
        page.setTotal(entityPage.getTotal());
        return page;
    }

    @Override
    public InterviewQuestionDetailVO selectQuestionDetailById(Long id, Long currentUserId) {
        PortalInterviewQuestion entity = questionMapper.selectById(id);
        if (entity == null) throw new ServiceException("题目不存在");
        InterviewQuestionDetailVO vo = new InterviewQuestionDetailVO();
        org.springframework.beans.BeanUtils.copyProperties(toQuestionVO(entity, currentUserId), vo);
        vo.setHint(entity.getHint());
        vo.setSolution(entity.getSolution());

        // 我的提交记录
        if (currentUserId != null) {
            List<PortalInterviewSubmission> submissions = submissionMapper.selectSubmissionsByQuestionAndUser(id, currentUserId);
            vo.setMySubmissions(submissions.stream().map(this::toSubmissionVO).limit(10).collect(Collectors.toList()));
        }
        // 关联查询通用标签
        if (vo != null && vo.getId() != null) {
            List<com.moyun.portal.domain.vo.TagVO> tagList = portalTagService.getTagsByEntity("interview_question", vo.getId());
            if (tagList != null) vo.setTagList(tagList);
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertQuestion(PortalInterviewQuestion question) {
        question.setCreateTime(LocalDateTime.now());
        question.setUpdateTime(LocalDateTime.now());
        if (question.getStatus() == null) question.setStatus("active");
        if (question.getAcceptanceRate() == null) question.setAcceptanceRate(BigDecimal.ZERO);
        if (question.getSubmissionCount() == null) question.setSubmissionCount(0L);
        if (question.getLikeCount() == null) question.setLikeCount(0L);
        int row = questionMapper.insert(question);
        // 同步绑定通用标签
        java.util.List<Long> extractedTagIds = new java.util.ArrayList<>();
        java.util.List<String> extractedTagNames = new java.util.ArrayList<>();
        if (question != null && question.getTags() != null && !question.getTags().trim().isEmpty()) {
            String[] parts = question.getTags().split(",");
            for (String p : parts) if (p != null && !p.trim().isEmpty()) extractedTagNames.add(p.trim());
        }
        portalTagService.bindTags("interview_question", question.getId(), extractedTagIds, extractedTagNames, "interview_question");
        return row;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateQuestion(PortalInterviewQuestion question) {
        question.setUpdateTime(LocalDateTime.now());
        int row = questionMapper.updateById(question);
        java.util.List<Long> extractedTagIds = new java.util.ArrayList<>();
        java.util.List<String> extractedTagNames = new java.util.ArrayList<>();
        if (question != null && question.getTags() != null && !question.getTags().trim().isEmpty()) {
            String[] parts = question.getTags().split(",");
            for (String p : parts) if (p != null && !p.trim().isEmpty()) extractedTagNames.add(p.trim());
        }
        portalTagService.bindTags("interview_question", question.getId(), extractedTagIds, extractedTagNames, "interview_question");
        return row;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteQuestionByIds(Long[] ids) {
        return questionMapper.deleteBatchIds(Arrays.asList(ids));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InterviewSubmissionVO submitAnswer(Long questionId, Long userId, Map<String, Object> body) {
        PortalInterviewQuestion question = questionMapper.selectById(questionId);
        if (question == null) throw new ServiceException("题目不存在");
        if (userId == null) throw new ServiceException("请登录后提交");

        // 1. 记录提交
        PortalInterviewSubmission submission = new PortalInterviewSubmission();
        submission.setQuestionId(questionId);
        submission.setUserId(userId);
        submission.setCode(body.get("code") != null ? body.get("code").toString() : null);
        submission.setContent(body.get("content") != null ? body.get("content").toString() : null);
        submission.setLanguage(body.get("language") != null ? body.get("language").toString() : "text");
        submission.setAnswerType(body.get("answerType") != null ? body.get("answerType").toString() : "text");
        submission.setNote(body.get("note") != null ? body.get("note").toString() : null);
        // 简单策略：非空答案视为通过，后续可集成代码评测系统
        boolean isSuccess = StringUtils.isNotEmpty(submission.getCode()) || StringUtils.isNotEmpty(submission.getContent());
        submission.setIsSuccess(isSuccess);
        submission.setStatus(isSuccess ? "accepted" : "pending");
        submission.setRuntime(null);
        submission.setMemoryUsage(null);
        submission.setCreateTime(LocalDateTime.now());
        submissionMapper.insert(submission);

        // 2. 更新题目提交数
        question.setSubmissionCount((question.getSubmissionCount() == null ? 0L : question.getSubmissionCount()) + 1);
        long totalSub = submissionMapper.countSubmissionsByQuestion(questionId);
        long successSub = submissionMapper.countSuccessByQuestion(questionId);
        BigDecimal rate = totalSub > 0 ? BigDecimal.valueOf(successSub * 100.0 / totalSub) : BigDecimal.ZERO;
        question.setAcceptanceRate(rate);
        questionMapper.updateById(question);

        // 3. 更新做题记录
        PortalInterviewAttempt attempt = attemptMapper.selectAttempt(questionId, userId);
        if (attempt == null) {
            attempt = new PortalInterviewAttempt();
            attempt.setQuestionId(questionId);
            attempt.setUserId(userId);
            attempt.setAttemptCount(1);
            attempt.setStatus(isSuccess ? "solved" : "attempted");
            attempt.setLastAttemptAt(LocalDateTime.now());
            if (isSuccess) {
                attempt.setFirstSolvedAt(LocalDateTime.now());
                attempt.setLastSolvedAt(LocalDateTime.now());
            }
            attemptMapper.insert(attempt);
        } else {
            attempt.setAttemptCount(attempt.getAttemptCount() + 1);
            attempt.setLastAttemptAt(LocalDateTime.now());
            if (isSuccess && "attempted".equals(attempt.getStatus())) {
                attempt.setStatus("solved");
                attempt.setFirstSolvedAt(LocalDateTime.now());
                attempt.setLastSolvedAt(LocalDateTime.now());
            } else if (isSuccess) {
                attempt.setLastSolvedAt(LocalDateTime.now());
            }
            attemptMapper.updateById(attempt);
        }
        return toSubmissionVO(submission);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleQuestionLike(Long questionId, Long userId) {
        if (userId == null) throw new ServiceException("请登录后操作");
        PortalInterviewQuestionLike exist = questionLikeMapper.selectLike(questionId, userId);
        PortalInterviewQuestion question = questionMapper.selectById(questionId);
        if (question == null) throw new ServiceException("题目不存在");

        Map<String, Object> result = new HashMap<>();
        if (exist != null) {
            // 取消点赞
            questionLikeMapper.deleteById(exist.getId());
            question.setLikeCount(Math.max(0L, (question.getLikeCount() == null ? 0L : question.getLikeCount()) - 1));
            result.put("liked", false);
        } else {
            // 新增点赞
            PortalInterviewQuestionLike like = new PortalInterviewQuestionLike();
            like.setQuestionId(questionId);
            like.setUserId(userId);
            like.setCreateTime(LocalDateTime.now());
            questionLikeMapper.insert(like);
            question.setLikeCount((question.getLikeCount() == null ? 0L : question.getLikeCount()) + 1);
            result.put("liked", true);
        }
        questionMapper.updateById(question);
        result.put("likeCount", question.getLikeCount());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleQuestionBookmark(Long questionId, Long userId, String note) {
        if (userId == null) throw new ServiceException("请登录后操作");
        PortalInterviewBookmark exist = bookmarkMapper.selectBookmark(questionId, userId);
        Map<String, Object> result = new HashMap<>();
        if (exist != null) {
            bookmarkMapper.deleteById(exist.getId());
            result.put("bookmarked", false);
        } else {
            PortalInterviewBookmark bm = new PortalInterviewBookmark();
            bm.setQuestionId(questionId);
            bm.setUserId(userId);
            bm.setNote(note);
            bm.setCreateTime(LocalDateTime.now());
            bookmarkMapper.insert(bm);
            result.put("bookmarked", true);
        }
        return result;
    }

    @Override
    public Page<InterviewBookmarkVO> selectBookmarkPage(Page<InterviewBookmarkVO> page, Long userId) {
        if (userId == null) { page.setRecords(Collections.emptyList()); return page; }
        List<PortalInterviewBookmark> list = bookmarkMapper.selectBookmarkListByUserId(userId);
        List<InterviewBookmarkVO> vos = list.stream().map(entity -> {
            InterviewBookmarkVO vo = new InterviewBookmarkVO();
            vo.setId(entity.getId());
            vo.setQuestionId(entity.getQuestionId());
            vo.setUserId(entity.getUserId());
            vo.setNote(entity.getNote());
            vo.setCreateTime(entity.getCreateTime());
            PortalInterviewQuestion q = questionMapper.selectById(entity.getQuestionId());
            if (q != null) vo.setQuestion(toQuestionVO(q, userId));
            return vo;
        }).skip((long) (int)((page.getCurrent() - 1) * page.getSize())).limit((int) page.getSize()).collect(Collectors.toList());
        page.setRecords(vos);
        page.setTotal((long) list.size());
        return page;
    }

    // ========================================================================
    // 面经管理
    // ========================================================================
    @Override
    public Page<InterviewExperienceVO> selectExperiencePage(Page<InterviewExperienceVO> page, InterviewExperienceQuery query, Long currentUserId) {
        LambdaQueryWrapper<PortalInterviewExperience> qw = Wrappers.lambdaQuery();
        qw.eq(PortalInterviewExperience::getStatus, query.getStatus() == null ? "published" : query.getStatus());
        if (StringUtils.isNotEmpty(query.getKeyword())) {
            qw.like(PortalInterviewExperience::getTitle, query.getKeyword()).or().like(PortalInterviewExperience::getContent, query.getKeyword());
        }
        if (StringUtils.isNotEmpty(query.getCompany())) qw.like(PortalInterviewExperience::getCompany, query.getCompany());
        if (query.getYear() != null) qw.eq(PortalInterviewExperience::getYear, query.getYear());
        if (query.getUserId() != null) qw.eq(PortalInterviewExperience::getUserId, query.getUserId());
        qw.orderByDesc(PortalInterviewExperience::getIsTop).orderByDesc(PortalInterviewExperience::getCreateTime);

        Page<PortalInterviewExperience> entityPage = new Page<>(page.getCurrent(), page.getSize());
        experienceMapper.selectPage(entityPage, qw);
        List<InterviewExperienceVO> vos = entityPage.getRecords().stream().map(e -> toExperienceVO(e, currentUserId)).collect(Collectors.toList());
        page.setRecords(vos);
        page.setTotal(entityPage.getTotal());
        return page;
    }

    @Override
    public InterviewExperienceVO selectExperienceDetailById(Long id, Long currentUserId) {
        PortalInterviewExperience entity = experienceMapper.selectById(id);
        if (entity == null) throw new ServiceException("面经不存在");
        // 原子更新浏览数
        experienceMapper.incrementViewCount(id);
        entity.setViewCount((entity.getViewCount() == null ? 0L : entity.getViewCount()) + 1);
        InterviewExperienceVO vo = toExperienceVO(entity, currentUserId);
        if (vo != null && vo.getId() != null) {
            List<com.moyun.portal.domain.vo.TagVO> tagList = portalTagService.getTagsByEntity("interview_experience", vo.getId());
            if (tagList != null) vo.setTagList(tagList);
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertExperience(PortalInterviewExperience experience, Long userId) {
        experience.setUserId(userId);
        experience.setCreateTime(LocalDateTime.now());
        experience.setUpdateTime(LocalDateTime.now());
        experience.setStatus(experience.getStatus() == null ? "pending" : experience.getStatus());
        if (experience.getViewCount() == null) experience.setViewCount(0L);
        if (experience.getLikeCount() == null) experience.setLikeCount(0L);
        if (experience.getCommentCount() == null) experience.setCommentCount(0L);
        int row = experienceMapper.insert(experience);
        // 同步绑定通用标签
        java.util.List<Long> extractedTagIds = new java.util.ArrayList<>();
        java.util.List<String> extractedTagNames = new java.util.ArrayList<>();
        if (experience != null && experience.getTags() != null && !experience.getTags().trim().isEmpty()) {
            String[] parts = experience.getTags().split(",");
            for (String p : parts) if (p != null && !p.trim().isEmpty()) extractedTagNames.add(p.trim());
        }
        portalTagService.bindTags("interview_experience", experience.getId(), extractedTagIds, extractedTagNames, "interview_experience");
        return row;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateExperience(PortalInterviewExperience experience, Long userId) {
        PortalInterviewExperience db = experienceMapper.selectById(experience.getId());
        if (db == null) throw new ServiceException("面经不存在");
        if (!db.getUserId().equals(userId)) throw new ServiceException("无权修改他人的面经");
        experience.setUpdateTime(LocalDateTime.now());
        int row = experienceMapper.updateById(experience);
        java.util.List<Long> extractedTagIds = new java.util.ArrayList<>();
        java.util.List<String> extractedTagNames = new java.util.ArrayList<>();
        if (experience != null && experience.getTags() != null && !experience.getTags().trim().isEmpty()) {
            String[] parts = experience.getTags().split(",");
            for (String p : parts) if (p != null && !p.trim().isEmpty()) extractedTagNames.add(p.trim());
        }
        portalTagService.bindTags("interview_experience", experience.getId(), extractedTagIds, extractedTagNames, "interview_experience");
        return row;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteExperienceById(Long id, Long userId) {
        PortalInterviewExperience db = experienceMapper.selectById(id);
        if (db == null) return 0;
        if (!db.getUserId().equals(userId)) throw new ServiceException("无权删除他人的面经");
        return experienceMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int auditExperience(Long id, String status, String remark) {
        PortalInterviewExperience entity = experienceMapper.selectById(id);
        if (entity == null) throw new ServiceException("面经不存在");
        entity.setStatus(status);
        entity.setUpdateTime(LocalDateTime.now());
        return experienceMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int topExperience(Long id, Boolean isTop) {
        PortalInterviewExperience entity = experienceMapper.selectById(id);
        if (entity == null) throw new ServiceException("面经不存在");
        entity.setIsTop(isTop);
        entity.setUpdateTime(LocalDateTime.now());
        return experienceMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleExperienceLike(Long experienceId, Long userId) {
        if (userId == null) throw new ServiceException("请登录后操作");
        PortalInterviewExperienceLike exist = experienceLikeMapper.selectLike(experienceId, userId);
        PortalInterviewExperience exp = experienceMapper.selectById(experienceId);
        if (exp == null) throw new ServiceException("面经不存在");
        Map<String, Object> result = new HashMap<>();
        if (exist != null) {
            experienceLikeMapper.deleteById(exist.getId());
            exp.setLikeCount(Math.max(0L, (exp.getLikeCount() == null ? 0L : exp.getLikeCount()) - 1));
            result.put("liked", false);
        } else {
            PortalInterviewExperienceLike like = new PortalInterviewExperienceLike();
            like.setExperienceId(experienceId);
            like.setUserId(userId);
            like.setCreateTime(LocalDateTime.now());
            experienceLikeMapper.insert(like);
            exp.setLikeCount((exp.getLikeCount() == null ? 0L : exp.getLikeCount()) + 1);
            result.put("liked", true);
        }
        experienceMapper.updateById(exp);
        result.put("likeCount", exp.getLikeCount());
        return result;
    }

    // ========================================================================
    // 评论管理
    // ========================================================================
    @Override
    public Page<InterviewCommentVO> selectCommentPage(Page<InterviewCommentVO> page, InterviewCommentQuery query, Long currentUserId) {
        LambdaQueryWrapper<PortalInterviewComment> qw = Wrappers.lambdaQuery();
        qw.eq(PortalInterviewComment::getStatus, query.getStatus() == null ? "published" : query.getStatus());
        if (query.getExperienceId() != null) qw.eq(PortalInterviewComment::getExperienceId, query.getExperienceId());
        if (query.getUserId() != null) qw.eq(PortalInterviewComment::getUserId, query.getUserId());
        if (StringUtils.isNotEmpty(query.getKeyword())) qw.like(PortalInterviewComment::getContent, query.getKeyword());
        qw.isNull(PortalInterviewComment::getParentId).orderByDesc(PortalInterviewComment::getCreateTime);

        Page<PortalInterviewComment> entityPage = new Page<>(page.getCurrent(), page.getSize());
        commentMapper.selectPage(entityPage, qw);
        List<InterviewCommentVO> vos = entityPage.getRecords().stream().map(e -> toCommentVO(e, currentUserId)).collect(Collectors.toList());
        page.setRecords(vos);
        page.setTotal(entityPage.getTotal());
        return page;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertComment(PortalInterviewComment comment, Long userId) {
        if (userId == null) throw new ServiceException("请登录后操作");
        PortalInterviewExperience exp = experienceMapper.selectById(comment.getExperienceId());
        if (exp == null) throw new ServiceException("面经不存在");
        comment.setUserId(userId);
        comment.setStatus(comment.getStatus() == null ? "published" : comment.getStatus());
        if (comment.getLikeCount() == null) comment.setLikeCount(0L);
        comment.setCreateTime(LocalDateTime.now());
        comment.setUpdateTime(LocalDateTime.now());
        int row = commentMapper.insert(comment);
        // 更新面经评论数
        experienceMapper.incrementCommentCount(comment.getExperienceId());
        return row;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteCommentById(Long id, Long userId) {
        PortalInterviewComment comment = commentMapper.selectById(id);
        if (comment == null) return 0;
        if (!comment.getUserId().equals(userId)) throw new ServiceException("无权删除他人的评论");
        return commentMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int auditComment(Long id, String status, String remark) {
        PortalInterviewComment entity = commentMapper.selectById(id);
        if (entity == null) throw new ServiceException("评论不存在");
        entity.setStatus(status);
        entity.setUpdateTime(LocalDateTime.now());
        return commentMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleCommentLike(Long commentId, Long userId) {
        if (userId == null) throw new ServiceException("请登录后操作");
        PortalInterviewCommentLike exist = commentLikeMapper.selectLike(commentId, userId);
        PortalInterviewComment comment = commentMapper.selectById(commentId);
        if (comment == null) throw new ServiceException("评论不存在");
        Map<String, Object> result = new HashMap<>();
        if (exist != null) {
            commentLikeMapper.deleteById(exist.getId());
            comment.setLikeCount(Math.max(0L, (comment.getLikeCount() == null ? 0L : comment.getLikeCount()) - 1));
            result.put("liked", false);
        } else {
            PortalInterviewCommentLike like = new PortalInterviewCommentLike();
            like.setCommentId(commentId);
            like.setUserId(userId);
            like.setCreateTime(LocalDateTime.now());
            commentLikeMapper.insert(like);
            comment.setLikeCount((comment.getLikeCount() == null ? 0L : comment.getLikeCount()) + 1);
            result.put("liked", true);
        }
        commentMapper.updateById(comment);
        result.put("likeCount", comment.getLikeCount());
        return result;
    }

    // ========================================================================
    // 简历模板管理
    // ========================================================================
    @Override
    public Page<InterviewResumeTemplateVO> selectResumeTemplatePage(Page<InterviewResumeTemplateVO> page, InterviewResumeTemplateQuery query, Long currentUserId) {
        LambdaQueryWrapper<PortalInterviewResumeTemplate> qw = Wrappers.lambdaQuery();
        qw.eq(PortalInterviewResumeTemplate::getStatus, query.getStatus() == null ? "active" : query.getStatus());
        if (StringUtils.isNotEmpty(query.getCategory())) qw.eq(PortalInterviewResumeTemplate::getCategory, query.getCategory());
        if (StringUtils.isNotEmpty(query.getFileType())) qw.eq(PortalInterviewResumeTemplate::getFileType, query.getFileType());
        if (query.getIsPremium() != null) qw.eq(PortalInterviewResumeTemplate::getIsPremium, query.getIsPremium());
        if (StringUtils.isNotEmpty(query.getKeyword())) {
            qw.like(PortalInterviewResumeTemplate::getTitle, query.getKeyword()).or().like(PortalInterviewResumeTemplate::getDescription, query.getKeyword());
        }
        qw.orderByAsc(PortalInterviewResumeTemplate::getSort).orderByDesc(PortalInterviewResumeTemplate::getCreateTime);

        Page<PortalInterviewResumeTemplate> entityPage = new Page<>(page.getCurrent(), page.getSize());
        resumeTemplateMapper.selectPage(entityPage, qw);
        List<InterviewResumeTemplateVO> vos = entityPage.getRecords().stream().map(entity -> toResumeTemplateVO(entity, currentUserId)).collect(Collectors.toList());
        page.setRecords(vos);
        page.setTotal(entityPage.getTotal());
        return page;
    }

    @Override
    public InterviewResumeTemplateVO selectResumeTemplateById(Long id) {
        PortalInterviewResumeTemplate entity = resumeTemplateMapper.selectById(id);
        InterviewResumeTemplateVO vo = entity == null ? null : toResumeTemplateVO(entity, null);
        if (vo != null && vo.getId() != null) {
            List<com.moyun.portal.domain.vo.TagVO> tagList = portalTagService.getTagsByEntity("interview_resume_template", vo.getId());
            if (tagList != null) vo.setTagList(tagList);
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertResumeTemplate(PortalInterviewResumeTemplate template) {
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        if (template.getStatus() == null) template.setStatus("active");
        if (template.getDownloadCount() == null) template.setDownloadCount(0L);
        if (template.getLikeCount() == null) template.setLikeCount(0L);
        int row = resumeTemplateMapper.insert(template);
        // 同步绑定通用标签（当前 entity 无 tags 字段，可由前端通过 /portal/tag/bind 单独绑定）
        java.util.List<Long> extractedTagIds = new java.util.ArrayList<>();
        java.util.List<String> extractedTagNames = new java.util.ArrayList<>();
        portalTagService.bindTags("interview_resume_template", template.getId(), extractedTagIds, extractedTagNames, "interview_resume_template");
        return row;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateResumeTemplate(PortalInterviewResumeTemplate template) {
        template.setUpdateTime(LocalDateTime.now());
        int row = resumeTemplateMapper.updateById(template);
        java.util.List<Long> extractedTagIds = new java.util.ArrayList<>();
        java.util.List<String> extractedTagNames = new java.util.ArrayList<>();
        portalTagService.bindTags("interview_resume_template", template.getId(), extractedTagIds, extractedTagNames, "interview_resume_template");
        return row;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteResumeTemplateByIds(Long[] ids) {
        return resumeTemplateMapper.deleteBatchIds(Arrays.asList(ids));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleResumeTemplateLike(Long templateId, Long userId) {
        PortalInterviewResumeTemplate template = resumeTemplateMapper.selectById(templateId);
        if (template == null) throw new ServiceException("模板不存在");
        Map<String, Object> result = new HashMap<>();
        // 简化：不按用户存储，每次调用视为点赞 +1
        template.setLikeCount((template.getLikeCount() == null ? 0L : template.getLikeCount()) + 1);
        resumeTemplateMapper.updateById(template);
        result.put("liked", true);
        result.put("likeCount", template.getLikeCount());
        return result;
    }

    // ========================================================================
    // 公司标签管理
    // ========================================================================
    @Override
    public List<InterviewCompanyVO> selectCompanyList(InterviewCompanyQuery query) {
        LambdaQueryWrapper<PortalInterviewCompany> qw = Wrappers.lambdaQuery();
        qw.eq(PortalInterviewCompany::getStatus, query.getStatus() == null ? "active" : query.getStatus());
        if (StringUtils.isNotEmpty(query.getIndustry())) qw.eq(PortalInterviewCompany::getIndustry, query.getIndustry());
        if (StringUtils.isNotEmpty(query.getKeyword())) {
            qw.like(PortalInterviewCompany::getName, query.getKeyword()).or().like(PortalInterviewCompany::getDescription, query.getKeyword());
        }
        qw.orderByAsc(PortalInterviewCompany::getSort).orderByDesc(PortalInterviewCompany::getQuestionCount);
        List<PortalInterviewCompany> list = companyMapper.selectList(qw);
        return list.stream().map(this::toCompanyVO).collect(Collectors.toList());
    }

    @Override
    public InterviewCompanyVO selectCompanyById(Long id) {
        PortalInterviewCompany entity = companyMapper.selectById(id);
        return entity == null ? null : toCompanyVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertCompany(PortalInterviewCompany company) {
        company.setCreateTime(LocalDateTime.now());
        company.setUpdateTime(LocalDateTime.now());
        if (company.getStatus() == null) company.setStatus("active");
        return companyMapper.insert(company);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateCompany(PortalInterviewCompany company) {
        company.setUpdateTime(LocalDateTime.now());
        return companyMapper.updateById(company);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteCompanyByIds(Long[] ids) {
        return companyMapper.deleteBatchIds(Arrays.asList(ids));
    }

    // ========================================================================
    // Entity -> VO 转换辅助
    // ========================================================================
    private InterviewCategoryVO toCategoryVO(PortalInterviewCategory entity) {
        InterviewCategoryVO vo = new InterviewCategoryVO();
        org.springframework.beans.BeanUtils.copyProperties(entity, vo);
        vo.setId(entity.getId());
        return vo;
    }

    private InterviewCompanyVO toCompanyVO(PortalInterviewCompany entity) {
        InterviewCompanyVO vo = new InterviewCompanyVO();
        org.springframework.beans.BeanUtils.copyProperties(entity, vo);
        vo.setId(entity.getId());
        return vo;
    }

    private InterviewQuestionVO toQuestionVO(PortalInterviewQuestion entity, Long currentUserId) {
        InterviewQuestionVO vo = new InterviewQuestionVO();
        org.springframework.beans.BeanUtils.copyProperties(entity, vo);
        vo.setId(entity.getId());
        // 标签字符串切分
        if (StringUtils.isNotEmpty(entity.getTags())) {
            vo.setTags(Arrays.asList(entity.getTags().split(",")).stream().map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList()));
        }
        // 关联公司（查询 portal_interview_question_company + portal_interview_company）
        List<PortalInterviewCompany> companies = companyMapper.selectCompaniesByQuestionId(entity.getId());
        vo.setCompanies(companies.stream().map(this::toCompanyVO).collect(Collectors.toList()));
        // 登录态关联信息
        if (currentUserId != null) {
            vo.setLiked(questionLikeMapper.selectLike(entity.getId(), currentUserId) != null);
            vo.setBookmarked(bookmarkMapper.selectBookmark(entity.getId(), currentUserId) != null);
            PortalInterviewAttempt attempt = attemptMapper.selectAttempt(entity.getId(), currentUserId);
            vo.setAttemptStatus(attempt == null ? "not_attempted" : attempt.getStatus());
        } else {
            vo.setLiked(false);
            vo.setBookmarked(false);
            vo.setAttemptStatus("not_attempted");
        }
        return vo;
    }

    private InterviewExperienceVO toExperienceVO(PortalInterviewExperience entity, Long currentUserId) {
        InterviewExperienceVO vo = new InterviewExperienceVO();
        org.springframework.beans.BeanUtils.copyProperties(entity, vo);
        vo.setId(entity.getId());
        if (StringUtils.isNotEmpty(entity.getTags())) {
            vo.setTags(Arrays.asList(entity.getTags().split(",")).stream().map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList()));
        }
        // 作者信息（这里省略用户名查询，可结合 sys_user 表或 portal_user 表）
        vo.setUserId(entity.getUserId());
        if (currentUserId != null) {
            vo.setLiked(experienceLikeMapper.selectLike(entity.getId(), currentUserId) != null);
        } else {
            vo.setLiked(false);
        }
        return vo;
    }

    private InterviewResumeTemplateVO toResumeTemplateVO(PortalInterviewResumeTemplate entity, Long currentUserId) {
        InterviewResumeTemplateVO vo = new InterviewResumeTemplateVO();
        org.springframework.beans.BeanUtils.copyProperties(entity, vo);
        vo.setId(entity.getId());
        return vo;
    }

    private InterviewCommentVO toCommentVO(PortalInterviewComment entity, Long currentUserId) {
        InterviewCommentVO vo = new InterviewCommentVO();
        org.springframework.beans.BeanUtils.copyProperties(entity, vo);
        vo.setId(entity.getId());
        if (currentUserId != null) {
            vo.setLiked(commentLikeMapper.selectLike(entity.getId(), currentUserId) != null);
        } else {
            vo.setLiked(false);
        }
        // 子评论（一级递归即可）
        List<PortalInterviewComment> children = commentMapper.selectCommentsByParentId(entity.getId());
        if (children != null && !children.isEmpty()) {
            vo.setReplies(children.stream().map(c -> toCommentVO(c, currentUserId)).collect(Collectors.toList()));
        }
        return vo;
    }

    private InterviewSubmissionVO toSubmissionVO(PortalInterviewSubmission entity) {
        InterviewSubmissionVO vo = new InterviewSubmissionVO();
        org.springframework.beans.BeanUtils.copyProperties(entity, vo);
        vo.setId(entity.getId());
        return vo;
    }
}
