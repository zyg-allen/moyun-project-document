package com.moyun.ext.flowable.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.system.domain.FlowProcDefDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FlowDeployMapper {
    /**
     * 查询流程定义列表（不分页）
     *
     * @param name 流程名称
     * @return 流程定义列表
     */
    List<FlowProcDefDto> selectDeployList(String name);

    /**
     * 分页查询流程定义列表
     *
     * @param page 分页对象
     * @param name 流程名称
     * @return 分页结果
     */
    IPage<FlowProcDefDto> selectDeployPage(Page<FlowProcDefDto> page, @Param("name") String name);
}