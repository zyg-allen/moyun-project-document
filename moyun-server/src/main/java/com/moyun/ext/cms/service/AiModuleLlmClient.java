package com.moyun.ext.cms.service;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.moyun.ext.ai.service.LLMService;
import com.moyun.util.string.StringUtils;

/**
 * LLM 真实实现：桥接到 AI 模块 LLMService（v10.8 统一 AI 能力通道）
 *
 * <p>注册条件：{@code moyun.ai.enabled=true}（此时 NoopLlmClient 不注册，本 Bean 提供真实通道）。
 * 模型/密钥无需在本模块重复配置——直接复用 AI 模块「模型配置」中启用的默认聊天模型。
 *
 * <p>失败容忍：调用异常时返回 null（记 warn），由调用方（如 ResumeAiAdviceService）回退规则化逻辑，
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
    public boolean isEnabled() {
        return llmService != null;
    }
}
