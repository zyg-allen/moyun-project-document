package com.moyun.core.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁工具（Redis + Lua，owner 校验 + 可选看门狗续期）
 *
 * <p>本工具把正确用法收口为唯一入口，避免各处重复手写：</p>
 * <ol>
 *   <li><b>加锁</b>：{@code SET key token NX PX ttl}，token 为进程内唯一值
 *       （{@code host:thread:uuid}），保证可判定归属；</li>
 *   <li><b>解锁</b>：Lua 脚本原子地「比较 token 再删除」，非持有者删除无效；</li>
 *   <li><b>续期</b>：Lua 脚本原子地「比较 token 再续 TTL」，供看门狗使用；</li>
 *   <li><b>看门狗</b>：{@link #lockWithWatchdog} 在持有期间按 TTL/3 周期续期，
 *       避免业务执行时间超过 TTL 导致锁提前失效（并发进入临界区）。</li>
 * </ol>
 *
 * <p><b>使用约定</b></p>
 * <pre>{@code
 * // 1) 非阻塞尝试（抢不到即跳过，适合定时任务、幂等消费）
 * try (var lock = lockUtil.tryLock("ledger:schedule:execute", Duration.ofMinutes(5))) {
 *     if (lock == null) { return; }   // 别的实例正在跑，本实例直接跳过
 *     doWork();
 * }
 *
 * // 2) 阻塞等待
 * DistributedLockUtil.Lock lock = lockUtil.lock("key", Duration.ofSeconds(30), Duration.ofSeconds(3));
 * if (lock != null) { try { doWork(); } finally { lockUtil.unlock(lock); } }
 * }</pre>
 *
 * <p><b>注意</b>：本工具是「单 Redis 实例/主从」语义下的互斥。Redis 主从切换期间
 * 存在理论上的双持有窗口（Redlock 亦无法完全消除）；对绝对互斥有要求的场景
 * 必须叠加业务侧幂等（唯一键 / 条件更新），不可仅依赖锁。</p>
 *
 * @author moyun
 * @since 2026-09-26
 */
@Slf4j
@Component
public class DistributedLockUtil {

    /** 解锁：仅当 token 匹配才删除（原子） */
    private static final RedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('del', KEYS[1]) "
                    + "else return 0 end",
            Long.class);

    /** 续期：仅当 token 匹配才刷新 TTL（原子） */
    private static final RedisScript<Long> RENEW_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('pexpire', KEYS[1], ARGV[2]) "
                    + "else return 0 end",
            Long.class);

    /** 实例标识：区分不同 JVM，便于排查"锁被谁持有" */
    private static final String INSTANCE_ID = buildInstanceId();

    private final StringRedisTemplate redisTemplate;

    /** 看门狗调度器（懒加载；无看门狗需求时不创建线程） */
    private volatile ScheduledExecutorService watchdog;
    private final Object watchdogLock = new Object();

    /** token → 续期任务，用于解锁时取消 */
    private final ConcurrentHashMap<String, ScheduledFuture<?>> renewTasks = new ConcurrentHashMap<>();

    public DistributedLockUtil(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ==================== 锁句柄 ====================

    /**
     * 已持有的锁句柄。{@link #close()} 等价于 {@link #unlock(Lock)}，
     * 故可用于 try-with-resources。
     */
    public static final class Lock implements AutoCloseable {
        private final String key;
        private final String token;
        private final long ttlMillis;
        private final DistributedLockUtil owner;

        private Lock(DistributedLockUtil owner, String key, String token, long ttlMillis) {
            this.owner = owner;
            this.key = key;
            this.token = token;
            this.ttlMillis = ttlMillis;
        }

        public String key() {
            return key;
        }

        public String token() {
            return token;
        }

        public long ttlMillis() {
            return ttlMillis;
        }

        @Override
        public void close() {
            owner.unlock(this);
        }
    }

    // ==================== 加锁 ====================

    /**
     * 非阻塞加锁：立即返回，抢不到返回 {@code null}。
     *
     * @param key       锁键（建议带业务前缀，如 {@code "ledger:schedule:run"}）
     * @param ttl       锁自动过期时间，必须为正
     * @return 锁句柄；未获取到返回 {@code null}
     */
    public Lock tryLock(String key, java.time.Duration ttl) {
        return tryLock(key, ttl, null);
    }

    /**
     * 非阻塞加锁（带看门狗）：持有期间按 {@code ttl/3} 周期自动续期。
     *
     * <p>适用于执行时间不确定、可能超过 TTL 的临界区。业务正常结束（{@code close}）
     * 或 JVM 退出时续期自动停止。</p>
     */
    public Lock tryLockWithWatchdog(String key, java.time.Duration ttl) {
        return tryLock(key, ttl, ttl);
    }

    private Lock tryLock(String key, java.time.Duration ttl, java.time.Duration watchdogPeriod) {
        if (redisTemplate == null) {
            // 无 Redis 环境（如未启用的模块）：降级为"总是成功"，但明确告警，
            // 避免静默地把互斥语义丢掉却不自知
            log.warn("[distlock] Redis 未配置，锁 {} 降级为无效锁（无互斥保证）", key);
            return new Lock(this, key, tokenOf(), 0L);
        }
        long ttlMillis = validTtl(ttl);
        String token = tokenOf();
        try {
            Boolean ok = redisTemplate.opsForValue()
                    .setIfAbsent(key, token, ttlMillis, TimeUnit.MILLISECONDS);
            if (!Boolean.TRUE.equals(ok)) {
                return null;
            }
            Lock lock = new Lock(this, key, token, ttlMillis);
            if (watchdogPeriod != null) {
                startWatchdog(lock, watchdogPeriod);
            }
            return lock;
        } catch (Exception e) {
            // Redis 故障：锁语义无法保证。此处选择 fail-open（返回 null 之外不再抛），
            // 由调用方决定"抢不到就跳过"。调用方若要求 fail-closed，应自行捕获并中断。
            log.warn("[distlock] 加锁异常（视为未获取）key={}: {}", key, e.getMessage());
            return null;
        }
    }

    /**
     * 阻塞加锁：在 {@code waitTime} 内轮询尝试，超时返回 {@code null}。
     *
     * @param key      锁键
     * @param ttl      锁自动过期时间
     * @param waitTime 最大等待时间
     * @return 锁句柄；等待超时返回 {@code null}
     */
    public Lock lock(String key, java.time.Duration ttl, java.time.Duration waitTime) {
        if (redisTemplate == null) {
            return tryLock(key, ttl);
        }
        long deadline = System.nanoTime() + waitTime.toNanos();
        do {
            Lock lock = tryLock(key, ttl);
            if (lock != null) {
                return lock;
            }
            try {
                Thread.sleep(50L);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return null;
            }
        } while (System.nanoTime() < deadline);
        return null;
    }

    // ==================== 解锁 / 续期 ====================

    /**
     * 解锁：仅当锁仍归本 token 所有时才删除。
     *
     * <p>即便因为 GC 停顿 / 业务超时导致锁已过期并被他人获取，
     * 本次解锁也不会误删他人的锁。</p>
     *
     * @return true=确实释放了本持有者的锁；false=锁已不属于本持有者（已过期或被他人获取）
     */
    public boolean unlock(Lock lock) {
        if (lock == null) {
            return false;
        }
        stopWatchdog(lock.token());
        if (redisTemplate == null) {
            return true;
        }
        try {
            Long released = redisTemplate.execute(
                    UNLOCK_SCRIPT, Collections.singletonList(lock.key()), lock.token());
            boolean ok = released != null && released > 0;
            if (!ok) {
                log.warn("[distlock] 解锁时锁已不属于当前持有者（可能已过期或被他人获取）key={}", lock.key());
            }
            return ok;
        } catch (Exception e) {
            log.warn("[distlock] 解锁异常 key={}: {}", lock.key(), e.getMessage());
            return false;
        }
    }

    /**
     * 续期：仅当锁仍归本 token 所有时刷新 TTL。返回 false 说明锁已丢失，
     * 调用方应视为「已失去互斥」并尽快中止/回滚临界区工作。
     */
    public boolean renew(Lock lock, java.time.Duration ttl) {
        if (lock == null || redisTemplate == null) {
            return false;
        }
        long ttlMillis = validTtl(ttl);
        try {
            Long renewed = redisTemplate.execute(RENEW_SCRIPT,
                    Collections.singletonList(lock.key()), lock.token(), String.valueOf(ttlMillis));
            return renewed != null && renewed > 0;
        } catch (Exception e) {
            log.warn("[distlock] 续期异常 key={}: {}", lock.key(), e.getMessage());
            return false;
        }
    }

    /** 当前锁是否仍由本持有者持有（排查用，非互斥判定依据） */
    public boolean isHeldBy(Lock lock) {
        if (lock == null || redisTemplate == null) {
            return false;
        }
        try {
            String current = redisTemplate.opsForValue().get(lock.key());
            return lock.token().equals(current);
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== 看门狗 ====================

    private void startWatchdog(Lock lock, java.time.Duration period) {
        long periodMillis = Math.max(1000L, period.toMillis() / 3);
        ScheduledFuture<?> task = watchdog().scheduleWithFixedDelay(() -> {
            try {
                if (!renew(lock, java.time.Duration.ofMillis(lock.ttlMillis()))) {
                    // 锁已丢失：停止续期并告警（业务侧应通过 renew 返回值或此日志感知）
                    log.error("[distlock] 看门狗续期失败，锁已丢失 key={}（临界区可能已被他人进入）", lock.key());
                    stopWatchdog(lock.token());
                }
            } catch (Exception e) {
                log.warn("[distlock] 看门狗续期异常 key={}: {}", lock.key(), e.getMessage());
            }
        }, periodMillis, periodMillis, TimeUnit.MILLISECONDS);
        renewTasks.put(lock.token(), task);
    }

    private void stopWatchdog(String token) {
        ScheduledFuture<?> task = renewTasks.remove(token);
        if (task != null) {
            task.cancel(false);
        }
    }

    private ScheduledExecutorService watchdog() {
        if (watchdog == null) {
            synchronized (watchdogLock) {
                if (watchdog == null) {
                    watchdog = Executors.newScheduledThreadPool(1, r -> {
                        Thread t = new Thread(r, "distlock-watchdog");
                        t.setDaemon(true);
                        return t;
                    });
                }
            }
        }
        return watchdog;
    }

    // ==================== 内部 ====================

    private static long validTtl(java.time.Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("锁 TTL 必须为正数");
        }
        return ttl.toMillis();
    }

    private static String tokenOf() {
        return INSTANCE_ID + ":" + Thread.currentThread().getName() + ":" + UUID.randomUUID();
    }

    private static String buildInstanceId() {
        String host;
        try {
            host = java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            host = "unknown-host";
        }
        return host;
    }
}
