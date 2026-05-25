package com.moyun.ext.job.service;

import com.moyun.ext.job.domain.entity.SysJobLog;

import java.util.List;

/**
 * 定时任务调度日志信息 服务层
 *
 * @author ruoyi
 */
public interface ISysJobLogService {

    List<SysJobLog> selectJobLogList(SysJobLog jobLog);

    SysJobLog selectJobLogById(Long jobLogId);

    void addJobLog(SysJobLog jobLog);

    int deleteJobLogByIds(Long[] logIds);

    int deleteJobLogById(Long jobId);

    void cleanJobLog();
}
