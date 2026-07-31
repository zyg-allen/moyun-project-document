package com.moyun.ext.job.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.moyun.ext.job.domain.entity.SysJobLog;
import com.moyun.ext.job.mapper.SysJobLogMapper;
import com.moyun.ext.job.service.ISysJobLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 定时任务调度日志服务实现
 *
 * @author ruoyi
 */
@Service
public class SysJobLogServiceImpl implements ISysJobLogService {

    @Autowired
    private SysJobLogMapper jobLogMapper;

    @Override
    public List<SysJobLog> selectJobLogList(SysJobLog jobLog) {
        return jobLogMapper.selectJobLogList(jobLog);
    }

    /**
     * 分页查询定时任务调度日志（MyBatis-Plus 标准分页）
     */
    @Override
    public IPage<SysJobLog> selectJobLogPage(IPage<SysJobLog> page, SysJobLog jobLog) {
        return jobLogMapper.selectJobLogPage(page, jobLog);
    }

    @Override
    public SysJobLog selectJobLogById(Long jobLogId) {
        return jobLogMapper.selectJobLogById(jobLogId);
    }

    @Override
    public void addJobLog(SysJobLog jobLog) {
        jobLogMapper.insertJobLog(jobLog);
    }

    @Override
    public int deleteJobLogByIds(Long[] logIds) {
        return jobLogMapper.deleteJobLogByIds(logIds);
    }

    @Override
    public int deleteJobLogById(Long jobLogId) {
        return jobLogMapper.deleteJobLogById(jobLogId);
    }

    @Override
    public void cleanJobLog() {
        jobLogMapper.cleanJobLog();
    }
}
