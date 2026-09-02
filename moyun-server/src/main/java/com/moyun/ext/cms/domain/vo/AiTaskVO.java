package com.moyun.ext.cms.domain.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通用 AI 异步任务状态 VO（v10.23）
 *
 * <p>前端轮询返回结构：任务类型、状态、进度提示、结果（success 时为任务 Handler
 * 返回对象序列化后的 JSON）与失败原因。</p>
 *
 * @author moyun
 */
@Data
public class AiTaskVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 任务ID */
    private Long id;

    /** 任务类型：resume_parse/job_match/ai_draft/deep_optimize */
    private String taskType;

    /** 任务状态：pending/running/success/failed */
    private String status;

    /** 进度提示文案 */
    private String progressMsg;

    /** 任务结果（status=success 时填充，具体结构由任务类型决定） */
    private JsonNode result;

    /** 失败原因（status=failed 时填充） */
    private String error;

    /** 任务提交时间 */
    private LocalDateTime createTime;

    /** 任务完成时间（成功或失败） */
    private LocalDateTime finishTime;
}
