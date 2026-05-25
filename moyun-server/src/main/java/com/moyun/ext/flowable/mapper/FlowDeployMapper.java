package com.moyun.ext.flowable.mapper;

import com.moyun.ext.flowable.domain.entity.FlowProcDefDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 流程定义Mapper
 *
 * @author ruoyi
 */
@Mapper
public interface FlowDeployMapper {

    List<FlowProcDefDto> selectProcessDefinitionList(FlowProcDefDto flowProcDefDto);

    FlowProcDefDto selectProcessDefinitionById(@Param("processDefinitionId") String processDefinitionId);

    FlowProcDefDto selectProcessDefinitionByKey(@Param("processDefinitionKey") String processDefinitionKey);

    int insertDeployment(FlowProcDefDto flowProcDefDto);

    int deleteDeploymentById(@Param("deploymentId") String deploymentId);
}
