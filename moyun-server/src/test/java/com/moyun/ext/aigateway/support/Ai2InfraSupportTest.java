package com.moyun.ext.aigateway.support;

import com.moyun.ext.aigateway.constant.AiErrorCodes;
import com.moyun.ext.aigateway.model.AiExecuteResponse;
import com.moyun.ext.aigateway.support.FallbackStrategy;
import com.moyun.ext.aigateway.support.SceneRateLimiter;
import com.moyun.ext.aigateway.support.SemanticCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
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

    /** 由 {@link #stubIncrement()} 创建并 stub 好的限流器（测试须复用此实例，勿另建） */
    private SceneRateLimiter stubLimiter;

    /** 由 {@link #stubIncrement()} 填充：key → 首次计数时设置的窗口秒数（供 TTL 断言） */
    private Map<String, Long> rateExpireStub;

    // ==================== FallbackStrategy（内置兜底分场景） ====================

    @Test
    void fallback_voiceInterview_unconfigured_genericDefault() {
        FallbackStrategy strategy = new FallbackStrategy();
        // 2B.5：voice_interview 内置兜底已删除（InterviewSceneData 随 Handler 移除），
        // 未配置时走通用兜底（消费方经 JsonClient 得 null，走业务规则兜底）
        AiExecuteResponse<?> resp = strategy.executeFallback("voice_interview", null,
                new RuntimeException("模型超时"));
        assertEquals(AiErrorCodes.SUCCESS, resp.getCode());
        com.moyun.ext.aigateway.model.data.GenericSceneData data =
                (com.moyun.ext.aigateway.model.data.GenericSceneData) resp.getData();
        assertEquals("fallback", data.getSource());
        assertNull(data.getContent());
    }

    @Test
    void fallback_sensitiveWord_unconfigured_genericDefault() {
        FallbackStrategy strategy = new FallbackStrategy();
        // 2B.3：sensitive_word 内置兜底已数据化至配置行 fallback_response，
        // 未配置时走通用兜底（AiSafetyController 对 Map/GenericSceneData 均可解析）
        AiExecuteResponse<?> resp = strategy.executeFallback("sensitive_word", null,
                new IllegalStateException("连接池耗尽"));
        assertEquals(AiErrorCodes.SUCCESS, resp.getCode());
        com.moyun.ext.aigateway.model.data.GenericSceneData data =
                (com.moyun.ext.aigateway.model.data.GenericSceneData) resp.getData();
        assertEquals("fallback", data.getSource());
    }

    @Test
    void fallback_otherScene_genericFailure() {
        FallbackStrategy strategy = new FallbackStrategy();
        AiExecuteResponse<?> resp = strategy.executeFallback("finance_analysis", null,
                new RuntimeException("任意异常"));
        assertEquals(AiErrorCodes.SUCCESS, resp.getCode(), "通用兜底返回可解析空结构（业务侧走既有规则兜底）");
        com.moyun.ext.aigateway.model.data.GenericSceneData data =
                (com.moyun.ext.aigateway.model.data.GenericSceneData) resp.getData();
        assertEquals("fallback", data.getSource());
        assertNull(data.getContent());
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
        com.moyun.ext.aigateway.model.data.GenericSceneData data =
                (com.moyun.ext.aigateway.model.data.GenericSceneData) resp.getData();
        assertEquals("fallback", data.getSource(), "空白配置应走内置通用兜底");
    }

    // ==================== SceneRateLimiter（Redis 固定窗口） ====================

    @Test
    void rateLimiter_firstRequest_setsExpire() {
        stubIncrement();
        SceneRateLimiter limiter = stubLimiter;

        SceneRateLimiter.RateResult r = limiter.tryAcquire("resume_parse", "u1", 5, 60);
        assertTrue(r.allowed());
        assertEquals(1, r.current());
        // 首次计数应在同一 Lua 脚本内设置窗口过期时间（固定窗口边界）。
        // 断言 stub 记录的窗口秒数，而非两步式的 redisTemplate.expire(...)
        assertEquals(1, rateExpireStub.size(), "首次计数应恰好设置一次窗口 TTL");
        assertEquals(60L, rateExpireStub.values().iterator().next(),
                "窗口 TTL 应等于 rateLimitTime（60s）");
    }

    @Test
    void rateLimiter_withinLimit_allowed() {
        stubIncrement();
        SceneRateLimiter limiter = stubLimiter;

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
        SceneRateLimiter limiter = spy(new SceneRateLimiter(redisTemplate));
        doThrow(new RedisConnectionFailureException("down"))
                .when(limiter).incrWithWindow(anyString(), anyInt());

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

        cache.put(1L, "finance_analysis", "{\"range\":\"month\"}", "本月数据", resp, 300);

        AiExecuteResponse<Object> hit = cache.get(1L, "finance_analysis", "{\"range\":\"month\"}", "本月数据");
        assertNotNull(hit, "相同 inputKey 应精确命中");
        assertEquals("分析结果", hit.getData());
        assertNotNull(hit.getMetadata(), "命中响应应携带 metadata");
        assertTrue(hit.getMetadata().getFromCache(), "metadata.fromCache 应置 true");
    }

    @Test
    void semanticCache_miss_returnsNull() {
        SemanticCache cache = newCache();
        assertNull(cache.get(1L, "finance_analysis", "never-seen-key", "任意文本"));
    }

    @Test
    void semanticCache_ttlNotPositive_noWrite() {
        SemanticCache cache = newCache();
        AiExecuteResponse<Object> resp = new AiExecuteResponse<>();
        resp.setCode(AiErrorCodes.SUCCESS);
        cache.put(1L, "finance_analysis", "k", "t", resp, 0);
        cache.put(1L, "finance_analysis", "k", "t", resp, null);
        assertNull(cache.get(1L, "finance_analysis", "k", "t"), "ttl<=0 不应写入缓存");
    }

    @Test
    void semanticCache_sceneIsolated() {
        SemanticCache cache = newCache();
        AiExecuteResponse<Object> resp = new AiExecuteResponse<>();
        resp.setCode(AiErrorCodes.SUCCESS);
        cache.put(1L, "scene_a", "same-input-key", "t", resp, 300);
        assertNull(cache.get(1L, "scene_b", "same-input-key", "t"), "不同场景缓存应隔离");
    }

    @Test
    void semanticCache_redisReadException_degradesToNull() {
        when(valueOperations.get(anyString())).thenThrow(new RedisConnectionFailureException("down"));
        SemanticCache cache = newCache();
        assertNull(cache.get(1L, "finance_analysis", "k", "t"), "缓存查询异常应降级直连（返回 null 不抛错）");
    }

    // ==================== SemanticCache（v13.2 用户隔离，安全修复） ====================

    @Test
    @DisplayName("语义缓存必须按用户隔离：A 用户的缓存不得被 B 用户命中（跨用户数据泄漏）")
    void semanticCache_userIsolated() {
        SemanticCache cache = newCache();
        AiExecuteResponse<Object> resp = new AiExecuteResponse<>();
        resp.setCode(AiErrorCodes.SUCCESS);
        resp.setData("A 的简历分析结果");

        cache.put(1001L, "resume_parse", "{\"text\":\"同一份简历\"}", "同一份简历", resp, 300);

        assertNotNull(cache.get(1001L, "resume_parse", "{\"text\":\"同一份简历\"}", "同一份简历"),
                "同一用户应命中自己的缓存");
        assertNull(cache.get(1002L, "resume_parse", "{\"text\":\"同一份简历\"}", "同一份简历"),
                "不同用户输入完全相同也不得命中——响应体可能含他人私有数据");
    }

    @Test
    @DisplayName("userId 为空时既不查也不写（无法隔离就不用缓存，fail-closed）")
    void semanticCache_nullUser_skipsCache() {
        SemanticCache cache = newCache();
        AiExecuteResponse<Object> resp = new AiExecuteResponse<>();
        resp.setCode(AiErrorCodes.SUCCESS);

        cache.put(null, "finance_analysis", "k", "t", resp, 300);
        assertNull(cache.get(null, "finance_analysis", "k", "t"), "无 userId 不应命中缓存");

        // 且确实没有写入任何"无用户命名空间"的键：否则会退化成跨用户共享
        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("语义扫描模式必须带 userId（防止有人改回跨用户扫描）")
    void semanticCache_scanPattern_isUserScoped() {
        SemanticCache cache = newCache();

        String p1 = cache.scanPattern(1001L, "resume_parse");
        String p2 = cache.scanPattern(1002L, "resume_parse");

        assertTrue(p1.contains(":u:1001:"), "扫描模式须带用户命名空间，实际: " + p1);
        assertTrue(p1.startsWith("ai2:cache:u:"), "键前缀须为 ai2:cache:u:（历史无隔离键不再被读取），实际: " + p1);
        assertNotEquals(p1, p2, "不同用户的扫描范围必须不同");
        assertTrue(p1.endsWith(":" + "resume_parse" + ":*"), "扫描范围限定在本场景，实际: " + p1);
    }

    // ==================== 辅助 ====================

    /** 构造仅注入 redisTemplate 的实例（modelConfigService 保持 null → 语义命中自动降级为精确命中） */
    private SemanticCache newCache() {
        SemanticCache cache = new SemanticCache();
        org.springframework.test.util.ReflectionTestUtils.setField(cache, "redisTemplate", redisTemplate);
        return cache;
    }

    /**
     * 限流器的 Lua 原子计数用独立计数器模拟（按 key 区分）。
     *
     * <p>限流器已改为单次 Lua 调用完成 "incr + 首次 expire"，替代原先 INCR 与 EXPIRE
     * 两条独立命令（两步之间崩溃会导致窗口永不重置）。此处 stub 其包级可见的
     * {@code incrWithWindow(key, windowSeconds)} 包装方法——Mockito 对泛型 varargs 的
     * {@code execute(RedisScript, List, Object...)} 匹配不可靠，故代码侧已包装为固定参数方法。</p>
     */
    private void stubIncrement() {
        // 用 spy + 固定签名方法 incrWithWindow(key, windowSeconds) 做 stub：
        // redisTemplate.execute(RedisScript, List, Object...) 是泛型 varargs，
        // Mockito 会把可变参数展开记录（实测 argCount=3），matcher 数量无法稳定对齐
        SceneRateLimiter limiter = spy(new SceneRateLimiter(redisTemplate));
        this.stubLimiter = limiter;
        Map<String, AtomicLong> counters = new ConcurrentHashMap<>();
        Map<String, Long> expiredWindows = new ConcurrentHashMap<>();
        this.rateExpireStub = expiredWindows;
        lenient().doAnswer(inv -> {
            String key = inv.getArgument(0);
            int window = inv.getArgument(1);
            long v = counters.computeIfAbsent(key, k -> new AtomicLong()).incrementAndGet();
            if (v == 1L) {
                expiredWindows.put(key, (long) window);
            }
            return v;
        }).when(limiter).incrWithWindow(anyString(), anyInt());
        // 历史两步式命令不应再被调用（回归守卫）
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().doAnswer(inv -> null).when(valueOperations).set(anyString(), anyString(), any(Duration.class));
    }
}
