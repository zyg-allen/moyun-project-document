package com.moyun.ext.cms.service;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.moyun.ext.ai.service.AiSceneResolver;
import com.moyun.ext.ai.service.LLMService;
import com.moyun.util.string.StringUtils;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import java.util.Collections;
import dev.langchain4j.model.chat.ChatLanguageModel;

/**
 * LLM 真实实现：桥接到 AI 模块 LLMService（v10.8 统一 AI 能力通道）
 *
 * <p>注册条件：{@code moyun.ai.enabled=true}（此时 NoopLlmClient 不注册，本 Bean 提供真实通道）。
 * 模型/密钥无需在本模块重复配置——直接复用 AI 模块「模型配置」中启用的默认聊天模型。
 *
 * <p>v11.39 场景感知（Strategy + Factory）：
 * <ul>
 *   <li>{@link #chat(String, String, String)} 带场景码调用 → 委托 {@link AiSceneResolver#resolveChatModel} 工厂解析</li>
 *   <li>工厂返回绑定的 ChatLanguageModel → 用该模型生成</li>
 *   <li>工厂返回 null（无绑定）→ 回落 {@link LLMService} 默认模型（行为与无场景调用一致）</li>
 * </ul>
 * 模型选择逻辑全部封装在 AiSceneResolver（责任链），本类只负责"拿到模型→调用"。
 *
 * <p>失败容忍：调用异常时返回 null（记 warn），由调用方回退到规则化逻辑，
 * 与 {@link NoopLlmClient} 的兜底语义一致。
 *
 * @author moyun
 */
@Component
@ConditionalOnProperty(prefix = "moyun.ai", name = "enabled", havingValue = "true")
public class AiModuleLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(AiModuleLlmClient.class);

    /** AI 模块统一 LLM 服务（未配置默认模型时调用会抛异常，由 null 返回触发规则化兜底） */
    @Autowired(required = false)
    private LLMService llmService;

    /** 场景解析器（Factory：scene_code → 绑定模型） */
    @Autowired(required = false)
    private AiSceneResolver sceneResolver;

    @Override
    public String chat(String systemPrompt, String userMessage) {
        if (llmService == null) {
            return null;
        }
        try {
            return llmService.generate(systemPrompt + "\n\n" + userMessage);
        } catch (Exception e) {
            log.warn("[LlmClient] AI 模块调用失败，回退规则化：{}", e.getMessage());
            return null;
        }
    }

    @Override
    public String chat(List<Map<String, String>> messages) {
        if (llmService == null || messages == null || messages.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (Map<String, String> msg : messages) {
            String role = msg.getOrDefault("role", "user");
            String content = msg.getOrDefault("content", "");
            if (StringUtils.isEmpty(content)) {
                continue;
            }
            if ("system".equals(role)) {
                sb.append(content).append("\n\n");
            } else if ("assistant".equals(role)) {
                sb.append("助手：").append(content).append("\n\n");
            } else {
                sb.append("用户：").append(content).append("\n\n");
            }
        }
        try {
            return llmService.generate(sb.toString().trim());
        } catch (Exception e) {
            log.warn("[LlmClient] AI 模块调用失败，回退规则化：{}", e.getMessage());
            return null;
        }
    }

    @Override
    public String chat(String sceneCode, String systemPrompt, String userMessage) {
        // Strategy：委托工厂解析绑定模型，有则用绑定模型，无则回落默认
        ChatLanguageModel boundModel = null;
        if (sceneResolver != null) {
            try {
                boundModel = sceneResolver.resolveChatModel(sceneCode);
            } catch (Exception e) {
                log.warn("[LlmClient] 场景({})模型解析异常，回落默认模型：{}", sceneCode, e.getMessage());
            }
        }
        if (boundModel != null) {
            try {
                // langchain4j 1.0.0-beta3 API：chat(messages)
                ChatResponse resp = boundModel.chat(Collections.singletonList(
                        new UserMessage(systemPrompt + "\n\n" + userMessage)));
                return resp == null ? null : (resp.aiMessage() == null ? null : resp.aiMessage().text());
            } catch (Exception e) {
                log.warn("[LlmClient] 场景({})绑定模型调用失败，回落默认模型：{}", sceneCode, e.getMessage());
            }
        }
        return chat(systemPrompt, userMessage);
    }

    @Override
    public boolean isSceneBound(String sceneCode) {
        if (sceneResolver == null) {
            return false;
        }
        try {
            return sceneResolver.resolveChatModel(sceneCode) != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isEnabled() {
        return llmService != null;
    }
}