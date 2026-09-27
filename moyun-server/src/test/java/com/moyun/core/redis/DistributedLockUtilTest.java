package com.moyun.core.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link DistributedLockUtil} 单测
 *
 * <p>为什么这组测试重要：本工具要修的核心缺陷是
 * <b>"解锁不校验持有者"</b> —— 历史实现（{@code KnowledgeProcessProgressServiceImpl.releaseLock}）
 * 直接 {@code redisTemplate.delete(key)}。当 A 的锁因 TTL 过期、B 随后取得同名锁时，
 * A 结束会删掉 <b>B 的锁</b>，互斥彻底失效。</p>
 *
 * <p>因此本测试锁定的关键行为是：<b>解锁走 Lua「比较 token 再删除」，且脚本返回 0
 * （表示锁已不属于本持有者）时必须判定为"未释放"</b>。这个语义无法靠真实 Redis 单测覆盖
 * （需要构造 TTL 过期 + 他人抢占的时序），故用 Mockito 直接驱动脚本返回值。</p>
 *
 * @author moyun
 */
class DistributedLockUtilTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOps;
    private DistributedLockUtil lockUtil;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        lockUtil = new DistributedLockUtil(redisTemplate);
    }

    // ==================== 加锁 ====================

    @Test
    @DisplayName("加锁成功：走 SET NX PX，且锁句柄携带 token 与 TTL")
    void tryLockSuccess() {
        when(valueOps.setIfAbsent(anyString(), anyString(), eq(30_000L), eq(TimeUnit.MILLISECONDS)))
                .thenReturn(true);

        DistributedLockUtil.Lock lock = lockUtil.tryLock("k1", Duration.ofSeconds(30));

        assertNotNull(lock);
        assertEquals("k1", lock.key());
        assertEquals(30_000L, lock.ttlMillis());
        assertNotNull(lock.token());
        assertFalse(lock.token().isEmpty());
    }

    @Test
    @DisplayName("加锁失败：SET NX 返回 false（他人持有）时返回 null，不抛异常")
    void tryLockContended() {
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(false);

        assertNull(lockUtil.tryLock("k1", Duration.ofSeconds(30)));
    }

    @Test
    @DisplayName("加锁必须带 TTL：零/负 TTL 直接拒绝（防止产生永不过期的死锁）")
    void tryLockRejectsNonPositiveTtl() {
        assertThrows(IllegalArgumentException.class,
                () -> lockUtil.tryLock("k1", Duration.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> lockUtil.tryLock("k1", Duration.ofSeconds(-1)));
        // 不应发生任何 Redis 写操作
        verify(valueOps, never()).setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    // ==================== 解锁（核心：owner 校验） ====================

    @Test
    @DisplayName("解锁语义：用 Lua 脚本按「比较 token 再删除」执行，而非无条件 DEL")
    @SuppressWarnings("unchecked")
    void unlockUsesCompareAndDeleteScript() {
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
        DistributedLockUtil.Lock lock = lockUtil.tryLock("k1", Duration.ofSeconds(30));
        assertNotNull(lock);

        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(1L);

        assertTrue(lockUtil.unlock(lock));

        // 校验下发给 Redis 的脚本确实是"比较后删除"，且 ARGV[1] 就是本锁 token
        ArgumentCaptor<RedisScript<Long>> scriptCaptor = ArgumentCaptor.forClass(RedisScript.class);
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<List> keysCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(redisTemplate).execute(scriptCaptor.capture(), keysCaptor.capture(), argsCaptor.capture());

        String lua = scriptCaptor.getValue().getScriptAsString();
        assertTrue(lua.contains("redis.call('get', KEYS[1])"),
                "脚本必须先读取当前持有者 token：" + lua);
        assertTrue(lua.contains("redis.call('del', KEYS[1])"),
                "脚本必须删除锁：" + lua);
        assertTrue(lua.contains("redis.call('get', KEYS[1]) == ARGV[1]"),
                "删除必须以 token 相等为前提（这正是防误删他人锁的关键）：" + lua);

        assertEquals(List.of("k1"), keysCaptor.getValue());
        assertEquals(lock.token(), argsCaptor.getValue()[0],
                "ARGV[1] 必须是本持有者 token，否则会误删他人锁");
    }

    @Test
    @DisplayName("解锁失败：脚本返回 0（锁已过期或被他人获取）时必须判定为未释放")
    void unlockReportsNotOwned() {
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
        DistributedLockUtil.Lock lock = lockUtil.tryLock("k1", Duration.ofSeconds(30));
        assertNotNull(lock);

        // 模拟：锁已因 TTL 过期后被其他实例获取 → Lua 比较不通过 → 返回 0
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(0L);

        assertFalse(lockUtil.unlock(lock),
                "锁已不属于本持有者时，unlock 必须返回 false（不得声称已释放）");
    }

    @Test
    @DisplayName("解锁工具方法：null 句柄与 Redis 异常均返回 false，不抛出")
    void unlockIsSafeOnNullAndFailure() {
        assertFalse(lockUtil.unlock(null));

        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
        DistributedLockUtil.Lock lock = lockUtil.tryLock("k1", Duration.ofSeconds(30));
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenThrow(new RuntimeException("redis down"));

        assertFalse(lockUtil.unlock(lock), "Redis 故障时解锁应返回 false 而非抛出，避免遮蔽业务异常");
    }

    @Test
    @DisplayName("try-with-resources：close() 等价于 unlock()")
    void closeReleasesLock() {
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(1L);

        try (DistributedLockUtil.Lock lock = lockUtil.tryLock("k1", Duration.ofSeconds(30))) {
            assertNotNull(lock);
        }
        verify(redisTemplate).execute(any(RedisScript.class), anyList(), any(Object[].class));
    }

    // ==================== 续期 / 归属判定 ====================

    @Test
    @DisplayName("续期：以 token 相等为前提刷新 TTL；锁已丢失时返回 false")
    void renewChecksOwnership() {
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
        DistributedLockUtil.Lock lock = lockUtil.tryLock("k1", Duration.ofSeconds(30));

        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(1L);
        assertTrue(lockUtil.renew(lock, Duration.ofSeconds(30)));

        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(0L);
        assertFalse(lockUtil.renew(lock, Duration.ofSeconds(30)), "锁已丢失时必须返回 false 以便调用方中止");
    }

    @Test
    @DisplayName("isHeldBy：仅当 Redis 当前值等于本 token 时为 true")
    void isHeldByComparesToken() {
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
        DistributedLockUtil.Lock lock = lockUtil.tryLock("k1", Duration.ofSeconds(30));

        when(valueOps.get("k1")).thenReturn(lock.token());
        assertTrue(lockUtil.isHeldBy(lock));

        when(valueOps.get("k1")).thenReturn("someone-else-token");
        assertFalse(lockUtil.isHeldBy(lock));
    }

    // ==================== 无 Redis 降级 ====================

    @Test
    @DisplayName("无 Redis 时降级为无效锁：仍返回句柄但不提供互斥保证（并告警）")
    void degradesGracefullyWithoutRedis() {
        DistributedLockUtil noRedis = new DistributedLockUtil(null);

        DistributedLockUtil.Lock lock = noRedis.tryLock("k1", Duration.ofSeconds(30));
        assertNotNull(lock, "降级时应返回句柄让业务继续，而非抛异常中断");
        // 解锁在无 Redis 时视为成功（无锁可释放）
        assertTrue(noRedis.unlock(lock));
        assertFalse(noRedis.renew(lock, Duration.ofSeconds(30)));
    }
}
