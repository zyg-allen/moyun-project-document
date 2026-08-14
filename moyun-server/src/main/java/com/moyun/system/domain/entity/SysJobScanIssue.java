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
 * 定时任务扫描结果表 sys_job_scan_issue（v8.1）
 * <p>
 * 定时任务执行过程中扫描出的异常/待处理项记录，人工在「任务管理 > 扫描结果」页面处理。
 * <p>
 * 不继承 BaseEntity（不启用逻辑删除），扫描记录需永久保留便于审计追溯。
 *
 * @author moyun
 */
@Data
@TableName("sys_job_scan_issue")
public class SysJobScanIssue implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 触发扫描的定时任务ID（sys_job.job_id） */
    private Long jobId;

    /** 定时任务名称 */
    private String jobName;

    /** 问题类型：sensitive_word/pending_overdue/anomaly/other */
    private String issueType;

    /** 问题描述 */
    private String issueDesc;

    /** 目标对象类型（如 article/comment/user） */
    private String targetType;

    /** 目标对象ID */
    private Long targetId;

    /** 目标对象标题/摘要 */
    private String targetTitle;

    /** 日志摘要（便于人工排查） */
    private String logExcerpt;

    /** 状态：pending/handled/ignored */
    private String status;

    /** 处理人ID */
    private Long handlerId;

    /** 处理人用户名 */
    private String handlerName;

    /** 处理结果说明 */
    private String handleResult;

    /** 处理时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime handleTime;

    /** 创建时间（扫描发现时间） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
