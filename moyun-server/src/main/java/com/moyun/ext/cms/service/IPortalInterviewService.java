package com.moyun.ext.cms.service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.moyun.core.base.dto.ImportResult;
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

    /**
     * 基于用户画像推荐题目（v5.9 阶段1：题库页"为你推荐"）
     * <p>
     * 三路召回策略（与模拟面试画像驱动抽题一致）：
     * 1. 薄弱点优先：失败率高的标签对应题目
     * 2. 岗位必备技能：命中岗位字典时召回必备技能相关题目
     * 3. 热门兜底：未命中画像或召回不足时，按点赞+提交数补齐
     * <p>
     * 返回的 VO 中 recommendReason / recommendTag 字段标识推荐来源。
     *
     * @param currentUserId 当前用户ID（未登录返回空列表）
     * @param limit         推荐数量上限（默认 6）
     * @return 推荐题目列表（已去重，按推荐来源排序）
     */
    List<InterviewQuestionVO> selectRecommendedQuestions(Long currentUserId, int limit);

    InterviewQuestionDetailVO selectQuestionDetailById(Long id, Long currentUserId);

    /**
     * 查询相邻题目（上一题 / 下一题导航，v12.0 做题页连续练习）
     * <p>
     * 查询条件与列表页保持一致（practiceMode/difficulty/keyword，status=published），
     * 排序与列表一致（sort 升序 + createTime 降序），在结果集中定位当前题目后返回前后题。
     * 当前题不在结果集（未发布/被筛选排除）时，返回就近的可练习题作为导航目标。
     *
     * @param questionId 当前题目ID
     * @param query      筛选条件（与来源列表页一致，通常携带 practiceMode）
     * @return prevId/prevTitle/nextId/nextTitle/currentIndex/total
     */
    Map<String, Object> selectQuestionNeighbor(Long questionId, InterviewQuestionQuery query);

    int insertQuestion(PortalInterviewQuestion question);

    int updateQuestion(PortalInterviewQuestion question);

    int deleteQuestionByIds(Long[] ids);

    /**
     * 查询题目列表（不分页，用于导出）
     *
     * @param query 查询条件
     * @return 题目实体列表
     */
    List<PortalInterviewQuestion> selectQuestionList(InterviewQuestionQuery query);

    /**
     * 批量导入题目（统一返回 ImportResult，含成功/失败统计与失败明细）
     * <p>
     * 实现要点：
     * 1. 逐行校验（必填、长度、枚举合法性），失败行保留原始数据与原因
     * 2. 校验通过的行批量 insert
     * 3. tags 字段每行调用 portalTagService.bindTags 同步标签引用计数
     *
     * @param rows           解析后的行数据（fieldName→value）
     * @param operName       操作人
     * @return 导入结果
     */
    ImportResult importQuestions(List<Map<String, String>> rows, String operName);

    InterviewSubmissionVO submitAnswer(Long questionId, Long userId, Map<String, Object> body);

    /**
     * OJ 判题终态回调：更新做题记录（attempt），首次通过时补发成长事件与答题动态
     * <p>
     * 由判题链路（同步判题 / 异步 Worker）在判题出终态后调用，
     * 使编程题与选择题共享同一套「做题 → 成长事件 → 成长时间线」闭环。
     * 判题记录（submission）与题目统计由判题链路自行落库，本方法不重复处理。
     *
     * @param questionId 题目ID
     * @param userId     做题用户ID
     * @param accepted   判题是否通过（ACCEPTED）
     */
    void finalizeJudgeResult(Long questionId, Long userId, boolean accepted);

    /**
     * 记录题目阅读行为（阅读模式 / 详情页阅读）
     * <p>
     * 阅读成长事件按「同用户 + 同题目 + 同一天」幂等，仅记一次；
     * 若请求附带笔记内容，则同步落一条阅读提交（answerType=reading，不计数入题目提交数）
     * 并记写笔记成长事件，作为题目详情页「精选笔记」的来源。
     *
     * @param questionId 题目ID
     * @param userId     当前用户ID
     * @param body       可选参数：note（笔记内容）、dwellSeconds（阅读停留秒数）
     * @return 阅读结果（readRecorded 本次是否新记录 / noteRecorded 是否记录笔记）
     */
    Map<String, Object> recordQuestionRead(Long questionId, Long userId, Map<String, Object> body);

    Map<String, Object> toggleQuestionLike(Long questionId, Long userId);

    Map<String, Object> toggleQuestionBookmark(Long questionId, Long userId, String note);

    Page<InterviewBookmarkVO> selectBookmarkPage(Page<InterviewBookmarkVO> page, Long userId);

    /**
     * 查询我的答题历史（分页，按提交时间倒序）
     *
     * @param page   分页参数
     * @param userId 当前用户ID
     * @return 提交记录分页
     */
    Page<InterviewSubmissionVO> selectMySubmissionList(Page<InterviewSubmissionVO> page, Long userId);

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

    /**
     * 查询我的面经列表（复用 selectExperiencePage，传入 userId 过滤）
     *
     * @param page  分页参数
     * @param query 查询条件（userId 会被强制覆盖）
     * @param userId 当前用户ID
     * @return 面经分页
     */
    Page<InterviewExperienceVO> selectMyExperienceList(Page<InterviewExperienceVO> page, InterviewExperienceQuery query, Long userId);

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

    /**
     * 下载简历模板：返回模板信息并原子递增下载次数
     *
     * @param id 模板ID
     * @return 模板信息（含下载地址）
     */
    InterviewResumeTemplateVO downloadResumeTemplate(Long id);

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
