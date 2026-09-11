package com.moyun.ext.ai2.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 场景日 Token 成本熔断单元测试（v11.57 P0-2）
 *
 * <p>覆盖：纯判定逻辑（不限/未达上限/达限/超限）、配额检查（Redis 正常值/脏数据/异常放行）、
 * 消费累计（空值跳过/首次写入设置 TTL/非首次不重设/异常不抛出）。</p>
 */
@ExtendWith(MockitoExtension.class)
class TokenCostGuardTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TokenCostGuard tokenCostGuard;

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
    void consume_firstWrite_shouldSetTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString(), eq(3000L))).thenReturn(3000L);

        tokenCostGuard.consume("voice_interview", 3000);

        verify(valueOperations).increment(anyString(), eq(3000L));
        verify(redisTemplate).expire(anyString(), any(Duration.class));
    }

    @Test
    void consume_subsequentWrite_shouldNotResetTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString(), eq(3000L))).thenReturn(8000L);

        tokenCostGuard.consume("voice_interview", 3000);

        verify(valueOperations).increment(anyString(), eq(3000L));
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    void consume_redisFailure_shouldNotThrow() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString(), anyLong())).thenThrow(new RedisConnectionFailureException("down"));

        tokenCostGuard.consume("voice_interview", 3000);
        // 不抛异常即通过：累计失败不影响业务主流程
    }
}
