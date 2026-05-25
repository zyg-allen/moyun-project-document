package com.moyun.ext.flowable.service.impl;

import com.moyun.ext.flowable.domain.entity.SysDeployForm;
import com.moyun.ext.flowable.mapper.SysDeployFormMapper;
import com.moyun.ext.flowable.service.ISysDeployFormService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 流程部署表单配置 服务实现
 *
 * @author ruoyi
 */
@Service
public class SysDeployFormServiceImpl implements ISysDeployFormService {

    @Autowired
    private SysDeployFormMapper sysDeployFormMapper;

    @Override
    public SysDeployForm selectDeployFormById(Long id) {
        return sysDeployFormMapper.selectById(id);
    }

    @Override
    public int insertDeployForm(SysDeployForm sysDeployForm) {
        return sysDeployFormMapper.insert(sysDeployForm);
    }

    @Override
    public int updateDeployForm(SysDeployForm sysDeployForm) {
        return sysDeployFormMapper.update(sysDeployForm);
    }

    @Override
    public int deleteDeployFormById(Long id) {
        return sysDeployFormMapper.deleteById(id);
    }
}
