package com.moyun.ext.flowable.mapper;

import com.moyun.ext.flowable.domain.entity.SysForm;
import org.apache.ibatis.annotations.Mapper;

/**
 * 流程表单配置Mapper
 *
 * @author ruoyi
 */
@Mapper
public interface SysFormMapper {

    int deleteById(Long id);

    int insert(SysForm sysForm);

    SysForm selectById(Long id);

    int update(SysForm sysForm);
}
