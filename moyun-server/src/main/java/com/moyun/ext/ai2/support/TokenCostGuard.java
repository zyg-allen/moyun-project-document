package com.moyun.ext.ai2.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 场景日 Token 成本熔断（v11.57 P0-2）
 *
 * <p>语义：场景级日累计（所有用户共享额度），保护平台总成本——一个死循环调用或注入攻击
 * 最多烧掉当日配额。参数来自 ai_scene_config.daily_token_limit（null/0=不限）。</p>
 *
 * <p>计数：Redis INCR，键含日期（ai2:token:{scene}:{yyyyMMdd}），跨日自然切换；
 * TTL 2 天兜底清理。与 {@link SceneRateLimiter} 同风格：Redis 异常放行（熔断降级为不限），
 * 避免基础设施抖动阻断业务。</p>
 *
 * <p>时序：先 checkQuota（用此前累计值判断）→ 执行 → 成功后 consume（按实际 tokenUsed 累加）。
 * 并发窗口内的少量超额可接受（成本保护非硬预算）。</p>
 *
 * @author laomao
 * @since 2026-09-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCostGuard {

    private final RedisTemplate<String, String> redisTemplate;

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 配额检查结果
     */
    public record QuotaResult(boolean allowed, int limit, long todayUsed) {
    }

    /**
     * 纯判定逻辑（可离线单测）：日累计未达上限即放行
     */
    static boolean quotaAllows(long todayUsed, int dailyLimit) {
        if (dailyLimit <= 0) {
            return true;
        }
        return todayUsed < dailyLimit;
    }

    /**
     * 配额检查（执行前调用）
     *
     * @param scene       场景代码
     * @param dailyLimit  日 Token 上限（null/<=0 视为不限）
     */
    public QuotaResult checkQuota(String scene, Integer dailyLimit) {
        int limit = dailyLimit != null ? dailyLimit : 0;
        if (limit <= 0) {
            return new QuotaResult(true, limit, 0);
        }
        try {
            String val = redisTemplate.opsForValue().get(key(scene));
            long used = 0;
            if (val != null && !val.isBlank()) {
                try {
                    used = Long.parseLong(val.trim());
                } catch (NumberFormatException e) {
                    used = 0;
                }
            }
            return new QuotaResult(quotaAllows(used, limit), limit, used);
        } catch (Exception e) {
            log.warn("[ai2:cost] 配额检查异常（放行）: scene={}, {}", scene, e.getMessage());
            return new QuotaResult(true, limit, 0);
        }
    }

    /**
     * 消费累计（调用成功后按实际 token 累加；失败/未回传 token 不计）
     */
    public void consume(String scene, Integer tokenUsed) {
        if (tokenUsed == null || tokenUsed <= 0) {
            return;
        }
        String key = key(scene);
        try {
            Long count = redisTemplate.opsForValue().increment(key, tokenUsed.longValue());
            if (count != null && count == tokenUsed.longValue()) {
                // 该键首次写入：设置 TTL（键含日期，2 天足够跨日清理）
                redisTemplate.expire(key, Duration.ofDays(2));
            }
        } catch (Exception e) {
            log.warn("[ai2:cost] Token累计失败（不影响业务）: scene={}, {}", scene, e.getMessage());
        }
    }

    private String key(String scene) {
        return "ai2:token:" + scene + ":" + LocalDate.now().format(DAY_FMT);
    }
}
