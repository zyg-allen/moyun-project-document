package com.moyun.system.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.moyun.system.domain.entity.SysOperLog;
import com.moyun.system.domain.query.OperLogQuery;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 操作日志 数据层
 *
 * @author ruoyi
 */
public interface SysOperLogMapper {
    /**
     * 新增操作日志
     *
     * @param operLog 操作日志对象
     */
    public void insertOperlog(SysOperLog operLog);

    /**
     * 查询系统操作日志集合
     *
     * @param query 操作日志查询条件
     * @return 操作日志集合
     */
    public List<SysOperLog> selectOperLogList(OperLogQuery query);

    /**
     * 分页查询操作日志（MyBatis-Plus 标准分页，配合 PaginationInnerInterceptor）
     *
     * @param page  分页对象
     * @param query 操作日志查询条件
     * @return 分页结果
     */
    IPage<SysOperLog> selectOperLogPage(IPage<SysOperLog> page, @Param("query") OperLogQuery query);

    /**
     * 批量删除系统操作日志
     *
     * @param operIds 需要删除的操作日志ID
     * @return 结果
     */
    public int deleteOperLogByIds(Long[] operIds);

    /**
     * 查询操作日志详细
     *
     * @param operId 操作ID
     * @return 操作日志对象
     */
    public SysOperLog selectOperLogById(Long operId);

    /**
     * 清空操作日志
     */
    public void cleanOperLog();
}
