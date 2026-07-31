package com.moyun.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.moyun.system.domain.entity.SysOperLog;
import com.moyun.system.domain.query.OperLogQuery;
import com.moyun.system.mapper.SysOperLogMapper;
import com.moyun.system.service.ISysOperLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 操作日志 服务层处理
 *
 * @author ruoyi
 */
@Service
public class SysOperLogServiceImpl implements ISysOperLogService {

    @Autowired
    private SysOperLogMapper operLogMapper;

    /**
     * 新增操作日志
     *
     * @param operLog 操作日志对象
     */
    @Override
    public void insertOperlog(SysOperLog operLog) {
        operLogMapper.insertOperlog(operLog);
    }

    /**
     * 查询系统操作日志集合
     *
     * @param query 操作日志查询条件
     * @return 操作日志集合
     */
    @Override
    public List<SysOperLog> selectOperLogList(OperLogQuery query) {
        return operLogMapper.selectOperLogList(query);
    }

    /**
     * 分页查询操作日志（MyBatis-Plus 标准分页）
     */
    @Override
    public IPage<SysOperLog> selectOperLogPage(IPage<SysOperLog> page, OperLogQuery query) {
        return operLogMapper.selectOperLogPage(page, query);
    }

    /**
     * 批量删除系统操作日志
     *
     * @param operIds 需要删除的操作日志ID
     * @return 结果
     */
    @Override
    public int deleteOperLogByIds(Long[] operIds) {
        return operLogMapper.deleteOperLogByIds(operIds);
    }

    /**
     * 查询操作日志详细
     *
     * @param operId 操作ID
     * @return 操作日志对象
     */
    @Override
    public SysOperLog selectOperLogById(Long operId) {
        return operLogMapper.selectOperLogById(operId);
    }

    /**
     * 清空操作日志
     */
    @Override
    public void cleanOperLog() {
        operLogMapper.cleanOperLog();
    }
}
