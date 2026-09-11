package com.moyun.ext.cms.service;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.ext.cms.domain.vo.AiTaskVO;
import com.moyun.portal.domain.entity.PortalAiTask;
import com.moyun.portal.mapper.PortalAiTaskMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用 AI 异步任务服务（v10.23）
 *
 * <p>统一门户 LLM 长耗时任务（简历解析/岗位匹配/空字段草稿/深度优化）的提交与查询：
 * 提交时校验任务类型有对应 {@link AiTaskHandler} 实现，同步入库 pending 记录并触发
 * 异步执行，调用方立即拿到任务 ID；前端通过 {@link #getTask} 轮询任务进度与结果。</p>
 *
 * <p>任务类型注册：Spring 自动收集所有 {@link AiTaskHandler} 实现，按
 * {@link AiTaskHandler#taskType()} 构建 handlerMap，新增类型零改动接入。</p>
 *
 * <p>原 portal_resume_optimize_task 表停止写入（代码已切换到 portal_ai_task），
 * 深度优化旧轮询接口由 Controller 做结构映射保持前端兼容。</p>
 *
 * <p><strong>AI 异步任务选型规则（v11.67 双轨制，勿再引入第三套）</strong>：
 * <ul>
 *   <li><strong>表驱动（本服务，portal_ai_task）</strong>：长任务（分钟级）/需审计追溯/
 *       结果需持久化供多次查看（如简历深度优化、岗位匹配）。任务记录永久留痕，
 *       支持失败原因回查与服务重启后孤儿任务恢复。</li>
 *   <li><strong>Redis + 线程池（LedgerAiAnalysisServiceImpl v11.55 模式）</strong>：
 *       短任务（秒级）/结果时效性强无需持久化（如财务分析，任务态 30 分钟 TTL，
 *       报告快照另有落表）。轻量、无表结构与治理开销。</li>
 * </ul>
 * 选型口诀：<strong>要留痕走表，要轻快走 Redis</strong>。OJ 判题（JudgeAsyncWorker）
 * 系代码执行领域专属基础设施（非 LLM 任务），不属于 AI 异步任务体系。</p>
 *
 * @author moyun
 */
@Service
public class AiTaskService {

    private static final Logger log = LoggerFactory.getLogger(AiTaskService.class);

    @Autowired
    private PortalAiTaskMapper aiTaskMapper;

    @Autowired
    private AiTaskAsyncExecutor asyncExecutor;

    @Autowired
    private ObjectMapper objectMapper;

    /** 任务类型 → Handler 映射（Spring 自动收集所有实现） */
    private final Map<String, AiTaskHandler> handlerMap = new HashMap<>();

    public AiTaskService(List<AiTaskHandler> handlers) {
        for (AiTaskHandler handler : handlers) {
            AiTaskHandler prev = handlerMap.put(handler.taskType(), handler);
            if (prev != null) {
                log.warn("[AiTask] 任务类型 {} 存在多个 Handler 实现（{} / {}），后者生效",
                        handler.taskType(), prev.getClass().getSimpleName(), handler.getClass().getSimpleName());
            }
        }
        log.info("[AiTask] 已注册 AI 任务类型 {} 种：{}", handlerMap.size(), handlerMap.keySet());
    }

    /**
     * 提交 AI 异步任务：同步入库返回任务ID，调用方立即响应前端。
     *
     * <p>不阻塞等待 LLM 结果，前端通过 {@link #getTask} 轮询任务进度。
     * 异步执行由独立 Bean {@link AiTaskAsyncExecutor} 承担，保证 @Async 通过 Spring 代理生效。</p>
     *
     * @param userId   用户ID
     * @param taskType 任务类型（须有对应 AiTaskHandler 实现）
     * @param bizRef   业务参数（如 {resumeId, jobTargetId}，序列化为 JSON 存库）
     * @return 任务ID
     */
    public Long submitTask(Long userId, String taskType, Map<String, Object> bizRef) {
        if (!handlerMap.containsKey(taskType)) {
            throw new ServiceException("未知的AI任务类型：" + taskType);
        }

        // 同步入库任务记录（pending），调用方立即返回
        PortalAiTask task = new PortalAiTask();
        task.setUserId(userId);
        task.setTaskType(taskType);
        task.setStatus("pending");
        task.setProgressMsg("任务已提交，排队中");
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        try {
            task.setBizRef(bizRef == null ? null : objectMapper.writeValueAsString(bizRef));
        } catch (Exception e) {
            throw new ServiceException("任务参数序列化失败：" + e.getMessage());
        }
        aiTaskMapper.insert(task);

        // 触发异步执行（独立 Bean 调用，确保 @Async 生效）
        try {
            JsonNode bizRefNode = bizRef == null ? null : objectMapper.valueToTree(bizRef);
            asyncExecutor.execute(task.getId(), userId, taskType, bizRefNode);
        } catch (Exception e) {
            // 异步触发失败（如线程池满被拒绝）回写失败状态，不阻塞调用方
            log.error("[AiTask] 异步任务触发失败 taskId={} taskType={}", task.getId(), taskType, e);
            PortalAiTask fail = new PortalAiTask();
            fail.setId(task.getId());
            fail.setStatus("failed");
            fail.setError("任务触发失败：" + e.getMessage());
            fail.setFinishTime(LocalDateTime.now());
            fail.setUpdateTime(LocalDateTime.now());
            aiTaskMapper.updateById(fail);
        }
        return task.getId();
    }

    /**
     * 查询任务状态（前端轮询调用）
     *
     * @param id     任务ID
     * @param userId 用户ID（权限校验，防止越权查询他人任务）
     * @return 任务状态 VO（result 为 Handler 返回对象反序列化后的 JSON）
     */
    public AiTaskVO getTask(Long id, Long userId) {
        PortalAiTask task = aiTaskMapper.selectById(id);
        if (task == null || !task.getUserId().equals(userId)) {
            throw new ServiceException("任务不存在或无权限");
        }
        AiTaskVO vo = new AiTaskVO();
        vo.setId(task.getId());
        vo.setTaskType(task.getTaskType());
        vo.setStatus(task.getStatus());
        vo.setProgressMsg(task.getProgressMsg());
        vo.setError(task.getError());
        vo.setCreateTime(task.getCreateTime());
        vo.setFinishTime(task.getFinishTime());
        // result 字符串解析为 JsonNode（解析失败不阻断轮询，仅记录告警）
        if (task.getResult() != null && !task.getResult().isBlank()) {
            try {
                vo.setResult(objectMapper.readTree(task.getResult()));
            } catch (Exception e) {
                log.warn("[AiTask] 任务结果 JSON 解析失败 taskId={}: {}", task.getId(), e.getMessage());
            }
        }
        return vo;
    }

    /**
     * 启动时恢复孤儿任务（v11.67 P1-6）：@Async 任务存活于 JVM 内存，应用重启后
     * 执行线程丢失，卡在 pending/running 的记录永远等不到终态（前端轮询挂死）。
     * 统一置为 failed 提示重新提交。
     *
     * <p>注：单实例部署语义正确；若未来多实例部署需改为仅恢复本实例中断前的任务
     * 或引入实例标识区分，当前架构（单体 Spring Boot）无此需求。</p>
     */
    @EventListener(ApplicationReadyEvent.class)
    public void recoverOrphanTasks() {
        UpdateWrapper<PortalAiTask> uw = new UpdateWrapper<>();
        uw.in("status", "pending", "running")
                .set("status", "failed")
                .set("error", "服务重启，任务中断，请重新提交")
                .set("finish_time", LocalDateTime.now())
                .set("update_time", LocalDateTime.now());
        int recovered = aiTaskMapper.update(null, uw);
        if (recovered > 0) {
            log.warn("[AiTask] 启动恢复：{} 个中断任务（pending/running）已置为 failed", recovered);
        }
    }
}
