package com.moyun.ext.job.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.moyun.common.constant.ScheduleConstants;
import com.moyun.common.exception.system.job.TaskException;
import com.moyun.ext.job.domain.entity.SysJob;
import com.moyun.ext.job.mapper.SysJobMapper;
import com.moyun.ext.job.service.ISysJobService;
import com.moyun.ext.job.util.CronUtils;
import com.moyun.ext.job.util.ScheduleUtils;
import com.moyun.util.string.StringUtils;
import jakarta.annotation.PostConstruct;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 定时任务调度服务实现
 *
 * @author allen-zyg
 */
@Service
public class SysJobServiceImpl implements ISysJobService {

    @Autowired
    private SysJobMapper jobMapper;

    @Autowired
    private Scheduler scheduler;

    /**
     * 项目启动时初始化定时器：清空 Quartz 内存任务后，从 sys_job 表全量重新注册。
     * （RAMJobStore 内存模式下 JobDetail 仅存于内存，SQL 直接插入的任务必须重启后经此重建才能触发）
     */
    @PostConstruct
    public void init() throws SchedulerException, TaskException {
        scheduler.clear();
        List<SysJob> jobList = jobMapper.selectJobList(new SysJob());
        for (SysJob job : jobList) {
            ScheduleUtils.createScheduleJob(scheduler, job);
        }
    }

    @Override
    public List<SysJob> selectJobList(SysJob job) {
        return jobMapper.selectJobList(job);
    }

    /**
     * 分页查询定时任务调度（MyBatis-Plus 标准分页）
     */
    @Override
    public IPage<SysJob> selectJobPage(IPage<SysJob> page, SysJob job) {
        return jobMapper.selectJobPage(page, job);
    }

    @Override
    public SysJob selectJobById(Long jobId) {
        return jobMapper.selectJobById(jobId);
    }

    @Override
    public int pauseJob(SysJob job) throws SchedulerException {
        Long jobId = job.getJobId();
        String jobGroup = job.getJobGroup();
        job.setStatus(ScheduleConstants.STATUS_PAUSE);
        int rows = jobMapper.updateJob(job);
        if (rows > 0) {
            scheduler.pauseJob(ScheduleUtils.getJobKey(jobId, jobGroup));
        }
        return rows;
    }

    @Override
    public int resumeJob(SysJob job) throws SchedulerException {
        Long jobId = job.getJobId();
        String jobGroup = job.getJobGroup();
        job.setStatus(ScheduleConstants.STATUS_NORMAL);
        int rows = jobMapper.updateJob(job);
        if (rows > 0) {
            scheduler.resumeJob(ScheduleUtils.getJobKey(jobId, jobGroup));
        }
        return rows;
    }

    @Override
    public int deleteJob(SysJob job) throws SchedulerException {
        Long jobId = job.getJobId();
        String jobGroup = job.getJobGroup();
        int rows = jobMapper.deleteJob(job);
        if (rows > 0) {
            scheduler.deleteJob(ScheduleUtils.getJobKey(jobId, jobGroup));
        }
        return rows;
    }

    @Override
    public void deleteJobByIds(Long[] jobIds) throws SchedulerException {
        for (Long jobId : jobIds) {
            SysJob job = jobMapper.selectJobById(jobId);
            if (job != null) {
                deleteJob(job);
            }
        }
    }

    @Override
    public int changeStatus(SysJob job) throws SchedulerException {
        int rows = 0;
        String status = job.getStatus();
        if (ScheduleConstants.STATUS_NORMAL.equals(status)) {
            rows = resumeJob(job);
        } else if (ScheduleConstants.STATUS_PAUSE.equals(status)) {
            rows = pauseJob(job);
        }
        return rows;
    }

    @Override
    public boolean run(SysJob job) throws SchedulerException {
        Long jobId = job.getJobId();
        String jobGroup = job.getJobGroup();
        SysJob properties = selectJobById(job.getJobId());
        if (StringUtils.isNull(properties)) {
            return false;
        }
        scheduler.triggerJob(ScheduleUtils.getJobKey(jobId, jobGroup));
        return true;
    }

    @Override
    public int insertJob(SysJob job) throws SchedulerException, TaskException {
        int rows = jobMapper.insertJob(job);
        if (rows > 0) {
            ScheduleUtils.createScheduleJob(scheduler, job);
        }
        return rows;
    }

    @Override
    public int updateJob(SysJob job) throws SchedulerException, TaskException {
        int rows = jobMapper.updateJob(job);
        if (rows > 0) {
            updateSchedulerJob(job);
        }
        return rows;
    }

    @Override
    public boolean checkCronExpressionIsValid(String cronExpression) {
        return CronUtils.isValid(cronExpression);
    }

    private void updateSchedulerJob(SysJob job) throws SchedulerException, TaskException {
        Long jobId = job.getJobId();
        String jobGroup = job.getJobGroup();
        scheduler.deleteJob(ScheduleUtils.getJobKey(jobId, jobGroup));
        ScheduleUtils.createScheduleJob(scheduler, job);
    }
}
