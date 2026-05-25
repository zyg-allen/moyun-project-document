package com.moyun.ext.job.mapper;

import com.moyun.ext.job.domain.entity.SysJob;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 定时任务调度Mapper接口
 *
 * @author ruoyi
 */
@Mapper
public interface SysJobMapper {

    /**
     * 查询定时任务调度
     *
     * @param job 定时任务调度
     * @return 定时任务调度
     */
    List<SysJob> selectJobList(SysJob job);

    /**
     * 查询所有定时任务调度
     *
     * @return 调度任务集合
     */
    List<SysJob> selectJobAll();

    /**
     * 通过ID查询定时任务调度
     *
     * @param jobId 调度任务ID
     * @return 调度任务对象信息
     */
    SysJob selectJobById(Long jobId);

    /**
     * 删除定时任务调度
     *
     * @param job 调度信息
     * @return 结果
     */
    int deleteJob(SysJob job);

    /**
     * 批量删除定时任务调度
     *
     * @param jobIds 需要删除的任务ID
     * @return 结果
     */
    int deleteJobByIds(Long[] jobIds);

    /**
     * 修改定时任务调度
     *
     * @param job 调度信息
     * @return 结果
     */
    int updateJob(SysJob job);

    /**
     * 新增定时任务调度
     *
     * @param job 调度信息
     * @return 结果
     */
    int insertJob(SysJob job);

    /**
     * 校验cron表达式是否有效
     *
     * @param cronExpression cron表达式
     * @return 结果
     */
    int checkCronExpressionIsValid(String cronExpression);
}
