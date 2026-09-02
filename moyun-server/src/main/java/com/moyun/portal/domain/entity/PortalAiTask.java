package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通用 AI 异步任务（v10.23：统一门户 LLM 长耗时任务的异步执行载体）
 *
 * <p>门户端大输入 LLM 场景（简历解析/岗位匹配/空字段草稿/深度优化）统一走本表：
 * 提交方同步插入 pending 记录立即返回任务 ID，后端线程池异步执行，
 * 前端通过轮询查询任务进度与结果。任务类型由 {@code AiTaskHandler} 实现类声明，
 * 新增任务类型只需新增 Handler，无需改本表结构。</p>
 *
 * <p>状态流转：pending（已提交）→ running（执行中）→ success/failed（终态）。</p>
 *
 * @author moyun
 */
@Data
@TableName("portal_ai_task")
public class PortalAiTask implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户 */
    private Long userId;

    /** 任务类型：resume_parse（简历解析）/ job_match（岗位匹配）/ ai_draft（空字段草稿）/ deep_optimize（深度优化） */
    private String taskType;

    /** 业务参数 JSON，如 {"resumeId":1,"jobTargetId":2} */
    private String bizRef;

    /** 任务状态：pending（排队）/ running（执行中）/ success（成功）/ failed（失败） */
    private String status;

    /** 进度提示文案（轮询时返回给前端展示） */
    private String progressMsg;

    /** 任务结果 JSON（success 时有值） */
    private String result;

    /** 失败原因（failed 时有值） */
    private String error;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 完成时间（成功或失败） */
    private LocalDateTime finishTime;
}
