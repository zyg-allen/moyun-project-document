package com.moyun.ext.job.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.moyun.common.annotation.Excel;
import com.moyun.common.annotation.Excel.ColumnType;
import com.moyun.core.base.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 定时任务调度日志表 sys_job_log
 *
 * @author ruoyi
 */
public class SysJobLog extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Excel(name = "日志序号", cellType = ColumnType.NUMERIC)
    private Long jobLogId;

    @Excel(name = "任务名称")
    private String jobName;

    @Excel(name = "任务组名")
    private String jobGroup;

    @Excel(name = "调用目标字符串")
    private String invokeTarget;

    @Excel(name = "日志信息")
    private String jobMessage;

    @Excel(name = "执行状态", readConverterExp = "0=成功,1=失败")
    private String status;

    @Excel(name = "异常信息")
    private String exceptionInfo;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "开始时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss", type = Type.EXPORT)
    private Date startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "停止时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss", type = Type.EXPORT)
    private Date stopTime;

    public Long getJobLogId() {
        return jobLogId;
    }

    public void setJobLogId(Long jobLogId) {
        this.jobLogId = jobLogId;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getJobGroup() {
        return jobGroup;
    }

    public void setJobGroup(String jobGroup) {
        this.jobGroup = jobGroup;
    }

    public String getInvokeTarget() {
        return invokeTarget;
    }

    public void setInvokeTarget(String invokeTarget) {
        this.invokeTarget = invokeTarget;
    }

    public String getJobMessage() {
        return jobMessage;
    }

    public void setJobMessage(String jobMessage) {
        this.jobMessage = jobMessage;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getExceptionInfo() {
        return exceptionInfo;
    }

    public void setExceptionInfo(String exceptionInfo) {
        this.exceptionInfo = exceptionInfo;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getStopTime() {
        return stopTime;
    }

    public void setStopTime(Date stopTime) {
        this.stopTime = stopTime;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("jobLogId", getJobLogId())
                .append("jobName", getJobName())
                .append("jobGroup", getJobGroup())
                .append("invokeTarget", getInvokeTarget())
                .append("jobMessage", getJobMessage())
                .append("status", getStatus())
                .append("exceptionInfo", getExceptionInfo())
                .append("startTime", getStartTime())
                .append("stopTime", getStopTime())
                .toString();
    }
}
