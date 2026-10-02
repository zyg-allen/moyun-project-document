package com.moyun.ext.aigateway.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

/**
 * 场景日 Token 成本熔断
 *
 * <p>语义：场景级日累计（所有用户共享额度），保护平台总成本——一个死循环调用或注入攻击
 * 最多烧掉当日配额。参数来自 ai_scene_config.daily_token_limit（null/0=不限）。</p>
 *
 * <p>计数：Redis 原子 INCRBY，键含日期（ai2:token:{scene}:{yyyyMMdd}），跨日自然切换；
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

    /**
     * 原子"累加 + 首次设 TTL"。
     *
     * <p>必须用 Lua 单次往返保证原子性：若用两次独立命令（INCRBY 后判断结果再 EXPIRE），
     * 连接中断，键将**永久无 TTL**：既不会跨日清理，值也永不重置 → 该场景日额度被
     * 一次异常永久占满，且 Redis 内存无限累积。</p>
     *
     * <p>KEYS[1]=计数键，ARGV[1]=增量，ARGV[2]=TTL 秒。返回累加后的值。</p>
     */
    private static final RedisScript<Long> INCR_WITH_TTL_SCRIPT = new DefaultRedisScript<>(
            "local v = redis.call('incrby', KEYS[1], ARGV[1]) "
                    + "if redis.call('ttl', KEYS[1]) < 0 then "
                    + "  redis.call('expire', KEYS[1], ARGV[2]) "
                    + "end "
                    + "return v",
            Long.class);

    /** 计数键 TTL：键含日期，2 天足够跨日清理 */
    private static final Duration KEY_TTL = Duration.ofDays(2);

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
    public static boolean quotaAllows(long todayUsed, int dailyLimit) {
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
            log.warn("[aigateway:cost] 配额检查异常（放行）: scene={}, {}", scene, e.getMessage());
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
        try {
            // 原子：incrby + 首次设 TTL（避免两次命令之间崩溃导致键永久无 TTL）
            incrWithTtl(key(scene), tokenUsed);
        } catch (Exception e) {
            log.warn("[aigateway:cost] Token累计失败（不影响业务）: scene={}, {}", scene, e.getMessage());
        }
    }

    /**
     * 执行"累加 + 首次设 TTL"脚本（包级可见，便于单测 stub）。
     *
     * <p>用实例方法包装 varargs 调用：Mockito 对 {@code execute(RedisScript, List, Object...)}
     * 这类泛型 varargs 的参数匹配不可靠（matcher 与实际调用记录对不上，
     * 表现为 stub 不生效 / verify 报 "Argument(s) are different"），
     * 包装为固定参数的实例方法后即可稳定 stub/verify。</p>
     */
    Long incrWithTtl(String key, int tokenUsed) {
        return redisTemplate.execute(INCR_WITH_TTL_SCRIPT,
                Collections.singletonList(key),
                String.valueOf(tokenUsed), String.valueOf(KEY_TTL.getSeconds()));
    }

    private String key(String scene) {
        return "ai2:token:" + scene + ":" + LocalDate.now().format(DAY_FMT);
    }
}
