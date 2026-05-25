package com.moyun.ext.flowable.service;

import com.moyun.ext.flowable.domain.entity.FlowProcDefDto;

import java.util.List;
import java.util.zip.ZipInputStream;

/**
 * 流程定义 服务层
 *
 * @author ruoyi
 */
public interface IFlowDefinitionService {

    List<FlowProcDefDto> selectProcessDefinitionList(FlowProcDefDto flowProcDefDto);

    FlowProcDefDto selectProcessDefinitionById(String processDefinitionId);

    FlowProcDefDto selectProcessDefinitionByKey(String processDefinitionKey);

    String importProcessDefinition(ZipInputStream zipInputStream, String filename);

    String deployStartProcess(String processDefinitionKey);

    void suspendOrActivateProcessDefinitionById(String processDefinitionId, String action);

    void deleteDeploymentById(String deploymentId);

    String getProcessDiagramResourceName(String processDefinitionId);
}
