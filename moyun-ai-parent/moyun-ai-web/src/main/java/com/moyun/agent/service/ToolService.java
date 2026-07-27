package com.moyun.agent.service;

import com.moyun.agent.engine.tool.ToolDefinition;
import com.moyun.agent.entity.AgentTool;

import java.util.List;

/**
 * 工具管理服务接口
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
public interface ToolService {

    /**
     * 获取所有工具列表
     *
     * @return 工具列表
     */
    List<AgentTool> getAllTools();

    /**
     * 获取启用的工具列表
     *
     * @return 启用状态的工具列表
     */
    List<AgentTool> getEnabledTools();

    /**
     * 获取智能体关联的工具
     *
     * @param agentId 智能体ID
     * @return 该智能体关联的启用工具列表
     */
    List<AgentTool> getToolsByAgentId(Long agentId);

    /**
     * 获取智能体的工具定义（给LLM使用）
     *
     * @param agentId 智能体ID
     * @return 工具定义列表
     */
    List<ToolDefinition> getToolDefinitionsForAgent(Long agentId);

    /**
     * 为智能体关联工具
     *
     * @param agentId 智能体ID
     * @param toolIds 工具ID列表
     */
    void bindToolsToAgent(Long agentId, List<Long> toolIds);

    /**
     * 获取智能体关联的工具ID列表
     *
     * @param agentId 智能体ID
     * @return 工具ID列表
     */
    List<Long> getToolIdsByAgentId(Long agentId);

    /**
     * 创建自定义工具
     *
     * @param tool 工具信息
     * @return 创建后的工具
     */
    AgentTool createTool(AgentTool tool);

    /**
     * 更新工具
     *
     * @param tool 工具信息
     */
    void updateTool(AgentTool tool);

    /**
     * 删除工具
     *
     * @param toolId 工具ID
     * @return 是否删除成功
     */
    boolean deleteTool(Long toolId);

    /**
     * 切换工具启用状态
     *
     * @param toolId 工具ID
     * @param enabled 是否启用
     */
    void toggleToolEnabled(Long toolId, boolean enabled);

    /**
     * 统计工具数量
     *
     * @return 工具总数
     */
    long count();
}
