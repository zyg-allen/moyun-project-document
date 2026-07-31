package com.moyun.ext.job.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.moyun.ext.job.domain.entity.SysJobLog;

import java.util.List;

/**
 * 定时任务调度日志信息 服务层
 *
 * @author ruoyi
 */
public interface ISysJobLogService {

    List<SysJobLog> selectJobLogList(SysJobLog jobLog);

    /**
     * 分页查询定时任务调度日志（MyBatis-Plus 标准分页，配合 PaginationInnerInterceptor）
     *
     * @param page   分页对象
     * @param jobLog 调度日志查询条件
     * @return 分页结果
     */
    IPage<SysJobLog> selectJobLogPage(IPage<SysJobLog> page, SysJobLog jobLog);

    SysJobLog selectJobLogById(Long jobLogId);

    void addJobLog(SysJobLog jobLog);

    int deleteJobLogByIds(Long[] logIds);

    int deleteJobLogById(Long jobId);

    void cleanJobLog();
}
