package com.moyun.ext.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.ext.ai.entity.Agent;

import java.util.List;

/**
 * 智能体服务接口
 *
 * <p>提供智能体的业务逻辑操作</p>
 *
 * @author laomao
 */
public interface AgentService extends IService<Agent> {

    /**
     * 获取所有启用的智能体
     *
     * @return 启用状态的智能体列表
     */
    List<Agent> listEnabled();

    /**
     * 获取所有智能体（按创建时间倒序）
     *
     * @return 智能体列表
     */
    List<Agent> listOrderByCreateTimeDesc();

    /**
     * 根据智能体ID获取关联的知识库ID列表
     *
     * @param agentId 智能体ID
     * @return 知识库ID列表
     */
    List<Long> getKnowledgeBaseIds(Long agentId);
    
    /**
     * 根据发布Token获取智能体
     *
     * @param publishToken 发布Token
     * @return 智能体
     */
    Agent getByPublishToken(String publishToken);
}
