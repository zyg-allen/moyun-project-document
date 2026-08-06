package com.moyun.ext.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.ext.ai.dto.WorkflowExecutionEvent;
import com.moyun.ext.ai.entity.Workflow;
import com.moyun.ext.ai.entity.WorkflowExecution;
import com.moyun.ext.ai.entity.WorkflowVersion;
import com.moyun.ext.ai.engine.workflow.WorkflowEngine;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 工作流服务接口
 *
 * @author laomao
 */
public interface WorkflowService extends IService<Workflow> {

    /**
     * 获取所有工作流
     */
    List<Workflow> listAll();

    /**
     * 获取启用的工作流
     */
    List<Workflow> listEnabled();

    /**
     * 创建工作流
     */
    Workflow create(Workflow workflow);

    /**
     * 更新工作流
     */
    Workflow updateWorkflow(Workflow workflow);

    /**
     * 保存工作流图
     */
    void saveGraph(Long workflowId, String graphData);

    /**
     * 发布工作流
     */
    void publish(Long workflowId);

    /**
     * 禁用工作流
     */
    void disable(Long workflowId);

    /**
     * 启用工作流
     */
    void enable(Long workflowId);

    /**
     * 执行工作流
     */
    WorkflowEngine.WorkflowResult execute(Long workflowId, Map<String, Object> input);
    
    /**
     * 测试执行工作流（跳过enabled检查）
     */
    WorkflowEngine.WorkflowResult executeForTest(Long workflowId, Map<String, Object> input);
    
    /**
     * 带事件回调的执行工作流（用于SSE实时推送）
     * 
     * @param workflowId 工作流ID
     * @param input 输入参数
     * @param testRun 是否测试运行
     * @param eventCallback 事件回调函数
     * @return 执行结果
     */
    WorkflowEngine.WorkflowResult executeWithCallback(Long workflowId, Map<String, Object> input, 
            boolean testRun, Consumer<WorkflowExecutionEvent> eventCallback);

    /**
     * 获取工作流执行历史
     */
    List<WorkflowExecution> getExecutionHistory(Long workflowId);

    /**
     * 获取执行详情
     */
    WorkflowExecution getExecution(Long executionId);

    /**
     * 删除工作流
     */
    void deleteWorkflow(Long workflowId);

    /**
     * 绑定工作流到智能体
     */
    void bindToAgent(Long workflowId, Long agentId);

    /**
     * 解绑工作流
     */
    void unbindFromAgent(Long workflowId, Long agentId);

    /**
     * 获取智能体绑定的工作流列表
     */
    List<Workflow> getAgentWorkflows(Long agentId);

    /**
     * 根据名称执行工作流
     */
    WorkflowEngine.WorkflowResult executeByName(String name, Map<String, Object> input);

    /**
     * 根据名称获取工作流
     */
    Workflow getByName(String name);

    /**
     * 获取工作流统计数据
     */
    Map<String, Object> getStats();

    /**
     * 获取工作流执行排行
     */
    List<Map<String, Object>> getExecutionRanking(int limit);

    /**
     * 获取工作流版本列表
     */
    List<WorkflowVersion> getVersions(Long workflowId);

    /**
     * 创建新版本
     */
    WorkflowVersion createVersion(Long workflowId, String graphData, String description);

    /**
     * 回滚到指定版本
     */
    Workflow rollbackVersion(Long workflowId, Integer targetVersion);
}
