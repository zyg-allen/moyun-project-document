package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 简历深度优化异步任务（v10.19：异步任务化，解决大模型调用超时问题）
 *
 * <p>用户提交深度优化请求后立即返回任务ID，后端异步调用 LLM 生成建议，
 * 任务状态持久化到本表，前端通过轮询查询任务进度与结果，支持失败重试与关闭页面后回来查看。</p>
 *
 * <p>状态流转：pending（已提交）→ running（执行中）→ success/failed（终态）</p>
 *
 * @author moyun
 */
@Data
@TableName(value = "portal_resume_optimize_task", autoResultMap = true)
public class PortalResumeOptimizeTask implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 简历ID（portal_user_resume.id） */
    private Long resumeId;

    /** 岗位目标ID */
    private Long jobTargetId;

    /**
     * 任务状态：pending（已提交，待执行）/ running（执行中）/ success（成功）/ failed（失败）
     */
    private String status;

    /**
     * 优化结果 JSON（成功时填充，对应 ResumeDeepOptimizeVO 序列化）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private com.fasterxml.jackson.databind.JsonNode resultJson;

    /** 失败原因（failed 时填充） */
    private String errorMsg;

    /** 是否 LLM 生成：0=规则兜底 1=LLM */
    private Integer aiPowered;

    /** 任务提交时间 */
    private LocalDateTime createTime;

    /** 任务开始执行时间 */
    private LocalDateTime startTime;

    /** 任务结束时间（成功或失败） */
    private LocalDateTime finishTime;
}
