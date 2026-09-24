package com.moyun.ext.aigateway.model;

import com.moyun.ext.ai.entity.AiSceneConfig;
import lombok.Data;

/**
 * 场景配置执行元数据（AI统一网关整改 2A.3）
 *
 * <p>配置行 {@code ai_scene_config} → 执行器视图的薄投影：DefaultSceneExecutor
 * 配置驱动执行全程消费本视图而非裸实体，实体演进（加列/改名）不直接冲击执行器。
 * 类名区别于响应元数据 {@link AiMetadata}（tokenUsed/modelUsed）——本类是"配置侧"元数据。</p>
 *
 * <p>含网关入口校验与执行兜底依赖的字段：outputMode / openApi / fallbackModelId /
 * fallbackResponse（v3.1 修正 ⑥-b 补齐）。</p>
 *
 * @author laomao
 * @since 2026-09-24
 */
@Data
public class AiSceneMetadata {

    /** 有效场景码（task 拆行后为全码 scene:task，与 ai_execute_log.scene_code 口径一致） */
    private String effectiveSceneCode;

    /** 场景名称 */
    private String sceneName;

    /** 场景分类: chat/analysis/generation/classification */
    private String sceneCategory;

    /** 用户提示词模板（{{variable}} 占位符） */
    private String userPromptTemplate;

    /** 输出结构定义 JSON */
    private String outputSchema;

    /** 解析器: json/markdown/text */
    private String outputParser;

    /** 输出模式: sync/stream/both */
    private String outputMode;

    /** 是否开放通用入口调用（open_api=1 才能经 /api/ai/execute 外部调用） */
    private Boolean openApi;

    /** 绑定的智能体（ai_agent.id） */
    private Long agentId;

    /** 直接绑定模型（ai_model_config.id，agent_id 为空时生效） */
    private Long modelConfigId;

    /** 备用模型 ID */
    private Long fallbackModelId;

    /** 兜底回复（AI 不可用时网关降级策略消费） */
    private String fallbackResponse;

    /**
     * 从配置行构建执行视图
     *
     * @param config 配置行（须非 null——网关入口已保证配置存在才路由到执行器）
     * @return 执行元数据
     */
    public static AiSceneMetadata from(AiSceneConfig config) {
        AiSceneMetadata meta = new AiSceneMetadata();
        meta.setEffectiveSceneCode(config.getSceneCode());
        meta.setSceneName(config.getSceneName());
        meta.setSceneCategory(config.getSceneCategory());
        meta.setUserPromptTemplate(config.getUserPromptTemplate());
        meta.setOutputSchema(config.getOutputSchema());
        meta.setOutputParser(config.getOutputParser());
        meta.setOutputMode(config.getOutputMode());
        meta.setOpenApi(config.getOpenApi());
        meta.setAgentId(config.getAgentId());
        meta.setModelConfigId(config.getModelConfigId());
        meta.setFallbackModelId(config.getFallbackModelId());
        meta.setFallbackResponse(config.getFallbackResponse());
        return meta;
    }

    /** 解析器是否为文本类（markdown/text → 输出包装 content，不走 JSON 提取） */
    public boolean isTextParser() {
        return "markdown".equals(outputParser) || "text".equals(outputParser);
    }
}
