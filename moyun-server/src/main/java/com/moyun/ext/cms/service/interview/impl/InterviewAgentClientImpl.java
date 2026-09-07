package com.moyun.ext.cms.service.interview.impl;

import com.moyun.ext.ai.entity.Agent;
import com.moyun.ext.ai.entity.ModelConfig;
import com.moyun.ext.ai.enums.ModelType;
import com.moyun.ext.ai.dto.AiSceneBinding;
import com.moyun.ext.ai.service.AgentService;
import com.moyun.ext.ai.service.AiSceneResolver;
import com.moyun.ext.ai.service.ModelConfigService;
import com.moyun.ext.cms.config.AiProperties;
import com.moyun.ext.cms.service.interview.InterviewAgentClient;
import com.moyun.system.service.ISysConfigService;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 面试官智能体客户端实现
 *
 * <p>模型路由：agent.modelConfigId → ModelConfigService（带 temperature/maxTokens 覆盖）；
 * agent 未配模型时回退默认 chat 配置。agent 编辑后 AgentServiceImpl 自动清缓存，动态生效。</p>
 *
 * @author moyun
 */
@Component
public class InterviewAgentClientImpl implements InterviewAgentClient {

    private static final Logger log = LoggerFactory.getLogger(InterviewAgentClientImpl.class);

    private static final String CONFIG_KEY_DEFAULT_AGENT = "voice.interview.defaultAgentId";
    private static final String CONFIG_KEY_DYNAMIC_MODE = "voice.interview.dynamicMode";

    private final AgentService agentService;
    private final AiSceneResolver sceneResolver;
    private final ModelConfigService modelConfigService;
    private final ISysConfigService sysConfigService;
    private final AiProperties aiProperties;

    public InterviewAgentClientImpl(AgentService agentService,
                                    ModelConfigService modelConfigService,
                                    ISysConfigService sysConfigService,
                                    AiProperties aiProperties,
                                    AiSceneResolver sceneResolver) {
        this.agentService = agentService;
        this.sceneResolver = sceneResolver;
        this.modelConfigService = modelConfigService;
        this.sysConfigService = sysConfigService;
        this.aiProperties = aiProperties;
    }

    @Override
    public Agent resolveAgent(Long agentId) {
        if (!aiProperties.isEnabled()) {
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
        if (!aiProperties.isEnabled()) {
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
        if (!aiProperties.isEnabled()) {
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
        if (!aiProperties.isEnabled()) {
            return false;
        }
        try {
            return modelConfigService.getDefaultChatConfig() != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean dynamicModeEnabled() {
        try {
            String value = sysConfigService.selectConfigByKey(CONFIG_KEY_DYNAMIC_MODE);
            return value != null && "true".equalsIgnoreCase(value.trim());
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
            ChatLanguageModel model = createModel(agent);
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
    public void chatStream(Agent agent, List<ChatMessage> messages,
                           Consumer<String> onToken, Consumer<String> onComplete, Consumer<Throwable> onError) {
        if (agent == null || messages == null || messages.isEmpty()) {
            onError.accept(new IllegalStateException("agent 或消息为空"));
            return;
        }
        try {
            StreamingChatLanguageModel model = createStreamingModel(agent);
            if (model == null) {
                // V11.0.1：全库无流式模型 → 同步调用 + 模拟流式分片推送（保留打字机协议，不降级报错）
                log.info("[VoiceInterview] 无可用流式模型，agent={} 使用同步调用模拟流式输出", agent.getId());
                simulateStreamBySync(agent, messages, onToken, onComplete, onError);
                return;
            }
            final StringBuilder buffer = new StringBuilder();
            model.chat(messages, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    if (partialResponse == null || partialResponse.isEmpty()) {
                        return;
                    }
                    buffer.append(partialResponse);
                    try {
                        onToken.accept(partialResponse);
                    } catch (Exception e) {
                        log.error("[VoiceInterview] onToken 回调异常：{}", e.getMessage(), e);
                    }
                }

                @Override
                public void onCompleteResponse(ChatResponse response) {
                    try {
                        onComplete.accept(buffer.toString());
                    } catch (Exception e) {
                        log.error("[VoiceInterview] onComplete 回调异常：{}", e.getMessage(), e);
                    }
                }

                @Override
                public void onError(Throwable error) {
                    log.warn("[VoiceInterview] agent 流式调用失败 agentId={}：{}",
                            agent.getId(), error.getMessage());
                    onError.accept(error);
                }
            });
        } catch (Exception e) {
            log.warn("[VoiceInterview] agent 流式调用初始化失败 agentId={}：{}", agent.getId(), e.getMessage());
            onError.accept(e);
        }
    }

    /**
     * V11.0.1：同步调用模拟流式输出。
     * 无任何流式模型时的最终兜底：同步拿到全文后按块推送 onToken，前端打字机协议无感兼容。
     */
    private void simulateStreamBySync(Agent agent, List<ChatMessage> messages,
                                      Consumer<String> onToken, Consumer<String> onComplete,
                                      Consumer<Throwable> onError) {
        try {
            String full = chat(agent, messages);
            if (full == null || full.isEmpty()) {
                onError.accept(new IllegalStateException("同步调用未返回内容"));
                return;
            }
            // 按 24 字符分片推送（同帧多次回调，前端 appendDelta 顺序拼接）
            final int chunk = 24;
            for (int i = 0; i < full.length(); i += chunk) {
                onToken.accept(full.substring(i, Math.min(full.length(), i + chunk)));
            }
            onComplete.accept(full);
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    @Override
    public String agentName(Long agentId) {
        if (agentId == null || !aiProperties.isEnabled()) {
            return null;
        }
        try {
            Agent agent = agentService.getById(agentId);
            return agent == null ? null : agent.getName();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<Map<String, Object>> listUsableAgents() {
        List<Map<String, Object>> result = new ArrayList<>();
        if (!aiProperties.isEnabled()) {
            return result;
        }
        try {
            List<Agent> agents = agentService.lambdaQuery()
                    .eq(Agent::getEnabled, true)
                    .orderByAsc(Agent::getId)
                    .last("LIMIT 50")
                    .list();
            for (Agent agent : agents) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", agent.getId());
                item.put("name", agent.getName());
                item.put("description", agent.getDescription());
                item.put("welcomeMessage", agent.getWelcomeMessage());
                result.add(item);
            }
        } catch (Exception e) {
            log.warn("[VoiceInterview] 查询可用 agent 列表失败：{}", e.getMessage());
        }
        return result;
    }

    private Long resolveModelConfigId(Agent agent) {
        if (agent.getModelConfigId() != null) {
            return agent.getModelConfigId();
        }
        ModelConfig def = modelConfigService.getDefaultChatConfig();
        return def == null ? null : def.getId();
    }

    private ChatLanguageModel createModel(Agent agent) {
        Long configId = resolveModelConfigId(agent);
        if (configId == null) {
            return null;
        }
        return modelConfigService.createChatModel(configId, agent.getTemperature(), agent.getMaxTokens());
    }

    private StreamingChatLanguageModel createStreamingModel(Agent agent) {
        Long configId = resolveStreamingModelConfigId(agent);
        if (configId == null) {
            return null;
        }
        return modelConfigService.createStreamingChatModel(configId, agent.getTemperature(), agent.getMaxTokens());
    }

    /**
     * V11.0.1 流式模型自动路由：
     * <ol>
     *   <li>agent 绑定模型（或默认 chat 配置）支持流式 → 直接使用；</li>
     *   <li>绑定的模型不支持流式 → 自动挑选一个「启用 + chat 类型 + 支持流式」的配置
     *       （默认配置优先，其余按 id 升序取第一个），避免每次调用都报错再降级；</li>
     *   <li>全库都没有流式模型 → 返回 null（chatStream 走同步模拟流式兜底）。</li>
     * </ol>
     */
    private Long resolveStreamingModelConfigId(Agent agent) {
        Long boundId = resolveModelConfigId(agent);
        if (boundId != null) {
            ModelConfig bound = modelConfigService.getById(boundId);
            if (isStreamableChatConfig(bound)) {
                return boundId;
            }
            log.info("[VoiceInterview] agent={} 绑定模型 configId={} 不支持流式，自动挑选流式模型",
                    agent.getId(), boundId);
        }
        // 默认 chat 配置支持流式则优先
        ModelConfig def = modelConfigService.getDefaultChatConfig();
        if (isStreamableChatConfig(def)) {
            return def.getId();
        }
        // 其余启用的流式 chat 配置按 id 升序取第一个
        List<ModelConfig> candidates = modelConfigService.lambdaQuery()
                .eq(ModelConfig::getEnabled, true)
                .eq(ModelConfig::getStreamingSupported, true)
                .eq(ModelConfig::getModelType, ModelType.CHAT.getCode())
                .orderByAsc(ModelConfig::getId)
                .list();
        return candidates.isEmpty() ? null : candidates.get(0).getId();
    }

    /** 配置是否为「启用 + chat 类型 + 支持流式」 */
    private boolean isStreamableChatConfig(ModelConfig config) {
        return config != null
                && Boolean.TRUE.equals(config.getEnabled())
                && Boolean.TRUE.equals(config.getStreamingSupported())
                && ModelType.CHAT.getCode().equals(config.getModelType());
    }
}
