package com.moyun.ext.cms.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.portal.domain.entity.PortalAiTask;
import com.moyun.portal.mapper.PortalAiTaskMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用 AI 异步任务执行器（v10.23）
 *
 * <p><strong>独立 Bean 设计原因</strong>：Spring 的 {@code @Async} 通过 AOP 代理生效，
 * 同一类内部自调用不会触发代理。把 {@code @Async} 方法抽到独立 Bean，由
 * {@link AiTaskService} 注入后调用，保证异步线程池真正生效。</p>
 *
 * <p>执行流程：pending（已提交）→ running（执行中）→ success/failed（终态）。
 * 按任务类型分派到对应 {@link AiTaskHandler}，Handler 返回对象序列化为 JSON 存入
 * {@code portal_ai_task.result}；失败原因回写 error 字段（超长截断 900 字符，
 * 列宽 1000）。</p>
 *
 * @author moyun
 */
@Service
public class AiTaskAsyncExecutor {

    private static final Logger log = LoggerFactory.getLogger(AiTaskAsyncExecutor.class);

    /** 失败原因最大长度（对应 error 列 varchar(1000)，留余量） */
    private static final int MAX_ERROR_LENGTH = 900;

    @Autowired
    private PortalAiTaskMapper aiTaskMapper;

    @Autowired
    private ObjectMapper objectMapper;

    /** 任务类型 → Handler 映射（Spring 自动收集所有实现） */
    private final Map<String, AiTaskHandler> handlerMap = new HashMap<>();

    public AiTaskAsyncExecutor(List<AiTaskHandler> handlers) {
        for (AiTaskHandler handler : handlers) {
            handlerMap.put(handler.taskType(), handler);
        }
    }

    /**
     * 异步执行 AI 任务（独立线程池 {@code aiTaskExecutor}，不阻塞 HTTP 请求线程）
     *
     * @param taskId   任务ID
     * @param userId   任务所属用户ID（异步上下文无 SecurityContext，显式传入）
     * @param taskType 任务类型（分派到对应 AiTaskHandler）
     * @param bizRef   业务参数 JSON
     */
    @Async("aiTaskExecutor")
    public void execute(Long taskId, Long userId, String taskType, JsonNode bizRef) {
        AiTaskHandler handler = handlerMap.get(taskType);
        if (handler == null) {
            // 提交后注册表被改动等极端场景，直接置为失败
            log.warn("[AiTask] 任务类型 {} 无对应 Handler，taskId={}", taskType, taskId);
            updateFailed(taskId, "未知的AI任务类型：" + taskType);
            return;
        }

        // 状态流转：pending → running
        PortalAiTask running = new PortalAiTask();
        running.setId(taskId);
        running.setStatus("running");
        running.setProgressMsg("任务执行中");
        running.setUpdateTime(LocalDateTime.now());
        aiTaskMapper.updateById(running);
        log.info("[AiTask] 任务开始执行 taskId={} taskType={} userId={}", taskId, taskType, userId);

        try {
            Object result = handler.execute(userId, bizRef);
            PortalAiTask success = new PortalAiTask();
            success.setId(taskId);
            success.setStatus("success");
            success.setProgressMsg("任务完成");
            success.setFinishTime(LocalDateTime.now());
            success.setUpdateTime(LocalDateTime.now());
            if (result != null) {
                success.setResult(objectMapper.writeValueAsString(result));
            }
            aiTaskMapper.updateById(success);
            log.info("[AiTask] 任务执行成功 taskId={} taskType={}", taskId, taskType);
        } catch (Exception e) {
            log.warn("[AiTask] 任务执行失败 taskId={} taskType={}", taskId, taskType, e);
            updateFailed(taskId, e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }

    /** 回写任务失败状态（error 超长截断 900 字符） */
    private void updateFailed(Long taskId, String errorMsg) {
        if (errorMsg != null && errorMsg.length() > MAX_ERROR_LENGTH) {
            errorMsg = errorMsg.substring(0, MAX_ERROR_LENGTH);
        }
        PortalAiTask failed = new PortalAiTask();
        failed.setId(taskId);
        failed.setStatus("failed");
        failed.setError(errorMsg);
        failed.setFinishTime(LocalDateTime.now());
        failed.setUpdateTime(LocalDateTime.now());
        aiTaskMapper.updateById(failed);
    }
}
