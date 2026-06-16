package com.moyun.system.service;

import com.moyun.system.domain.entity.SysOperLog;
import com.moyun.system.domain.query.OperLogQuery;

import java.util.List;

/**
 * 操作日志 服务层
 *
 * @author ruoyi
 */
public interface ISysOperLogService {

    /**
     * 新增操作日志
     *
     * @param operLog 操作日志对象
     */
    void insertOperlog(SysOperLog operLog);

    /**
     * 查询系统操作日志集合
     *
     * @param query 操作日志查询条件
     * @return 操作日志集合
     */
    List<SysOperLog> selectOperLogList(OperLogQuery query);

    /**
     * 批量删除系统操作日志
     *
     * @param operIds 需要删除的操作日志ID
     * @return 结果
     */
    int deleteOperLogByIds(Long[] operIds);

    /**
     * 查询操作日志详细
     *
     * @param operId 操作ID
     * @return 操作日志对象
     */
    SysOperLog selectOperLogById(Long operId);

    /**
     * 清空操作日志
     */
    void cleanOperLog();
}
