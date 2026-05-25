package com.moyun.ext.flowable.service;

import com.moyun.ext.flowable.domain.entity.SysForm;

/**
 * 流程表单配置 服务层
 *
 * @author ruoyi
 */
public interface ISysFormService {

    SysForm selectFormById(Long id);

    int insertForm(SysForm sysForm);

    int updateForm(SysForm sysForm);

    int deleteFormById(Long id);
}
