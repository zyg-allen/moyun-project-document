package com.moyun.ext.cms.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.moyun.common.exception.system.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * AI 填充空字段草稿任务 Handler（v10.23，taskType=ai_draft）
 *
 * <p>异步执行 {@link ResumeDeepOptimizeService#aiDraftEmptyFields}：为空的工作经历/
 * 项目经历/自我介绍生成初始草稿（LLM 未启用/失败时返回空结果 + 提示，不抛异常）。</p>
 *
 * <p>bizRef 参数：{resumeId: 简历ID, jobTargetId: 岗位目标ID（可空，提供时带入 JD 上下文）}</p>
 *
 * @author moyun
 */
@Service
public class AiDraftTaskHandler implements AiTaskHandler {

    /** 任务类型标识 */
    public static final String TASK_TYPE = "ai_draft";

    @Autowired
    private ResumeDeepOptimizeService resumeDeepOptimizeService;

    @Override
    public String taskType() {
        return TASK_TYPE;
    }

    @Override
    public Object execute(Long userId, JsonNode bizRef) {
        Long resumeId = bizRef.path("resumeId").asLong(0L);
        Long jobTargetId = bizRef.path("jobTargetId").asLong(0L);
        if (resumeId == null || resumeId <= 0) {
            throw new ServiceException("任务参数缺失：resumeId 必填");
        }
        // jobTargetId 可空：asLong(0L) 缺省时归一为 null 传入
        Map<String, Object> draft = resumeDeepOptimizeService.aiDraftEmptyFields(
                resumeId, userId, jobTargetId != null && jobTargetId > 0 ? jobTargetId : null);
        return draft;
    }
}
