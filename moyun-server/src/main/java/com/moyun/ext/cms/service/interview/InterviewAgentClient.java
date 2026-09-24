package com.moyun.ext.cms.service.interview;

import com.moyun.ext.ai.dto.AiSceneBinding;
import com.moyun.ext.ai.entity.Agent;
import dev.langchain4j.data.message.ChatMessage;

import java.util.List;

/**
 * 面试官智能体客户端
 *
 * <p>落实 agent 绑定的模型路由：不走只支持默认模型的 {@code LLMService}，
 * 同步调用委托网关侧 {@code AgentModelRouter}（模型路由公共能力收口）。</p>
 *
 * <p>流式主干已收口统一网关会话流式通道（AiGatewayService.executeConversationStream），
 * 本客户端仅保留 agent 解析与同步调用（开场白/提示等短链路）。</p>
 *
 * <p>所有方法异常安全：失败返回 null，由调用方降级，保证面试链路在 AI 不可用时依然完整可用。</p>
 *
 * @author moyun
 */
public interface InterviewAgentClient {

    /**
     * 解析面试官 agent：入参优先 → sys_config 默认（voice.interview.defaultAgentId）→ null
     *
     * @param agentId 前端显式指定的 agent（可空）
     * @return 启用状态的 agent；无可用时返回 null（走旧提示词逻辑）
     */
    Agent resolveAgent(Long agentId);

    /**
     * 解析 AI 场景绑定（委托 AiSceneResolver；AI 未启用/异常返回空绑定）
     */
    AiSceneBinding resolveScene(String sceneCode);

    /**
     * 场景化 agent 解析（v11.x）：显式入参 > 场景绑定 Agent > 场景直绑模型（合成伪 Agent）> sys_config 默认
     *
     * @param sceneBinding 已解析的场景绑定（调用方先 resolveScene 一次，避免重复灰度轮询）
     * @param agentId      前端显式指定的 agent（可空）
     * @return 可用 agent（伪 Agent 仅含模型路由）；无可用时返回 null（走旧提示词逻辑）
     */
    Agent resolveAgentForScene(AiSceneBinding sceneBinding, Long agentId);

    /** AI 能力是否可用（moyun.ai.enabled 且存在可用 chat 模型） */
    boolean isEnabled();

    /**
     * 同步调用 agent 绑定的模型
     *
     * @return 完整回复文本；失败/未启用返回 null
     */
    String chat(Agent agent, List<ChatMessage> messages);

    /**
     * 查询 agent 名称（前端顶栏展示；agent 不存在/未启用返回 null）
     */
    String agentName(Long agentId);
}
