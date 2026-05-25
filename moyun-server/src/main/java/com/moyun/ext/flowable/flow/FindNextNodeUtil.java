package com.moyun.ext.flowable.flow;

import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.*;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.task.api.Task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 查找下一节点工具类
 */
public class FindNextNodeUtil {

    /**
     * 获取下一个用户任务节点信息
     *
     * @param repositoryService 仓库服务
     * @param task 当前任务
     * @param variables 流程变量
     * @return 下一个用户任务节点列表
     */
    public static List<UserTask> getNextUserTasks(RepositoryService repositoryService, Task task, Map<String, Object> variables) {
        List<UserTask> nextUserTasks = new ArrayList<>();
        
        // 获取流程定义
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(task.getProcessDefinitionId())
                .singleResult();
        
        if (processDefinition == null) {
            return nextUserTasks;
        }
        
        // 获取BPMN模型
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
        Process process = bpmnModel.getProcesses().get(0);
        
        // 获取所有节点
        Collection<FlowElement> flowElements = process.getFlowElements();
        
        // 查找当前任务节点
        FlowElement currentFlowElement = null;
        for (FlowElement flowElement : flowElements) {
            if (flowElement.getId().equals(task.getTaskDefinitionKey())) {
                currentFlowElement = flowElement;
                break;
            }
        }
        
        if (currentFlowElement == null) {
            return nextUserTasks;
        }
        
        // 查找下一个用户任务节点
        findNextUserTasks(currentFlowElement, nextUserTasks, flowElements, variables);
        
        return nextUserTasks;
    }

    /**
     * 发起流程时获取下一个用户任务节点信息
     *
     * @param repositoryService 仓库服务
     * @param processDefinition 流程定义
     * @param variables 流程变量
     * @return 下一个用户任务节点列表
     */
    public static List<UserTask> getNextUserTasksByStart(RepositoryService repositoryService, ProcessDefinition processDefinition, Map<String, Object> variables) {
        List<UserTask> nextUserTasks = new ArrayList<>();
        
        if (processDefinition == null) {
            return nextUserTasks;
        }
        
        // 获取BPMN模型
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
        Process process = bpmnModel.getProcesses().get(0);
        
        // 获取开始节点
        Collection<FlowElement> flowElements = process.getFlowElements();
        FlowElement startEvent = null;
        for (FlowElement flowElement : flowElements) {
            if (flowElement instanceof StartEvent) {
                startEvent = flowElement;
                break;
            }
        }
        
        if (startEvent == null) {
            return nextUserTasks;
        }
        
        // 查找下一个用户任务节点
        findNextUserTasks(startEvent, nextUserTasks, flowElements, variables);
        
        return nextUserTasks;
    }

    /**
     * 递归查找下一个用户任务节点
     */
    private static void findNextUserTasks(FlowElement flowElement, List<UserTask> userTasks, Collection<FlowElement> flowElements, Map<String, Object> variables) {
        if (flowElement instanceof FlowNode) {
            FlowNode flowNode = (FlowNode) flowElement;
            List<SequenceFlow> outgoingFlows = flowNode.getOutgoingFlows();
            
            for (SequenceFlow sequenceFlow : outgoingFlows) {
                FlowElement targetFlowElement = sequenceFlow.getTargetFlowElement();
                
                if (targetFlowElement instanceof UserTask) {
                    userTasks.add((UserTask) targetFlowElement);
                } else if (targetFlowElement instanceof Gateway) {
                    // 如果是网关，继续递归查找
                    findNextUserTasks(targetFlowElement, userTasks, flowElements, variables);
                } else if (targetFlowElement instanceof FlowNode) {
                    // 其他类型的节点，继续查找
                    findNextUserTasks(targetFlowElement, userTasks, flowElements, variables);
                }
            }
        }
    }
}