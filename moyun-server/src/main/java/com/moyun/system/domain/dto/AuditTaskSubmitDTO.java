package com.moyun.system.domain.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 审核任务提交参数（v8.1）
 * <p>
 * 业务模块在「提交审核」时调用 {@code IAuditTaskService.submit()} 传入本 DTO，
 * 由统一审核服务写入 sys_audit_task。
 *
 * @author moyun
 */
@Data
public class AuditTaskSubmitDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 任务类型（article/column/topic/interview_exp/interview_comment/certification/feedback/report） */
    private String taskType;

    /** 业务子类型（如 report 的 spam/infringement，可为空） */
    private String bizType;

    /** 业务记录ID */
    private Long bizId;

    /** 任务标题（有标题用标题，无则由 Service 用类型名+ID 兜底） */
    private String title;

    /** 任务描述/摘要 */
    private String description;

    /** 提交人ID（门户用户ID） */
    private Long submitterId;

    /** 提交人用户名 */
    private String submitterName;

    /** 优先级：high/medium/low，默认 medium */
    private String priority;

    /** 查看详情跳转路径（不填则用任务类型默认路径） */
    private String routePath;

    /** 扩展数据 JSON（如举报图片、反馈联系方式） */
    private String extraData;
}
