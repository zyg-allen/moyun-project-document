package com.moyun.ext.cms.service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

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
import com.moyun.portal.domain.entity.PortalInterviewCategory;
import com.moyun.portal.domain.entity.PortalInterviewComment;
import com.moyun.portal.domain.entity.PortalInterviewCompany;
import com.moyun.portal.domain.entity.PortalInterviewExperience;
import com.moyun.portal.domain.entity.PortalInterviewQuestion;
import com.moyun.portal.domain.entity.PortalInterviewResumeTemplate;

/**
 * 面试模块 Service 接口
 *
 * @author moyun
 */
public interface IPortalInterviewService {

    // ==================== 首页聚合 ====================
    InterviewHomeDataVO getHomeData(Long currentUserId);

    // ==================== 分类 ====================
    List<InterviewCategoryVO> selectCategoryList();

    InterviewCategoryVO selectCategoryById(Long id);

    int insertCategory(PortalInterviewCategory category);

    int updateCategory(PortalInterviewCategory category);

    int deleteCategoryByIds(Long[] ids);

    // ==================== 题目 ====================
    Page<InterviewQuestionVO> selectQuestionPage(Page<InterviewQuestionVO> page, InterviewQuestionQuery query, Long currentUserId);

    InterviewQuestionDetailVO selectQuestionDetailById(Long id, Long currentUserId);

    int insertQuestion(PortalInterviewQuestion question);

    int updateQuestion(PortalInterviewQuestion question);

    int deleteQuestionByIds(Long[] ids);

    InterviewSubmissionVO submitAnswer(Long questionId, Long userId, Map<String, Object> body);

    Map<String, Object> toggleQuestionLike(Long questionId, Long userId);

    Map<String, Object> toggleQuestionBookmark(Long questionId, Long userId, String note);

    Page<InterviewBookmarkVO> selectBookmarkPage(Page<InterviewBookmarkVO> page, Long userId);

    /**
     * 后台采纳/取消采纳提交笔记为精选
     *
     * @param submissionId 提交记录ID
     * @param isFeatured   true=采纳，false=取消采纳
     * @return 操作结果
     */
    Map<String, Object> adoptSubmission(Long submissionId, boolean isFeatured);

    /**
     * 查询某题目的精选笔记列表
     *
     * @param questionId 题目ID
     * @return 精选提交记录列表
     */
    List<InterviewSubmissionVO> selectFeaturedSubmissions(Long questionId);

    // ==================== 面经 ====================
    Page<InterviewExperienceVO> selectExperiencePage(Page<InterviewExperienceVO> page, InterviewExperienceQuery query, Long currentUserId);

    InterviewExperienceVO selectExperienceDetailById(Long id, Long currentUserId);

    int insertExperience(PortalInterviewExperience experience, Long userId);

    int updateExperience(PortalInterviewExperience experience, Long userId);

    int deleteExperienceById(Long id, Long userId);

    int auditExperience(Long id, String status, String remark);

    int topExperience(Long id, Boolean isTop);

    Map<String, Object> toggleExperienceLike(Long experienceId, Long userId);

    // ==================== 评论 ====================
    Page<InterviewCommentVO> selectCommentPage(Page<InterviewCommentVO> page, InterviewCommentQuery query, Long currentUserId);

    int insertComment(PortalInterviewComment comment, Long userId);

    int deleteCommentById(Long id, Long userId);

    int auditComment(Long id, String status, String remark);

    Map<String, Object> toggleCommentLike(Long commentId, Long userId);

    // ==================== 简历模板 ====================
    Page<InterviewResumeTemplateVO> selectResumeTemplatePage(Page<InterviewResumeTemplateVO> page, InterviewResumeTemplateQuery query, Long currentUserId);

    InterviewResumeTemplateVO selectResumeTemplateById(Long id);

    int insertResumeTemplate(PortalInterviewResumeTemplate template);

    int updateResumeTemplate(PortalInterviewResumeTemplate template);

    int deleteResumeTemplateByIds(Long[] ids);

    Map<String, Object> toggleResumeTemplateLike(Long templateId, Long userId);

    // ==================== 公司标签 ====================
    List<InterviewCompanyVO> selectCompanyList(InterviewCompanyQuery query);

    InterviewCompanyVO selectCompanyById(Long id);

    int insertCompany(PortalInterviewCompany company);

    int updateCompany(PortalInterviewCompany company);

    int deleteCompanyByIds(Long[] ids);
}
