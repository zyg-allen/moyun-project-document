package com.moyun.ext.cms.domain.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

/**
 * 简历深度优化异步任务状态 VO（v10.19）
 *
 * <p>前端轮询返回结构：包含任务状态、进度百分比与结果（success 时填充）。</p>
 *
 * @author moyun
 */
@Data
public class ResumeOptimizeTaskVO {

    /** 任务ID */
    private Long taskId;

    /** 任务状态：pending/running/success/failed */
    private String status;

    /**
     * 进度百分比 0-100（粗粒度估算）：
     * pending=10, running=50, success=100, failed=0
     */
    private Integer progress;

    /** 优化结果（status=success 时填充，对应 ResumeDeepOptimizeVO） */
    private JsonNode result;

    /** 失败原因（status=failed 时填充） */
    private String errorMsg;
}
