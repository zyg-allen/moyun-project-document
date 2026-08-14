package com.moyun.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 统一审核任务表 sys_audit_task（v8.1）
 * <p>
 * 替代分散的各业务表 status 聚合查询，作为审核入口索引 + 审核记录。
 * 业务表 status 字段保留作真实状态，本表作索引 + 记录（双写策略）。
 * <p>
 * 不继承 BaseEntity（不启用逻辑删除），审核记录需永久保留。
 *
 * @author moyun
 */
@Data
@TableName("sys_audit_task")
public class SysAuditTask implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务类型：article/column/topic/interview_exp/interview_comment/certification/feedback/report */
    private String taskType;

    /** 业务子类型（如 report 的 spam/infringement） */
    private String bizType;

    /** 业务记录ID */
    private Long bizId;

    /** 任务标题（有标题用标题，无则用类型名+ID） */
    private String title;

    /** 任务描述/摘要 */
    private String description;

    /** 提交人ID（门户用户ID） */
    private Long submitterId;

    /** 提交人用户名 */
    private String submitterName;

    /** 状态：pending/approved/rejected */
    private String status;

    /** 处理人ID（系统用户ID） */
    private Long auditorId;

    /** 处理人用户名 */
    private String auditorName;

    /** 审核意见（驳回时必填） */
    private String auditOpinion;

    /** 审核操作类型：approve/reject */
    private String auditAction;

    /** 提交时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime submitTime;

    /** 处理时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditTime;

    /** 优先级：high/medium/low */
    private String priority;

    /** 查看详情跳转路径（业务管理页，如 /cms/article） */
    private String routePath;

    /** 扩展数据 JSON（如举报图片、反馈联系方式） */
    private String extraData;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
