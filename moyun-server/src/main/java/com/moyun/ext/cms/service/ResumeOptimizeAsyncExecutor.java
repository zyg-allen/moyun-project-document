package com.moyun.ext.cms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.ext.cms.domain.vo.ResumeDeepOptimizeVO;
import com.moyun.ext.cms.domain.vo.UserResumeVO;
import com.moyun.portal.domain.entity.PortalResumeOptimizeTask;
import com.moyun.portal.mapper.PortalResumeOptimizeTaskMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 简历深度优化异步执行器（v10.19）
 *
 * <p><strong>独立 Bean 设计原因</strong>：Spring 的 {@code @Async} 通过 AOP 代理生效，
 * 同一类内部的 {@code this.executeTask()} 自调用不会触发代理，会导致方法退化为同步执行。
 * 把 {@code @Async} 方法抽到独立 Bean，由 {@link ResumeDeepOptimizeService} 注入后调用，
 * 保证异步线程池真正生效。</p>
 *
 * <p><strong>依赖方向</strong>：本 Bean 依赖 {@link ResumeDeepOptimizeGenerator}（生成能力），
 * 不反向依赖 {@link ResumeDeepOptimizeService}，避免 A→B→A 循环依赖。
 * 依赖图：Service → Executor → Generator（单向无环）。</p>
 *
 * <p>执行流程：pending（已提交）→ running（执行中）→ success/failed（终态）。
 * 任务状态持久化到 {@code portal_resume_optimize_task} 表，失败原因回写 error_msg 字段。</p>
 *
 * @author moyun
 */
@Service
public class ResumeOptimizeAsyncExecutor {

    private static final Logger log = LoggerFactory.getLogger(ResumeOptimizeAsyncExecutor.class);

    /** 生成器（持有 LLM/Mapper，不依赖 Service，打破循环依赖） */
    @Autowired
    private ResumeDeepOptimizeGenerator generator;

    @Autowired
    private PortalResumeOptimizeTaskMapper optimizeTaskMapper;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 异步执行深度优化任务（v10.19）
     *
     * <p>独立线程池 {@code resumeOptimizeExecutor} 执行，不阻塞 HTTP 请求线程。
     * 任务状态流转：pending → running → success/failed。</p>
     *
     * @param taskId      任务ID
     * @param resume      简历详情（异步上下文需显式传入，避免 SecurityContext 丢失）
     * @param jobTargetId 岗位目标ID
     */
    @Async("resumeOptimizeExecutor")
    public void executeTask(Long taskId, UserResumeVO resume, Long jobTargetId) {
        PortalResumeOptimizeTask running = new PortalResumeOptimizeTask();
        running.setId(taskId);
        running.setStatus("running");
        running.setStartTime(LocalDateTime.now());
        optimizeTaskMapper.updateById(running);

        try {
            ResumeDeepOptimizeVO vo = generator.generate(resume, jobTargetId);
            PortalResumeOptimizeTask success = new PortalResumeOptimizeTask();
            success.setId(taskId);
            success.setStatus("success");
            success.setResultJson(objectMapper.valueToTree(vo));
            success.setFinishTime(LocalDateTime.now());
            optimizeTaskMapper.updateById(success);
            log.info("[DeepOptimizeTask] 任务执行成功 taskId={} resumeId={} items={}",
                    taskId, resume.getId(), vo.getItems() != null ? vo.getItems().size() : 0);
        } catch (Exception e) {
            log.error("[DeepOptimizeTask] 任务执行失败 taskId={}", taskId, e);
            PortalResumeOptimizeTask failed = new PortalResumeOptimizeTask();
            failed.setId(taskId);
            failed.setStatus("failed");
            failed.setErrorMsg(e.getMessage());
            failed.setFinishTime(LocalDateTime.now());
            optimizeTaskMapper.updateById(failed);
        }
    }
}
