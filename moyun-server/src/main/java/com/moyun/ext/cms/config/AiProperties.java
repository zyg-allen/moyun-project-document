package com.moyun.ext.cms.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI 配置属性（v5.9 阶段3：AI 模型接入框架）
 * <p>
 * 在 application.yaml 中配置：
 * <pre>
 * moyun:
 *   ai:
 *     enabled: false  # 是否启用 AI 模型（默认 false，规则化兜底）
 *     provider: dashscope  # 服务提供方：dashscope（通义千问）/ openai / noop
 *     api-key: ""         # API Key（启用时必填）
 *     model: "qwen-turbo" # 模型名
 *     base-url: ""        # 自定义 endpoint（可选）
 *     timeout: 30          # 超时秒数
 *     max-tokens: 2048     # 最大 token 数
 * </pre>
 * <p>
 * 设计原则：默认禁用，规则化逻辑保留为 fallback；启用后通过 LlmClient 注入到各业务服务。
 *
 * @author moyun
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "moyun.ai")
public class AiProperties {

    /** 是否启用 AI 模型（默认 false，所有 AI 调用回退到规则化实现） */
    private boolean enabled = false;

    /** 服务提供方：dashscope（通义千问）/ openai / noop */
    private String provider = "noop";

    /** API Key（启用时必填） */
    private String apiKey = "";

    /** 模型名（如 qwen-turbo / gpt-4o-mini） */
    private String model = "qwen-turbo";

    /** 自定义 endpoint（可选，留空使用默认） */
    private String baseUrl = "";

    /** 超时秒数 */
    private int timeout = 30;

    /** 最大 token 数 */
    private int maxTokens = 2048;

    /** 简历 AI 建议是否启用 AI 模型（独立开关，受全局 enabled 约束） */
    private boolean resumeAdviceEnabled = false;

    /** 模拟面试 AI 反馈是否启用（独立开关，受全局 enabled 约束） */
    private boolean mockInterviewFeedbackEnabled = false;
}
