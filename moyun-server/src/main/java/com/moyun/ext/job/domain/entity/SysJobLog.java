package com.moyun.ext.job.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.moyun.common.annotation.Excel;
import com.moyun.common.annotation.Excel.ColumnType;
import com.moyun.core.base.BaseEntity;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 定时任务调度日志表 sys_job_log
 *
 * @author ruoyi
 */
@Data
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
    @Excel(name = "开始时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss", type = Excel.Type.EXPORT)
    private Date startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "停止时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss", type = Excel.Type.EXPORT)
    private Date stopTime;
}
