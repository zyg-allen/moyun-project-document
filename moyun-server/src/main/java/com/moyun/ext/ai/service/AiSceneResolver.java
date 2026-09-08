package com.moyun.ext.ai.service;

import com.moyun.ext.ai.dto.AiSceneBinding;
import com.moyun.ext.ai.entity.AiSceneConfig;
import dev.langchain4j.model.chat.ChatLanguageModel;

/**
 * AI 场景解析器（Factory + Chain of Responsibility）
 *
 * <p>将业务场景代码解析为具体的 Agent/模型/知识库/工具/工作流绑定。
 * 任何异常均返回空绑定（empty），不阻断业务主流程。</p>
 *
 * <p>解析策略：
 * <ul>
 *   <li>启用配置仅 1 行 → 直接命中</li>
 *   <li>多行 → 按 weight 轮盘赌灰度（权重全 0 时 is_default 优先，再按 priority DESC）</li>
 * </ul>
 *
 * <p>v11.39：新增 {@link #resolveChatModel(String)} 工厂方法——
 * 业务方只需传 scene_code 即可获得场景绑定的 ChatLanguageModel，
 * 模型选择逻辑（责任链：Agent 绑定 → 直绑模型 → null 回落默认）封装在解析器内部，
 * LlmClient 等 Strategy 层不再关心模型怎么选。
 *
 * @author moyun
 */
public interface AiSceneResolver {

    /**
     * 按场景代码解析运行时绑定（含灰度）
     *
     * @param sceneCode 场景代码，如 voice_interview
     * @return 解析结果（异常/无配置时返回 empty，不抛出）
     */
    AiSceneBinding resolve(String sceneCode);

    /**
     * 将指定配置行构建为绑定（供后台「测试」按钮直接验证某一行配置）
     */
    AiSceneBinding bind(AiSceneConfig config);

    /**
     * 工厂方法（v11.39）：按场景代码解析绑定的聊天模型
     *
     * <p>责任链解析顺序：
     * <ol>
     *   <li>场景绑定了启用的 Agent → 取 Agent 的 modelConfigId + temperature + maxTokens 构建模型</li>
     *   <li>Agent 为空但直绑了模型 → 用该 modelConfigId 构建模型（默认温度）</li>
     *   <li>无绑定/解析异常/模型构建失败 → 返回 null（调用方回落默认模型）</li>
     * </ol>
     *
     * @param sceneCode 场景代码（AiSceneEnum 注册值）
     * @return 场景绑定的 ChatLanguageModel；无绑定或构建失败返回 null
     */
    ChatLanguageModel resolveChatModel(String sceneCode);
}