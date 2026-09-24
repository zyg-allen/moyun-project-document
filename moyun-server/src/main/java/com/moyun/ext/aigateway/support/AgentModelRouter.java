package com.moyun.ext.aigateway.support;

import com.moyun.ext.ai.entity.Agent;
import com.moyun.ext.ai.entity.ModelConfig;
import com.moyun.ext.ai.enums.ModelType;
import com.moyun.ext.ai.service.ModelConfigService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * per-agent 模型路由（迁移自 InterviewAgentClientImpl，网关侧公共能力收口）
 *
 * <p>路由链：agent.modelConfigId → 默认 chat 配置。流式额外自动路由：
 * 绑定模型不支持流式时，自动挑选「启用 + chat + 支持流式」的配置（默认优先，其余按 id 升序）。</p>
 *
 * @author moyun
 * @since 2026-09-24
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentModelRouter {

    private final ModelConfigService modelConfigService;

    /** agent 绑定的同步模型（agent 未配模型时回退默认 chat 配置；无可用返回 null） */
    public ChatLanguageModel createModel(Agent agent) {
        Long configId = resolveModelConfigId(agent);
        return configId == null ? null
                : modelConfigService.createChatModel(configId, agent.getTemperature(), agent.getMaxTokens());
    }

    /** agent 绑定的流式模型（绑定模型不支持流式时自动挑选，含 temperature/maxTokens 覆盖） */
    public StreamingChatLanguageModel createStreamingModel(Agent agent) {
        Long configId = resolveStreamingModelConfigId(agent);
        return configId == null ? null
                : modelConfigService.createStreamingChatModel(configId, agent.getTemperature(), agent.getMaxTokens());
    }

    private Long resolveModelConfigId(Agent agent) {
        if (agent.getModelConfigId() != null) {
            return agent.getModelConfigId();
        }
        ModelConfig def = modelConfigService.getDefaultChatConfig();
        return def == null ? null : def.getId();
    }

    private Long resolveStreamingModelConfigId(Agent agent) {
        Long boundId = resolveModelConfigId(agent);
        if (boundId != null) {
            ModelConfig bound = modelConfigService.getById(boundId);
            if (isStreamableChatConfig(bound)) {
                return boundId;
            }
            log.info("[aigateway:AgentModelRouter] agent={} 绑定模型 configId={} 不支持流式，自动挑选流式模型",
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
