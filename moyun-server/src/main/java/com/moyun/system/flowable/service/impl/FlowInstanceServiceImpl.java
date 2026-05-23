package com.moyun.system.flowable.service.impl;

import com.moyun.common.core.domain.AjaxResult;
import com.moyun.common.utils.SecurityUtils;
import com.moyun.system.flowable.domain.vo.FlowTaskVo;
import com.moyun.system.flowable.factory.FlowServiceFactory;
import com.moyun.system.flowable.service.IFlowInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.history.HistoricProcessInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;

/**
 * <p>工作流流程实例管理<p>
 *
 * @author Tony
 * @date 2021-04-03
 */
@Service
@Slf4j
public class FlowInstanceServiceImpl extends FlowServiceFactory implements IFlowInstanceService {

    /**
     * 结束流程实例
     *
     * @param vo 流程任务信息
     */
    @Override
    public void stopProcessInstance(FlowTaskVo vo) {
        String taskId = vo.getTaskId();
        // TODO: 实现结束流程实例逻辑
    }

    /**
     * 激活或挂起流程实例
     *
     * @param state      状态（1:激活,2:挂起）
     * @param instanceId 流程实例ID
     */
    @Override
    public void updateState(Integer state, String instanceId) {
        // 激活
        if (state == 1) {
            runtimeService.activateProcessInstanceById(instanceId);
        }
        // 挂起
        if (state == 2) {
            runtimeService.suspendProcessInstanceById(instanceId);
        }
    }

    /**
     * 删除流程实例
     *
     * @param instanceId   流程实例ID
     * @param deleteReason 删除原因
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String instanceId, String deleteReason) {
        // 查询历史数据
        HistoricProcessInstance historicProcessInstance = getHistoricProcessInstanceById(instanceId);
        if (historicProcessInstance.getEndTime() != null) {
            historyService.deleteHistoricProcessInstance(historicProcessInstance.getId());
            return;
        }
        // 删除流程实例
        runtimeService.deleteProcessInstance(instanceId, deleteReason);
        // 删除历史流程实例
        historyService.deleteHistoricProcessInstance(instanceId);
    }

    /**
     * 根据实例ID查询历史实例数据
     *
     * @param processInstanceId 流程实例ID
     * @return 历史流程实例
     */
    @Override
    public HistoricProcessInstance getHistoricProcessInstanceById(String processInstanceId) {
        HistoricProcessInstance historicProcessInstance =
                historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        if (Objects.isNull(historicProcessInstance)) {
            throw new FlowableObjectNotFoundException("流程实例不存在: " + processInstanceId);
        }
        return historicProcessInstance;
    }

    /**
     * 根据流程定义ID启动流程实例
     *
     * @param procDefId 流程定义Id
     * @param variables 流程变量
     * @return 结果
     */
    @Override
    public AjaxResult startProcessInstanceById(String procDefId, Map<String, Object> variables) {
        try {
            // 设置流程发起人Id到流程中
            Long userId = SecurityUtils.getLoginUser().getUser().getUserId();
            variables.put("initiator", userId);
            variables.put("_FLOWABLE_SKIP_EXPRESSION_ENABLED", true);
            runtimeService.startProcessInstanceById(procDefId, variables);
            return AjaxResult.success("流程启动成功");
        } catch (Exception e) {
            log.error("流程启动错误", e);
            return AjaxResult.error("流程启动错误");
        }
    }
}