package com.moyun.system.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moyun.system.domain.entity.SysAuditTask;

/**
 * 统一审核任务表 sys_audit_task 数据层（v8.1）
 *
 * @author moyun
 */
@Mapper
public interface SysAuditTaskMapper extends BaseMapper<SysAuditTask> {

    /**
     * 按 status 分组统计任务数（待办/已通过/已驳回）。
     */
    List<java.util.Map<String, Object>> countByStatus();

    /**
     * 按 task_type 分组统计待办数（用于审核中心 Tab 角标）。
     */
    List<java.util.Map<String, Object>> countPendingByType();

    /**
     * 幂等查询：根据 task_type + biz_id 查询是否存在记录。
     */
    SysAuditTask selectByBiz(@Param("taskType") String taskType, @Param("bizId") Long bizId);
}
