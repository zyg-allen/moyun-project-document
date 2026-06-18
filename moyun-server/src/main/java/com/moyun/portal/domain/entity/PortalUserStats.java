package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 门户用户统计聚合表
 *
 * @author moyun
 */
@Data
@TableName("portal_user_stats")
public class PortalUserStats {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 门户用户ID */
    private Long userId;

    /** 发布文章数 */
    private Integer articleCount;

    /** 文章总浏览量 */
    private Long articleViewSum;

    /** 文章总获赞数 */
    private Long articleLikeSum;

    /** 文章总收藏数 */
    private Long articleBookmarkSum;

    /** 累计创作字数 */
    private Long articleWordSum;

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
    private Integer followerCount;

    /** 关注数 */
    private Integer followingCount;

    /** 跨模块评论总数 */
    private Integer commentCount;

    /** 跨模块总获赞 */
    private Long totalLikeReceived;

    /** 连续签到天数 */
    private Integer checkinStreak;

    /** 最后签到日期 */
    private LocalDate lastCheckinDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
