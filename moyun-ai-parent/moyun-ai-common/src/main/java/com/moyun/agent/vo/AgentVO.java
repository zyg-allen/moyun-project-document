package com.moyun.agent.vo;

import com.moyun.agent.entity.Agent;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 智能体视图对象（包含扩展信息）
 *
 * <p>继承自 Agent 实体，额外包含前端展示所需的统计信息，如工具数量等</p>
 *
 * @author laomao
 * @time 2025/11/25
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentVO extends Agent {
    
    /**
     * 关联的工具数量
     */
    private Integer toolCount;
    
    /**
     * 会话数量（统计）
     */
    private Integer sessionCount;
    
    /**
     * 消息数量（统计）
     */
    private Integer messageCount;
    
    /**
     * Token消耗总量（统计）
     */
    private Long totalTokens;
    
    /**
     * 从 Agent 实体转换为 AgentVO
     */
    public static AgentVO from(Agent agent) {
        AgentVO vo = new AgentVO();
        vo.setId(agent.getId());
        vo.setName(agent.getName());
        vo.setDescription(agent.getDescription());
        vo.setSystemPrompt(agent.getSystemPrompt());
        vo.setKnowledgeBaseIds(agent.getKnowledgeBaseIds());
        vo.setKnowledgeLibraryIds(agent.getKnowledgeLibraryIds());  // 新增：知识库ID列表
        vo.setKnowledgeBaseWeights(agent.getKnowledgeBaseWeights());  // 新增：知识库权重
        vo.setModelConfigId(agent.getModelConfigId());  // 新增：模型配置ID
        vo.setTemperature(agent.getTemperature());
        vo.setMaxTokens(agent.getMaxTokens());
        vo.setRagMinScore(agent.getRagMinScore());
        vo.setRagMaxResults(agent.getRagMaxResults());
        vo.setRagRecallMultiplier(agent.getRagRecallMultiplier());
        vo.setRagEnableHybridSearch(agent.getRagEnableHybridSearch());
        vo.setRagEnableQueryExpansion(agent.getRagEnableQueryExpansion());
        vo.setRagBm25Weight(agent.getRagBm25Weight());
        vo.setRagVectorWeight(agent.getRagVectorWeight());
        vo.setEnabled(agent.getEnabled());
        // 扩展字段
        vo.setWelcomeMessage(agent.getWelcomeMessage());
        vo.setSuggestedQuestions(agent.getSuggestedQuestions());
        vo.setShowCitations(agent.getShowCitations());
        vo.setMaxHistoryTurns(agent.getMaxHistoryTurns());
        vo.setApiEnabled(agent.getApiEnabled());
        vo.setApiKey(agent.getApiKey());
        // 工作流关联
        vo.setWorkflowId(agent.getWorkflowId());
        vo.setWorkflowTriggerMode(agent.getWorkflowTriggerMode());
        vo.setWorkflowTriggerKeywords(agent.getWorkflowTriggerKeywords());
        // 应用发布
        vo.setPublishEnabled(agent.getPublishEnabled());
        vo.setPublishToken(agent.getPublishToken());
        vo.setPublishSettings(agent.getPublishSettings());
        vo.setCreateTime(agent.getCreateTime());
        vo.setUpdateTime(agent.getUpdateTime());
        vo.setToolCount(0);
        return vo;
    }
    
    /**
     * 工作流名称（用于展示）
     */
    private String workflowName;
}
