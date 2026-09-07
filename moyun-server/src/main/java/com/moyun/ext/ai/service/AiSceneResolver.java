package com.moyun.ext.ai.service;

import com.moyun.ext.ai.dto.AiSceneBinding;
import com.moyun.ext.ai.entity.AiSceneConfig;

/**
 * AI 场景解析器
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
}