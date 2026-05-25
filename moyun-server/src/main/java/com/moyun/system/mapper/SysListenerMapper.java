package com.moyun.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.system.domain.entity.SysListener;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 流程监听Mapper接口
 * 
 * @author Tony
 * @date 2022-12-25
 */
@Mapper
public interface SysListenerMapper extends BaseMapper<SysListener> {
    
    /**
     * 查询流程监听列表
     * 
     * @param sysListener 流程监听
     * @return 流程监听集合
     */
    List<SysListener> selectSysListenerList(SysListener sysListener);
}