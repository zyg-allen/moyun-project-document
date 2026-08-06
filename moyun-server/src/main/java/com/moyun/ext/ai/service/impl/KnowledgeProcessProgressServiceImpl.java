package com.moyun.ext.ai.service.impl;

import com.moyun.ext.ai.constant.RedisKeys;
import com.moyun.ext.ai.service.KnowledgeProcessProgressService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 知识库处理进度管理服务实现类
 *
 * <p>基于Redis实现的进度管理，提供：
 * <ul>
 *   <li>实时更新处理进度</li>
 *   <li>刷新页面后进度不丢失</li>
 *   <li>分布式锁防止重复处理</li>
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
        Boolean success = redisTemplate.opsForValue().setIfAbsent(
            lockKey,
            Thread.currentThread().getName(),
            RedisKeys.KNOWLEDGE_LOCK_EXPIRE_SECONDS,
            TimeUnit.SECONDS
        );

        if (Boolean.TRUE.equals(success)) {
            log.info("获取处理锁成功 - ID={}, 线程={}", knowledgeId, Thread.currentThread().getName());
            return true;
        } else {
            log.warn("处理锁已被占用 - ID={}, 当前持有者={}",
                    knowledgeId, redisTemplate.opsForValue().get(lockKey));
            return false;
        }
    }

    @Override
    public void releaseLock(Long knowledgeId) {
        String lockKey = RedisKeys.knowledgeLock(knowledgeId);
        redisTemplate.delete(lockKey);
        log.info("释放处理锁 - ID={}", knowledgeId);
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
