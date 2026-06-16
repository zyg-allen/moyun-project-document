package com.moyun.ext.cms.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 面试首页聚合数据 VO
 *
 * @author moyun
 */
@Data
public class InterviewHomeDataVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 分类列表 */
    private List<InterviewCategoryVO> categories;

    /** 热门题目 */
    private List<InterviewQuestionVO> hotQuestions;

    /** 热门面经 */
    private List<InterviewExperienceVO> hotExperiences;

    /** 简历模板 */
    private List<InterviewResumeTemplateVO> resumeTemplates;

    /** 热门公司 */
    private List<InterviewCompanyVO> hotCompanies;

    /** 平台题目总数 */
    private Long totalQuestionCount;

    /** 平台提交总数 */
    private Long totalSubmissionCount;
}
