package com.moyun.ext.cms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * LLM 空实现（v5.9 阶段3：AI 未启用或无真实实现时的默认兜底）
 * <p>
 * 使用 {@link ConditionalOnMissingBean} 确保任意配置下容器中至少有一个 {@link LlmClient} Bean：
 * - 当容器中不存在其他 LlmClient 实现时，此 Bean 生效，所有调用返回 null，业务层回退规则化
 * - 当新增了 DashScopeLlmClient 等真实实现时，此 Bean 自动让位（条件不匹配）
 * <p>
 * 这样避免了"moyun.ai.enabled=true 但无真实实现时 Spring 启动失败"的陷阱。
 *
 * @author moyun
 */
@Component
@ConditionalOnMissingBean(LlmClient.class)
public class NoopLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(NoopLlmClient.class);

    @Override
    public String chat(String systemPrompt, String userMessage) {
        log.debug("[LlmClient] Noop 实现：无真实 LLM Bean，返回 null 触发规则化兜底");
        return null;
    }

    @Override
    public String chat(List<Map<String, String>> messages) {
        log.debug("[LlmClient] Noop 实现：无真实 LLM Bean，返回 null 触发规则化兜底");
        return null;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}
