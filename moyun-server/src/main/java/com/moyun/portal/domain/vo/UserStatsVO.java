package com.moyun.portal.domain.vo;

import lombok.Data;

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
}
