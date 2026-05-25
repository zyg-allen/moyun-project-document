package com.moyun.ext.job.mapper;

import com.moyun.ext.job.domain.entity.SysJobLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 定时任务调度日志Mapper接口
 *
 * @author ruoyi
 */
@Mapper
public interface SysJobLogMapper {

    /**
     * 查询定时任务调度日志
     *
     * @param jobLog 调度日志信息
     * @return 调度日志集合
     */
    List<SysJobLog> selectJobLogList(SysJobLog jobLog);

    /**
     * 查询所有调度日志
     *
     * @return 调度日志列表
     */
    List<SysJobLog> selectJobLogAll();

    /**
     * 通过调度日志ID查询调度日志
     *
     * @param jobLogId 调度日志ID
     * @return 调度日志对象信息
     */
    SysJobLog selectJobLogById(Long jobLogId);

    /**
     * 删除调度日志
     *
     * @param logId 调度日志ID
     * @return 结果
     */
    int deleteJobLogById(Long logId);

    /**
     * 批量删除调度日志
     *
     * @param logIds 需要删除的日志ID
     * @return 结果
     */
    int deleteJobLogByIds(Long[] logIds);

    /**
     * 清空调度日志
     */
    void cleanJobLog();

    /**
     * 新增调度日志
     *
     * @param jobLog 调度日志信息
     * @return 结果
     */
    int insertJobLog(SysJobLog jobLog);
}
