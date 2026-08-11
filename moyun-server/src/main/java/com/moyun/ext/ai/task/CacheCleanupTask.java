package com.moyun.ext.ai.task;

import com.moyun.ext.ai.service.impl.DataSourceServiceImpl;
import com.moyun.ext.ai.util.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 缓存清理定时任务
 *
 * <p>调度入口已迁移至 Quartz：见 sys_job 中 invoke_target：
 * <ul>
 *   <li>{@code cacheCleanupTask.cleanupExpiredCache()}  - 每5分钟清理表结构缓存</li>
 *   <li>{@code cacheCleanupTask.cleanupRateLimiter()}    - 每2分钟清理限流器过期数据</li>
 * </ul>
 *
 * <p>历史：原为 Spring @Scheduled(fixedRate=...) 注解触发，现统一由 sys_job 调度，
 * 便于后台「监控 - 定时任务」可视化管控（暂停/立即执行/调整 cron）。
 */
@Slf4j
@Component
public class CacheCleanupTask {

    @Autowired
    private DataSourceServiceImpl dataSourceService;

    /**
     * 清理过期的表结构缓存
     * <p>调度：sys_job 每5分钟执行一次（cron 见 sys_job.cron_expression）
     */
    public void cleanupExpiredCache() {
        try {
            log.debug("开始清理过期缓存");
            dataSourceService.clearExpiredCache();
        } catch (Exception e) {
            log.error("清理过期缓存失败", e);
        }
    }

    /**
     * 清理限流器过期数据
     * <p>调度：sys_job 每2分钟执行一次（cron 见 sys_job.cron_expression）
     */
    public void cleanupRateLimiter() {
        try {
            log.debug("开始清理限流器过期数据");
            RateLimiter.cleanup();
        } catch (Exception e) {
            log.error("清理限流器失败", e);
        }
    }
}
