package com.moyun.ext.cms.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.ext.cms.domain.vo.ResumeDeepOptimizeVO;
import com.moyun.ext.cms.domain.vo.UserResumeVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 简历深度优化任务 Handler（v10.23，taskType=deep_optimize）
 *
 * <p>承接原 ResumeOptimizeAsyncExecutor（v10.19 已删除）的执行逻辑：
 * 调用 {@link ResumeDeepOptimizeGenerator#generate} 组装 DeepOptimizeVO
 * （LLM 基于 JD 对简历逐项生成前后对比建议）。任务状态流转由
 * {@link AiTaskAsyncExecutor} 统一管理。</p>
 *
 * <p>bizRef 参数：{resumeId: 简历ID, jobTargetId: 岗位目标ID}</p>
 *
 * @author moyun
 */
@Service
public class DeepOptimizeTaskHandler implements AiTaskHandler {

    /** 任务类型标识 */
    public static final String TASK_TYPE = "deep_optimize";

    @Autowired
    private IUserResumeService userResumeService;

    @Autowired
    private ResumeDeepOptimizeGenerator generator;

    @Override
    public String taskType() {
        return TASK_TYPE;
    }

    @Override
    public Object execute(Long userId, JsonNode bizRef) {
        Long resumeId = bizRef.path("resumeId").asLong(0L);
        Long jobTargetId = bizRef.path("jobTargetId").asLong(0L);
        if (resumeId == null || resumeId <= 0 || jobTargetId == null || jobTargetId <= 0) {
            throw new ServiceException("任务参数缺失：resumeId/jobTargetId 必填");
        }
        // 简历归属校验（异步上下文需按 userId 显式查询）
        UserResumeVO resume = userResumeService.selectResumeDetail(resumeId, userId);
        if (resume == null) {
            throw new ServiceException("简历不存在或无权访问");
        }
        ResumeDeepOptimizeVO vo = generator.generate(resume, jobTargetId);
        return vo;
    }
}
