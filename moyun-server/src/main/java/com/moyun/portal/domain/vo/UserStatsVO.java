package com.moyun.portal.domain.vo;

import lombok.Data;

import java.time.LocalDate;

/**
 * 用户统计信息 VO
 *
 * @author moyun
 */
@Data
public class UserStatsVO {

    /** 用户ID */
    private Long userId;

    // ===== 文章模块 =====
    /** 发布文章数 */
    private Integer articles;
    /** 文章总浏览量 */
    private Long views;
    /** 文章总获赞数 */
    private Long likes;
    /** 文章总收藏数 */
    private Long bookmarks;
    /** 累计创作字数 */
    private Long wordCount;

    // ===== 读书空间 =====
    /** 读完的书 */
    private Integer bookFinished;
    /** 创建书单数 */
    private Integer booklistCount;
    /** 发布金句数 */
    private Integer quoteCount;
    /** 累计阅读时长(分钟) */
    private Long readingMinutes;

    // ===== 面试空间 =====
    /** 解题数 */
    private Integer questionSolved;
    /** 笔记数 */
    private Integer noteCount;
    /** 面经数 */
    private Integer experienceCount;
    /** 笔记被精选数 */
    private Integer noteAdopted;

    // ===== 通用 =====
    /** 粉丝数 */
    private Integer followers;
    /** 关注数 */
    private Integer following;
    /** 跨模块评论总数 */
    private Integer comments;
    /** 跨模块总获赞 */
    private Long totalLikes;
    /** 连续签到天数 */
    private Integer checkinStreak;
    /** 最后签到日期（前端据此判断今日是否已签到，刷新后状态不丢失） */
    private LocalDate lastCheckinDate;

    /**
     * 今日是否已签到（**服务端判定**）。
     *
     * <p>为什么必须由后端给：签到日期是服务器本地日期（{@code LocalDate.now()}），
     * 前端原先用 {@code new Date().toISOString().slice(0,10)}（**UTC** 日期）与之比较 ——
     * 在东八区，本地 00:00–08:00 期间 UTC 日期还是"昨天"，导致**已签到却显示未签到**
     * （按钮可点、点了又被后端以"今日已签到"拒绝）。客户端时区不可信，故把判断移到服务端。</p>
     */
    private Boolean checkedInToday;
}
