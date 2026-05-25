package com.moyun.ext.flowable.service.impl;

import com.moyun.ext.flowable.domain.entity.SysExpression;
import com.moyun.ext.flowable.mapper.SysExpressionMapper;
import com.moyun.ext.flowable.service.ISysExpressionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 流程表达式配置 服务实现
 *
 * @author ruoyi
 */
@Service
public class SysExpressionServiceImpl implements ISysExpressionService {

    @Autowired
    private SysExpressionMapper sysExpressionMapper;

    @Override
    public SysExpression selectExpressionById(Long id) {
        return sysExpressionMapper.selectById(id);
    }

    @Override
    public int insertExpression(SysExpression sysExpression) {
        return sysExpressionMapper.insert(sysExpression);
    }

    @Override
    public int updateExpression(SysExpression sysExpression) {
        return sysExpressionMapper.update(sysExpression);
    }

    @Override
    public int deleteExpressionById(Long id) {
        return sysExpressionMapper.deleteById(id);
    }
}
