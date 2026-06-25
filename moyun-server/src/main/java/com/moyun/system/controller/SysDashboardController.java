package com.moyun.system.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.system.domain.vo.DashboardVO;
import com.moyun.system.service.ISysDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台运营首页 Controller
 * 提供首页所需的聚合统计数据、待办任务、排行榜、系统动态等接口
 * 所有接口仅登录即可访问（首页为通用页面），数据使用 Redis 缓存5分钟
 *
 * @author moyun
 */
@Tag(name = "后台运营首页", description = "运营首页聚合数据：核心指标、今日统计、趋势、排行榜、待办、动态")
@RestController
@RequestMapping("/system/dashboard")
public class SysDashboardController extends BaseController {

    @Autowired
    private ISysDashboardService dashboardService;

    /**
     * 获取运营首页完整数据（一次请求返回所有区块，减少前端并发）
     */
    @Operation(summary = "获取运营首页完整数据", description = "聚合首页所有展示区块数据，Redis 缓存5分钟")
    @GetMapping("/all")
    public AjaxResult getAll() {
        DashboardVO data = dashboardService.getDashboardData();
        return success(data);
    }

    /**
     * 核心指标卡片（文章/用户/浏览/评论等总量）
     */
    @Operation(summary = "核心指标卡片", description = "文章总数、已发布、待审核、总浏览/点赞/评论数")
    @GetMapping("/metrics")
    public AjaxResult getMetrics() {
        return success(dashboardService.getMetrics().getMetrics());
    }

    /**
     * 今日统计（访客UV/PV、登录人数/次数/成功率、新增文章/用户）
     */
    @Operation(summary = "今日统计", description = "今日访客、登录、新增等数据")
    @GetMapping("/today")
    public AjaxResult getTodayStats() {
        return success(dashboardService.getTodayStats());
    }

    /**
     * 近7天登录趋势（折线图，区分成功/失败）
     */
    @Operation(summary = "近7天登录趋势", description = "登录趋势折线图数据")
    @GetMapping("/login-trend")
    public AjaxResult getLoginTrend() {
        return success(dashboardService.getLoginTrend());
    }

    /**
     * 近7天文章发布趋势（柱状图）
     */
    @Operation(summary = "近7天文章发布趋势", description = "发布趋势柱状图数据")
    @GetMapping("/publish-trend")
    public AjaxResult getPublishTrend() {
        return success(dashboardService.getPublishTrend());
    }

    /**
     * 栏目排行榜（按文章数+浏览量排序，Redis ZSet 维护）
     */
    @Operation(summary = "栏目排行榜", description = "按文章数和浏览量排序的栏目Top10")
    @GetMapping("/category-ranking")
    public AjaxResult getCategoryRanking() {
        return success(dashboardService.getCategoryRanking());
    }

    /**
     * 待办任务列表（待审核文章 + 系统通知）
     */
    @Operation(summary = "待办任务列表", description = "待审核文章与未读系统通知")
    @GetMapping("/todo-tasks")
    public AjaxResult getTodoTasks() {
        return success(dashboardService.getTodoTasks());
    }

    /**
     * 与我相关的任务（已办）
     */
    @Operation(summary = "与我相关任务", description = "当前用户最近处理过的任务")
    @GetMapping("/my-tasks")
    public AjaxResult getMyTasks() {
        return success(dashboardService.getMyTasks());
    }

    /**
     * 系统更新动态（操作日志 + 系统通知合并）
     */
    @Operation(summary = "系统更新动态", description = "最近的操作日志与系统通知动态")
    @GetMapping("/activities")
    public AjaxResult getSystemActivities() {
        return success(dashboardService.getSystemActivities());
    }

    /**
     * 热门文章 Top5（Redis ZSet）
     */
    @Operation(summary = "热门文章Top5", description = "按热度分数排序的热门文章")
    @GetMapping("/hot-articles")
    public AjaxResult getHotArticles() {
        return success(dashboardService.getHotArticles());
    }

    /**
     * 系统配置概览
     */
    @Operation(summary = "系统配置概览", description = "站点信息、版本、缓存、运行状态等")
    @GetMapping("/config-overview")
    public AjaxResult getConfigOverview() {
        return success(dashboardService.getConfigOverview());
    }

    /**
     * 手动刷新 Redis 缓存
     */
    @Operation(summary = "刷新首页缓存", description = "手动清空首页所有Redis缓存，下次请求重新加载")
    @PreAuthorize("@ss.hasPermi('system:dashboard:refresh')")
    @PostMapping("/refresh-cache")
    public AjaxResult refreshCache() {
        dashboardService.refreshCache();
        return success("缓存已刷新");
    }
}
