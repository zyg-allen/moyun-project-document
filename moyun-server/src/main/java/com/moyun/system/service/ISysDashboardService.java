package com.moyun.system.service;

import com.moyun.system.domain.vo.DashboardVO;

/**
 * 后台运营首页 Service 接口
 *
 * @author moyun
 */
public interface ISysDashboardService {

    /**
     * 获取运营首页完整数据（聚合所有区块，Redis 缓存5分钟）
     *
     * @return 首页数据 VO
     */
    DashboardVO getDashboardData();

    /**
     * 获取核心指标卡片（文章/用户/浏览/评论）
     */
    DashboardVO getMetrics();

    /**
     * 获取今日统计（访客/登录/新增）
     */
    DashboardVO.TodayStats getTodayStats();

    /**
     * 获取近7天登录趋势
     */
    java.util.List<DashboardVO.TrendPoint> getLoginTrend();

    /**
     * 获取近7天文章发布趋势
     */
    java.util.List<DashboardVO.TrendPoint> getPublishTrend();

    /**
     * 获取栏目排行榜（Redis ZSet 维护）
     */
    java.util.List<DashboardVO.CategoryRank> getCategoryRanking();

    /**
     * 获取待办任务列表（待审核文章 + 系统通知）
     */
    java.util.List<DashboardVO.TaskItem> getTodoTasks();

    /**
     * 获取与我相关的任务（已办）
     */
    java.util.List<DashboardVO.TaskItem> getMyTasks();

    /**
     * 获取系统更新动态（操作日志 + 系统通知）
     */
    java.util.List<DashboardVO.ActivityItem> getSystemActivities();

    /**
     * 获取热门文章 Top5（Redis ZSet）
     */
    java.util.List<DashboardVO.HotArticle> getHotArticles();

    /**
     * 获取系统配置概览
     */
    DashboardVO.SystemConfigOverview getConfigOverview();

    /**
     * 刷新 Redis 缓存（手动触发）
     */
    void refreshCache();
}
