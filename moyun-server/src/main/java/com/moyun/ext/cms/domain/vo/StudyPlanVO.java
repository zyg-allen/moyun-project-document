package com.moyun.ext.cms.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学习计划 VO（含进度统计）
 *
 * @author moyun
 */
@Data
public class StudyPlanVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long userId;

    private String title;

    /** 计划类型 daily_question/weekly_reading/custom */
    private String planType;

    private Integer targetCount;

    private String targetCategory;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /** 状态 active/completed/abandoned */
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    // ==================== 进度统计 ====================

    /** 已完成总数（来自 plan_log 汇总） */
    private Integer doneCount;

    /** 今日完成数 */
    private Integer todayDoneCount;

    /** 进度百分比 0-100 */
    private Integer progressPercent;

    /** 连续打卡天数 */
    private Integer streakDays;
}
