package com.moyun.agent.task;

import com.moyun.agent.service.impl.DataSourceServiceImpl;
import com.moyun.agent.util.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 缓存清理定时任务
 */
@Slf4j
@Component
public class CacheCleanupTask {
    
    @Autowired
    private DataSourceServiceImpl dataSourceService;
    
    /**
     * 清理过期的表结构缓存
     * 每5分钟执行一次
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
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
     * 每2分钟执行一次
     */
    @Scheduled(fixedRate = 2 * 60 * 1000)
    public void cleanupRateLimiter() {
        try {
            log.debug("开始清理限流器过期数据");
            RateLimiter.cleanup();
        } catch (Exception e) {
            log.error("清理限流器失败", e);
        }
    }
}
