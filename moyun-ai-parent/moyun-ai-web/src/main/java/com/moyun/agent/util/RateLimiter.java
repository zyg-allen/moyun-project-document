package com.moyun.agent.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 简单的限流器
 */
public class RateLimiter {
    
    /**
     * 用户访问计数 (userId -> count)
     */
    private static final Map<String, AtomicInteger> REQUEST_COUNT = new ConcurrentHashMap<>();
    
    /**
     * 用户首次访问时间 (userId -> timestamp)
     */
    private static final Map<String, Long> FIRST_REQUEST_TIME = new ConcurrentHashMap<>();
    
    /**
     * 时间窗口（毫秒）- 默认1分钟
     */
    private static final long TIME_WINDOW = 60 * 1000;
    
    /**
     * 默认限制次数
     */
    private static final int DEFAULT_LIMIT = 20;
    
    /**
     * 检查是否允许访问
     * 
     * @param userId 用户ID
     * @return 是否允许
     */
    public static boolean tryAcquire(String userId) {
        return tryAcquire(userId, DEFAULT_LIMIT);
    }
    
    /**
     * 检查是否允许访问
     * 
     * @param userId 用户ID
     * @param limit 限制次数
     * @return 是否允许
     */
    public static boolean tryAcquire(String userId, int limit) {
        long now = System.currentTimeMillis();
        
        // 获取首次请求时间
        Long firstTime = FIRST_REQUEST_TIME.get(userId);
        
        // 如果是新用户或时间窗口已过期，重置计数
        if (firstTime == null || now - firstTime > TIME_WINDOW) {
            FIRST_REQUEST_TIME.put(userId, now);
            REQUEST_COUNT.put(userId, new AtomicInteger(1));
            return true;
        }
        
        // 增加计数
        AtomicInteger count = REQUEST_COUNT.get(userId);
        if (count == null) {
            count = new AtomicInteger(0);
            REQUEST_COUNT.put(userId, count);
        }
        
        int currentCount = count.incrementAndGet();
        
        // 检查是否超过限制
        if (currentCount > limit) {
            System.out.println("[限流] 用户 " + userId + " 超过限制: " + currentCount + "/" + limit);
            return false;
        }
        
        return true;
    }
    
    /**
     * 获取剩余次数
     */
    public static int getRemainingCount(String userId, int limit) {
        AtomicInteger count = REQUEST_COUNT.get(userId);
        if (count == null) {
            return limit;
        }
        return Math.max(0, limit - count.get());
    }
    
    /**
     * 清理过期数据（定时任务调用）
     */
    public static void cleanup() {
        long now = System.currentTimeMillis();
        FIRST_REQUEST_TIME.entrySet().removeIf(entry -> 
            now - entry.getValue() > TIME_WINDOW
        );
        
        // 清理计数
        FIRST_REQUEST_TIME.keySet().forEach(userId -> {
            if (!REQUEST_COUNT.containsKey(userId)) {
                REQUEST_COUNT.remove(userId);
            }
        });
    }
}
