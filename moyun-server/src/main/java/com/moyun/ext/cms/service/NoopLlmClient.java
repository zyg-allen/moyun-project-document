package com.moyun.ext.cms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * LLM 空实现（v5.9 阶段3：AI 未启用时的默认兜底）
 * <p>
 * 注册条件：{@code moyun.ai.enabled=false} 或未配置（matchIfMissing=true）。
 * <p>
 * 设计要点：
 * - 使用 {@link ConditionalOnProperty} 而非 {@code @ConditionalOnMissingBean}：
 *   后者只能用于自动配置类（@Configuration），用在 @Component 上时 Bean 评估顺序不确定，
 *   会触发"找不到 LlmClient Bean"的启动异常（Spring Boot 官方文档明确限制）。
 * - 当 moyun.ai.enabled=true 时，此 Bean 不注册，需由真实实现（如 DashScopeLlmClient）提供；
 *   若未提供真实实现，启动将失败——这是合理的，因为用户显式启用了 AI 却未配置实现。
 * - 业务层调用 {@link LlmClient#isEnabled()} 为 false 时回退规则化逻辑。
 *
 * @author moyun
 */
@Component
@ConditionalOnProperty(prefix = "moyun.ai", name = "enabled", havingValue = "false", matchIfMissing = true)
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
