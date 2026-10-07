package com.moyun.ext.ai.service;

import java.util.function.Consumer;

/**
 * LLM服务接口
 *
 * <p>封装LLM调用，简化业务代码</p>
 *
 * <p><b>计量口径（v14.72 统一收口）</b>：所有对外重载默认按 {@link #DIRECT_SCENE}
 * 场景记录 ai_execute_log（Token 真实 usage 优先、缺失本地估算）——admin 直连
 * （提示词生成/SQL生成/图表XML/会话摘要等）不再是成本盲区。网关内部回落调用
 * 传 {@code meterScene=null} 跳过记账（网关按场景统一计量，避免同一次调用双行记账）。</p>
 *
 * @author laomao
 */
public interface LLMService {

    /** admin 直连计量的统一场景口径（ai_execute_log.scene_code；无配置行，仅记账） */
    String DIRECT_SCENE = "admin.direct";

    /**
     * 使用默认模型生成文本（按 admin.direct 场景计量记账）
     *
     * @param prompt 提示词
     * @return 生成的文本
     */
    default String generate(String prompt) {
        return generate(prompt, DIRECT_SCENE);
    }

    /**
     * 使用默认模型生成文本（指定计量场景）
     *
     * @param prompt      提示词
     * @param meterScene  计量场景口径（ai_execute_log.scene_code）；
     *                    {@code null} = 跳过记账（网关内部调用，由网关统一计量）
     * @return 生成的文本
     */
    String generate(String prompt, String meterScene);

    /**
     * 使用指定模型生成文本（按 admin.direct 场景计量记账）
     *
     * @param modelConfigId 模型配置ID
     * @param prompt 提示词
     * @return 生成的文本
     */
    String generate(Long modelConfigId, String prompt);

    /**
     * 流式生成文本（SSE，按 admin.direct 场景计量记账）
     *
     * @param prompt 提示词
     * @param onToken 每个token的回调
     * @param onComplete 完成回调
     * @param onError 错误回调
     */
    default void generateStream(String prompt,
                                Consumer<String> onToken,
                                Runnable onComplete,
                                Consumer<Throwable> onError) {
        generateStream(prompt, DIRECT_SCENE, onToken, onComplete, onError);
    }

    /**
     * 流式生成文本（SSE，指定计量场景）
     *
     * @param prompt      提示词
     * @param meterScene  计量场景口径；{@code null} = 跳过记账（网关内部调用）
     * @param onToken     每个token的回调
     * @param onComplete  完成回调
     * @param onError     错误回调
     */
    void generateStream(String prompt, String meterScene,
                        Consumer<String> onToken,
                        Runnable onComplete,
                        Consumer<Throwable> onError);
}
