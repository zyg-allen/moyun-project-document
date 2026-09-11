package com.moyun.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 审核任务展示 VO（v8.1）
 * <p>
 * 用于审核中心列表、首页待办、我的待办/已办等场景。
 *
 * @author moyun
 */
@Data
public class AuditTaskVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 审核任务ID */
    private Long id;

    /** 任务类型 code */
    private String taskType;

    /** 任务类型显示名（如「文章审核」） */
    private String taskTypeLabel;

    /** 业务子类型 */
    private String bizType;

    /** 业务记录ID */
    private Long bizId;

    /** 任务标题 */
    private String title;

    /** 任务描述/摘要 */
    private String description;

    /** 提交人用户名 */
    private String submitterName;

    /** 状态：pending/approved/rejected */
    private String status;

    /** 状态显示名 */
    private String statusLabel;

    /** 处理人用户名 */
    private String auditorName;

    /** 审核意见 */
    private String auditOpinion;

    /** 审核操作类型 */
    private String auditAction;

    /** 提交时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime submitTime;

    /** 处理时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditTime;

    /** 优先级 */
    private String priority;

    /** 优先级显示名 */
    private String priorityLabel;

    /** 查看详情跳转路径（审核中心） */
    private String routePath;

    /** 原业务管理页路由（v11.35.1，「查看原业务」直达业务管理菜单页） */
    private String bizRoutePath;

    /** 业务详情（由 AuditBizHandler.getBizDetail 返回，结构因业务而异） */
    @JsonProperty("bizDetail")
    private Map<String, Object> bizDetail;

    /** 扩展数据（解析后的对象，便于前端展示） */
    @JsonProperty("extra")
    private Map<String, Object> extra;
}
