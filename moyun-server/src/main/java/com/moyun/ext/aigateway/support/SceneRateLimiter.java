package com.moyun.ext.aigateway.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;

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

    /**
     * 原子"自增 + 首次设 TTL"固定窗口计数。
     *
     * <p>历史实现是 INCR 后判断是否为 1 再 EXPIRE 两次独立命令：若在两步之间崩溃/断开，
     * 键将**永久无 TTL** → 该窗口计数永不重置，用户会被限流到 Redis 手动清理为止。
     * 改为 Lua 单次往返保证原子性。</p>
     *
     * <p>KEYS[1]=计数键，ARGV[1]=窗口秒数。返回自增后的值。</p>
     */
    private static final RedisScript<Long> FIXED_WINDOW_SCRIPT = new DefaultRedisScript<>(
            "local v = redis.call('incr', KEYS[1]) "
                    + "if v == 1 then "
                    + "  redis.call('expire', KEYS[1], ARGV[1]) "
                    + "end "
                    + "return v",
            Long.class);

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
            // 原子：incr + 首次设 TTL（避免两次命令之间崩溃导致窗口永不重置）
            Long count = incrWithWindow(key, windowSeconds);
            int current = count != null ? count.intValue() : 1;
            boolean allowed = current <= limit;
            if (!allowed) {
                log.info("[aigateway:rate] 限流触发: scene={}, identity={}, {}/{}",
                        scene, identity, current, limit);
            }
            return new RateResult(allowed, limit, current, windowSeconds);
        } catch (Exception e) {
            // Redis 异常不阻断业务（限流降级为放行）
            log.warn("[aigateway:rate] 限流检查异常（放行）: {}", e.getMessage());
            return new RateResult(true, limit, 0, windowSeconds);
        }
    }

    /**
     * 执行固定窗口脚本（包级可见，便于单测 spy/stub）。
     *
     * <p>用实例方法包装 {@code redisTemplate.execute(RedisScript, List, Object...)}：
     * 该类调用是泛型 varargs，Mockito 会把 varargs <b>展开</b>记录（实测 1 个可变参数被记成
     * 独立实参，argCount=3），matcher 数量极难对齐；包装为固定签名实例方法后可稳定 stub。</p>
     */
    Long incrWithWindow(String key, int windowSeconds) {
        return redisTemplate.execute(FIXED_WINDOW_SCRIPT,
                Collections.singletonList(key), String.valueOf(windowSeconds));
    }
}
