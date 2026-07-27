package com.moyun.agent.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.agent.entity.Conversation;
import com.moyun.agent.entity.ConversationMessage;

import java.util.List;

/**
 * 对话会话服务接口
 *
 * <p>管理用户与智能体的对话会话，包括会话创建、消息存储、历史查询等</p>
 *
 * @author laomao
 */
public interface ConversationService extends IService<Conversation> {

    /**
     * 创建新会话
     *
     * @param agentId 智能体ID
     * @param userId 用户ID
     * @return 新创建的会话
     */
    Conversation createConversation(Long agentId, String userId);

    /**
     * 获取用户的会话列表（按更新时间倒序）
     *
     * @param agentId 智能体ID
     * @param userId 用户ID
     * @return 会话列表
     */
    List<Conversation> listByAgentId(Long agentId, String userId);

    /**
     * 添加消息到会话
     *
     * @param conversationId 会话ID
     * @param role 角色（user/assistant）
     * @param content 消息内容
     * @param referenceSources 引用来源JSON
     */
    void addMessage(Long conversationId, String role, String content, String referenceSources);

    /**
     * 获取会话的历史消息
     *
     * @param conversationId 会话ID
     * @return 消息列表
     */
    List<ConversationMessage> getMessages(Long conversationId);

    /**
     * 删除会话（级联删除消息）
     *
     * @param conversationId 会话ID
     */
    void deleteConversation(Long conversationId);

    /**
     * 更新会话标题
     *
     * @param conversationId 会话ID
     * @param title 新标题
     */
    void updateTitle(Long conversationId, String title);
}
