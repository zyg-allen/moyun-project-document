package com.moyun.ext.cms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.core.base.page.PageDomain;
import com.moyun.ext.cms.domain.vo.MockInterviewDetailVO;
import com.moyun.portal.domain.entity.PortalMockInterview;
import com.moyun.portal.domain.entity.PortalMockInterviewQA;

/**
 * AI 模拟面试官 Service 接口（任务 3.10 学习者成长闭环）
 * <p>
 * 简化实现：不依赖外部 LLM，使用规则化评分（关键词匹配 + 答案长度）。
 * 题目来源：portal_interview_question 表。
 *
 * @author moyun
 */
public interface IMockInterviewService {

    /**
     * 开始一次模拟面试：按岗位/场景从题库抽取 5 道题，生成会话与问答记录。
     *
     * @return 面试详情（含初始题目列表，answer 为空）
     */
    MockInterviewDetailVO start(Long userId, String position, String scene);

    /**
     * 查询面试详情（含问答列表）。仅本人可查。
     */
    MockInterviewDetailVO getDetail(Long id, Long userId);

    /**
     * 提交某题答案，返回 AI 规则评分结果（含 score 与 aiFeedback）。
     *
     * @param questionIdx 题目序号（从 0 开始）
     * @param answer      用户回答
     */
    PortalMockInterviewQA answer(Long interviewId, Long userId, Integer questionIdx, String answer);

    /**
     * 结束面试：计算总分（已答题平均分），生成总结。
     */
    MockInterviewDetailVO finish(Long interviewId, Long userId);

    /**
     * 我的模拟面试列表（分页，按创建时间倒序）。
     */
    Page<PortalMockInterview> listMy(Long userId, PageDomain query);
}
