package com.moyun.system.flowable.flow;

import org.flowable.bpmn.model.*;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Flowable工具类
 *
 * @author Tony
 * @date 2021-04-03
 */
public class FlowableUtils {

    private static final Logger log = LoggerFactory.getLogger(FlowableUtils.class);

    /**
     * 获取元素出口连线
     */
    public static List<SequenceFlow> getElementOutgoingFlows(FlowElement flowElement) {
        List<SequenceFlow> sequenceFlows = null;
        if (flowElement instanceof FlowNode) {
            sequenceFlows = ((FlowNode) flowElement).getOutgoingFlows();
        }
        return sequenceFlows;
    }

    /**
     * 获取元素入口连线
     */
    public static List<SequenceFlow> getElementIncomingFlows(FlowElement flowElement) {
        List<SequenceFlow> sequenceFlows = null;
        if (flowElement instanceof FlowNode) {
            sequenceFlows = ((FlowNode) flowElement).getIncomingFlows();
        }
        return sequenceFlows;
    }

    /**
     * 获取当前任务节点
     */
    public static FlowElement getCurrentElement(RepositoryService repositoryService, Task task) {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(task.getProcessDefinitionId())
                .singleResult();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
        return bpmnModel.getFlowElement(task.getTaskDefinitionKey());
    }

    /**
     * 从 flowElement 获取指定名称的拓展元素
     */
    public static ExtensionElement getExtensionElementFromFlowElementByName(FlowElement flowElement, String extensionElementName) {
        if (flowElement == null) {
            return null;
        }
        Map<String, List<ExtensionElement>> extensionElements = flowElement.getExtensionElements();
        for (Map.Entry<String, List<ExtensionElement>> stringEntry : extensionElements.entrySet()) {
            if (stringEntry.getKey().equals(extensionElementName)) {
                for (ExtensionElement extensionElement : stringEntry.getValue()) {
                    if (extensionElement.getName().equals(extensionElementName)) {
                        return extensionElement;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 获取当前任务节点扩展属性信息
     */
    public static List<Object> getPropertyElement(RepositoryService repositoryService, Task task) {
        FlowElement flowElement = getCurrentElement(repositoryService, task);
        ExtensionElement extensionElement = getExtensionElementFromFlowElementByName(flowElement, "properties");
        if (extensionElement == null) {
            return Collections.emptyList();
        }
        return getPropertyExtensionElementByName(extensionElement, "property");
    }

    /**
     * 根据属性名获取扩展元素中的扩展属性列表
     */
    public static List<Object> getPropertyExtensionElementByName(ExtensionElement extensionElement, String attributesName) {
        try {
            return Optional.ofNullable(extensionElement.getChildElements().get(attributesName))
                    .orElse(Collections.emptyList())
                    .stream()
                    .map(element -> {
                        Map<String, List<ExtensionAttribute>> attributes = element.getAttributes();
                        Object propertyDto = new Object();
                        Arrays.stream(propertyDto.getClass().getDeclaredFields())
                                .forEach(field -> {
                                    field.setAccessible(true);
                                    attributes.getOrDefault(field.getName(), Collections.emptyList())
                                            .stream()
                                            .findFirst()
                                            .ifPresent(attribute -> {
                                                try {
                                                    field.set(propertyDto, attribute.getValue());
                                                } catch (IllegalAccessException e) {
                                                    log.error("反射设置属性值失败", e);
                                                }
                                            });
                                });
                        return propertyDto;
                    }).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取扩展属性列表失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取所有节点信息（包含子节点）
     */
    public static Collection<FlowElement> getAllElements(Collection<FlowElement> flowElements, Collection<FlowElement> allElements) {
        if (allElements == null) {
            allElements = new ArrayList<>();
        }
        if (flowElements == null) {
            return allElements;
        }
        for (FlowElement flowElement : flowElements) {
            allElements.add(flowElement);
            if (flowElement instanceof SubProcess) {
                SubProcess subProcess = (SubProcess) flowElement;
                getAllElements(subProcess.getFlowElements(), allElements);
            }
        }
        return allElements;
    }

    /**
     * 迭代获取父级用户任务节点
     */
    public static List<UserTask> iteratorFindParentUserTasks(FlowElement source, Set<String> hasSequenceFlow, List<UserTask> userTaskList) {
        if (userTaskList == null) {
            userTaskList = new ArrayList<>();
        }
        if (source == null) {
            return userTaskList;
        }
        if (hasSequenceFlow == null) {
            hasSequenceFlow = new HashSet<>();
        }
        // 获取入口连线
        List<SequenceFlow> sequenceFlows = getElementIncomingFlows(source);
        if (sequenceFlows != null) {
            for (SequenceFlow sequenceFlow : sequenceFlows) {
                if (hasSequenceFlow.contains(sequenceFlow.getId())) {
                    continue;
                }
                hasSequenceFlow.add(sequenceFlow.getId());
                FlowElement sourceFlowElement = sequenceFlow.getSourceFlowElement();
                if (sourceFlowElement instanceof UserTask) {
                    userTaskList.add((UserTask) sourceFlowElement);
                } else {
                    iteratorFindParentUserTasks(sourceFlowElement, hasSequenceFlow, userTaskList);
                }
            }
        }
        return userTaskList;
    }

    /**
     * 迭代获取子级用户任务节点
     */
    public static List<UserTask> iteratorFindChildUserTasks(FlowElement source, List<String> runTaskKeyList, Set<String> hasSequenceFlow, List<UserTask> userTaskList) {
        if (userTaskList == null) {
            userTaskList = new ArrayList<>();
        }
        if (source == null) {
            return userTaskList;
        }
        if (hasSequenceFlow == null) {
            hasSequenceFlow = new HashSet<>();
        }
        // 获取出口连线
        List<SequenceFlow> sequenceFlows = getElementOutgoingFlows(source);
        if (sequenceFlows != null) {
            for (SequenceFlow sequenceFlow : sequenceFlows) {
                if (hasSequenceFlow.contains(sequenceFlow.getId())) {
                    continue;
                }
                hasSequenceFlow.add(sequenceFlow.getId());
                FlowElement targetFlowElement = sequenceFlow.getTargetFlowElement();
                if (targetFlowElement instanceof UserTask) {
                    UserTask userTask = (UserTask) targetFlowElement;
                    if (runTaskKeyList != null && runTaskKeyList.contains(userTask.getId())) {
                        userTaskList.add(userTask);
                    }
                } else {
                    iteratorFindChildUserTasks(targetFlowElement, runTaskKeyList, hasSequenceFlow, userTaskList);
                }
            }
        }
        return userTaskList;
    }

    /**
     * 历史任务实例清洗，将回滚导致的脏数据清洗掉
     */
    public static List<String> historicTaskInstanceClean(Collection<FlowElement> allElements, List<HistoricTaskInstance> historicTaskInstanceList) {
        List<String> lastHistoricTaskInstanceList = new ArrayList<>();
        if (historicTaskInstanceList == null || historicTaskInstanceList.size() == 0) {
            return lastHistoricTaskInstanceList;
        }
        // 倒序遍历
        for (int i = historicTaskInstanceList.size() - 1; i >= 0; i--) {
            HistoricTaskInstance historicTaskInstance = historicTaskInstanceList.get(i);
            lastHistoricTaskInstanceList.add(historicTaskInstance.getTaskDefinitionKey());
        }
        return lastHistoricTaskInstanceList;
    }

    /**
     * 迭代检查是否为串行关系
     */
    public static Boolean iteratorCheckSequentialReferTarget(FlowElement source, String targetKey, Set<String> hasSequenceFlow, Boolean isSequential) {
        if (isSequential == null) {
            isSequential = false;
        }
        if (source == null) {
            return isSequential;
        }
        if (hasSequenceFlow == null) {
            hasSequenceFlow = new HashSet<>();
        }
        // 获取出口连线
        List<SequenceFlow> sequenceFlows = getElementOutgoingFlows(source);
        if (sequenceFlows != null) {
            for (SequenceFlow sequenceFlow : sequenceFlows) {
                if (hasSequenceFlow.contains(sequenceFlow.getId())) {
                    continue;
                }
                hasSequenceFlow.add(sequenceFlow.getId());
                FlowElement targetFlowElement = sequenceFlow.getTargetFlowElement();
                if (targetFlowElement.getId().equals(targetKey)) {
                    isSequential = true;
                    break;
                } else {
                    isSequential = iteratorCheckSequentialReferTarget(targetFlowElement, targetKey, hasSequenceFlow, isSequential);
                    if (isSequential) {
                        break;
                    }
                }
            }
        }
        return isSequential;
    }

    /**
     * 查找路线
     */
    public static List<List<UserTask>> findRoad(UserTask source, Set<String> hasSequenceFlow, List<UserTask> road, List<List<UserTask>> roads) {
        if (roads == null) {
            roads = new ArrayList<>();
        }
        if (road == null) {
            road = new ArrayList<>();
        }
        if (source == null) {
            return roads;
        }
        if (hasSequenceFlow == null) {
            hasSequenceFlow = new HashSet<>();
        }
        // 获取入口连线
        List<SequenceFlow> sequenceFlows = getElementIncomingFlows(source);
        if (sequenceFlows != null && sequenceFlows.size() > 0) {
            for (SequenceFlow sequenceFlow : sequenceFlows) {
                if (hasSequenceFlow.contains(sequenceFlow.getId())) {
                    continue;
                }
                hasSequenceFlow.add(sequenceFlow.getId());
                FlowElement sourceFlowElement = sequenceFlow.getSourceFlowElement();
                if (sourceFlowElement instanceof UserTask) {
                    List<UserTask> newRoad = new ArrayList<>(road);
                    newRoad.add((UserTask) sourceFlowElement);
                    findRoad((UserTask) sourceFlowElement, hasSequenceFlow, newRoad, roads);
                } else {
                    findRoad(source, hasSequenceFlow, road, roads);
                }
            }
        } else {
            roads.add(road);
        }
        return roads;
    }
}