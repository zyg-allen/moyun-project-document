package com.moyun.ext.flowable.service;

import java.util.List;
import java.util.Map;

/**
 * 流程任务 服务层
 *
 * @author ruoyi
 */
public interface IFlowTaskService {

    List<Map<String, Object>> selectTodoTaskList(Map<String, Object> params);

    List<Map<String, Object>> selectFinishedTaskList(Map<String, Object> params);

    Map<String, Object> selectTaskById(String taskId);

    Map<String, Object> selectTaskByProcInsId(String processInstanceId);

    void completeTask(String taskId, Map<String, Object> variables);

    void delegateTask(String taskId, String userId);

    void assigneeTask(String taskId, String userId);

    Map<String, Object> getProcessDiagram(String processInstanceId);
}
