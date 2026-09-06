package com.moyun.ext.cms.service.interview;

import com.moyun.ext.ai.entity.Agent;
import dev.langchain4j.data.message.ChatMessage;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 面试官智能体客户端
 *
 * <p>落实 agent 绑定的模型路由：不走只支持默认模型的 {@code LLMService}，
 * 直连 {@code ModelConfigService.createChatModel(agent.modelConfigId, temperature, maxTokens)}。</p>
 *
 * <p>所有方法异常安全：失败返回 null / 回调 onError，由调用方降级规则链路，
 * 保证面试链路在 AI 不可用时依然完整可用。</p>
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

    /** AI 能力是否可用（moyun.ai.enabled 且存在可用 chat 模型） */
    boolean isEnabled();

    /** 动态出题模式默认开关（sys_config: voice.interview.dynamicMode） */
    boolean dynamicModeEnabled();

    /**
     * 同步调用 agent 绑定的模型
     *
     * @return 完整回复文本；失败/未启用返回 null
     */
    String chat(Agent agent, List<ChatMessage> messages);

    /**
     * 流式调用 agent 绑定的模型
     *
     * @param onToken     增量文本回调（回调内抛异常会被吞掉并记日志，不影响后续回调）
     * @param onComplete  完成回调（参数为全文）
     * @param onError     失败回调
     */
    void chatStream(Agent agent, List<ChatMessage> messages,
                    Consumer<String> onToken, Consumer<String> onComplete, Consumer<Throwable> onError);

    /**
     * 查询 agent 名称（前端顶栏展示；agent 不存在/未启用返回 null）
     */
    String agentName(Long agentId);

    /**
     * 列出可绑定的启用状态 agent（portal 选择面试官用）
     *
     * @return id/name/description/welcomeMessage 摘要列表
     */
    List<Map<String, Object>> listUsableAgents();
}
