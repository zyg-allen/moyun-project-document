package com.moyun.system.flowable.service;

import com.moyun.common.core.domain.AjaxResult;
import com.moyun.system.flowable.domain.vo.FlowTaskVo;
import org.flowable.engine.history.HistoricProcessInstance;

import java.util.Map;

/**
 * 流程实例Service接口
 *
 * @author Tony
 * @date 2021-04-03
 */
public interface IFlowInstanceService {
    
    /**
     * 结束流程实例
     *
     * @param vo 流程任务信息
     */
    void stopProcessInstance(FlowTaskVo vo);

    /**
     * 激活或挂起流程实例
     *
     * @param state      状态（1:激活,2:挂起）
     * @param instanceId 流程实例ID
     */
    void updateState(Integer state, String instanceId);

    /**
     * 删除流程实例
     *
     * @param instanceId   流程实例ID
     * @param deleteReason  删除原因
     */
    void delete(String instanceId, String deleteReason);

    /**
     * 根据实例ID查询历史实例数据
     *
     * @param processInstanceId 流程实例ID
     * @return 历史流程实例
     */
    HistoricProcessInstance getHistoricProcessInstanceById(String processInstanceId);

    /**
     * 根据流程定义ID启动流程实例
     *
     * @param procDefId 流程定义Id
     * @param variables 流程变量
     * @return 结果
     */
    AjaxResult startProcessInstanceById(String procDefId, Map<String, Object> variables);
}