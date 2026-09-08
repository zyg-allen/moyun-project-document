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
 * - 场景感知（v11.39）：chat(sceneCode, ...) 按 ai_scene_config 解析绑定模型/Agent，
 *   未绑定或绑定未启用时回落默认模型——业务代码只带 scene_code，不再硬编码模型选择
 *
 * @author moyun
 */
public interface LlmClient {

    /**
     * 同步对话（单轮，默认模型）
     *
     * @param systemPrompt 系统提示词（定义 AI 角色与任务约束）
     * @param userMessage  用户输入
     * @return AI 回复文本；调用失败或未启用时返回 null（由调用方回退到规则化）
     */
    String chat(String systemPrompt, String userMessage);

    /**
     * 同步对话（多轮上下文，默认模型）
     *
     * @param messages 消息列表，每条含 role 与 content
     * @return AI 回复文本；失败或未启用时返回 null
     */
    String chat(List<Map<String, String>> messages);

    /**
     * 场景感知同步对话（单轮，v11.39）
     *
     * <p>解析顺序（与 AiSceneResolver 一致）：
     * 1）ai_scene_config 中 scene_code 有启用的绑定 → 用绑定的模型（Agent 绑定取 Agent 的模型与温度）
     * 2）无绑定/绑定未启用/解析失败 → 回落 AI 模块默认聊天模型（与 {@link #chat(String, String)} 行为一致）
     *
     * @param sceneCode     场景代码（AiSceneEnum 注册值，如 voice_interview/resume_optimize/finance_analysis）
     * @param systemPrompt  系统提示词
     * @param userMessage   用户输入
     * @return AI 回复文本；调用失败或未启用时返回 null
     */
    String chat(String sceneCode, String systemPrompt, String userMessage);

    /**
     * 场景是否已绑定模型（v11.39）
     *
     * @return true 表示该场景在 ai_scene_config 有启用绑定；false 表示走默认模型
     */
    boolean isSceneBound(String sceneCode);

    /**
     * 是否已启用真实 AI 模型
     *
     * @return true 表示当前 Bean 会调用真实 LLM；false 表示 Noop 实现（规则化兜底）
     */
    boolean isEnabled();
}