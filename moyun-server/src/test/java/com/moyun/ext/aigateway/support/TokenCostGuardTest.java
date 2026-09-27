package com.moyun.ext.aigateway.support;

import com.moyun.ext.aigateway.support.TokenCostGuard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 场景日 Token 成本熔断单元测试（v11.57 P0-2）
 *
 * <p>覆盖：纯判定逻辑（不限/未达上限/达限/超限）、配额检查（Redis 正常值/脏数据/异常放行）、
 * 消费累计（空值跳过/走原子脚本路径/异常不抛出）。</p>
 *
 * <p><b>消费累计</b>：累加与"首次设 TTL"已合并为单次 Lua 脚本调用（替代原先 INCR+EXPIRE
 * 两步，两步之间崩溃会留下永不过期的键）。代码侧把泛型 varargs 的
 * {@code execute(RedisScript, List, Object...)} 包装为固定参数方法 {@code incrWithTtl(key, delta)}
 * ——Mockito 对泛型 varargs 的匹配不可靠——故此处以 spy 验证走的是该原子路径。</p>
 */
@ExtendWith(MockitoExtension.class)
class TokenCostGuardTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    /** 非 spy 实例：用于 checkQuota 等只读场景（@InjectMocks 等价） */
    private TokenCostGuard tokenCostGuard;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        tokenCostGuard = new TokenCostGuard(redisTemplate);
    }

    // ==================== quotaAllows：纯判定逻辑 ====================

    @Test
    void quotaAllows_zeroOrNegativeLimit_shouldAlwaysAllow() {
        assertTrue(TokenCostGuard.quotaAllows(999999L, 0));
        assertTrue(TokenCostGuard.quotaAllows(999999L, -1));
    }

    @Test
    void quotaAllows_belowLimit_shouldAllow() {
        assertTrue(TokenCostGuard.quotaAllows(9999L, 10000));
        assertTrue(TokenCostGuard.quotaAllows(0L, 10000));
    }

    @Test
    void quotaAllows_reachOrExceedLimit_shouldDeny() {
        assertFalse(TokenCostGuard.quotaAllows(10000L, 10000));
        assertFalse(TokenCostGuard.quotaAllows(10001L, 10000));
    }

    // ==================== checkQuota ====================

    @Test
    void checkQuota_nullLimit_shouldAllowWithoutRedis() {
        TokenCostGuard.QuotaResult r = tokenCostGuard.checkQuota("voice_interview", null);
        assertTrue(r.allowed());
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void checkQuota_zeroLimit_shouldAllowWithoutRedis() {
        TokenCostGuard.QuotaResult r = tokenCostGuard.checkQuota("voice_interview", 0);
        assertTrue(r.allowed());
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void checkQuota_belowLimit_shouldAllow() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("5000");

        TokenCostGuard.QuotaResult r = tokenCostGuard.checkQuota("voice_interview", 10000);
        assertTrue(r.allowed());
        assertEquals(10000, r.limit());
        assertEquals(5000L, r.todayUsed());
    }

    @Test
    void checkQuota_reachLimit_shouldDeny() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("10000");

        TokenCostGuard.QuotaResult r = tokenCostGuard.checkQuota("voice_interview", 10000);
        assertFalse(r.allowed());
        assertEquals(10000, r.limit());
        assertEquals(10000L, r.todayUsed());
    }

    @Test
    void checkQuota_dirtyRedisValue_shouldTreatAsZero() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("not-a-number");

        TokenCostGuard.QuotaResult r = tokenCostGuard.checkQuota("voice_interview", 100);
        assertTrue(r.allowed());
        assertEquals(0L, r.todayUsed());
    }

    @Test
    void checkQuota_redisFailure_shouldFailOpen() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenThrow(new RedisConnectionFailureException("down"));

        // Redis 抖动时熔断降级为不限，避免基础设施故障阻断业务
        TokenCostGuard.QuotaResult r = tokenCostGuard.checkQuota("voice_interview", 100);
        assertTrue(r.allowed());
    }

    // ==================== consume ====================

    @Test
    void consume_nullOrNonPositiveToken_shouldSkip() {
        tokenCostGuard.consume("voice_interview", null);
        tokenCostGuard.consume("voice_interview", 0);
        tokenCostGuard.consume("voice_interview", -5);
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void consume_shouldUseAtomicIncrWithTtl() {
        // 核心不变量：累加与"首次设 TTL"必须在**同一次脚本调用**中完成。
        // 历史实现分两步（INCRBY 后单独 EXPIRE），若两步之间进程崩溃/连接中断，
        // 键会永久无 TTL → 该场景日额度永不重置且内存累积。
        // 代码侧已把 varargs 的 execute(...) 包装为固定参数方法 incrWithTtl(key, delta)，
        // 故此处用 spy 验证"走的是该原子路径"，并断言不再出现两步式命令。
        TokenCostGuard spy = spy(new TokenCostGuard(redisTemplate));
        doReturn(3L).when(spy).incrWithTtl(anyString(), anyInt());

        spy.consume("voice_interview", 3);

        verify(spy).incrWithTtl(anyString(), anyInt());
        verify(valueOperations, never()).increment(anyString(), anyLong());
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    void consume_passesTokenDeltaToScript() {
        TokenCostGuard spy = spy(new TokenCostGuard(redisTemplate));
        doReturn(3000L).when(spy).incrWithTtl(anyString(), anyInt());

        spy.consume("voice_interview", 3000);

        ArgumentCaptor<Integer> deltaCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(spy).incrWithTtl(anyString(), deltaCaptor.capture());
        assertEquals(3000, deltaCaptor.getValue(), "应把本次实际 token 数作为累加增量");
    }

    @Test
    void consume_redisFailure_shouldNotThrow() {
        TokenCostGuard spy = spy(new TokenCostGuard(redisTemplate));
        doThrow(new RedisConnectionFailureException("down"))
                .when(spy).incrWithTtl(anyString(), anyInt());

        spy.consume("voice_interview", 3000);
        // 不抛异常即通过：累计失败不影响业务主流程
    }
}
