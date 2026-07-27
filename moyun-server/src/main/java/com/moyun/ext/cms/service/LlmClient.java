package com.moyun.ext.cms.service;

import java.util.List;
import java.util.Map;

/**
 * 大语言模型客户端抽象（v5.9 阶段3：AI 模型接入框架）
 * <p>
 * 统一封装 LLM 调用，业务层通过此接口注入，不感知具体实现。
 * 当前默认实现为 {@link NoopLlmClient}（规则化兜底，不调用真实模型）。
 * 后期接入 langchain4j + DashScope 时，新增 DashScopeLlmClient 实现并替换默认 Bean。
 * <p>
 * 设计要点：
 * - 同步 chat 方法：适用于短回复场景（简历建议、面试反馈）
 * - 消息格式：role + content，兼容 system/user/assistant 三种角色
 * - 失败容忍：实现内部应捕获异常并返回 null，由调用方回退到规则化逻辑
 *
 * @author moyun
 */
public interface LlmClient {

    /**
     * 同步对话（单轮）
     *
     * @param systemPrompt 系统提示词（定义 AI 角色与任务约束）
     * @param userMessage  用户输入
     * @return AI 回复文本；调用失败或未启用时返回 null（由调用方回退到规则化）
     */
    String chat(String systemPrompt, String userMessage);

    /**
     * 同步对话（多轮上下文）
     *
     * @param messages 消息列表，每条含 role 与 content
     * @return AI 回复文本；失败或未启用时返回 null
     */
    String chat(List<Map<String, String>> messages);

    /**
     * 是否已启用真实 AI 模型
     *
     * @return true 表示当前 Bean 会调用真实 LLM；false 表示 Noop 实现（规则化兜底）
     */
    boolean isEnabled();
}
