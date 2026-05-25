package com.moyun.ext.flowable.mapper;

import com.moyun.ext.flowable.domain.entity.SysListener;
import org.apache.ibatis.annotations.Mapper;

/**
 * 流程监听器配置Mapper
 *
 * @author ruoyi
 */
@Mapper
public interface SysListenerMapper {

    int deleteById(Long id);

    int insert(SysListener sysListener);

    SysListener selectById(Long id);

    int update(SysListener sysListener);
}
