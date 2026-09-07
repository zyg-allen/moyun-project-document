package com.moyun.ext.ai.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.ext.ai.dto.AiSceneBinding;
import com.moyun.ext.ai.entity.Agent;
import com.moyun.ext.ai.entity.AiSceneConfig;
import com.moyun.ext.ai.entity.ModelConfig;
import com.moyun.ext.ai.service.AgentService;
import com.moyun.ext.ai.service.AiSceneConfigService;
import com.moyun.ext.ai.service.AiSceneResolver;
import com.moyun.ext.ai.service.ModelConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * AI 场景解析器实现
 *
 * <p>灰度策略：多版本启用时按 weight 轮盘赌；权重全 0 时 is_default 优先，再按 priority DESC。
 * 任何异常返回 empty()，业务方走原有默认逻辑，保证不阻断。</p>
 *
 * @author moyun
 */
@Slf4j
@Service
public class AiSceneResolverImpl implements AiSceneResolver {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private AiSceneConfigService sceneConfigService;

    @Autowired
    private AgentService agentService;

    @Autowired
    private ModelConfigService modelConfigService;

    @Override
    public AiSceneBinding resolve(String sceneCode) {
        try {
            List<AiSceneConfig> configs = sceneConfigService.listEnabledBySceneCode(sceneCode);
            if (configs == null || configs.isEmpty()) {
                return AiSceneBinding.empty();
            }
            AiSceneConfig hit = configs.size() == 1 ? configs.get(0) : pickByWeight(configs);
            if (hit == null) {
                return AiSceneBinding.empty();
            }
            AiSceneBinding binding = bind(hit);
            log.debug("[ai-scene] 场景 {} 命中配置 id={} version={} agentId={} modelConfigId={}",
                    sceneCode, hit.getId(), hit.getVersion(), hit.getAgentId(), hit.getModelConfigId());
            return binding;
        } catch (Exception e) {
            // 场景解析失败不阻断业务：返回空绑定走原有默认逻辑
            log.warn("[ai-scene] 场景 {} 解析异常，降级为空绑定: {}", sceneCode, e.getMessage());
            return AiSceneBinding.empty();
        }
    }

    @Override
    public AiSceneBinding bind(AiSceneConfig config) {
        AiSceneBinding binding = new AiSceneBinding();
        if (config == null) {
            return binding;
        }
        binding.setSceneConfig(config);
        binding.setWorkflowId(config.getWorkflowId());

        // 1. Agent 优先（未启用/已删除视为未绑定）
        if (config.getAgentId() != null) {
            try {
                Agent agent = agentService.getById(config.getAgentId());
                if (agent != null && Boolean.TRUE.equals(agent.getEnabled())) {
                    binding.setAgent(agent);
                }
            } catch (Exception e) {
                log.warn("[ai-scene] 配置 {} 加载 Agent {} 失败: {}", config.getId(), config.getAgentId(), e.getMessage());
            }
        }

        // 2. 直绑模型兜底（仅 Agent 未命中时生效）
        if (!binding.hasAgent() && config.getModelConfigId() != null) {
            try {
                ModelConfig modelConfig = modelConfigService.getById(config.getModelConfigId());
                if (modelConfig != null && Boolean.TRUE.equals(modelConfig.getEnabled())) {
                    binding.setModelConfig(modelConfig);
                }
            } catch (Exception e) {
                log.warn("[ai-scene] 配置 {} 加载模型 {} 失败: {}", config.getId(), config.getModelConfigId(), e.getMessage());
            }
        }

        // 3. 解析 JSON 字段（宽容解析，失败保持空集合）
        binding.setKnowledgeLibraryIds(parseIdList(config.getKnowledgeLibraryIds()));
        binding.setToolIds(parseIdList(config.getToolIds()));
        binding.setConfigJson(parseConfigJson(config.getConfigJson()));
        return binding;
    }

    /**
     * 多版本灰度：权重和 > 0 轮盘赌；全 0 时 is_default 优先 → priority DESC → 首行
     */
    private AiSceneConfig pickByWeight(List<AiSceneConfig> configs) {
        int totalWeight = configs.stream()
                .mapToInt(c -> c.getWeight() == null ? 0 : Math.max(0, c.getWeight()))
                .sum();
        if (totalWeight <= 0) {
            return configs.stream()
                    .filter(c -> Boolean.TRUE.equals(c.getIsDefault()))
                    .findFirst()
                    .orElse(configs.get(0));
        }
        int cursor = ThreadLocalRandom.current().nextInt(totalWeight);
        for (AiSceneConfig config : configs) {
            int weight = config.getWeight() == null ? 0 : Math.max(0, config.getWeight());
            if (cursor < weight) {
                return config;
            }
            cursor -= weight;
        }
        return configs.get(configs.size() - 1);
    }

    private List<Long> parseIdList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<Long> ids = MAPPER.readValue(json, new TypeReference<List<Long>>() {
            });
            return ids == null ? List.of() : ids;
        } catch (Exception e) {
            log.warn("[ai-scene] ID 列表 JSON 解析失败: {} -> {}", json, e.getMessage());
            return List.of();
        }
    }

    private Map<String, Object> parseConfigJson(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, Object> map = MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {
            });
            return map == null ? Map.of() : map;
        } catch (Exception e) {
            log.warn("[ai-scene] configJson 解析失败: {} -> {}", json, e.getMessage());
            return Map.of();
        }
    }
}