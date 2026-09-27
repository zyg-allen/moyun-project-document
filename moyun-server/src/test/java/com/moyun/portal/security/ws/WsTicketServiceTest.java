package com.moyun.portal.security.ws;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * WebSocket 一次性握手票据（v13.21，报告 §6.4「token 走 URL query」）
 *
 * <p>锁定的关键语义：</p>
 * <ol>
 *   <li>票据随机且不可猜测（16 字节 SecureRandom → 32 hex）；</li>
 *   <li>写入 Redis 时**必须带 TTL**（60s）——不能出现"永不失效的握手凭证"；</li>
 *   <li>消费走**原子 Lua（GET + DEL）**，因此只能用一次：第二次拿到 null；</li>
 *   <li>空/空白票据不碰 Redis（避免无意义往返与键探测）。</li>
 * </ol>
 *
 * @author moyun
 */
class WsTicketServiceTest {

    private WsTicketService service;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = new WsTicketService();
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "redisTemplate", redisTemplate);
    }

    @Test
    @DisplayName("签发：32 位 hex 随机票据 + 必须带 60s TTL，值为 userId")
    void issueWritesRandomTicketWithTtl() {
        String first = service.issue(7L);
        String second = service.issue(7L);

        assertEquals(32, first.length(), "16 字节 hex = 32 字符");
        assertTrue(first.matches("[0-9a-f]{32}"), "必须是 hex（不可猜测）");
        assertNotEquals(first, second, "同一用户两次签发的票据必须不同");

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> value = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> ttl = ArgumentCaptor.forClass(Long.class);
        verify(valueOperations, times(2)).set(key.capture(), value.capture(), ttl.capture(), eq(TimeUnit.SECONDS));

        assertEquals(WsTicketService.TTL_SECONDS, ttl.getValue(), "必须设置 TTL（不能是永久凭证）");
        assertEquals("7", value.getValue());
        assertEquals("ws:ticket:" + second, key.getValue());
    }

    @Test
    @DisplayName("签发：userId 为空直接拒绝（不允许匿名票据）")
    void issueRejectsNullUser() {
        assertThrows(IllegalArgumentException.class, () -> service.issue(null));
        verify(valueOperations, never()).set(anyString(), anyString(), any(Long.class), any(TimeUnit.class));
    }

    @Test
    @DisplayName("消费：走原子 Lua（GET+DEL），返回值解析为 userId")
    void consumeUsesAtomicScript() {
        when(redisTemplate.execute(any(RedisScript.class), anyList())).thenReturn("9");

        assertEquals(9L, service.consume("abc123"));

        ArgumentCaptor<List<String>> keys = ArgumentCaptor.forClass(List.class);
        verify(redisTemplate).execute(any(RedisScript.class), keys.capture());
        assertEquals(List.of("ws:ticket:abc123"), keys.getValue(), "必须按前缀 + 票据定位 Redis 键");
    }

    @Test
    @DisplayName("消费：票据不存在/已用过（Lua 返回 null）→ null；值非法也返回 null 而不抛异常")
    void consumeReturnsNullWhenMissingOrMalformed() {
        when(redisTemplate.execute(any(RedisScript.class), anyList())).thenReturn(null);
        assertNull(service.consume("used-once"));

        when(redisTemplate.execute(any(RedisScript.class), anyList())).thenReturn("not-a-number");
        assertNull(service.consume("weird"), "非法值不得抛出异常（握手失败即可）");
    }

    @Test
    @DisplayName("消费：空/空白票据不访问 Redis（防无谓往返与键探测）")
    void consumeSkipsBlank() {
        assertNull(service.consume(null));
        assertNull(service.consume(""));
        assertNull(service.consume("   "));
        verify(redisTemplate, never()).execute(any(RedisScript.class), anyList());
    }
}
