package com.moyun.ext.cms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.ext.cms.domain.query.WrongQuestionQuery;
import com.moyun.ext.cms.domain.vo.WrongQuestionVO;

import java.util.List;

/**
 * 错题本 Service 接口（任务 3.3）
 *
 * @author moyun
 */
public interface IWrongQuestionService {

    /**
     * 错题列表（分页，按状态/标签/关键词筛选）
     */
    Page<WrongQuestionVO> listWrongQuestions(Long userId, WrongQuestionQuery query);

    /**
     * 标记题目已掌握（status -> mastered）
     *
     * @return 影响行数
     */
    int markMastered(Long userId, Long questionId);

    /**
     * 今日待复习错题列表
     */
    List<WrongQuestionVO> listTodayReview(Long userId);

    /**
     * 答题失败时自动加入错题本（幂等：已存在则递增 wrong_count）
     * 供答题提交链路调用。
     *
     * @return 错题记录ID
     */
    Long recordWrongQuestion(Long userId, Long questionId, Long attemptId);

    /**
     * 统计某用户错题数（status 为 null 时统计全部）
     */
    Long countWrong(Long userId, String status);

    /**
     * 统计今日待复习错题数
     */
    Long countTodayReview(Long userId);

    /**
     * 查询最近 N 条错题（用于学习中心预览）
     */
    List<WrongQuestionVO> listRecentWrong(Long userId, int limit);
}
