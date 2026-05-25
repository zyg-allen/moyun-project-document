package com.moyun.ext.job.service;

import com.moyun.common.exception.system.job.TaskException;
import com.moyun.ext.job.domain.entity.SysJob;
import org.quartz.SchedulerException;

import java.util.List;

/**
 * 定时任务调度信息 服务层
 *
 * @author ruoyi
 */
public interface ISysJobService {

    List<SysJob> selectJobList(SysJob job);

    SysJob selectJobById(Long jobId);

    int pauseJob(SysJob job) throws SchedulerException;

    int resumeJob(SysJob job) throws SchedulerException;

    int deleteJob(SysJob job) throws SchedulerException;

    void deleteJobByIds(Long[] jobIds) throws SchedulerException;

    int changeStatus(SysJob job) throws SchedulerException;

    boolean run(SysJob job) throws SchedulerException;

    int insertJob(SysJob job) throws SchedulerException, TaskException;

    int updateJob(SysJob job) throws SchedulerException, TaskException;

    boolean checkCronExpressionIsValid(String cronExpression);
}
