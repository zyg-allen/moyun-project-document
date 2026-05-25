package com.moyun.ext.flowable.service;

import com.moyun.ext.flowable.domain.entity.FlowProcDefDto;

import java.io.InputStream;
import java.util.Map;

/**
 * 流程实例 服务层
 *
 * @author ruoyi
 */
public interface IFlowInstanceService {

    Map<String, Object> selectProcessInstanceById(String processInstanceId);

    Map<String, Object> selectProcessInstanceByProcInsId(String processInstanceId);

    void deleteProcessInstanceByIds(String[] ids);

    void deleteProcessInstanceById(String processInstanceId, String deleteReason);

    Map<String, Object> startProcessInstanceByKey(String processDefinitionKey, Map<String, Object> variables);

    Map<String, Object> startProcessInstanceById(String processDefinitionId, Map<String, Object> variables);

    void completeTask(String taskId, Map<String, Object> variables);
}
