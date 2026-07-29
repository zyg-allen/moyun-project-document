package com.moyun.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.core.config.redis.RedisCache;
import com.moyun.portal.domain.entity.PortalCreatorCertification;
import com.moyun.portal.domain.entity.PortalFeedback;
import com.moyun.portal.domain.entity.PortalReport;
import com.moyun.portal.mapper.PortalArticleMapper;
import com.moyun.portal.mapper.PortalArticleViewMapper;
import com.moyun.portal.mapper.PortalCreatorCertificationMapper;
import com.moyun.portal.mapper.PortalFeedbackMapper;
import com.moyun.portal.mapper.PortalReportMapper;
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
    private PortalReportMapper reportMapper;

    @Autowired
    private PortalFeedbackMapper feedbackMapper;

    @Autowired
    private PortalCreatorCertificationMapper creatorCertificationMapper;

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

    @Override
    public DashboardVO getDashboardData() {
        // 尝试命中完整缓存
        DashboardVO cached = redisCache.getCacheObject(CACHE_KEY_FULL);
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

        // 写入缓存
        redisCache.setCacheObject(CACHE_KEY_FULL, vo, (int) CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        return vo;
    }

    @Override
    public DashboardVO getMetrics() {
        DashboardVO cached = redisCache.getCacheObject(CACHE_KEY_METRICS);
        if (cached != null) return cached;
        DashboardVO vo = new DashboardVO();
        vo.setMetrics(buildMetrics());
        redisCache.setCacheObject(CACHE_KEY_METRICS, vo, (int) CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        return vo;
    }

    @Override
    public DashboardVO.TodayStats getTodayStats() {
        DashboardVO.TodayStats cached = redisCache.getCacheObject(CACHE_KEY_TODAY);
        if (cached != null) return cached;
        DashboardVO.TodayStats stats = buildTodayStats();
        redisCache.setCacheObject(CACHE_KEY_TODAY, stats, (int) CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        return stats;
    }

    @Override
    public List<DashboardVO.TrendPoint> getLoginTrend() {
        List<DashboardVO.TrendPoint> cached = redisCache.getCacheObject(CACHE_KEY_LOGIN_TREND);
        if (cached != null) return cached;
        List<DashboardVO.TrendPoint> trend = buildLoginTrend();
        redisCache.setCacheObject(CACHE_KEY_LOGIN_TREND, trend, (int) CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        return trend;
    }

    @Override
    public List<DashboardVO.TrendPoint> getPublishTrend() {
        List<DashboardVO.TrendPoint> cached = redisCache.getCacheObject(CACHE_KEY_PUBLISH_TREND);
        if (cached != null) return cached;
        List<DashboardVO.TrendPoint> trend = buildPublishTrend();
        redisCache.setCacheObject(CACHE_KEY_PUBLISH_TREND, trend, (int) CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        return trend;
    }

    @Override
    public List<DashboardVO.CategoryRank> getCategoryRanking() {
        List<DashboardVO.CategoryRank> cached = redisCache.getCacheObject(CACHE_KEY_CATEGORY_RANK);
        if (cached != null) return cached;
        List<DashboardVO.CategoryRank> ranking = buildCategoryRanking();
        redisCache.setCacheObject(CACHE_KEY_CATEGORY_RANK, ranking, (int) CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        return ranking;
    }

    @Override
    public List<DashboardVO.TaskItem> getTodoTasks() {
        List<DashboardVO.TaskItem> cached = redisCache.getCacheObject(CACHE_KEY_TODO);
        if (cached != null) return cached;
        List<DashboardVO.TaskItem> tasks = buildTodoTasks();
        redisCache.setCacheObject(CACHE_KEY_TODO, tasks, (int) CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        return tasks;
    }

    @Override
    public List<DashboardVO.TaskItem> getMyTasks() {
        List<DashboardVO.TaskItem> cached = redisCache.getCacheObject(CACHE_KEY_MY_TASKS);
        if (cached != null) return cached;
        List<DashboardVO.TaskItem> tasks = buildMyTasks();
        redisCache.setCacheObject(CACHE_KEY_MY_TASKS, tasks, (int) CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        return tasks;
    }

    @Override
    public List<DashboardVO.ActivityItem> getSystemActivities() {
        List<DashboardVO.ActivityItem> cached = redisCache.getCacheObject(CACHE_KEY_ACTIVITIES);
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
        DashboardVO.SystemConfigOverview cached = redisCache.getCacheObject(CACHE_KEY_CONFIG);
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
        // 同步清理排行榜 ZSet，否则下次取热门文章/栏目排行仍读旧数据
        redisCache.redisTemplate.delete(ZSET_KEY_HOT_ARTICLES);
        redisCache.redisTemplate.delete(ZSET_KEY_CATEGORY_VIEWS);
        log.info("[Dashboard] 缓存已手动刷新（含 ZSet 排行榜）");
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
            long pendingArticles = toLong(stats.get("pendingArticles"));
            long totalViews = toLong(stats.get("totalViews"));
            long totalLikes = toLong(stats.get("totalLikes"));
            long totalComments = toLong(stats.get("totalComments"));

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
            // 按日期分组，合并 success/fail
            Map<String, long[]> grouped = new TreeMap<>();
            for (Map<String, Object> row : raw) {
                String date = String.valueOf(row.get("date"));
                long value = toLong(row.get("value"));
                String label = String.valueOf(row.get("label"));
                long[] arr = grouped.computeIfAbsent(date, k -> new long[2]);
                if ("success".equals(label)) arr[0] += value;
                else arr[1] += value;
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
     */
    private List<DashboardVO.TaskItem> buildTodoTasks() {
        List<DashboardVO.TaskItem> tasks = new ArrayList<>();
        try {
            // 1. 待审核文章
            List<Map<String, Object>> pending = articleMapper.selectPendingArticles(8);
            for (Map<String, Object> row : pending) {
                DashboardVO.TaskItem item = new DashboardVO.TaskItem();
                item.setId(toLong(row.get("id")));
                item.setType("article_audit");
                item.setTitle(String.valueOf(row.get("title")));
                item.setDescription("文章待审核");
                item.setStatus("pending");
                // 注意：MyBatis 开启 map-underscore-to-camel-case，@Select 返回 Map 时
                // create_time 列会被转为 createTime 键，不能用 row.get("create_time")
                Object createTime = row.get("createTime");
                if (createTime == null) {
                    createTime = row.get("create_time");
                }
                item.setCreateTime(createTime != null ? String.valueOf(createTime) : "");
                String nickname = row.get("authorNickname") != null ? String.valueOf(row.get("authorNickname")) : String.valueOf(row.get("authorUsername"));
                item.setSubmitter(nickname);
                item.setPriority("high");
                // audit.vue 读取 route.query.id，必须用 query 形式跳转
                item.setRoutePath("/cms/article/audit?id=" + item.getId());
                tasks.add(item);
            }

            // 2. 系统通知（待处理）：查询当前用户的通知列表，过滤未读
            Long currentUserId = SecurityUtils.getUserId();
            SysNotification queryNotif = new SysNotification();
            queryNotif.setUserId(currentUserId);
            queryNotif.setUserType("sys");
            List<SysNotification> notifs = notificationMapper.selectNotificationList(queryNotif);
            if (notifs != null) {
                // 只取未读通知作为待办
                List<SysNotification> unread = notifs.stream()
                        .filter(n -> n.getIsRead() == null || !Boolean.TRUE.equals(n.getIsRead()))
                        .limit(5)
                        .collect(Collectors.toList());
                for (SysNotification n : unread) {
                    DashboardVO.TaskItem item = new DashboardVO.TaskItem();
                    item.setId(n.getId());
                    item.setType("notification");
                    item.setTitle(n.getTitle() != null ? n.getTitle() : "系统通知");
                    item.setDescription(n.getContent() != null ? truncate(n.getContent(), 60) : "");
                    item.setStatus("pending");
                    item.setCreateTime(n.getCreateTime() != null ? n.getCreateTime().format(DATETIME_FMT) : "");
                    item.setSubmitter("系统");
                    item.setPriority("medium".equals(n.getType()) ? "medium" : "low");
                    item.setRoutePath("/system/notification");
                    tasks.add(item);
                }
            }

            // 3. 待处理举报（pending 状态）
            try {
                LambdaQueryWrapper<PortalReport> reportWrapper = new LambdaQueryWrapper<>();
                reportWrapper.eq(PortalReport::getStatus, "pending")
                        .orderByDesc(PortalReport::getCreateTime).last("limit 5");
                List<PortalReport> pendingReports = reportMapper.selectList(reportWrapper);
                for (PortalReport r : pendingReports) {
                    DashboardVO.TaskItem item = new DashboardVO.TaskItem();
                    item.setId(r.getId());
                    item.setType("report");
                    item.setTitle("举报：" + (r.getReportType() != null ? r.getReportType() : "其他"));
                    item.setDescription(r.getDescription() != null ? truncate(r.getDescription(), 60) : "");
                    item.setStatus("pending");
                    item.setCreateTime(r.getCreateTime() != null ? r.getCreateTime().format(DATETIME_FMT) : "");
                    item.setSubmitter(r.getUsername() != null ? r.getUsername() : "匿名");
                    item.setPriority("high");
                    item.setRoutePath("/cms/report");
                    tasks.add(item);
                }
            } catch (Exception ex) {
                log.warn("[Dashboard] 构建举报待办失败：{}", ex.getMessage());
            }

            // 4. 待处理反馈（pending 状态）
            try {
                LambdaQueryWrapper<PortalFeedback> feedbackWrapper = new LambdaQueryWrapper<>();
                feedbackWrapper.eq(PortalFeedback::getStatus, "pending")
                        .orderByDesc(PortalFeedback::getCreateTime).last("limit 5");
                List<PortalFeedback> pendingFeedbacks = feedbackMapper.selectList(feedbackWrapper);
                for (PortalFeedback f : pendingFeedbacks) {
                    DashboardVO.TaskItem item = new DashboardVO.TaskItem();
                    item.setId(f.getId());
                    item.setType("feedback");
                    item.setTitle("反馈：" + (f.getSubject() != null ? f.getSubject() : f.getFeedbackType()));
                    item.setDescription(f.getDescription() != null ? truncate(f.getDescription(), 60) : "");
                    item.setStatus("pending");
                    item.setCreateTime(f.getCreateTime() != null ? f.getCreateTime().format(DATETIME_FMT) : "");
                    item.setSubmitter(f.getUsername() != null ? f.getUsername() : "匿名");
                    item.setPriority("medium");
                    item.setRoutePath("/cms/feedback");
                    tasks.add(item);
                }
            } catch (Exception ex) {
                log.warn("[Dashboard] 构建反馈待办失败：{}", ex.getMessage());
            }

            // 5. 创作者认证待审核（pending 状态）
            try {
                LambdaQueryWrapper<PortalCreatorCertification> certWrapper = new LambdaQueryWrapper<>();
                certWrapper.eq(PortalCreatorCertification::getStatus, "pending")
                        .orderByDesc(PortalCreatorCertification::getCreatedTime).last("limit 5");
                List<PortalCreatorCertification> pendingCerts = creatorCertificationMapper.selectList(certWrapper);
                for (PortalCreatorCertification c : pendingCerts) {
                    DashboardVO.TaskItem item = new DashboardVO.TaskItem();
                    item.setId(c.getId());
                    item.setType("creator_certification");
                    item.setTitle("认证申请：" + c.getRealName());
                    item.setDescription("创作者认证待审核（" + (c.getCertType() != null ? c.getCertType() : "creator") + "）");
                    item.setStatus("pending");
                    item.setCreateTime(c.getCreatedTime() != null ? c.getCreatedTime().format(DATETIME_FMT) : "");
                    // 申请人昵称（如查不到则用 realName 兜底）
                    String submitter = c.getRealName();
                    if (c.getUserId() != null) {
                        try {
                            com.moyun.portal.domain.entity.PortalUser u = portalUserMapper.selectPortalUserById(c.getUserId());
                            if (u != null && u.getNickname() != null) {
                                submitter = u.getNickname();
                            }
                        } catch (Exception ignored) {
                        }
                    }
                    item.setSubmitter(submitter);
                    item.setPriority("medium");
                    item.setRoutePath("/certification/audit");
                    tasks.add(item);
                }
            } catch (Exception ex) {
                log.warn("[Dashboard] 构建创作者认证待办失败：{}", ex.getMessage());
            }
        } catch (Exception e) {
            log.error("[Dashboard] 构建待办任务失败", e);
        }
        return tasks;
    }

    /**
     * 构建与我相关任务（已办）
     */
    private List<DashboardVO.TaskItem> buildMyTasks() {
        List<DashboardVO.TaskItem> tasks = new ArrayList<>();
        try {
            String username = SecurityUtils.getUsername();
            // 查询当前用户最近的操作日志（已办）
            List<SysOperLog> operLogs = operLogMapper.selectOperLogList(new OperLogQuery());
            if (operLogs != null) {
                List<SysOperLog> myLogs = operLogs.stream()
                        .filter(l -> username != null && username.equals(l.getOperName()))
                        .sorted(Comparator.comparing(SysOperLog::getOperTime).reversed())
                        .limit(8)
                        .collect(Collectors.toList());
                for (SysOperLog oper : myLogs) {
                    DashboardVO.TaskItem item = new DashboardVO.TaskItem();
                    item.setId(oper.getOperId());
                    item.setType("operation");
                    item.setTitle(oper.getTitle() != null ? oper.getTitle() : "操作记录");
                    item.setDescription(buildOperDesc(oper));
                    item.setStatus("processed");
                    item.setCreateTime(oper.getOperTime() != null ? oper.getOperTime().format(DATETIME_FMT) : "");
                    item.setSubmitter(oper.getOperName());
                    item.setPriority("low");
                    item.setRoutePath("/monitor/operlog");
                    tasks.add(item);
                }
            }
        } catch (Exception e) {
            log.error("[Dashboard] 构建已办任务失败", e);
        }
        return tasks;
    }

    /**
     * 构建系统动态（操作日志 + 系统通知合并）
     */
    private List<DashboardVO.ActivityItem> buildSystemActivities() {
        List<DashboardVO.ActivityItem> activities = new ArrayList<>();
        try {
            // 1. 最近操作日志
            List<SysOperLog> operLogs = operLogMapper.selectOperLogList(new OperLogQuery());
            if (operLogs != null) {
                List<SysOperLog> recent = operLogs.stream()
                        .sorted(Comparator.comparing(SysOperLog::getOperTime).reversed())
                        .limit(10)
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
            // 2. 最近系统通知（广播）
            SysNotification queryNotif = new SysNotification();
            queryNotif.setScope("all");
            queryNotif.setUserType("sys");
            List<SysNotification> notifs = notificationMapper.selectNotificationList(queryNotif);
            if (notifs != null) {
                List<SysNotification> recent = notifs.stream()
                        .sorted(Comparator.comparing(SysNotification::getCreateTime).reversed())
                        .limit(5)
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

        // 合并后按时间排序，取 Top 12
        activities.sort(Comparator.comparing(DashboardVO.ActivityItem::getCreateTime).reversed());
        if (activities.size() > 12) {
            activities = activities.subList(0, 12);
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

    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        return str.length() > maxLen ? str.substring(0, maxLen) + "..." : str;
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
