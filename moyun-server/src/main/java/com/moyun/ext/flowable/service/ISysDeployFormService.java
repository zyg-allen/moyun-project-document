package com.moyun.ext.flowable.service;

import com.moyun.ext.flowable.domain.entity.SysDeployForm;

/**
 * 流程部署表单配置 服务层
 *
 * @author ruoyi
 */
public interface ISysDeployFormService {

    SysDeployForm selectDeployFormById(Long id);

    int insertDeployForm(SysDeployForm sysDeployForm);

    int updateDeployForm(SysDeployForm sysDeployForm);

    int deleteDeployFormById(Long id);
}
