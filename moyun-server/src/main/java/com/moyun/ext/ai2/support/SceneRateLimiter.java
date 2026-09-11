package com.moyun.ext.ai2.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 场景限流器（Redis 固定窗口计数）
 *
 * <p>依据《AI能力统一接入层 — 完整方案文档》V2.0 §5.3 第4步。限流维度：场景 × 用户，
 * 参数来自 ai_scene_config 的 rate_limit_count / rate_limit_time。</p>
 *
 * <p>与底座 RateLimiter（内存版，单机）不同，本实现基于 Redis INCR + EXPIRE，多实例部署下同样生效。</p>
 *
 * @author laomao
 * @since 2026-09-09
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SceneRateLimiter {

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 限流结果
     */
    public record RateResult(boolean allowed, int limit, int current, long windowSeconds) {
    }

    /**
     * 检查是否放行（固定窗口计数）
     *
     * @param scene         场景代码
     * @param identity      限流主体（用户ID或IP）
     * @param limit         窗口内最大次数
     * @param windowSeconds 窗口时长（秒）
     */
    public RateResult tryAcquire(String scene, String identity, int limit, int windowSeconds) {
        if (limit <= 0 || windowSeconds <= 0) {
            return new RateResult(true, limit, 0, windowSeconds);
        }
        String key = "ai2:rate:" + scene + ":" + identity;
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
            }
            int current = count != null ? count.intValue() : 1;
            boolean allowed = current <= limit;
            if (!allowed) {
                log.info("[ai2:rate] 限流触发: scene={}, identity={}, {}/{}",
                        scene, identity, current, limit);
            }
            return new RateResult(allowed, limit, current, windowSeconds);
        } catch (Exception e) {
            // Redis 异常不阻断业务（限流降级为放行）
            log.warn("[ai2:rate] 限流检查异常（放行）: {}", e.getMessage());
            return new RateResult(true, limit, 0, windowSeconds);
        }
    }
}
