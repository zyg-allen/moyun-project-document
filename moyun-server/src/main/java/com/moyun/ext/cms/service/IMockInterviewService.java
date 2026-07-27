package com.moyun.ext.cms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.core.base.page.PageDomain;
import com.moyun.ext.cms.domain.vo.MockInterviewDetailVO;
import com.moyun.ext.cms.domain.vo.UserProfileSnapshotVO;
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
     * @param personalized 是否基于用户画像（薄弱点 + 岗位必备技能）驱动抽题；为 false 或无画像时降级为随机抽题
     * @return 面试详情（含初始题目列表，answer 为空）
     */
    MockInterviewDetailVO start(Long userId, String position, String scene, boolean personalized);

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
     * <p>结束后会同步刷新用户面试统计与薄弱点画像。</p>
     */
    MockInterviewDetailVO finish(Long interviewId, Long userId);

    /**
     * 我的模拟面试列表（分页，按创建时间倒序）。
     */
    Page<PortalMockInterview> listMy(Long userId, PageDomain query);

    /**
     * 获取当前用户的画像快照（薄弱点 + 岗位必备技能 + 面试统计）。
     * <p>用于前端展示"基于我的画像出题"前置信息。</p>
     *
     * @param userId   门户用户ID
     * @param position 目标岗位（可空）
     * @param scene    面试场景（可空）
     */
    UserProfileSnapshotVO getMyProfile(Long userId, String position, String scene);
}
