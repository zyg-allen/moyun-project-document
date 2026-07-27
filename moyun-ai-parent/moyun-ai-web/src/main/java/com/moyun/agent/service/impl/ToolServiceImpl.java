package com.moyun.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.agent.engine.tool.ToolDefinition;
import com.moyun.agent.engine.tool.ToolRegistry;
import com.moyun.agent.entity.AgentTool;
import com.moyun.agent.entity.AgentToolRelation;
import com.moyun.agent.mapper.AgentToolMapper;
import com.moyun.agent.mapper.AgentToolRelationMapper;
import com.moyun.agent.service.ToolService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工具管理服务实现类
 *
 * <p>提供 Function Calling 工具的业务逻辑，包括：
 * <ul>
 *   <li>工具的CRUD操作</li>
 *   <li>智能体与工具的关联管理</li>
 *   <li>工具状态管理</li>
 * </ul>
 * </p>
 *
 * @author laomao
 */
@Slf4j
@Service
public class ToolServiceImpl implements ToolService {

    @Autowired
    private AgentToolMapper agentToolMapper;

    @Autowired
    private AgentToolRelationMapper agentToolRelationMapper;

    @Autowired
    private ToolRegistry toolRegistry;

    @Override
    public List<AgentTool> getAllTools() {
        return agentToolMapper.selectList(
                new LambdaQueryWrapper<AgentTool>()
                        .orderByDesc(AgentTool::getIsSystem)
                        .orderByAsc(AgentTool::getCategory)
                        .orderByAsc(AgentTool::getId)
        );
    }

    @Override
    public List<AgentTool> getEnabledTools() {
        return agentToolMapper.selectList(
                new LambdaQueryWrapper<AgentTool>()
                        .eq(AgentTool::getEnabled, true)
                        .orderByAsc(AgentTool::getCategory)
        );
    }

    @Override
    public List<AgentTool> getToolsByAgentId(Long agentId) {
        return agentToolMapper.selectToolsByAgentId(agentId);
    }

    @Override
    public List<ToolDefinition> getToolDefinitionsForAgent(Long agentId) {
        return toolRegistry.getToolsForAgent(agentId);
    }

    @Override
    @Transactional
    public void bindToolsToAgent(Long agentId, List<Long> toolIds) {
        // 先删除原有关联
        agentToolRelationMapper.delete(
                new LambdaQueryWrapper<AgentToolRelation>()
                        .eq(AgentToolRelation::getAgentId, agentId)
        );

        // 如果工具列表为空，直接返回（已清空关联）
        if (toolIds == null || toolIds.isEmpty()) {
            log.info("智能体 {} 已清空工具关联", agentId);
            return;
        }

        // 添加新关联
        for (Long toolId : toolIds) {
            AgentToolRelation relation = AgentToolRelation.builder()
                    .agentId(agentId)
                    .toolId(toolId)
                    .enabled(true)
                    .createTime(LocalDateTime.now())
                    .build();
            agentToolRelationMapper.insert(relation);
        }

        log.info("智能体 {} 关联了 {} 个工具", agentId, toolIds.size());
    }

    @Override
    public List<Long> getToolIdsByAgentId(Long agentId) {
        List<AgentToolRelation> relations = agentToolRelationMapper.selectList(
                new LambdaQueryWrapper<AgentToolRelation>()
                        .eq(AgentToolRelation::getAgentId, agentId)
                        .eq(AgentToolRelation::getEnabled, true)
        );
        return relations.stream().map(AgentToolRelation::getToolId).toList();
    }

    @Override
    public AgentTool createTool(AgentTool tool) {
        tool.setIsSystem(false);
        tool.setCreateTime(LocalDateTime.now());
        tool.setUpdateTime(LocalDateTime.now());
        agentToolMapper.insert(tool);
        return tool;
    }

    @Override
    public void updateTool(AgentTool tool) {
        tool.setUpdateTime(LocalDateTime.now());
        agentToolMapper.updateById(tool);
    }

    @Override
    public boolean deleteTool(Long toolId) {
        AgentTool tool = agentToolMapper.selectById(toolId);
        if (tool == null) {
            return false;
        }
        if (Boolean.TRUE.equals(tool.getIsSystem())) {
            log.warn("不能删除系统内置工具: {}", tool.getName());
            return false;
        }

        // 删除关联关系
        agentToolRelationMapper.delete(
                new LambdaQueryWrapper<AgentToolRelation>()
                        .eq(AgentToolRelation::getToolId, toolId)
        );

        // 删除工具
        agentToolMapper.deleteById(toolId);
        return true;
    }

    @Override
    public void toggleToolEnabled(Long toolId, boolean enabled) {
        AgentTool tool = new AgentTool();
        tool.setId(toolId);
        tool.setEnabled(enabled);
        tool.setUpdateTime(LocalDateTime.now());
        agentToolMapper.updateById(tool);
    }

    @Override
    public long count() {
        return agentToolMapper.selectCount(null);
    }
}
