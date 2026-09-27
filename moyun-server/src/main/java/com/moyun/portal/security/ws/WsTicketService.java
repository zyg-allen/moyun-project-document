package com.moyun.portal.security.ws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket 握手**一次性短时效票据**（v13.21）
 *
 * <h3>为什么需要它</h3>
 * <p>浏览器的 {@code new WebSocket(url)} **不能自定义请求头**，所以 WebSocket 握手没法像普通请求那样带
 * {@code Authorization}。历史实现只能把 token 塞进 URL（{@code /ws-asr?token=xxx}），于是门户 JWT
 * 会进入 <b>Nginx access log / 浏览器历史 / 代理日志</b>——而 JWT 在有效期内可重放，泄漏即等于账号被接管。</p>
 *
 * <h3>票据语义</h3>
 * <ul>
 *   <li>{@link #issue(Long)}：为已登录用户签发一张随机票据（{@code SecureRandom} 32 hex），
 *       在 Redis 里存 {@code userId}，**TTL 60 秒**；</li>
 *   <li>{@link #consume(String)}：**原子取并删**（Lua GET+DEL），因此票据**只能用一次**——
 *       即使它被写进访问日志，重放也拿不到身份；</li>
 *   <li>密钥不会出现在 URL 之外的任何地方：客户端先走普通 HTTP（带 {@code Authorization}）换取票据，
 *       再用票据建连。</li>
 * </ul>
 *
 * <p><b>残留风险（如实记录）</b>：票据本身仍出现在 URL 里，所以"访问日志留痕"依旧存在，但
 * 已经**一次性 + 60 秒**，泄漏价值从"账号接管"降到"极短窗口内的一次握手"。要彻底消除需改用
 * 子协议传参（{@code Sec-WebSocket-Protocol}，依赖服务端回显与代理转发）或 Cookie 会话，
 * 属独立议题。</p>
 *
 * @author moyun
 */
@Slf4j
@Service
public class WsTicketService {

    /** 票据有效期（秒）：够客户端拿到后立刻建连；越短越安全 */
    public static final long TTL_SECONDS = 60;

    private static final String KEY_PREFIX = "ws:ticket:";

    /** 原子"取并删"：保证一次性（GETDEL 需 Redis 6.2+，用 Lua 兼容更老的版本） */
    private static final RedisScript<String> CONSUME_SCRIPT = RedisScript.of(
            "local v = redis.call('GET', KEYS[1]) "
                    + "if v then redis.call('DEL', KEYS[1]) end "
                    + "return v",
            String.class);

    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 签发一张一次性票据
     *
     * @param userId 门户用户ID（必须非空）
     * @return 票据字符串（32 位 hex，不可猜测）
     */
    public String issue(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("签发 WS 票据需要门户用户ID");
        }
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        String ticket = HexFormat.of().formatHex(bytes);
        redisTemplate.opsForValue().set(KEY_PREFIX + ticket, String.valueOf(userId), TTL_SECONDS, TimeUnit.SECONDS);
        log.debug("[ws-ticket] 已签发 userId={} ttl={}s", userId, TTL_SECONDS);
        return ticket;
    }

    /**
     * 消费票据（原子取并删，只能成功一次）
     *
     * @param ticket 票据（可为 null/空白）
     * @return 用户ID；票据不存在、已用过、已过期返回 null
     */
    public Long consume(String ticket) {
        if (ticket == null || ticket.isBlank()) {
            return null;
        }
        String value = redisTemplate.execute(CONSUME_SCRIPT, List.of(KEY_PREFIX + ticket));
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            log.warn("[ws-ticket] 票据值不是合法用户ID：{}", value);
            return null;
        }
    }
}
