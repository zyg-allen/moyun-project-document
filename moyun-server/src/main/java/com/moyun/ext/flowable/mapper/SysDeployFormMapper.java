package com.moyun.ext.flowable.mapper;

import com.moyun.ext.flowable.domain.entity.SysDeployForm;
import org.apache.ibatis.annotations.Mapper;

/**
 * 流程部署表单配置Mapper
 *
 * @author ruoyi
 */
@Mapper
public interface SysDeployFormMapper {

    int deleteById(Long id);

    int insert(SysDeployForm sysDeployForm);

    SysDeployForm selectById(Long id);

    int update(SysDeployForm sysDeployForm);
}
