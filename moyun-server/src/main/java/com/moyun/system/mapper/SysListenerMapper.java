package com.moyun.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.moyun.system.domain.entity.SysListener;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

    /**
     * 分页查询流程监听（MyBatis-Plus 标准分页，配合 PaginationInnerInterceptor）
     *
     * @param page        分页对象
     * @param sysListener 流程监听查询条件
     * @return 分页结果
     */
    IPage<SysListener> selectSysListenerPage(IPage<SysListener> page, @Param("query") SysListener sysListener);
}