package com.moyun.portal.judge;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * OJ 异步判题 Redis 队列（v8.0）
 * <p>
 * 基于 Redis List 实现：
 * <ul>
 *   <li>生产端 {@link #enqueue(JudgeTask)} 使用 LPUSH 入队；</li>
 *   <li>消费端 {@link #blockingPop(long)} 使用 BLPOP 阻塞拉取，避免空轮询；</li>
 *   <li>队列 key 由 {@link JudgeProperties.Queue#getName()} 配置。</li>
 * </ul>
 * 选择 List + LPUSH/BLPOP 而非 Stream：
 * <ul>
 *   <li>判题为一次性消费，无需保留消息历史；</li>
 *   <li>BLPOP 在多 Worker 下天然公平分发，且重试可由 Worker 重新 LPUSH 实现；</li>
 *   <li>避免引入 Stream 消费组管理复杂度。</li>
 * </ul>
 *
 * @author moyun
 */
@Component
public class JudgeQueueService {

    private static final Logger log = LoggerFactory.getLogger(JudgeQueueService.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private JudgeProperties properties;

    /** 当前队列 key（每次调用读取配置以便运行期热更新） */
    private String queueKey() {
        return properties.getQueue().getName();
    }

    /**
     * 入队判题任务。
     *
     * @param task 判题任务
     */
    public void enqueue(JudgeTask task) {
        if (task == null || task.getSubmissionId() == null) {
            log.warn("[OJ-Queue] 入队任务非法，已忽略: {}", task);
            return;
        }
        task.setEnqueuedAt(System.currentTimeMillis());
        ListOperations<String, Object> ops = redisTemplate.opsForList();
        Long size = ops.leftPush(queueKey(), task);
        log.info("[OJ-Queue] 入队 submissionId={} qid={} lang={} 当前队列长度={}",
                task.getSubmissionId(), task.getQuestionId(), task.getLanguage(),
                size == null ? -1 : size);
    }

    /**
     * 阻塞拉取一个判题任务（BLPOP）。
     *
     * @param timeoutSeconds 阻塞超时秒；超时返回 null（Worker 据此判断是否退出循环）
     * @return 判题任务，超时返回 null
     */
    public JudgeTask blockingPop(long timeoutSeconds) {
        Object raw = redisTemplate.opsForList().rightPop(queueKey(),
                Duration.ofSeconds(timeoutSeconds));
        if (raw == null) {
            return null;
        }
        if (raw instanceof JudgeTask task) {
            return task;
        }
        // 兼容反序列化为 LinkedHashMap 等中间类型：通过 ObjectMapper 二次转换
        try {
            return convertValue(raw, JudgeTask.class);
        } catch (Exception e) {
            log.error("[OJ-Queue] 任务反序列化失败 raw={} err={}", raw, e.getMessage());
            return null;
        }
    }

    /**
     * 查询当前队列长度（监控用）。
     */
    public long size() {
        Long size = redisTemplate.opsForList().size(queueKey());
        return size == null ? 0L : size;
    }

    /**
     * 写入提交结果状态缓存（PENDING 期间用于快速过滤，最终态由 DB 兜底）。
     *
     * @param submissionId 提交 ID
     * @param status       状态码（PENDING / SE 等）
     */
    public void cacheStatus(Long submissionId, String status) {
        if (submissionId == null) return;
        String key = properties.getQueue().getResultKeyPrefix() + submissionId;
        redisTemplate.opsForValue().set(key, status,
                properties.getQueue().getResultTtlSeconds(), TimeUnit.SECONDS);
    }

    /**
     * 读取提交结果状态缓存（命中返回状态码，未命中返回 null）。
     */
    public String readStatus(Long submissionId) {
        if (submissionId == null) return null;
        String key = properties.getQueue().getResultKeyPrefix() + submissionId;
        Object v = redisTemplate.opsForValue().get(key);
        return v == null ? null : v.toString();
    }

    /**
     * 清除状态缓存（结果回写后调用，避免前端误判为 PENDING）。
     */
    public void evictStatus(Long submissionId) {
        if (submissionId == null) return;
        String key = properties.getQueue().getResultKeyPrefix() + submissionId;
        redisTemplate.delete(key);
    }

    private static <T> T convertValue(Object raw, Class<T> target) {
        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
        om.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        return om.convertValue(raw, target);
    }
}
