package com.moyun.system.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 后台运营首页数据 VO
 * 聚合首页所有展示区块的数据
 *
 * @author moyun
 */
@Data
public class DashboardVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 核心指标卡片 */
    private List<MetricCard> metrics;

    /** 今日访客与登录趋势 */
    private TodayStats todayStats;

    /** 近7天登录趋势（折线图） */
    private List<TrendPoint> loginTrend;

    /** 近7天文章发布趋势（柱状图） */
    private List<TrendPoint> publishTrend;

    /** 栏目排行榜（按文章数/浏览量排序） */
    private List<CategoryRank> categoryRanking;

    /** 待办任务列表（待审核文章 + 待处理通知） */
    private List<TaskItem> todoTasks;

    /** 与我相关的任务（已办：已审核/已处理） */
    private List<TaskItem> myTasks;

    /** 系统更新动态（最近操作日志 + 系统通知） */
    private List<ActivityItem> systemActivities;

    /** 热门文章 Top5（Redis ZSet 维护） */
    private List<HotArticle> hotArticles;

    /** 系统配置概览 */
    private SystemConfigOverview configOverview;

    /**
     * 核心指标卡片
     */
    @Data
    public static class MetricCard implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /** 指标键名（articleCount/userCount/viewCount/commentCount） */
        private String key;
        /** 指标名称 */
        private String label;
        /** 指标数值 */
        private Long value;
        /** 图标标识 */
        private String icon;
        /** 趋势百分比（正数上升，负数下降） */
        private Double trend;
        /** 趋势方向：up/down/flat */
        private String trendDirection;
    }

    /**
     * 今日统计
     */
    @Data
    public static class TodayStats implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /** 今日访客数（UV，基于 portal_article_view 去重） */
        private Long todayVisitors;
        /** 今日页面浏览量（PV） */
        private Long todayPageViews;
        /** 今日登录人数 */
        private Long todayLoginUsers;
        /** 今日登录总次数 */
        private Long todayLoginCount;
        /** 今日登录成功率 */
        private Double loginSuccessRate;
        /** 今日新增文章数 */
        private Long todayNewArticles;
        /** 今日新增用户数 */
        private Long todayNewUsers;
    }

    /**
     * 趋势数据点
     */
    @Data
    public static class TrendPoint implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /** 日期（yyyy-MM-dd） */
        private String date;
        /** 数值 */
        private Long value;
        /** 附加维度（如成功/失败） */
        private String label;
    }

    /**
     * 栏目排行
     */
    @Data
    public static class CategoryRank implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /** 栏目ID */
        private Long categoryId;
        /** 栏目名称 */
        private String categoryName;
        /** 文章总数 */
        private Long articleCount;
        /** 总浏览量 */
        private Long totalViews;
        /** 总点赞数 */
        private Long totalLikes;
        /** 排名 */
        private Integer rank;
    }

    /**
     * 任务项（待办/已办通用）
     */
    @Data
    public static class TaskItem implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /** 任务ID（文章ID或通知ID） */
        private Long id;
        /** 任务类型：article_audit/notification/comment_report */
        private String type;
        /** 任务标题 */
        private String title;
        /** 任务描述 */
        private String description;
        /** 状态：pending/processed */
        private String status;
        /** 创建时间 */
        private String createTime;
        /** 提交人 */
        private String submitter;
        /** 优先级：high/medium/low */
        private String priority;
        /** 跳转路径 */
        private String routePath;
    }

    /**
     * 系统动态
     */
    @Data
    public static class ActivityItem implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /** 动态ID */
        private Long id;
        /** 动态类型：operation/notification/system */
        private String type;
        /** 操作模块 */
        private String module;
        /** 操作描述 */
        private String content;
        /** 操作人 */
        private String operator;
        /** 操作时间 */
        private String createTime;
        /** 业务类型（INSERT/UPDATE/DELETE/EXPORT） */
        private String businessType;
    }

    /**
     * 热门文章
     */
    @Data
    public static class HotArticle implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /** 文章ID */
        private Long id;
        /** 标题 */
        private String title;
        /** 作者 */
        private String author;
        /** 浏览量 */
        private Long views;
        /** 点赞数 */
        private Long likes;
        /** 热度分数（Redis ZSet 分数） */
        private Double score;
        /** 排名 */
        private Integer rank;
    }

    /**
     * 系统配置概览
     */
    @Data
    public static class SystemConfigOverview implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /** 站点名 */
        private String siteName;
        /** 站点描述 */
        private String siteDescription;
        /** 当前版本 */
        private String version;
        /** 服务器运行时长（小时） */
        private Long uptimeHours;
        /** 缓存命中率 */
        private Double cacheHitRate;
        /** 数据库表数量 */
        private Long tableCount;
        /** Redis 内存使用（MB） */
        private Double redisMemoryMb;
        /** 配置项列表 */
        private List<Map<String, Object>> configItems;
    }
}
