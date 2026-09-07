package com.moyun.ext.ai.dto;

import com.moyun.ext.ai.entity.Agent;
import com.moyun.ext.ai.entity.AiSceneConfig;
import com.moyun.ext.ai.entity.ModelConfig;
import lombok.Data;

import java.util.Collections;
import java.util.Map;

/**
 * 场景解析结果 DTO
 *
 * <p>由 {@link com.moyun.ext.ai.service.AiSceneResolver} 产出，业务方按以下顺序消费：
 * <ol>
 *   <li>{@link #hasAgent()}：场景绑定了启用的智能体，直接使用 agent（含其模型/RAG/知识库配置）</li>
 *   <li>{@link #hasModelOnly()}：未绑定 Agent 但直绑了模型，业务方合成伪 Agent（空 systemPrompt 自动回退旧人设）</li>
 *   <li>{@link #isEmpty()}：场景无配置或解析异常，走业务原有默认逻辑（不阻断）</li>
 * </ol>
 *
 * @author moyun
 */
@Data
public class AiSceneBinding {

    /** 命中的场景配置行 */
    private AiSceneConfig sceneConfig;

    /** 绑定的智能体（agent_id 非空且启用时非 null） */
    private Agent agent;

    /** 直接绑定的模型配置（agent 为空且 model_config_id 非空时非 null） */
    private ModelConfig modelConfig;

    /** 解析后的知识库ID列表 */
    private java.util.List<Long> knowledgeLibraryIds = Collections.emptyList();

    /** 解析后的工具ID列表 */
    private java.util.List<Long> toolIds = Collections.emptyList();

    /** 绑定的工作流ID */
    private Long workflowId;

    /** 解析后的场景策略配置（config_json） */
    private Map<String, Object> configJson = Collections.emptyMap();

    /** 场景绑定了启用的智能体 */
    public boolean hasAgent() {
        return agent != null && Boolean.TRUE.equals(agent.getEnabled());
    }

    /** 未绑定 Agent 但直绑了模型 */
    public boolean hasModelOnly() {
        return agent == null && modelConfig != null;
    }

    /** 空绑定（无配置/异常降级） */
    public boolean isEmpty() {
        return sceneConfig == null;
    }

    /** 从策略配置读取布尔值（缺失返回 defaultValue） */
    public boolean getBooleanConfig(String key, boolean defaultValue) {
        Object v = configJson == null ? null : configJson.get(key);
        if (v instanceof Boolean) {
            return (Boolean) v;
        }
        if (v instanceof String) {
            return Boolean.parseBoolean((String) v);
        }
        return defaultValue;
    }

    /** 空绑定（业务方走原有默认逻辑） */
    public static AiSceneBinding empty() {
        return new AiSceneBinding();
    }
}