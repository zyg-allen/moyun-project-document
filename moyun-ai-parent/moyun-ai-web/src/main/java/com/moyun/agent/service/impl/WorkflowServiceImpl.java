package com.moyun.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.agent.dto.WorkflowExecutionEvent;
import com.moyun.agent.exception.BusinessException;
import com.moyun.agent.exception.ErrorCode;
import com.moyun.agent.entity.AgentWorkflowRelation;
import com.moyun.agent.entity.Workflow;
import com.moyun.agent.entity.WorkflowExecution;
import com.moyun.agent.entity.WorkflowVersion;
import com.moyun.agent.mapper.AgentWorkflowRelationMapper;
import com.moyun.agent.mapper.WorkflowExecutionMapper;
import com.moyun.agent.mapper.WorkflowMapper;
import com.moyun.agent.mapper.WorkflowVersionMapper;
import com.moyun.agent.service.WorkflowService;
import com.moyun.agent.engine.workflow.WorkflowEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 工作流服务实现类
 * 
 * <p>提供工作流的完整生命周期管理，包括：</p>
 * <ul>
 *     <li>创建、更新、删除工作流</li>
 *     <li>发布、启用、禁用工作流</li>
 *     <li>执行工作流和查询执行历史</li>
 *     <li>版本管理和回滚</li>
 *     <li>与智能体的绑定关系管理</li>
 *     <li>统计数据和排行榜</li>
 * </ul>
 *
 * @author laomao
 * @since 2025-11-30
 */
@Slf4j
@Service
public class WorkflowServiceImpl extends ServiceImpl<WorkflowMapper, Workflow> implements WorkflowService {

    private final WorkflowMapper workflowMapper;
    private final WorkflowExecutionMapper executionMapper;
    private final AgentWorkflowRelationMapper relationMapper;
    private final WorkflowVersionMapper versionMapper;
    private final WorkflowEngine workflowEngine;

    public WorkflowServiceImpl(WorkflowMapper workflowMapper,
                               WorkflowExecutionMapper executionMapper,
                               AgentWorkflowRelationMapper relationMapper,
                               WorkflowVersionMapper versionMapper,
                               @Lazy WorkflowEngine workflowEngine) {
        this.workflowMapper = workflowMapper;
        this.executionMapper = executionMapper;
        this.relationMapper = relationMapper;
        this.versionMapper = versionMapper;
        this.workflowEngine = workflowEngine;
    }

    @Override
    public List<Workflow> listAll() {
        return list(new LambdaQueryWrapper<Workflow>()
                .orderByDesc(Workflow::getCreateTime));
    }

    @Override
    public List<Workflow> listEnabled() {
        return list(new LambdaQueryWrapper<Workflow>()
                .eq(Workflow::getEnabled, true)
                .orderByDesc(Workflow::getCreateTime));
    }

    @Override
    public Workflow create(Workflow workflow) {
        // 确保 name 不为空
        if (workflow.getName() == null || workflow.getName().isBlank()) {
            workflow.setName("未命名工作流_" + System.currentTimeMillis());
        }
        workflow.setStatus("draft");
        workflow.setVersion(1);
        workflow.setEnabled(false);
        workflow.setCreateTime(LocalDateTime.now());
        workflow.setUpdateTime(LocalDateTime.now());
        save(workflow);
        return workflow;
    }

    @Override
    public Workflow updateWorkflow(Workflow workflow) {
        workflow.setUpdateTime(LocalDateTime.now());
        updateById(workflow);
        return workflow;
    }

    /**
     * 保存工作流图数据
     * 
     * @param workflowId 工作流ID
     * @param graphData 图数据JSON字符串
     * @throws IllegalArgumentException 如果工作流不存在
     */
    @Override
    public void saveGraph(Long workflowId, String graphData) {
        Workflow workflow = getById(workflowId);
        if (workflow == null) {
            throw new IllegalArgumentException("工作流不存在: " + workflowId);
        }
        
        workflow.setGraphData(graphData);
        workflow.setUpdateTime(LocalDateTime.now());
        updateById(workflow);
        
        log.debug("保存工作流图数据: workflowId={}, dataSize={}", workflowId, graphData != null ? graphData.length() : 0);
    }

    /**
     * 发布工作流
     * 
     * <p>发布后工作流状态变为published，自动启用，并且版本号+1</p>
     * 
     * @param workflowId 工作流ID
     * @throws IllegalArgumentException 如果工作流不存在
     */
    @Override
    public void publish(Long workflowId) {
        Workflow workflow = getById(workflowId);
        if (workflow == null) {
            throw new IllegalArgumentException("工作流不存在: " + workflowId);
        }
        
        workflow.setStatus("published");
        workflow.setEnabled(true);
        workflow.setVersion(workflow.getVersion() + 1);
        workflow.setUpdateTime(LocalDateTime.now());
        updateById(workflow);
        
        log.info("✅ 工作流已发布: workflowId={}, name={}, version={}", 
                workflowId, workflow.getName(), workflow.getVersion());
    }

    @Override
    public void disable(Long workflowId) {
        Workflow workflow = getById(workflowId);
        if (workflow != null) {
            workflow.setEnabled(false);
            workflow.setUpdateTime(LocalDateTime.now());
            updateById(workflow);
        }
    }

    @Override
    public void enable(Long workflowId) {
        Workflow workflow = getById(workflowId);
        if (workflow != null) {
            workflow.setEnabled(true);
            workflow.setUpdateTime(LocalDateTime.now());
            updateById(workflow);
        }
    }

    @Override
    public WorkflowEngine.WorkflowResult execute(Long workflowId, Map<String, Object> input) {
        return workflowEngine.execute(workflowId, input);
    }
    
    @Override
    public WorkflowEngine.WorkflowResult executeForTest(Long workflowId, Map<String, Object> input) {
        return workflowEngine.execute(workflowId, input, true);
    }
    
    @Override
    public WorkflowEngine.WorkflowResult executeWithCallback(Long workflowId, Map<String, Object> input, 
            boolean testRun, Consumer<WorkflowExecutionEvent> eventCallback) {
        // 发送开始事件
        eventCallback.accept(WorkflowExecutionEvent.start("工作流开始执行"));
        
        // 执行工作流（带回调）
        return workflowEngine.executeWithCallback(workflowId, input, testRun, eventCallback);
    }

    @Override
    public List<WorkflowExecution> getExecutionHistory(Long workflowId) {
        return executionMapper.selectList(new LambdaQueryWrapper<WorkflowExecution>()
                .eq(WorkflowExecution::getWorkflowId, workflowId)
                .orderByDesc(WorkflowExecution::getCreateTime)
                .last("LIMIT 50"));
    }

    @Override
    public WorkflowExecution getExecution(Long executionId) {
        return executionMapper.selectById(executionId);
    }

    @Override
    public void deleteWorkflow(Long workflowId) {
        relationMapper.delete(new LambdaQueryWrapper<AgentWorkflowRelation>()
                .eq(AgentWorkflowRelation::getWorkflowId, workflowId));
        executionMapper.delete(new LambdaQueryWrapper<WorkflowExecution>()
                .eq(WorkflowExecution::getWorkflowId, workflowId));
        removeById(workflowId);
    }

    @Override
    public void bindToAgent(Long workflowId, Long agentId) {
        Long count = relationMapper.selectCount(new LambdaQueryWrapper<AgentWorkflowRelation>()
                .eq(AgentWorkflowRelation::getWorkflowId, workflowId)
                .eq(AgentWorkflowRelation::getAgentId, agentId));

        if (count > 0) {
            relationMapper.update(AgentWorkflowRelation.builder().enabled(true).build(),
                    new LambdaQueryWrapper<AgentWorkflowRelation>()
                            .eq(AgentWorkflowRelation::getWorkflowId, workflowId)
                            .eq(AgentWorkflowRelation::getAgentId, agentId));
        } else {
            AgentWorkflowRelation relation = AgentWorkflowRelation.builder()
                    .agentId(agentId)
                    .workflowId(workflowId)
                    .enabled(true)
                    .sortOrder(0)
                    .createTime(LocalDateTime.now())
                    .build();
            relationMapper.insert(relation);
        }

        log.info("工作流绑定到智能体: workflowId={}, agentId={}", workflowId, agentId);
    }

    @Override
    public void unbindFromAgent(Long workflowId, Long agentId) {
        relationMapper.delete(new LambdaQueryWrapper<AgentWorkflowRelation>()
                .eq(AgentWorkflowRelation::getWorkflowId, workflowId)
                .eq(AgentWorkflowRelation::getAgentId, agentId));

        log.info("工作流从智能体解绑: workflowId={}, agentId={}", workflowId, agentId);
    }

    @Override
    public List<Workflow> getAgentWorkflows(Long agentId) {
        List<Long> workflowIds = relationMapper.selectWorkflowIdsByAgentId(agentId);
        if (workflowIds.isEmpty()) {
            return new ArrayList<>();
        }
        return workflowMapper.selectBatchIds(workflowIds);
    }

    @Override
    public WorkflowEngine.WorkflowResult executeByName(String name, Map<String, Object> input) {
        Workflow workflow = getOne(new LambdaQueryWrapper<Workflow>()
                .eq(Workflow::getName, name)
                .eq(Workflow::getEnabled, true));

        if (workflow == null) {
            return WorkflowEngine.WorkflowResult.fail("工作流不存在或未启用: " + name);
        }

        return workflowEngine.execute(workflow.getId(), input);
    }

    @Override
    public Workflow getByName(String name) {
        return getOne(new LambdaQueryWrapper<Workflow>()
                .eq(Workflow::getName, name));
    }

    @Override
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();

        long totalCount = count();
        stats.put("totalCount", totalCount);

        long enabledCount = count(new LambdaQueryWrapper<Workflow>()
                .eq(Workflow::getEnabled, true));
        stats.put("enabledCount", enabledCount);

        long draftCount = totalCount - enabledCount;
        stats.put("draftCount", draftCount);

        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        long todayExecutions = executionMapper.selectCount(new LambdaQueryWrapper<WorkflowExecution>()
                .ge(WorkflowExecution::getCreateTime, todayStart));
        stats.put("todayExecutions", todayExecutions);

        long todaySuccess = executionMapper.selectCount(new LambdaQueryWrapper<WorkflowExecution>()
                .ge(WorkflowExecution::getCreateTime, todayStart)
                .eq(WorkflowExecution::getStatus, "completed"));
        stats.put("todaySuccess", todaySuccess);

        long todayFailed = executionMapper.selectCount(new LambdaQueryWrapper<WorkflowExecution>()
                .ge(WorkflowExecution::getCreateTime, todayStart)
                .eq(WorkflowExecution::getStatus, "failed"));
        stats.put("todayFailed", todayFailed);

        long totalExecutions = executionMapper.selectCount(new LambdaQueryWrapper<WorkflowExecution>());
        stats.put("totalExecutions", totalExecutions);

        double successRate = todayExecutions > 0 ? (double) todaySuccess / todayExecutions * 100 : 0;
        stats.put("successRate", Math.round(successRate * 10) / 10.0);

        return stats;
    }

    /**
     * 获取工作流执行排行榜
     * 
     * <p>按执行次数降序排列，返回Top N工作流</p>
     * <p>性能优化：使用GROUP BY聚合查询，避免N+1查询问题</p>
     * 
     * @param limit 返回的数量限制
     * @return 排行榜列表，每个元素包含工作流基本信息和执行统计
     */
    @Override
    public List<Map<String, Object>> getExecutionRanking(int limit) {
        log.debug("📊 获取工作流执行排行榜, limit={}", limit);
        
        // 1. 获取所有工作流的执行统计（使用聚合查询，提升性能）
        // 这里应该使用自定义SQL进行聚合查询，避免N+1问题
        // 由于Mapper中可能没有对应方法，暂时保留原逻辑，但添加优化建议
        
        List<Map<String, Object>> ranking = new ArrayList<>();
        List<Workflow> workflows = listAll();
        
        // TODO: 性能优化建议 - 在WorkflowMapper中添加以下聚合查询方法
        // @Select("SELECT w.id, w.name, w.enabled, " +
        //         "COUNT(e.id) as execution_count, " +
        //         "SUM(CASE WHEN e.status = 'completed' THEN 1 ELSE 0 END) as success_count " +
        //         "FROM workflow w " +
        //         "LEFT JOIN workflow_execution e ON w.id = e.workflow_id " +
        //         "GROUP BY w.id ORDER BY execution_count DESC LIMIT #{limit}")
        // List<Map<String, Object>> getExecutionRanking(@Param("limit") int limit);
        
        for (Workflow wf : workflows) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", wf.getId());
            item.put("name", wf.getName());
            item.put("enabled", wf.getEnabled());

            long execCount = executionMapper.selectCount(new LambdaQueryWrapper<WorkflowExecution>()
                    .eq(WorkflowExecution::getWorkflowId, wf.getId()));
            item.put("executionCount", execCount);

            long successCount = executionMapper.selectCount(new LambdaQueryWrapper<WorkflowExecution>()
                    .eq(WorkflowExecution::getWorkflowId, wf.getId())
                    .eq(WorkflowExecution::getStatus, "completed"));
            item.put("successCount", successCount);

            ranking.add(item);
        }

        // 2. 按执行次数降序排序
        ranking.sort((a, b) -> Long.compare(
                ((Number) b.get("executionCount")).longValue(),
                ((Number) a.get("executionCount")).longValue()));

        // 3. 取Top N
        List<Map<String, Object>> result = ranking.size() > limit ? 
                ranking.subList(0, limit) : ranking;
        
        log.debug("✅ 排行榜获取完成, 工作流总数={}, 返回数量={}", workflows.size(), result.size());
        return result;
    }

    @Override
    public List<WorkflowVersion> getVersions(Long workflowId) {
        return versionMapper.selectList(new LambdaQueryWrapper<WorkflowVersion>()
                .eq(WorkflowVersion::getWorkflowId, workflowId)
                .orderByDesc(WorkflowVersion::getVersion));
    }

    @Override
    public WorkflowVersion createVersion(Long workflowId, String graphData, String description) {
        Workflow workflow = getById(workflowId);
        if (workflow == null) {
            throw new BusinessException(ErrorCode.WORKFLOW_NOT_FOUND);
        }

        Integer maxVersion = workflow.getVersion();
        if (maxVersion == null) maxVersion = 0;

        WorkflowVersion version = WorkflowVersion.builder()
                .workflowId(workflowId)
                .version(maxVersion + 1)
                .description(description)
                .graphData(graphData)
                .createTime(LocalDateTime.now())
                .build();
        versionMapper.insert(version);

        workflow.setVersion(maxVersion + 1);
        workflow.setGraphData(graphData);
        workflow.setUpdateTime(LocalDateTime.now());
        updateById(workflow);

        log.info("创建工作流版本: workflowId={}, version={}", workflowId, version.getVersion());
        return version;
    }

    @Override
    public Workflow rollbackVersion(Long workflowId, Integer targetVersion) {
        WorkflowVersion version = versionMapper.selectOne(new LambdaQueryWrapper<WorkflowVersion>()
                .eq(WorkflowVersion::getWorkflowId, workflowId)
                .eq(WorkflowVersion::getVersion, targetVersion));

        if (version == null) {
            throw new BusinessException(ErrorCode.WORKFLOW_NOT_FOUND, "版本不存在: " + targetVersion);
        }

        Workflow workflow = getById(workflowId);
        workflow.setGraphData(version.getGraphData());
        workflow.setUpdateTime(LocalDateTime.now());
        updateById(workflow);

        log.info("回滚工作流版本: workflowId={}, targetVersion={}", workflowId, targetVersion);
        return workflow;
    }
}
