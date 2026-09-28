package com.moyun.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.core.config.redis.RedisCache;
import com.moyun.portal.mapper.PortalArticleMapper;
import com.moyun.portal.mapper.PortalArticleViewMapper;
import com.moyun.system.domain.entity.SysNotification;
import com.moyun.system.domain.entity.SysOperLog;
import com.moyun.system.domain.query.OperLogQuery;
import com.moyun.system.domain.vo.DashboardVO;
import com.moyun.system.mapper.SysLogininforMapper;
import com.moyun.system.mapper.SysNotificationMapper;
import com.moyun.system.mapper.SysOperLogMapper;
import com.moyun.system.service.ISysDashboardService;
import com.moyun.util.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 后台运营首页 Service 实现
 * 使用 Redis 缓存聚合数据（5分钟），排行榜使用 ZSet 数据结构
 *
 * @author moyun
 */
@Service
public class SysDashboardServiceImpl implements ISysDashboardService {

    private static final Logger log = LoggerFactory.getLogger(SysDashboardServiceImpl.class);

    /** Redis 缓存键前缀 */
    private static final String CACHE_PREFIX = "dashboard:";
    private static final String CACHE_KEY_FULL = CACHE_PREFIX + "full";
    private static final String CACHE_KEY_METRICS = CACHE_PREFIX + "metrics";
    private static final String CACHE_KEY_TODAY = CACHE_PREFIX + "today";
    private static final String CACHE_KEY_LOGIN_TREND = CACHE_PREFIX + "loginTrend";
    private static final String CACHE_KEY_PUBLISH_TREND = CACHE_PREFIX + "publishTrend";
    private static final String CACHE_KEY_CATEGORY_RANK = CACHE_PREFIX + "categoryRanking";
    private static final String CACHE_KEY_TODO = CACHE_PREFIX + "todoTasks";
    private static final String CACHE_KEY_MY_TASKS = CACHE_PREFIX + "myTasks";
    private static final String CACHE_KEY_ACTIVITIES = CACHE_PREFIX + "activities";
    private static final String CACHE_KEY_CONFIG = CACHE_PREFIX + "config";

    /** Redis ZSet 键：热门文章排行榜 */
    private static final String ZSET_KEY_HOT_ARTICLES = "dashboard:zset:hotArticles";
    /** Redis ZSet 键：栏目浏览量排行榜 */
    private static final String ZSET_KEY_CATEGORY_VIEWS = "dashboard:zset:categoryViews";

    /** 缓存有效期5分钟（秒） */
    private static final long CACHE_TTL_SECONDS = 300;

    /** 排行榜 Top N */
    private static final int RANK_LIMIT = 10;
    private static final int HOT_ARTICLE_LIMIT = 5;

    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private PortalArticleMapper articleMapper;

    @Autowired
    private PortalArticleViewMapper articleViewMapper;

    @Autowired
    private SysLogininforMapper logininforMapper;

    @Autowired
    private SysOperLogMapper operLogMapper;

    @Autowired
    private SysNotificationMapper notificationMapper;

    @Autowired
    private com.moyun.portal.mapper.PortalUserMapper portalUserMapper;

    @Autowired
    private com.moyun.system.service.ISysConfigService configService;

    @Autowired
    private com.moyun.common.config.RuoYiConfig ruoYiConfig;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private com.moyun.system.service.IAuditTaskService auditTaskService;

    @Autowired
    private com.moyun.system.mapper.SysDashboardStatsMapper dashboardStatsMapper;

    @Override
    public DashboardVO getDashboardData() {
        // 尝试命中完整缓存
        // 注意：旧版本曾将 List.subList() 视图直接序列化进 Redis，反序列化会抛
        // SerializationException（ArrayList$SubList 无默认构造器）。这里做防御性
        // 读取——若缓存数据损坏则删除脏 key 并回源重建，避免线上持续报错。
        DashboardVO cached = readCacheSafely(CACHE_KEY_FULL);
        if (cached != null) {
            log.debug("[Dashboard] 命中完整缓存");
            return cached;
        }

        log.debug("[Dashboard] 缓存未命中，开始聚合数据");
        DashboardVO vo = new DashboardVO();
        vo.setMetrics(buildMetrics());
        vo.setTodayStats(buildTodayStats());
        vo.setLoginTrend(buildLoginTrend());
        vo.setPublishTrend(buildPublishTrend());
        vo.setCategoryRanking(buildCategoryRanking());
        vo.setTodoTasks(buildTodoTasks());
        vo.setMyTasks(buildMyTasks());
        vo.setSystemActivities(buildSystemActivities());
        vo.setHotArticles(buildHotArticles());
        vo.setConfigOverview(buildConfigOverview());
        // 分平台运营概览（v13.25 起）：平台定位 + 端/模块统计 + 运营警报
        vo.setPlatformIdentity(buildPlatformIdentity());
        vo.setPlatformStats(buildPlatformStats());
        vo.setAlerts(buildAlerts());

        // 写入缓存：若待办/已办为空（可能首次启动或审核任务索引未回填），
        // 仅短缓存 20s，避免空列表被缓存 5 分钟导致"实际有数据但首页待办空"的感知；
        // 数据齐全时仍缓存 5 分钟以保护数据库。
        boolean hasAnyTask = (vo.getTodoTasks() != null && !vo.getTodoTasks().isEmpty())
                || (vo.getMyTasks() != null && !vo.getMyTasks().isEmpty());
        long ttl = hasAnyTask ? CACHE_TTL_SECONDS : 20L;
        redisCache.setCacheObject(CACHE_KEY_FULL, vo, (int) ttl, TimeUnit.SECONDS);
        return vo;
    }

    @Override
    public DashboardVO getMetrics() {
        DashboardVO cached = readCacheSafely(CACHE_KEY_METRICS);
        if (cached != null) return cached;
        DashboardVO vo = new DashboardVO();
        vo.setMetrics(buildMetrics());
        redisCache.setCacheObject(CACHE_KEY_METRICS, vo, (int) CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        return vo;
    }

    @Override
    public DashboardVO.TodayStats getTodayStats() {
        DashboardVO.TodayStats cached = readCacheSafely(CACHE_KEY_TODAY);
        if (cached != null) return cached;
        DashboardVO.TodayStats stats = buildTodayStats();
        redisCache.setCacheObject(CACHE_KEY_TODAY, stats, (int) CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        return stats;
    }

    @Override
    public List<DashboardVO.TrendPoint> getLoginTrend() {
        List<DashboardVO.TrendPoint> cached = readCacheSafely(CACHE_KEY_LOGIN_TREND);
        if (cached != null) return cached;
        List<DashboardVO.TrendPoint> trend = buildLoginTrend();
        redisCache.setCacheObject(CACHE_KEY_LOGIN_TREND, trend, (int) CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        return trend;
    }

    @Override
    public List<DashboardVO.TrendPoint> getPublishTrend() {
        List<DashboardVO.TrendPoint> cached = readCacheSafely(CACHE_KEY_PUBLISH_TREND);
        if (cached != null) return cached;
        List<DashboardVO.TrendPoint> trend = buildPublishTrend();
        redisCache.setCacheObject(CACHE_KEY_PUBLISH_TREND, trend, (int) CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        return trend;
    }

    @Override
    public List<DashboardVO.CategoryRank> getCategoryRanking() {
        List<DashboardVO.CategoryRank> cached = readCacheSafely(CACHE_KEY_CATEGORY_RANK);
        if (cached != null) return cached;
        List<DashboardVO.CategoryRank> ranking = buildCategoryRanking();
        redisCache.setCacheObject(CACHE_KEY_CATEGORY_RANK, ranking, (int) CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        return ranking;
    }

    @Override
    public List<DashboardVO.TaskItem> getTodoTasks() {
        List<DashboardVO.TaskItem> cached = readCacheSafely(CACHE_KEY_TODO);
        if (cached != null) return cached;
        List<DashboardVO.TaskItem> tasks = buildTodoTasks();
        // 空列表不缓存（兼容历史脏数据：sys_audit_task 索引行缺失但业务表有 pending 时，
        // 补数/回填后下次即可立即命中，不会被空列表缓存挡 5 分钟）
        if (tasks != null && !tasks.isEmpty()) {
            redisCache.setCacheObject(CACHE_KEY_TODO, tasks, (int) CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        }
        return tasks;
    }

    @Override
    public List<DashboardVO.TaskItem> getMyTasks() {
        // "与我相关（已办）" 是按当前用户个性化的数据，缓存 key 必须按用户ID区分
        Long currentUserId = SecurityUtils.getUserId();
        String cacheKey = CACHE_KEY_MY_TASKS + (currentUserId != null ? ":" + currentUserId : "");
        List<DashboardVO.TaskItem> cached = readCacheSafely(cacheKey);
        if (cached != null) return cached;
        List<DashboardVO.TaskItem> tasks = buildMyTasks();
        // 空列表不缓存
        if (tasks != null && !tasks.isEmpty()) {
            redisCache.setCacheObject(cacheKey, tasks, (int) CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        }
        return tasks;
    }

    @Override
    public List<DashboardVO.ActivityItem> getSystemActivities() {
        List<DashboardVO.ActivityItem> cached = readCacheSafely(CACHE_KEY_ACTIVITIES);
        if (cached != null) return cached;
        List<DashboardVO.ActivityItem> activities = buildSystemActivities();
        redisCache.setCacheObject(CACHE_KEY_ACTIVITIES, activities, (int) CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        return activities;
    }

    @Override
    public List<DashboardVO.HotArticle> getHotArticles() {
        return buildHotArticles();
    }

    @Override
    public DashboardVO.SystemConfigOverview getConfigOverview() {
        DashboardVO.SystemConfigOverview cached = readCacheSafely(CACHE_KEY_CONFIG);
        if (cached != null) return cached;
        DashboardVO.SystemConfigOverview overview = buildConfigOverview();
        redisCache.setCacheObject(CACHE_KEY_CONFIG, overview, (int) CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        return overview;
    }

    @Override
    public void refreshCache() {
        Set<String> keys = new HashSet<>(Arrays.asList(
                CACHE_KEY_FULL, CACHE_KEY_METRICS, CACHE_KEY_TODAY,
                CACHE_KEY_LOGIN_TREND, CACHE_KEY_PUBLISH_TREND,
                CACHE_KEY_CATEGORY_RANK, CACHE_KEY_TODO, CACHE_KEY_MY_TASKS,
                CACHE_KEY_ACTIVITIES, CACHE_KEY_CONFIG
        ));
        redisCache.deleteObject(keys);
        // 同步清理按用户ID区分的 myTasks 缓存（如 dashboard:myTasks:1）
        Set<String> myTasksUserKeys = redisCache.redisTemplate.keys(CACHE_KEY_MY_TASKS + ":*");
        if (myTasksUserKeys != null && !myTasksUserKeys.isEmpty()) {
            redisCache.deleteObject(myTasksUserKeys);
        }
        // 同步清理排行榜 ZSet，否则下次取热门文章/栏目排行仍读旧数据
        redisCache.redisTemplate.delete(ZSET_KEY_HOT_ARTICLES);
        redisCache.redisTemplate.delete(ZSET_KEY_CATEGORY_VIEWS);
        log.info("[Dashboard] 缓存已手动刷新（含 ZSet 排行榜）");
    }

    /**
     * 防御性读取缓存：遇到反序列化异常（如历史脏数据含 ArrayList$SubList 视图）
     * 时删除脏 key 并返回 null，触发回源重建，避免接口持续 500。
     */
    private <T> T readCacheSafely(String key) {
        try {
            return redisCache.getCacheObject(key);
        } catch (org.springframework.data.redis.serializer.SerializationException e) {
            log.warn("[Dashboard] 缓存 key={} 反序列化失败，删除脏数据并回源：{}", key, e.getMessage());
            try {
                redisCache.deleteObject(key);
            } catch (Exception ignore) {
                // 删除失败不影响主流程，等待 TTL 自动过期
            }
            return null;
        }
    }

    // ========== 数据构建方法 ==========

    /**
     * 构建核心指标卡片
     */
    private List<DashboardVO.MetricCard> buildMetrics() {
        List<DashboardVO.MetricCard> cards = new ArrayList<>();
        try {
            Map<String, Object> stats = articleMapper.selectArticleMetrics();
            long totalArticles = toLong(stats.get("totalArticles"));
            long publishedArticles = toLong(stats.get("publishedArticles"));
            long totalViews = toLong(stats.get("totalViews"));
            long totalLikes = toLong(stats.get("totalLikes"));
            long totalComments = toLong(stats.get("totalComments"));

            // 口径统一：待审核文章从统一审核任务表统计（taskType=article 且 status=pending），
            // 与审核中心待办列表同源，避免业务表存在孤儿 pending 文章（无对应审核任务）导致两处数字不一致
            long pendingArticles = auditTaskService.countPendingByType().getOrDefault("article", 0L);

            cards.add(buildCard("articleCount", "文章总数", totalArticles, "Document", null));
            cards.add(buildCard("publishedArticles", "已发布文章", publishedArticles, "CircleCheck", null));
            cards.add(buildCard("pendingArticles", "待审核文章", pendingArticles, "Clock", null));
            cards.add(buildCard("totalViews", "总浏览量", totalViews, "View", null));
            cards.add(buildCard("totalLikes", "总点赞数", totalLikes, "Star", null));
            cards.add(buildCard("totalComments", "总评论数", totalComments, "ChatDotRound", null));
        } catch (Exception e) {
            log.error("[Dashboard] 构建核心指标失败", e);
        }
        return cards;
    }

    /**
     * 构建今日统计
     */
    private DashboardVO.TodayStats buildTodayStats() {
        DashboardVO.TodayStats stats = new DashboardVO.TodayStats();
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        try {
            stats.setTodayVisitors(articleViewMapper.countTodayVisitors(todayStart));
            stats.setTodayPageViews(articleViewMapper.countTodayPageViews(todayStart));
            stats.setTodayLoginUsers(logininforMapper.countTodayLoginUsers(todayStart));
            stats.setTodayLoginCount(logininforMapper.countTodayLoginCount(todayStart));
            // 按前后台来源拆分登录人数（用于卡片细维度展示）
            stats.setTodayPortalLoginUsers(logininforMapper.countTodayLoginUsersByType(todayStart, "portal"));
            stats.setTodaySysLoginUsers(logininforMapper.countTodayLoginUsersByType(todayStart, "sys"));
            long successCount = logininforMapper.countTodayLoginSuccess(todayStart);
            long totalCount = stats.getTodayLoginCount();
            stats.setLoginSuccessRate(totalCount > 0 ? (successCount * 100.0 / totalCount) : 100.0);

            // 今日新增文章数（按 create_time 过滤，口径与"今日新增文章"卡片名称一致，含所有状态）
            stats.setTodayNewArticles(articleMapper.countTodayNewArticles(todayStart));
            // 今日新增用户数：查询 PortalUser 今日注册量
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.moyun.portal.domain.entity.PortalUser> userWrapper =
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            userWrapper.ge(com.moyun.portal.domain.entity.PortalUser::getCreateTime, todayStart);
            stats.setTodayNewUsers(toLong(portalUserMapper.selectCount(userWrapper)));
        } catch (Exception e) {
            log.error("[Dashboard] 构建今日统计失败", e);
        }
        return stats;
    }

    /**
     * 构建近7天登录趋势
     */
    private List<DashboardVO.TrendPoint> buildLoginTrend() {
        List<DashboardVO.TrendPoint> result = new ArrayList<>();
        LocalDateTime startTime = LocalDateTime.now().minusDays(6).withHour(0).withMinute(0).withSecond(0).withNano(0);
        try {
            List<Map<String, Object>> raw = logininforMapper.selectDailyLoginTrend(startTime);
            // 按日期分组，合并 success/fail（聚合前后台来源，保持前端 label=success/fail 不变）
            // label 维度由 SQL 输出为 portal_success/portal_fail/sys_success/sys_fail，这里统一归到 success/fail
            Map<String, long[]> grouped = new TreeMap<>();
            for (Map<String, Object> row : raw) {
                String date = String.valueOf(row.get("date"));
                long value = toLong(row.get("value"));
                String label = String.valueOf(row.get("label"));
                long[] arr = grouped.computeIfAbsent(date, k -> new long[2]);
                if (label != null && label.endsWith("success")) {
                    arr[0] += value;
                } else {
                    arr[1] += value;
                }
            }
            // 填充缺失日期
            for (int i = 6; i >= 0; i--) {
                String date = LocalDateTime.now().minusDays(i).toLocalDate().toString();
                long[] arr = grouped.getOrDefault(date, new long[2]);
                DashboardVO.TrendPoint success = new DashboardVO.TrendPoint();
                success.setDate(date);
                success.setValue(arr[0]);
                success.setLabel("success");
                result.add(success);
                DashboardVO.TrendPoint fail = new DashboardVO.TrendPoint();
                fail.setDate(date);
                fail.setValue(arr[1]);
                fail.setLabel("fail");
                result.add(fail);
            }
        } catch (Exception e) {
            log.error("[Dashboard] 构建登录趋势失败", e);
        }
        return result;
    }

    /**
     * 构建近7天文章发布趋势
     */
    private List<DashboardVO.TrendPoint> buildPublishTrend() {
        List<DashboardVO.TrendPoint> result = new ArrayList<>();
        LocalDateTime startTime = LocalDateTime.now().minusDays(6).withHour(0).withMinute(0).withSecond(0).withNano(0);
        try {
            List<Map<String, Object>> raw = articleMapper.selectDailyPublishTrend(startTime);
            Map<String, Long> dateMap = new TreeMap<>();
            for (Map<String, Object> row : raw) {
                dateMap.put(String.valueOf(row.get("date")), toLong(row.get("value")));
            }
            // 填充缺失日期
            for (int i = 6; i >= 0; i--) {
                String date = LocalDateTime.now().minusDays(i).toLocalDate().toString();
                DashboardVO.TrendPoint point = new DashboardVO.TrendPoint();
                point.setDate(date);
                point.setValue(dateMap.getOrDefault(date, 0L));
                point.setLabel("publish");
                result.add(point);
            }
        } catch (Exception e) {
            log.error("[Dashboard] 构建发布趋势失败", e);
        }
        return result;
    }

    /**
     * 构建栏目排行榜（使用 Redis ZSet 维护浏览量排名）
     */
    private List<DashboardVO.CategoryRank> buildCategoryRanking() {
        List<DashboardVO.CategoryRank> result = new ArrayList<>();
        try {
            List<Map<String, Object>> raw = articleMapper.selectCategoryRanking(RANK_LIMIT);
            int rank = 1;
            for (Map<String, Object> row : raw) {
                DashboardVO.CategoryRank item = new DashboardVO.CategoryRank();
                item.setCategoryId(toLong(row.get("categoryId")));
                item.setCategoryName(String.valueOf(row.get("categoryName")));
                item.setArticleCount(toLong(row.get("articleCount")));
                item.setTotalViews(toLong(row.get("totalViews")));
                item.setTotalLikes(toLong(row.get("totalLikes")));
                item.setRank(rank++);
                result.add(item);

                // 同步到 Redis ZSet（以浏览量为分数）
                try {
                    redisCache.redisTemplate.opsForZSet().add(
                            ZSET_KEY_CATEGORY_VIEWS,
                            String.valueOf(row.get("categoryName")),
                            item.getTotalViews()
                    );
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            log.error("[Dashboard] 构建栏目排行失败", e);
        }
        return result;
    }

    /**
     * 构建待办任务列表
     * <p>统一从 sys_audit_task 聚合（status=pending），覆盖文章/面经/专栏/话题/
     * 创作者认证/反馈/举报/面经评论等全部业务类型，替代分散的业务表查询。
     * <p>点击跳转审核中心对应 Tab 并打开详情（routePath=/cms/audit-center?taskId=xx）。
     */
    private List<DashboardVO.TaskItem> buildTodoTasks() {
        List<DashboardVO.TaskItem> tasks = new ArrayList<>();
        try {
            List<com.moyun.system.domain.vo.AuditTaskVO> list = auditTaskService.listTodoSummary(8);
            if (list == null || list.isEmpty()) {
                return tasks;
            }
            for (com.moyun.system.domain.vo.AuditTaskVO vo : list) {
                tasks.add(toTaskItem(vo, true));
            }
        } catch (Exception e) {
            log.error("[Dashboard] 构建待办任务失败", e);
        }
        return tasks;
    }

    /**
     * 构建与我相关任务（已办）
     * <p>统一从 sys_audit_task 聚合（auditor_id=当前用户，status in approved/rejected），
     * 替代分散的业务表按 handler/auditor 查询。
     * <p>点击跳转审核中心对应 Tab 并打开详情（routePath=/cms/audit-center?taskId=xx）。
     */
    private List<DashboardVO.TaskItem> buildMyTasks() {
        List<DashboardVO.TaskItem> tasks = new ArrayList<>();
        try {
            Long currentUserId = SecurityUtils.getUserId();
            if (currentUserId == null) {
                return tasks;
            }
            List<com.moyun.system.domain.vo.AuditTaskVO> list = auditTaskService.listMyHandledSummary(8);
            if (list == null || list.isEmpty()) {
                return tasks;
            }
            for (com.moyun.system.domain.vo.AuditTaskVO vo : list) {
                tasks.add(toTaskItem(vo, false));
            }
        } catch (Exception e) {
            log.error("[Dashboard] 构建已办任务失败", e);
        }
        return tasks;
    }

    /**
     * 将 AuditTaskVO 转换为首页 TaskItem。
     * <p>待办跳审核中心打开详情（routePath=/cms/audit-center?taskId=xx）；
     * 已办同样跳审核中心（查看审核记录）。审核中心详情弹窗的「查看原帖」按钮再用
     * AuditTaskVO.routePath 跳转到各业务管理页（如 /cms/article）。
     *
     * @param vo    审核任务 VO
     * @param todo  true=待办（按提交时间）；false=已办（按处理时间）
     */
    private DashboardVO.TaskItem toTaskItem(com.moyun.system.domain.vo.AuditTaskVO vo, boolean todo) {
        DashboardVO.TaskItem item = new DashboardVO.TaskItem();
        item.setId(vo.getId());
        item.setType(vo.getTaskType());
        item.setTitle(vo.getTitle() != null ? vo.getTitle() : vo.getTaskTypeLabel());
        item.setDescription(vo.getTaskTypeLabel() + (todo ? " 待处理" : ("：" + vo.getStatusLabel())));
        item.setStatus(vo.getStatus());
        java.time.LocalDateTime time = todo ? vo.getSubmitTime() : vo.getAuditTime();
        if (time == null) {
            time = vo.getSubmitTime();
        }
        item.setCreateTime(time != null ? time.format(DATETIME_FMT) : "");
        item.setSubmitter(vo.getSubmitterName() != null ? vo.getSubmitterName() : "-");
        item.setPriority(vo.getPriority() != null ? vo.getPriority() : "medium");
        // 首页待办/已办点击 → 审核中心对应 Tab + 打开详情
        // routePath 取 vo.routePath（AuditTaskType.defaultRoutePath，形如 /portal/audit-center），
        // 追加 taskId + tab（taskType）让审核中心 handleRouteQuery 自动打开详情并按类型过滤
        String baseRoute = vo.getRoutePath() != null ? vo.getRoutePath() : "/portal/audit-center";
        // 兼容旧数据：若 routePath 仍为 /cms/audit-center，统一替换为 /portal/audit-center
        baseRoute = baseRoute.replaceFirst("^/cms/audit-center", "/portal/audit-center");
        item.setRoutePath(baseRoute + "?taskId=" + vo.getId() + "&tab=" + vo.getTaskType());
        return item;
    }

    /**
     * 构建系统动态（操作日志 + 系统通知合并）
     */
    private List<DashboardVO.ActivityItem> buildSystemActivities() {
        List<DashboardVO.ActivityItem> activities = new ArrayList<>();
        try {
            // 1. 最近操作日志（Top 8）
            List<SysOperLog> operLogs = operLogMapper.selectOperLogList(new OperLogQuery());
            if (operLogs != null) {
                List<SysOperLog> recent = operLogs.stream()
                        .sorted(Comparator.comparing(SysOperLog::getOperTime).reversed())
                        .limit(8)
                        .collect(Collectors.toList());
                for (SysOperLog oper : recent) {
                    DashboardVO.ActivityItem item = new DashboardVO.ActivityItem();
                    item.setId(oper.getOperId());
                    item.setType("operation");
                    item.setModule(oper.getTitle());
                    item.setContent(buildOperDesc(oper));
                    item.setOperator(oper.getOperName());
                    item.setCreateTime(oper.getOperTime() != null ? oper.getOperTime().format(DATETIME_FMT) : "");
                    item.setBusinessType(businessTypeName(oper.getBusinessType()));
                    activities.add(item);
                }
            }
        } catch (Exception e) {
            log.error("[Dashboard] 构建操作日志动态失败", e);
        }
        try {
            // 2. 最近系统通知（广播，Top 4）
            SysNotification queryNotif = new SysNotification();
            queryNotif.setScope("all");
            queryNotif.setUserType("sys");
            List<SysNotification> notifs = notificationMapper.selectNotificationList(queryNotif);
            if (notifs != null) {
                List<SysNotification> recent = notifs.stream()
                        .sorted(Comparator.comparing(SysNotification::getCreateTime).reversed())
                        .limit(4)
                        .collect(Collectors.toList());
                for (SysNotification n : recent) {
                    DashboardVO.ActivityItem item = new DashboardVO.ActivityItem();
                    item.setId(n.getId());
                    item.setType("notification");
                    item.setModule("系统通知");
                    item.setContent(n.getTitle() != null ? n.getTitle() : "");
                    item.setOperator(n.getCreateBy() != null ? n.getCreateBy() : "系统");
                    item.setCreateTime(n.getCreateTime() != null ? n.getCreateTime().format(DATETIME_FMT) : "");
                    item.setBusinessType("NOTIFICATION");
                    activities.add(item);
                }
            }
        } catch (Exception e) {
            log.error("[Dashboard] 构建通知动态失败", e);
        }
        try {
            // 3. 最近门户动态：已发布文章（按 create_time 倒序取 5 条）
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.moyun.portal.domain.entity.PortalArticle> artWrapper =
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            artWrapper.eq(com.moyun.portal.domain.entity.PortalArticle::getStatus, "published")
                    .orderByDesc(com.moyun.portal.domain.entity.PortalArticle::getCreateTime)
                    .last("LIMIT 5");
            List<com.moyun.portal.domain.entity.PortalArticle> recentArticles = articleMapper.selectList(artWrapper);
            if (recentArticles != null) {
                for (com.moyun.portal.domain.entity.PortalArticle a : recentArticles) {
                    DashboardVO.ActivityItem item = new DashboardVO.ActivityItem();
                    item.setId(a.getId());
                    item.setType("article_publish");
                    item.setModule("文章发布");
                    item.setContent(a.getTitle() != null ? a.getTitle() : "");
                    item.setOperator(a.getCreateBy() != null ? a.getCreateBy() : "门户作者");
                    item.setCreateTime(a.getCreateTime() != null ? a.getCreateTime().format(DATETIME_FMT) : "");
                    item.setBusinessType("PUBLISH");
                    activities.add(item);
                }
            }
        } catch (Exception e) {
            log.error("[Dashboard] 构建文章发布动态失败", e);
        }
        try {
            // 4. 最近门户动态：新用户注册（按 create_time 倒序取 3 条）
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.moyun.portal.domain.entity.PortalUser> userWrapper =
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            userWrapper.orderByDesc(com.moyun.portal.domain.entity.PortalUser::getCreateTime)
                    .last("LIMIT 3");
            List<com.moyun.portal.domain.entity.PortalUser> recentUsers = portalUserMapper.selectList(userWrapper);
            if (recentUsers != null) {
                for (com.moyun.portal.domain.entity.PortalUser u : recentUsers) {
                    DashboardVO.ActivityItem item = new DashboardVO.ActivityItem();
                    item.setId(u.getId());
                    item.setType("user_register");
                    item.setModule("用户注册");
                    item.setContent((u.getNickname() != null ? u.getNickname() : u.getUsername()) + " 加入了平台");
                    item.setOperator(u.getUsername() != null ? u.getUsername() : "新用户");
                    item.setCreateTime(u.getCreateTime() != null ? u.getCreateTime().format(DATETIME_FMT) : "");
                    item.setBusinessType("REGISTER");
                    activities.add(item);
                }
            }
        } catch (Exception e) {
            log.error("[Dashboard] 构建用户注册动态失败", e);
        }

        // 合并后按时间倒序，统一限制取 Top 20，避免首页动态过长
        activities.sort(Comparator.comparing(DashboardVO.ActivityItem::getCreateTime).reversed());
        if (activities.size() > 20) {
            activities = new ArrayList<>(activities.subList(0, 20));
        }
        return activities;
    }

    /**
     * 将 SysOperLog.businessType 数字转为前端期望的枚举名
     * 对应 com.moyun.common.enums.BusinessType：OTHER=0, INSERT=1, UPDATE=2, DELETE=3, GRANT=4, EXPORT=5, IMPORT=6
     */
    private String businessTypeName(Integer businessType) {
        if (businessType == null) return "OTHER";
        switch (businessType) {
            case 1: return "INSERT";
            case 2: return "UPDATE";
            case 3: return "DELETE";
            case 4: return "GRANT";
            case 5: return "EXPORT";
            case 6: return "IMPORT";
            default: return "OTHER";
        }
    }

    /**
     * 构建热门文章 Top5（Redis ZSet）
     */
    private List<DashboardVO.HotArticle> buildHotArticles() {
        List<DashboardVO.HotArticle> result = new ArrayList<>();
        try {
            // 先从 ZSet 取 Top N
            Set<Object> zsetResult = redisCache.redisTemplate.opsForZSet()
                    .reverseRangeByScore(ZSET_KEY_HOT_ARTICLES, 0, Double.MAX_VALUE, 0, HOT_ARTICLE_LIMIT);
            boolean zsetEmpty = zsetResult == null || zsetResult.isEmpty();

            if (zsetEmpty) {
                // ZSet 为空，从 DB 加载并初始化
                List<Map<String, Object>> hot = articleMapper.selectHotArticlesForRanking(HOT_ARTICLE_LIMIT);
                int rank = 1;
                for (Map<String, Object> row : hot) {
                    DashboardVO.HotArticle item = new DashboardVO.HotArticle();
                    item.setId(toLong(row.get("id")));
                    item.setTitle(String.valueOf(row.get("title")));
                    item.setAuthor(row.get("author") != null ? String.valueOf(row.get("author")) : "");
                    item.setViews(toLong(row.get("views")));
                    item.setLikes(toLong(row.get("likes")));
                    item.setScore(toDouble(row.get("score")));
                    item.setRank(rank++);
                    result.add(item);

                    // 写入 ZSet
                    try {
                        redisCache.redisTemplate.opsForZSet().add(
                                ZSET_KEY_HOT_ARTICLES,
                                String.valueOf(row.get("id")),
                                item.getScore()
                        );
                    } catch (Exception ignored) {
                    }
                }
            } else {
                // 从 ZSet 还原（需补全标题/作者等字段）
                int rank = 1;
                for (Object idObj : zsetResult) {
                    Long id = Long.parseLong(String.valueOf(idObj));
                    Double score = redisCache.redisTemplate.opsForZSet().score(ZSET_KEY_HOT_ARTICLES, idObj);
                    // 查文章详情补全信息
                    com.moyun.portal.domain.entity.PortalArticle article = articleMapper.selectPortalArticleById(id);
                    if (article == null) {
                        // 文章已删除，跳过
                        continue;
                    }
                    DashboardVO.HotArticle item = new DashboardVO.HotArticle();
                    item.setId(id);
                    item.setTitle(article.getTitle());
                    item.setViews(article.getViews() != null ? article.getViews() : 0L);
                    item.setLikes(article.getLikes() != null ? article.getLikes() : 0L);
                    item.setScore(score != null ? score : 0.0);
                    item.setRank(rank++);
                    // 查作者名
                    try {
                        com.moyun.portal.domain.entity.PortalUser author = portalUserMapper.selectById(article.getAuthorId());
                        item.setAuthor(author != null ? (author.getNickname() != null ? author.getNickname() : author.getUsername()) : "");
                    } catch (Exception ignored) {
                        item.setAuthor("");
                    }
                    result.add(item);
                }
            }
        } catch (Exception e) {
            log.error("[Dashboard] 构建热门文章失败", e);
        }
        return result;
    }

    /**
     * 构建系统配置概览
     */
    private DashboardVO.SystemConfigOverview buildConfigOverview() {
        DashboardVO.SystemConfigOverview overview = new DashboardVO.SystemConfigOverview();
        try {
            // 站点名：优先从 sys_config 查，没有则用 RuoYiConfig
            String siteName = configService.selectConfigByKey("sys.index.siteName");
            overview.setSiteName(siteName != null && !siteName.isEmpty() ? siteName : ruoYiConfig.getName());
            // 站点描述
            String siteDesc = configService.selectConfigByKey("sys.index.siteDescription");
            overview.setSiteDescription(siteDesc != null ? siteDesc : "");
            // 版本
            overview.setVersion(ruoYiConfig.getVersion());
            // 运行时长（JVM 启动至今的小时数）
            long uptimeMs = java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime();
            overview.setUptimeHours(uptimeMs / 3600000);
            // 数据库表数量
            try {
                String dbName = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
                Long tableCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = ? AND table_type = 'BASE TABLE'",
                        Long.class, dbName);
                overview.setTableCount(tableCount != null ? tableCount : 0L);
            } catch (Exception e) {
                log.warn("[Dashboard] 查询表数量失败：{}", e.getMessage());
                overview.setTableCount(0L);
            }
            // Redis 内存使用（MB）+ 缓存命中率（keyspace_hits / (keyspace_hits + keyspace_misses)）
            try {
                java.util.Properties redisInfo = redisCache.redisTemplate.getConnectionFactory()
                        .getConnection().info();
                if (redisInfo != null && redisInfo.getProperty("used_memory") != null) {
                    long usedBytes = Long.parseLong(redisInfo.getProperty("used_memory"));
                    overview.setRedisMemoryMb(usedBytes / 1024.0 / 1024.0);
                } else {
                    overview.setRedisMemoryMb(0.0);
                }
                // 缓存命中率：Redis INFO stats 中的 keyspace_hits 与 keyspace_misses
                // 未启用 keyspace 时两值均为 0，此时命中率为 null（前端显示"-"而非假数据 0%）
                long hits = parseRedisLong(redisInfo.getProperty("keyspace_hits"));
                long misses = parseRedisLong(redisInfo.getProperty("keyspace_misses"));
                if (hits + misses > 0) {
                    overview.setCacheHitRate(hits * 100.0 / (hits + misses));
                } else {
                    overview.setCacheHitRate(null);
                }
            } catch (Exception e) {
                log.warn("[Dashboard] 查询Redis内存失败：{}", e.getMessage());
                overview.setRedisMemoryMb(0.0);
                overview.setCacheHitRate(null);
            }

            // 配置项列表（真实 sys_config 值）
            List<Map<String, Object>> configItems = new ArrayList<>();
            configItems.add(buildConfigItem("siteName", "站点名称", overview.getSiteName()));
            configItems.add(buildConfigItem("registerEnabled", "开放注册",
                    configService.selectConfigByKey("sys.account.registerUser")));
            configItems.add(buildConfigItem("articleAuditEnabled", "文章审核",
                    configService.selectConfigByKey("sys.index.articleAudit")));
            configItems.add(buildConfigItem("cacheTtl", "缓存时长", CACHE_TTL_SECONDS + "秒"));
            overview.setConfigItems(configItems);
        } catch (Exception e) {
            log.error("[Dashboard] 构建配置概览失败", e);
        }
        return overview;
    }

    private Map<String, Object> buildConfigItem(String key, String label, Object value) {
        Map<String, Object> item = new HashMap<>();
        item.put("key", key);
        item.put("label", label);
        item.put("value", value != null ? value : "");
        return item;
    }

    // ========== 分平台运营概览（v13.25） ==========

    /**
     * 构建平台定位（品牌条）：平台名/口号/战略 + 端清单。
     * <p>简介文案取自 sys_platform（端定位的权威来源）；口号与战略属产品口径，
     * 与 README「品牌口号」「产品策略」一致，硬编码在此以便首页零配置可用。
     */
    private DashboardVO.PlatformIdentity buildPlatformIdentity() {
        DashboardVO.PlatformIdentity identity = new DashboardVO.PlatformIdentity();
        identity.setName("旭林知行");
        identity.setSlogan("知行合一，助你上岸");
        identity.setPositioning("AI 驱动的求职面试与学习成长平台");
        identity.setStrategy("内容先行引流 → 体验留存 → 优质内容促进消费");
        try {
            List<Map<String, Object>> rows = dashboardStatsMapper.selectEnabledPlatforms();
            // 该端是否已接入可统计的业务数据（预留端在此登记，避免首页展示假数据）
            Map<String, Boolean> dataReady = Map.of("portal", true, "ledger", true, "admin", true);
            List<DashboardVO.PlatformBrief> briefs = new ArrayList<>();
            for (Map<String, Object> row : rows == null ? List.<Map<String, Object>>of() : rows) {
                String code = text(row.get("platformCode"));
                DashboardVO.PlatformBrief brief = new DashboardVO.PlatformBrief();
                brief.setCode(code);
                brief.setName(text(row.get("platformName")));
                brief.setType(text(row.get("platformType")));
                brief.setDescription(text(row.get("description")));
                brief.setDomain(text(row.get("domain")));
                brief.setIcon(text(row.get("icon")));
                brief.setDataReady(dataReady.getOrDefault(code, false));
                briefs.add(brief);
            }
            // 端清单汇总：已接入端 / 总端数（运营一眼可读）
            long readyCount = briefs.stream().filter(b -> Boolean.TRUE.equals(b.getDataReady())).count();
            for (DashboardVO.PlatformBrief b : briefs) {
                b.setSummary(briefs.size() + " 端中的第 " + (briefs.indexOf(b) + 1) + " 端"
                        + (Boolean.TRUE.equals(b.getDataReady()) ? "" : "（预留）"));
            }
            // 门户端摘要补上模块数，便于端卡片判断"该端业务丰富度"
            identity.setPlatforms(briefs);
            log.debug("[Dashboard] 端清单 {} 个（已接入 {} 个）", briefs.size(), readyCount);
        } catch (Exception e) {
            log.error("[Dashboard] 构建平台定位失败", e);
            identity.setPlatforms(new ArrayList<>());
        }
        return identity;
    }

    /**
     * 构建分平台分模块运营统计（端 → 模块 → 指标卡）。
     * <p>每端只取 2~5 个模块、每模块 3~5 个指标，聚焦"运营能据此做决策"的数字；
     * 任一聚合失败只影响对应模块，不拖垮整页（各块独立 try/catch）。
     */
    private List<DashboardVO.PlatformStats> buildPlatformStats() {
        List<DashboardVO.PlatformStats> list = new ArrayList<>();
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();

        // 共享聚合结果：一次取出，避免每个模块重复查库
        Map<String, Object> articleStats = safeQuery(articleMapper::selectArticleMetrics);
        Map<String, Object> auditSummary = safeQuery(() -> dashboardStatsMapper.selectAuditTaskSummary(todayStart));
        Map<String, Object> aiSummary = safeQuery(dashboardStatsMapper::selectAiExecuteSummary);
        Map<String, Object> aiToday = safeQuery(() -> dashboardStatsMapper.selectAiExecuteTodaySummary(todayStart));
        Map<String, Long> pendingByType = safeQueryMap(auditTaskService::countPendingByType);

        list.add(buildPortalPlatform(articleStats, pendingByType, todayStart));
        list.add(buildLedgerPlatform(todayStart));
        list.add(buildAdminPlatform(auditSummary, aiSummary, aiToday));
        // 预留端（无业务数据）：仅占位，前端灰显
        list.addAll(buildReservedPlatforms());
        return list;
    }

    // ---------- 门户端 ----------

    private DashboardVO.PlatformStats buildPortalPlatform(Map<String, Object> articleStats,
                                                          Map<String, Long> pendingByType,
                                                          LocalDateTime todayStart) {
        DashboardVO.PlatformStats p = newPlatform("portal", "门户端", "c端",
                "求职、学习、成长", "portal", true);

        Long totalArticles = boxed(toLong(articleStats.get("totalArticles")));
        Long publishedArticles = boxed(toLong(articleStats.get("publishedArticles")));
        Long totalViews = boxed(toLong(articleStats.get("totalViews")));
        Long totalLikes = boxed(toLong(articleStats.get("totalLikes")));
        Long totalComments = boxed(toLong(articleStats.get("totalComments")));
        Long pendingTotal = boxed(pendingByType.values().stream().mapToLong(Long::longValue).sum());

        // 端级 KPI：该端最该被运营关注的四个数字
        p.getKpis().add(metric("publishedArticles", "已发布内容", boxed(publishedArticles), "green", "篇", "CircleCheck", null));
        p.getKpis().add(metric("totalViews", "累计浏览", boxed(totalViews), "blue", "次", "View", null));
        p.getKpis().add(metric("portalUsers", "门户用户", toLong(safeQuery(dashboardStatsMapper::selectContentAuxCounts).get("portalUsers")), "purple", "人", "User", null));
        p.getKpis().add(metric("pendingTotal", "待审核", boxed(pendingTotal), pendingTotal > 0 ? "orange" : "gray", "条", "Clock", null));

        // 模块1：内容社区
        DashboardVO.ModuleStats content = newModule("content", "内容社区", "documentation", "/cms/article");
        content.getMetrics().add(metric("articleCount", "文章总数", totalArticles, "blue", "篇", "Document", null));
        content.getMetrics().add(metric("publishedArticles", "已发布", publishedArticles, "green", "篇", "CircleCheck", null));
        content.getMetrics().add(metric("totalComments", "累计评论", totalComments, "cyan", "条", "ChatDotRound", null));
        content.getMetrics().add(metric("totalLikes", "累计点赞", totalLikes, "red", "次", "Star", null));
        p.getModules().add(content);

        // 模块2：AI 语音面试
        Map<String, Object> voice = safeQuery(() -> dashboardStatsMapper.selectVoiceInterviewSummary(todayStart));
        Long voiceTotal = boxed(toLong(voice.get("total")));
        Long voiceFinished = boxed(toLong(voice.get("finished")));
        DashboardVO.ModuleStats interview = newModule("voice_interview", "AI 语音面试", "job", "/portal/interview/voiceInterview");
        interview.getMetrics().add(metric("interviewTotal", "面试总场次", voiceTotal, "purple", "场", "Mic", null));
        interview.getMetrics().add(metric("interviewFinished", "已完成", voiceFinished, "green", "场", "CircleCheck", null));
        interview.getMetrics().add(metric("interviewAvgScore", "平均得分", toLong(voice.get("avgScore")), "orange", "分", "TrendCharts", null));
        interview.getMetrics().add(metric("interviewToday", "今日新增", toLong(voice.get("todayNew")), "blue", "场", "Plus", null));
        p.getModules().add(interview);

        // 模块3：简历中心
        Map<String, Object> resume = safeQuery(dashboardStatsMapper::selectResumeSummary);
        Map<String, Long> aiTasks = aggregateAiTasks(safeList(dashboardStatsMapper::selectPortalAiTaskSummary));
        DashboardVO.ModuleStats resumeModule = newModule("resume", "简历中心", "clipboard", "/portal/resume/template");
        resumeModule.getMetrics().add(metric("resumeTotal", "简历总数", toLong(resume.get("total")), "blue", "份", "Document", null));
        resumeModule.getMetrics().add(metric("resumePublished", "已发布", toLong(resume.get("published")), "green", "份", "CircleCheck", null));
        resumeModule.getMetrics().add(metric("jobMatchCount", "岗位匹配", toLong(aiTasks.get("job_match")), "purple", "次", "Search", null));
        resumeModule.getMetrics().add(metric("optimizeCount", "深度优化", toLong(aiTasks.get("deep_optimize")), "orange", "次", "MagicStick", null));
        p.getModules().add(resumeModule);

        // 模块4：学习工具（OJ 判题）
        Map<String, Object> sub = safeQuery(() -> dashboardStatsMapper.selectInterviewSubmissionSummary(todayStart));
        Long subTotal = boxed(toLong(sub.get("total")));
        Long subAccepted = boxed(toLong(sub.get("accepted")));
        DashboardVO.ModuleStats learn = newModule("learn", "学习中心 / OJ 判题", "education", "/portal/learn/question");
        learn.getMetrics().add(metric("submissionTotal", "编程题提交", subTotal, "blue", "次", "Upload", null));
        learn.getMetrics().add(metric("submissionAccepted", "判题通过", subAccepted, "green", "次", "CircleCheck", null));
        learn.getMetrics().add(metric("submissionToday", "今日提交", toLong(sub.get("todayNew")), "orange", "次", "Plus", null));
        Long subPassRate = boxed(subTotal > 0 ? Math.round(subAccepted * 100.0 / subTotal) : 0L);
        learn.getMetrics().add(metric("submissionPassRate", "通过率", subPassRate, "cyan", "%", "DataLine", null));
        p.getModules().add(learn);

        // 模块5：成长体系
        Map<String, Object> growth = safeQuery(dashboardStatsMapper::selectGrowthSummary);
        DashboardVO.ModuleStats growthModule = newModule("growth", "成长体系", "star", "/cms/growth/rule");
        growthModule.getMetrics().add(metric("growthLogs", "成长记录", toLong(growth.get("growthLogs")), "purple", "条", "TrendCharts", null));
        growthModule.getMetrics().add(metric("leveledUsers", "已获等级", toLong(growth.get("leveled")), "blue", "人", "Medal", null));
        growthModule.getMetrics().add(metric("badges", "徽章发放", toLong(growth.get("badges")), "orange", "枚", "Trophy", null));
        p.getModules().add(growthModule);
        return p;
    }

    // ---------- 记账端 ----------

    private DashboardVO.PlatformStats buildLedgerPlatform(LocalDateTime todayStart) {
        DashboardVO.PlatformStats p = newPlatform("ledger", "记账端", "c端",
                "个人资产管理", "ledger", true);

        List<Map<String, Object>> txRows = safeList(() -> dashboardStatsMapper.selectLedgerTransactionSummary(todayStart));
        Long txTotal = 0L, txToday = 0L;
        Map<String, Long> byType = new LinkedHashMap<>();
        for (Map<String, Object> row : txRows) {
            Long c = boxed(toLong(row.get("cnt")));
            txTotal += c;
            txToday += toLong(row.get("todayCnt"));
            byType.put(text(row.get("type")), c);
        }

        Map<String, Long> accounts = new LinkedHashMap<>();
        for (Map<String, Object> row : safeList(dashboardStatsMapper::selectLedgerAccountSummary)) {
            accounts.put(text(row.get("accountKind")), toLong(row.get("cnt")));
        }
        Long accountTotal = boxed(accounts.values().stream().mapToLong(Long::longValue).sum());
        Map<String, Object> aux = safeQuery(dashboardStatsMapper::selectLedgerAuxCounts);
        Long activeUsers = boxed(toLong(aux.get("activeUsers")));

        p.getKpis().add(metric("txTotal", "记账总笔数", txTotal, "green", "笔", "Money", null));
        p.getKpis().add(metric("ledgerActiveUsers", "记账活跃用户", activeUsers, "blue", "人", "User", null));
        p.getKpis().add(metric("ledgerAccounts", "账户总数", accountTotal, "purple", "个", "Wallet", null));
        p.getKpis().add(metric("txToday", "今日记账", txToday, txToday > 0 ? "orange" : "gray", "笔", "Plus", null));

        // 模块1：个人记账
        DashboardVO.ModuleStats book = newModule("ledger_book", "个人记账", "money", "/ledger-app/category");
        book.getMetrics().add(metric("expenseCount", "支出笔数", byType.getOrDefault("expense", 0L), "red", "笔", "Minus", null));
        book.getMetrics().add(metric("incomeCount", "收入笔数", byType.getOrDefault("income", 0L), "green", "笔", "Plus", null));
        book.getMetrics().add(metric("borrowCount", "借款笔数", byType.getOrDefault("borrow", 0L), "orange", "笔", "CreditCard", null));
        book.getMetrics().add(metric("budgetCount", "预算设置", toLong(aux.get("budgets")), "cyan", "项", "Tickets", null));
        p.getModules().add(book);

        // 模块2：资产负债与 AI 财务分析
        Map<String, Long> aiTasks = aggregateAiTasks(safeList(dashboardStatsMapper::selectPortalAiTaskSummary));
        DashboardVO.ModuleStats finance = newModule("ledger_ai", "AI 财务分析", "chart", "/ledger-app/stats");
        finance.getMetrics().add(metric("aiReportCount", "分析报告", safeQueryLong(dashboardStatsMapper::countLedgerAiReports), "purple", "份", "Document", null));
        finance.getMetrics().add(metric("netWorthSnapshots", "净资产快照", toLong(aux.get("snapshots")), "blue", "份", "DataLine", null));
        finance.getMetrics().add(metric("assetAccounts", "资产账户", accounts.getOrDefault("asset", 0L), "green", "个", "Wallet", null));
        finance.getMetrics().add(metric("liabilityAccounts", "负债账户", accounts.getOrDefault("liability", 0L), "red", "个", "CreditCard", null));
        p.getModules().add(finance);
        return p;
    }

    // ---------- 管理端 ----------

    private DashboardVO.PlatformStats buildAdminPlatform(Map<String, Object> auditSummary,
                                                        Map<String, Object> aiSummary,
                                                        Map<String, Object> aiToday) {
        DashboardVO.PlatformStats p = newPlatform("admin", "管理端", "b端",
                "后台管理与运营", "admin", true);

        Long aiTotal = boxed(toLong(aiSummary.get("total")));
        Long aiSuccess = boxed(toLong(aiSummary.get("success")));
        Long aiFail = boxed(toLong(aiSummary.get("fail")));
        Long aiTokens = boxed(toLong(aiSummary.get("tokens")));
        Long pending = boxed(toLong(auditSummary.get("pending")));

        p.getKpis().add(metric("auditPending", "待审核", pending, pending > 0 ? "orange" : "gray", "条", "Clock", null));
        Long aiSuccessRate = boxed(aiTotal > 0 ? Math.round(aiSuccess * 100.0 / aiTotal) : 0L);
        p.getKpis().add(metric("aiSuccessRate", "AI 成功率", aiSuccessRate, aiFail > 0 ? "orange" : "green", "%", "Cpu", null));
        p.getKpis().add(metric("aiTokens", "AI Token", aiTokens, "purple", "", "DataLine", null));
        Map<String, Object> sysAux = safeQuery(dashboardStatsMapper::selectAdminAccountSummary);
        p.getKpis().add(metric("sysUsers", "后台账号", toLong(sysAux.get("sysUsers")), "blue", "人", "User", null));

        // 模块1：运营审核
        DashboardVO.ModuleStats audit = newModule("audit", "运营审核", "list", "/portal/audit-center");
        audit.getMetrics().add(metric("auditPending", "待审核", pending, pending > 0 ? "orange" : "gray", "条", "Clock", null));
        audit.getMetrics().add(metric("auditApproved", "已通过", toLong(auditSummary.get("approved")), "green", "条", "CircleCheck", null));
        audit.getMetrics().add(metric("auditRejected", "已驳回", toLong(auditSummary.get("rejected")), "red", "条", "CircleClose", null));
        audit.getMetrics().add(metric("auditToday", "今日处理", toLong(auditSummary.get("todayHandled")), "blue", "条", "Finished", null));
        p.getModules().add(audit);

        // 模块2：AI 网关
        DashboardVO.ModuleStats gateway = newModule("ai_gateway", "AI 统一网关", "chart", "/ai/execute-log");
        gateway.getMetrics().add(metric("aiTotal", "调用总数", aiTotal, "blue", "次", "Cpu", null));
        gateway.getMetrics().add(metric("aiFail", "调用失败", aiFail, aiFail > 0 ? "red" : "gray", "次", "WarningFilled", null));
        gateway.getMetrics().add(metric("aiToday", "今日调用", toLong(aiToday.get("todayTotal")), "cyan", "次", "Plus", null));
        gateway.getMetrics().add(metric("aiCost", "累计花费", toLong(aiSummary.get("costYuan")), "orange", "元", "Money", null));
        p.getModules().add(gateway);

        // 模块3：系统健康（复用已有 configOverview 口径）
        DashboardVO.SystemConfigOverview overview = buildConfigOverview();
        DashboardVO.ModuleStats health = newModule("system_health", "系统健康", "monitor", "/monitor/server-panel");
        health.getMetrics().add(metric("uptimeHours", "运行时长", toLong(overview.getUptimeHours()), "green", "小时", "Timer", null));
        health.getMetrics().add(metric("tableCount", "数据库表", toLong(overview.getTableCount()), "blue", "张", "Coin", null));
        Long cacheHitRate = boxed(overview.getCacheHitRate() == null ? 0L : Math.round(overview.getCacheHitRate()));
        health.getMetrics().add(metric("redisMemoryMb", "Redis 内存", toLong(overview.getRedisMemoryMb()), "purple", "MB", "Coin", null));
        health.getMetrics().add(metric("cacheHitRate", "缓存命中率", cacheHitRate, "cyan", "%", "DataLine", null));
        p.getModules().add(health);

        // 模块4：平台与权限配置
        DashboardVO.ModuleStats config = newModule("platform_config", "平台与权限", "system", "/system/platform");
        config.getMetrics().add(metric("sysRoles", "角色数", toLong(sysAux.get("sysRoles")), "blue", "个", "Peoples", null));
        config.getMetrics().add(metric("platformCount", "接入端", boxed((long) safeList(dashboardStatsMapper::selectEnabledPlatforms).size()), "purple", "个", "Grid", null));
        config.getMetrics().add(metric("sysUsers", "后台账号", toLong(sysAux.get("sysUsers")), "green", "人", "User", null));
        p.getModules().add(config);
        return p;
    }

    /**
     * 预留端占位：sys_platform 中已登记但尚无业务数据的端（如人格分析端）。
     * <p>只标记 dataReady=false，不编造任何统计数字。
     */
    private List<DashboardVO.PlatformStats> buildReservedPlatforms() {
        List<DashboardVO.PlatformStats> list = new ArrayList<>();
        try {
            for (Map<String, Object> row : safeList(dashboardStatsMapper::selectEnabledPlatforms)) {
                String code = text(row.get("platformCode"));
                if (code.isEmpty() || "portal".equals(code) || "ledger".equals(code) || "admin".equals(code)) {
                    continue;
                }
                list.add(newPlatform(code, text(row.get("platformName")), text(row.get("platformType")),
                        text(row.get("description")), text(row.get("icon")), false));
            }
        } catch (Exception e) {
            log.error("[Dashboard] 构建预留端失败", e);
        }
        return list;
    }

    // ---------- 运营警报 ----------

    /**
     * 构建运营警报：只在确有异常时产出条目，无异常返回空列表（前端整条隐藏）。
     * <p>阈值取运营可解释的口径，不做趋势预测，避免编造。
     */
    private List<DashboardVO.OpsAlert> buildAlerts() {
        List<DashboardVO.OpsAlert> alerts = new ArrayList<>();
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        try {
            Map<String, Object> auditSummary = safeQuery(() -> dashboardStatsMapper.selectAuditTaskSummary(todayStart));
            long pending = toLong(auditSummary.get("pending"));
            if (pending > 0) {
                alerts.add(newAlert(pending >= 20 ? "danger" : "warning", "admin",
                        "审核待办积压 " + pending + " 条",
                        "待办越积越多会拖慢内容上线节奏，建议优先清理。",
                        "/portal/audit-center"));
            }

            Map<String, Object> aiSummary = safeQuery(dashboardStatsMapper::selectAiExecuteSummary);
            long aiTotal = toLong(aiSummary.get("total"));
            long aiFail = toLong(aiSummary.get("fail"));
            long todayFail = toLong(safeQuery(() -> dashboardStatsMapper.selectAiExecuteTodaySummary(todayStart)).get("todayFail"));
            if (todayFail > 0) {
                alerts.add(newAlert(todayFail >= 10 ? "danger" : "warning", "admin",
                        "AI 网关今日失败 " + todayFail + " 次",
                        "今日调用失败会直接影响 AI 面试/简历/财务分析体验，建议查看执行日志定位场景。",
                        "/ai/execute-log"));
            }
            // 累计失败率偏高（样本 >= 20 才有统计意义，避免小样本误报）
            if (aiTotal >= 20 && aiFail * 100.0 / aiTotal >= 15) {
                alerts.add(newAlert("warning", "admin",
                        "AI 累计失败率 " + Math.round(aiFail * 100.0 / aiTotal) + "%",
                        "失败率高于 15% 的经验阈值，建议核对模型配置与场景提示词。",
                        "/ai/execute-log"));
            }

            // 未完成的语音面试（用户中途退出）会形成"僵尸会话"，运营需关注
            Map<String, Object> voice = safeQuery(() -> dashboardStatsMapper.selectVoiceInterviewSummary(todayStart));
            long unfinished = toLong(voice.get("unfinished"));
            if (unfinished > 0) {
                alerts.add(newAlert("info", "portal",
                        "有 " + unfinished + " 场面试未完成",
                        "未完成场次不计入有效面试数据，若持续增长需排查语音链路稳定性。",
                        "/cms/interview"));
            }
        } catch (Exception e) {
            log.error("[Dashboard] 构建运营警报失败", e);
        }
        return alerts;
    }

    // ---------- 分平台统计辅助 ----------

    private DashboardVO.PlatformStats newPlatform(String code, String name, String type,
                                                  String description, String icon, boolean dataReady) {
        DashboardVO.PlatformStats p = new DashboardVO.PlatformStats();
        p.setPlatformCode(code);
        p.setPlatformName(name);
        p.setPlatformType(type);
        p.setDescription(description);
        p.setIcon(icon);
        p.setDataReady(dataReady);
        p.setKpis(new ArrayList<>());
        p.setModules(new ArrayList<>());
        return p;
    }

    private DashboardVO.ModuleStats newModule(String code, String name, String icon, String routePath) {
        DashboardVO.ModuleStats m = new DashboardVO.ModuleStats();
        m.setModuleCode(code);
        m.setModuleName(name);
        m.setIcon(icon);
        m.setRoutePath(routePath);
        m.setMetrics(new ArrayList<>());
        return m;
    }

    private DashboardVO.OpsAlert newAlert(String level, String platformCode, String title,
                                          String detail, String routePath) {
        DashboardVO.OpsAlert alert = new DashboardVO.OpsAlert();
        alert.setLevel(level);
        alert.setPlatformCode(platformCode);
        alert.setTitle(title);
        alert.setDetail(detail);
        alert.setRoutePath(routePath);
        return alert;
    }

    /** 构建带主题色/单位的指标卡（单一签名，避免重载推断歧义） */
    private DashboardVO.MetricCard metric(String key, String label, Long value,
                                          String tone, String unit, String icon, Double trend) {
        DashboardVO.MetricCard c = buildCard(key, label, value, icon, trend);
        c.setTone(tone);
        c.setUnit(unit);
        return c;
    }

    /**
     * 把 portal_ai_task 的 (taskType,status) 明细压成 taskType → 总次数。
     * <p>简历中心的"岗位匹配/深度优化"等次数即来自该表。
     */
    private Map<String, Long> aggregateAiTasks(List<Map<String, Object>> rows) {
        Map<String, Long> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            String type = text(row.get("taskType"));
            if (type.isEmpty()) continue;
            result.merge(type, toLong(row.get("cnt")), Long::sum);
        }
        return result;
    }

    /** 聚合查询失败时返回空 Map（首页不应因单块失败整体 500） */
    private Map<String, Object> safeQuery(java.util.function.Supplier<Map<String, Object>> supplier) {
        try {
            Map<String, Object> r = supplier.get();
            return r != null ? r : new HashMap<>();
        } catch (Exception e) {
            log.warn("[Dashboard] 聚合查询失败：{}", e.getMessage());
            return new HashMap<>();
        }
    }

    private List<Map<String, Object>> safeList(java.util.function.Supplier<List<Map<String, Object>>> supplier) {
        try {
            List<Map<String, Object>> r = supplier.get();
            return r != null ? r : new ArrayList<>();
        } catch (Exception e) {
            log.warn("[Dashboard] 列表聚合查询失败：{}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private Long safeQueryLong(java.util.function.Supplier<Long> supplier) {
        try {
            Long r = supplier.get();
            return r != null ? r : 0L;
        } catch (Exception e) {
            log.warn("[Dashboard] 计数查询失败：{}", e.getMessage());
            return 0L;
        }
    }

    private Map<String, Long> safeQueryMap(java.util.function.Supplier<Map<String, Long>> supplier) {
        try {
            Map<String, Long> r = supplier.get();
            return r != null ? r : new HashMap<>();
        } catch (Exception e) {
            log.warn("[Dashboard] 分组计数查询失败：{}", e.getMessage());
            return new HashMap<>();
        }
    }

    /** 显式装箱：避免三元表达式（long 分支）在重载/泛型推断下不被自动装箱 */
    private Long boxed(long v) {
        return Long.valueOf(v);
    }

    /** null 安全的字符串取值（Map 中不存在或为 null 时返回空串） */
    private String text(Object obj) {
        return obj != null ? String.valueOf(obj) : "";
    }

    // ========== 工具方法 ==========

    private DashboardVO.MetricCard buildCard(String key, String label, Long value, String icon, Double trend) {
        DashboardVO.MetricCard card = new DashboardVO.MetricCard();
        card.setKey(key);
        card.setLabel(label);
        card.setValue(value);
        card.setIcon(icon);
        card.setTrend(trend);
        // trend 为 null 时不编造趋势方向，前端据此隐藏趋势行
        if (trend != null) {
            card.setTrendDirection(trend > 0 ? "up" : (trend < 0 ? "down" : "flat"));
        } else {
            card.setTrendDirection(null);
        }
        return card;
    }

    private long toLong(Object obj) {
        if (obj == null) return 0L;
        if (obj instanceof Number) return ((Number) obj).longValue();
        try {
            return Long.parseLong(String.valueOf(obj));
        } catch (Exception e) {
            return 0L;
        }
    }

    private double toDouble(Object obj) {
        if (obj == null) return 0.0;
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try {
            return Double.parseDouble(String.valueOf(obj));
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * 解析 Redis INFO 返回的数值字段（可能为 null 或非数字），失败返回 0
     */
    private long parseRedisLong(String val) {
        if (val == null || val.isEmpty()) return 0L;
        try {
            return Long.parseLong(val.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private String buildOperDesc(SysOperLog oper) {
        String bizType = switch (oper.getBusinessType() != null ? oper.getBusinessType() : 0) {
            case 1 -> "新增";
            case 2 -> "修改";
            case 3 -> "删除";
            case 4 -> "授权";
            case 5 -> "导出";
            case 6 -> "导入";
            case 7 -> "强退";
            case 8 -> "生成代码";
            case 9 -> "清空数据";
            default -> "操作";
        };
        return bizType + (oper.getTitle() != null ? oper.getTitle() : "");
    }
}
