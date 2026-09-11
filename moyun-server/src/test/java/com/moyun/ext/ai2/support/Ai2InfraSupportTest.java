package com.moyun.ext.ai2.support;

import com.moyun.ext.ai2.constant.AiErrorCodes;
import com.moyun.ext.ai2.model.AiExecuteResponse;
import com.moyun.ext.ai2.model.data.InterviewSceneData;
import com.moyun.ext.ai2.model.data.SensitiveWordSceneData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AI 网关基础设施单测（v11.60 P0-4 第三组：FallbackStrategy / SceneRateLimiter / SemanticCache）
 *
 * <p>FallbackStrategy 纯逻辑直测；限流器/缓存 mock RedisTemplate（内存 Map 模拟 get/set、
 * AtomicLong 模拟 increment）验证固定窗口计数、Redis 异常放行、精确命中往返。</p>
 *
 * @author laomao
 * @since 2026-09-11
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class Ai2InfraSupportTest {

    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    // ==================== FallbackStrategy（内置兜底分场景） ====================

    @Test
    void fallback_voiceInterview_builtin() {
        FallbackStrategy strategy = new FallbackStrategy();
        AiExecuteResponse<?> resp = strategy.executeFallback("voice_interview", null,
                new RuntimeException("模型超时"));
        assertEquals(AiErrorCodes.SUCCESS, resp.getCode(), "降级响应应对调用方可解析（SUCCESS）");
        InterviewSceneData data = (InterviewSceneData) resp.getData();
        assertEquals("end", data.getNextAction(), "面试场景降级应结束面试而非卡死");
        assertTrue(data.getEvaluation().contains("不可用"));
    }

    @Test
    void fallback_sensitiveWord_safeDefault() {
        FallbackStrategy strategy = new FallbackStrategy();
        AiExecuteResponse<?> resp = strategy.executeFallback("sensitive_word", null,
                new IllegalStateException("连接池耗尽"));
        SensitiveWordSceneData data = (SensitiveWordSceneData) resp.getData();
        assertFalse(data.getHasSensitive(), "内容安全场景降级默认放行（不阻断业务）");
        assertEquals("low", data.getRiskLevel());
    }

    @Test
    void fallback_otherScene_genericFailure() {
        FallbackStrategy strategy = new FallbackStrategy();
        AiExecuteResponse<?> resp = strategy.executeFallback("finance_analysis", null,
                new RuntimeException("任意异常"));
        assertEquals(AiErrorCodes.AI_CALL_FAILED, resp.getCode());
        assertTrue(resp.getMsg().contains("不可用"));
    }

    @Test
    void fallback_configuredJson_takesPriority() {
        FallbackStrategy strategy = new FallbackStrategy();
        AiExecuteResponse<?> resp = strategy.executeFallback("resume_parse",
                "{\"name\":\"张三\",\"skills\":[\"Java\"]}", new RuntimeException("x"));
        assertEquals(AiErrorCodes.SUCCESS, resp.getCode());
        Map<?, ?> data = (Map<?, ?>) resp.getData();
        assertEquals("张三", data.get("name"));
    }

    @Test
    void fallback_configuredInvalidJson_returnsAsText() {
        FallbackStrategy strategy = new FallbackStrategy();
        AiExecuteResponse<?> resp = strategy.executeFallback("daily_topic",
                "抱歉今天生成不了", new RuntimeException("x"));
        assertEquals("抱歉今天生成不了", resp.getData(), "非法 JSON 兜底原样作为文本返回");
    }

    @Test
    void fallback_configuredBlank_usesBuiltin() {
        FallbackStrategy strategy = new FallbackStrategy();
        AiExecuteResponse<?> resp = strategy.executeFallback("sensitive_word", "   ",
                new RuntimeException("x"));
        SensitiveWordSceneData data = (SensitiveWordSceneData) resp.getData();
        assertFalse(data.getHasSensitive(), "空白配置应走内置兜底");
    }

    // ==================== SceneRateLimiter（Redis 固定窗口） ====================

    @Test
    void rateLimiter_firstRequest_setsExpire() {
        stubIncrement();
        SceneRateLimiter limiter = new SceneRateLimiter(redisTemplate);

        SceneRateLimiter.RateResult r = limiter.tryAcquire("resume_parse", "u1", 5, 60);
        assertTrue(r.allowed());
        assertEquals(1, r.current());
        // 首次计数应设置窗口过期时间（固定窗口的窗口边界）
        verify(redisTemplate).expire(anyString(), eq(Duration.ofSeconds(60)));
    }

    @Test
    void rateLimiter_withinLimit_allowed() {
        stubIncrement();
        SceneRateLimiter limiter = new SceneRateLimiter(redisTemplate);

        for (int i = 1; i <= 5; i++) {
            SceneRateLimiter.RateResult r = limiter.tryAcquire("resume_parse", "u1", 5, 60);
            assertTrue(r.allowed(), "第 " + i + " 次应放行");
        }
        SceneRateLimiter.RateResult r6 = limiter.tryAcquire("resume_parse", "u1", 5, 60);
        assertFalse(r6.allowed(), "第 6 次应限流");
        assertEquals(6, r6.current());
        assertEquals(5, r6.limit());
    }

    @Test
    void rateLimiter_limitNotPositive_alwaysAllows() {
        SceneRateLimiter limiter = new SceneRateLimiter(redisTemplate);
        assertTrue(limiter.tryAcquire("s", "u", 0, 60).allowed(), "limit=0 应视为未配置限流");
        assertTrue(limiter.tryAcquire("s", "u", 5, 0).allowed(), "window=0 应视为未配置限流");
    }

    @Test
    void rateLimiter_redisException_failsOpen() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenThrow(new RedisConnectionFailureException("down"));
        SceneRateLimiter limiter = new SceneRateLimiter(redisTemplate);

        SceneRateLimiter.RateResult r = limiter.tryAcquire("resume_parse", "u1", 1, 60);
        assertTrue(r.allowed(), "Redis 异常应放行（限流不阻断业务）");
    }

    // ==================== SemanticCache（精确命中） ====================

    @BeforeEach
    void setUpRedisStore() {
        // 内存 Map 模拟 Redis get/set（put→get 往返）；modelConfigService 未注入 → 语义链路自动降级
        Map<String, String> store = new ConcurrentHashMap<>();
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.get(anyString())).thenAnswer(inv -> store.get(inv.getArgument(0)));
        lenient().doAnswer(inv -> {
            store.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(valueOperations).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void semanticCache_isEnabled_bySceneConfig() {
        SemanticCache cache = new SemanticCache();
        // modelConfigService 字段为 null（未 mock 注入），仅验证开关判定
        assertTrue(cache.isEnabled(1));
        assertFalse(cache.isEnabled(0));
        assertFalse(cache.isEnabled(null));
    }

    @Test
    void semanticCache_putThenGet_exactHit() {
        SemanticCache cache = newCache();
        AiExecuteResponse<Object> resp = new AiExecuteResponse<>();
        resp.setCode(AiErrorCodes.SUCCESS);
        resp.setData("分析结果");

        cache.put("finance_analysis", "{\"range\":\"month\"}", "本月数据", resp, 300);

        AiExecuteResponse<Object> hit = cache.get("finance_analysis", "{\"range\":\"month\"}", "本月数据");
        assertNotNull(hit, "相同 inputKey 应精确命中");
        assertEquals("分析结果", hit.getData());
        assertNotNull(hit.getMetadata(), "命中响应应携带 metadata");
        assertTrue(hit.getMetadata().getFromCache(), "metadata.fromCache 应置 true");
    }

    @Test
    void semanticCache_miss_returnsNull() {
        SemanticCache cache = newCache();
        assertNull(cache.get("finance_analysis", "never-seen-key", "任意文本"));
    }

    @Test
    void semanticCache_ttlNotPositive_noWrite() {
        SemanticCache cache = newCache();
        AiExecuteResponse<Object> resp = new AiExecuteResponse<>();
        resp.setCode(AiErrorCodes.SUCCESS);
        cache.put("finance_analysis", "k", "t", resp, 0);
        cache.put("finance_analysis", "k", "t", resp, null);
        assertNull(cache.get("finance_analysis", "k", "t"), "ttl<=0 不应写入缓存");
    }

    @Test
    void semanticCache_sceneIsolated() {
        SemanticCache cache = newCache();
        AiExecuteResponse<Object> resp = new AiExecuteResponse<>();
        resp.setCode(AiErrorCodes.SUCCESS);
        cache.put("scene_a", "same-input-key", "t", resp, 300);
        assertNull(cache.get("scene_b", "same-input-key", "t"), "不同场景缓存应隔离");
    }

    @Test
    void semanticCache_redisReadException_degradesToNull() {
        when(valueOperations.get(anyString())).thenThrow(new RedisConnectionFailureException("down"));
        SemanticCache cache = newCache();
        assertNull(cache.get("finance_analysis", "k", "t"), "缓存查询异常应降级直连（返回 null 不抛错）");
    }

    // ==================== 辅助 ====================

    /** 构造仅注入 redisTemplate 的实例（modelConfigService 保持 null → 语义命中自动降级为精确命中） */
    private SemanticCache newCache() {
        SemanticCache cache = new SemanticCache();
        org.springframework.test.util.ReflectionTestUtils.setField(cache, "redisTemplate", redisTemplate);
        return cache;
    }

    /** increment 用独立计数器模拟（按 key 区分），首次 set expire 不必验证（put 不依赖） */
    private void stubIncrement() {
        Map<String, AtomicLong> counters = new ConcurrentHashMap<>();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenAnswer(inv -> {
            String key = inv.getArgument(0);
            long v = counters.computeIfAbsent(key, k -> new AtomicLong()).incrementAndGet();
            return v;
        });
        lenient().doAnswer(inv -> null).when(valueOperations).set(anyString(), anyString(), any(Duration.class));
    }
}
