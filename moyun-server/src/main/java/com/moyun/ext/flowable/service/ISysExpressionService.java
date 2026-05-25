package com.moyun.ext.flowable.service;

import com.moyun.ext.flowable.domain.entity.SysExpression;

/**
 * 流程表达式配置 服务层
 *
 * @author ruoyi
 */
public interface ISysExpressionService {

    SysExpression selectExpressionById(Long id);

    int insertExpression(SysExpression sysExpression);

    int updateExpression(SysExpression sysExpression);

    int deleteExpressionById(Long id);
}
