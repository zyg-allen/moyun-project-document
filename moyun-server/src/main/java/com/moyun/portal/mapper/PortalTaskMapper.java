package com.moyun.portal.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.moyun.portal.domain.entity.PortalTask;

/**
 * 任务定义 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalTaskMapper extends BaseMapper<PortalTask> {

    /**
     * 通过任务编码查询任务
     */
    @Select("SELECT * FROM portal_task WHERE code = #{code}")
    PortalTask selectByCode(@Param("code") String code);

    /**
     * 查询所有启用的任务
     */
    @Select("SELECT * FROM portal_task WHERE status = 'active' ORDER BY task_type ASC, id ASC")
    List<PortalTask> selectAllActive();
}
