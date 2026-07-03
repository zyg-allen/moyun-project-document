package com.moyun.ext.cms.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 学习中心聚合数据 VO（任务 3.1）
 *
 * @author moyun
 */
@Data
public class LearnDashboardVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 当前登录用户ID（未登录为 null） */
    private Long userId;

    /** 用户昵称（用于问候语，未登录为 null） */
    private String nickname;

    /** 是否已登录 */
    private Boolean loggedIn;

    // ==================== 统计卡片 ====================

    /** 累计答题数 */
    private Long totalQuestionCount;

    /** 累计通过数 */
    private Long successCount;

    /** 通过率（0-100） */
    private Integer passRate;

    /** 连续打卡天数 */
    private Integer streakDays;

    /** 错题数（未掌握） */
    private Long wrongCount;

    /** 今日待复习错题数 */
    private Long todayReviewCount;

    /** 进行中的学习计划数 */
    private Long activePlanCount;

    /** 今日完成题数 */
    private Long todayDoneCount;

    // ==================== 今日任务（聚合 active 计划） ====================

    /** 进行中的学习计划列表（含进度） */
    private List<StudyPlanVO> activePlans;

    // ==================== 错题入口 ====================

    /** 最近错题（最多 5 条，用于首页预览） */
    private List<WrongQuestionVO> recentWrongQuestions;
}
