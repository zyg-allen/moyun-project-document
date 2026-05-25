package com.moyun.ext.flowable.service.impl;

import com.moyun.ext.flowable.domain.entity.SysForm;
import com.moyun.ext.flowable.mapper.SysFormMapper;
import com.moyun.ext.flowable.service.ISysFormService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 流程表单配置 服务实现
 *
 * @author ruoyi
 */
@Service
public class SysFormServiceImpl implements ISysFormService {

    @Autowired
    private SysFormMapper sysFormMapper;

    @Override
    public SysForm selectFormById(Long id) {
        return sysFormMapper.selectById(id);
    }

    @Override
    public int insertForm(SysForm sysForm) {
        return sysFormMapper.insert(sysForm);
    }

    @Override
    public int updateForm(SysForm sysForm) {
        return sysFormMapper.update(sysForm);
    }

    @Override
    public int deleteFormById(Long id) {
        return sysFormMapper.deleteById(id);
    }
}
