package com.moyun.ext.cms.service.interview.impl;

import com.moyun.ext.ai.dto.AiSceneBinding;
import com.moyun.ext.ai.entity.Agent;
import com.moyun.ext.ai.service.AgentService;
import com.moyun.ext.ai.service.AiGlobalSwitch;
import com.moyun.ext.ai.service.AiSceneResolver;
import com.moyun.ext.ai.service.ModelConfigService;
import com.moyun.ext.aiapp.support.AgentModelRouter;
import com.moyun.ext.cms.service.interview.InterviewAgentClient;
import com.moyun.system.service.ISysConfigService;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 面试官智能体客户端实现
 *
 * <p>V4 瘦身：流式主干已收口统一网关会话流式通道（AiGatewayService.executeConversationStream，
 * 治理前置/滑窗记忆/token 累计统一由网关承担），灰度开关与治理前置随之删除。
 * 本类仅保留 agent 解析链与同步调用（模型路由委托网关侧 {@link AgentModelRouter}）。</p>
 *
 * @author moyun
 */
@Component
public class InterviewAgentClientImpl implements InterviewAgentClient {

    private static final Logger log = LoggerFactory.getLogger(InterviewAgentClientImpl.class);

    private static final String CONFIG_KEY_DEFAULT_AGENT = "voice.interview.defaultAgentId";

    private final AgentService agentService;
    private final AiSceneResolver sceneResolver;
    private final ModelConfigService modelConfigService;
    private final ISysConfigService sysConfigService;
    /** AI 全局运行时开关（sys_config ai.global.enabled，替代 yaml AiProperties） */
    private final AiGlobalSwitch aiGlobalSwitch;
    /** per-agent 模型路由（网关侧公共能力，迁移自本类） */
    private final AgentModelRouter agentModelRouter;

    public InterviewAgentClientImpl(AgentService agentService,
                                    ModelConfigService modelConfigService,
                                    ISysConfigService sysConfigService,
                                    AiGlobalSwitch aiGlobalSwitch,
                                    AiSceneResolver sceneResolver,
                                    AgentModelRouter agentModelRouter) {
        this.agentService = agentService;
        this.sceneResolver = sceneResolver;
        this.modelConfigService = modelConfigService;
        this.sysConfigService = sysConfigService;
        this.aiGlobalSwitch = aiGlobalSwitch;
        this.agentModelRouter = agentModelRouter;
    }

    @Override
    public Agent resolveAgent(Long agentId) {
        if (!aiGlobalSwitch.isEnabled()) {
            return null;
        }
        try {
            if (agentId != null) {
                Agent agent = agentService.getById(agentId);
                return usable(agent) ? agent : null;
            }
            String value = sysConfigService.selectConfigByKey(CONFIG_KEY_DEFAULT_AGENT);
            if (value != null && !value.isBlank()) {
                Long id = Long.parseLong(value.trim());
                Agent agent = agentService.getById(id);
                return usable(agent) ? agent : null;
            }
        } catch (Exception e) {
            log.warn("[VoiceInterview] 解析面试官 agent 失败（回退默认逻辑）：{}", e.getMessage());
        }
        return null;
    }

    @Override
    public AiSceneBinding resolveScene(String sceneCode) {
        if (!aiGlobalSwitch.isEnabled()) {
            return AiSceneBinding.empty();
        }
        try {
            return sceneResolver.resolve(sceneCode);
        } catch (Exception e) {
            log.warn("[VoiceInterview] 场景 {} 解析异常，返回空绑定：{}", sceneCode, e.getMessage());
            return AiSceneBinding.empty();
        }
    }

    @Override
    public Agent resolveAgentForScene(AiSceneBinding sceneBinding, Long agentId) {
        if (!aiGlobalSwitch.isEnabled()) {
            return null;
        }
        try {
            // 1. 前端显式指定优先
            if (agentId != null) {
                Agent agent = agentService.getById(agentId);
                if (usable(agent)) {
                    return agent;
                }
            }
            // 2. 场景绑定 Agent
            if (sceneBinding != null && sceneBinding.hasAgent()) {
                return sceneBinding.getAgent();
            }
            // 3. 场景直绑模型：合成伪 Agent（空人设由 buildAgentSystemMessage 回退旧提示词）
            if (sceneBinding != null && sceneBinding.hasModelOnly()) {
                return synthesizePseudoAgent(sceneBinding);
            }
            // 4. sys_config 默认链
            return resolveAgent(null);
        } catch (Exception e) {
            log.warn("[VoiceInterview] 场景化 agent 解析失败，回退默认链：{}", e.getMessage());
            return resolveAgent(null);
        }
    }

    /** 场景直绑模型的伪 Agent：仅含模型路由，无 id（不落 interview.agentId）与人设 */
    private Agent synthesizePseudoAgent(AiSceneBinding sceneBinding) {
        Agent pseudo = new Agent();
        pseudo.setName("场景模型");
        pseudo.setModelConfigId(sceneBinding.getModelConfig().getId());
        pseudo.setEnabled(true);
        return pseudo;
    }

    private boolean usable(Agent agent) {
        return agent != null && Boolean.TRUE.equals(agent.getEnabled());
    }

    @Override
    public boolean isEnabled() {
        if (!aiGlobalSwitch.isEnabled()) {
            return false;
        }
        try {
            return modelConfigService.getDefaultChatConfig() != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String chat(Agent agent, List<ChatMessage> messages) {
        if (agent == null || messages == null || messages.isEmpty()) {
            return null;
        }
        try {
            ChatLanguageModel model = agentModelRouter.createModel(agent);
            if (model == null) {
                return null;
            }
            ChatResponse response = model.chat(messages);
            String text = response == null || response.aiMessage() == null
                    ? null : response.aiMessage().text();
            log.debug("[VoiceInterview] agent 同步调用完成 agentId={} respLen={}",
                    agent.getId(), text == null ? 0 : text.length());
            return text;
        } catch (Exception e) {
            log.warn("[VoiceInterview] agent 同步调用失败 agentId={}：{}", agent.getId(), e.getMessage());
            return null;
        }
    }

    @Override
    public String agentName(Long agentId) {
        if (agentId == null || !aiGlobalSwitch.isEnabled()) {
            return null;
        }
        try {
            Agent agent = agentService.getById(agentId);
            return agent == null ? null : agent.getName();
        } catch (Exception e) {
            return null;
        }
    }
}
