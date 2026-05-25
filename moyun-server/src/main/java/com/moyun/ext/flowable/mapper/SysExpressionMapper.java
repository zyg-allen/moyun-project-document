package com.moyun.ext.flowable.mapper;

import com.moyun.ext.flowable.domain.entity.SysExpression;
import org.apache.ibatis.annotations.Mapper;

/**
 * 流程表达式配置Mapper
 *
 * @author ruoyi
 */
@Mapper
public interface SysExpressionMapper {

    int deleteById(Long id);

    int insert(SysExpression sysExpression);

    SysExpression selectById(Long id);

    int update(SysExpression sysExpression);
}
