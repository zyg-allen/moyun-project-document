package com.moyun.ext.cms.domain.vo;

import lombok.Data;

/**
 * 门户用户业务统计 VO（后台画像专用）
 *
 * <p>聚合两类统计：
 * <ul>
 *   <li>复用 {@link com.moyun.portal.domain.vo.UserStatsVO} 已有的文章/读书/面试/粉丝/关注/签到等指标</li>
 *   <li>本服务补充：收藏数、书架书籍数、简历数、反馈数（含待处理）、举报数（作为举报人/被举报人）</li>
 * </ul>
 *
 * <p>命名说明：字段直接铺平，便于前端 el-descriptions / 统计卡片直接渲染。
 *
 * @author moyun
 */
@Data
public class PortalUserBusinessStatsVO {

    // ===== 来自 UserStatsVO（复用 IPortalGrowthService.getUserStats） =====
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

    /** 读完的书 */
    private Integer bookFinished;
    /** 创建书单数 */
    private Integer booklistCount;
    /** 发布金句数 */
    private Integer quoteCount;
    /** 累计阅读时长(分钟) */
    private Long readingMinutes;

    /** 解题数 */
    private Integer questionSolved;
    /** 笔记数 */
    private Integer noteCount;
    /** 面经数 */
    private Integer experienceCount;
    /** 笔记被精选数 */
    private Integer noteAdopted;

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

    // ===== 本服务补充 =====
    /** 话题帖数 */
    private Integer topicPosts;
    /** 文章收藏数 */
    private Integer bookmarksArticle;
    /** 书架书籍数 */
    private Integer bookshelfCount;
    /** 简历数（status<>archived） */
    private Integer resumeCount;

    /** 反馈总数 */
    private Integer feedbackCount;
    /** 待处理反馈数 */
    private Integer feedbackPending;

    /** 作为举报人发起的举报数 */
    private Integer reportAsReporter;
    /** 作为被举报人的被举报数 */
    private Integer reportAsTarget;
}
