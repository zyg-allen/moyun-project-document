package com.moyun.ext.ai.service.impl;

import com.moyun.core.redis.DistributedLockUtil;
import com.moyun.ext.ai.constant.RedisKeys;
import com.moyun.ext.ai.service.KnowledgeProcessProgressService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 知识库处理进度管理服务实现类
 *
 * <p>基于Redis实现的进度管理，提供：
 * <ul>
 *   <li>实时更新处理进度</li>
 *   <li>刷新页面后进度不丢失</li>
 *   <li>分布式锁防止重复处理（{@link DistributedLockUtil}，owner 校验）</li>
 *   <li>支持应用重启后状态恢复</li>
 * </ul>
 * </p>
 *
 * @author laomao
 */
@Slf4j
@Service
public class KnowledgeProcessProgressServiceImpl implements KnowledgeProcessProgressService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private DistributedLockUtil lockUtil;

    /**
     * 本线程成功持有的锁句柄（按 knowledgeId 索引）。
     *
     * <p>用于让 {@link #releaseLock(Long)} 能校验持有者——接口签名只有 knowledgeId，
     * 而锁归属必须靠 token 判定。保留 {@link ThreadLocal} 是因为加锁/解锁本就在同一
     * 处理线程内成对调用（Spring 定时/异步任务语义）。</p>
     *
     * <p><b>注意</b>：若加锁与解锁发生在不同线程（如加锁后交给线程池执行），
     * 需改为显式传递 {@code Lock} 句柄，不能依赖本 ThreadLocal。</p>
     */
    private final ThreadLocal<java.util.Map<Long, DistributedLockUtil.Lock>> heldLocks =
            ThreadLocal.withInitial(java.util.HashMap::new);

    @Override
    public void updateProgress(Long knowledgeId, int progress, String message, String currentStep) {
        String key = RedisKeys.knowledgeProgress(knowledgeId);

        ProcessProgress progressInfo = new ProcessProgress();
        progressInfo.setKnowledgeId(knowledgeId);
        progressInfo.setProgress(Math.min(100, Math.max(0, progress)));
        progressInfo.setMessage(message);
        progressInfo.setCurrentStep(currentStep);
        progressInfo.setUpdateTime(System.currentTimeMillis());

        redisTemplate.opsForValue().set(key, progressInfo, RedisKeys.KNOWLEDGE_PROGRESS_EXPIRE_HOURS, TimeUnit.HOURS);
        log.debug("更新进度 - ID={}, 进度={}%, 步骤={}", knowledgeId, progress, currentStep);
    }

    @Override
    public ProcessProgress getProgress(Long knowledgeId) {
        String key = RedisKeys.knowledgeProgress(knowledgeId);
        Object obj = redisTemplate.opsForValue().get(key);
        return obj != null ? (ProcessProgress) obj : null;
    }

    @Override
    public boolean tryLock(Long knowledgeId) {
        String lockKey = RedisKeys.knowledgeLock(knowledgeId);
        // 使用统一分布式锁：token 归属唯一，解锁走 Lua 比较-删除（防误删他人锁）
        DistributedLockUtil.Lock lock = lockUtil.tryLockWithWatchdog(
                lockKey, Duration.ofSeconds(RedisKeys.KNOWLEDGE_LOCK_EXPIRE_SECONDS));
        if (lock == null) {
            log.warn("处理锁已被占用 - ID={}", knowledgeId);
            return false;
        }
        heldLocks.get().put(knowledgeId, lock);
        log.info("获取处理锁成功 - ID={}, 线程={}", knowledgeId, Thread.currentThread().getName());
        return true;
    }

    @Override
    public void releaseLock(Long knowledgeId) {
        DistributedLockUtil.Lock lock = heldLocks.get().remove(knowledgeId);
        if (lock == null) {
            // 非本线程持有的锁：不得删除
            log.warn("释放处理锁跳过：当前线程未持有该锁 - ID={}, 线程={}",
                    knowledgeId, Thread.currentThread().getName());
            return;
        }
        boolean released = lockUtil.unlock(lock);
        log.info("释放处理锁 - ID={}, released={}", knowledgeId, released);
        if (heldLocks.get().isEmpty()) {
            // 防止 ThreadLocal 在池化线程上残留 Map 引用（内存泄漏）
            heldLocks.remove();
        }
    }

    @Override
    public boolean isProcessing(Long knowledgeId) {
        String lockKey = RedisKeys.knowledgeLock(knowledgeId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
    }

    @Override
    public void clearProgress(Long knowledgeId) {
        String progressKey = RedisKeys.knowledgeProgress(knowledgeId);
        redisTemplate.delete(progressKey);
        log.info("清除进度信息 - ID={}", knowledgeId);
    }
}
