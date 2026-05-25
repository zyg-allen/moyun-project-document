package com.moyun.ext.flowable.mapper;

import com.moyun.ext.flowable.domain.entity.SysTaskForm;
import org.apache.ibatis.annotations.Mapper;

/**
 * 流程任务表单配置Mapper
 *
 * @author ruoyi
 */
@Mapper
public interface SysTaskFormMapper {

    int deleteById(Long id);

    int insert(SysTaskForm sysTaskForm);

    SysTaskForm selectById(Long id);

    int update(SysTaskForm sysTaskForm);
}
