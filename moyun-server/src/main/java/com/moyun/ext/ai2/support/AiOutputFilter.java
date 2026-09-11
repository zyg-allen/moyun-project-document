package com.moyun.ext.ai2.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.moyun.ext.ai.entity.AiSceneConfig;
import com.moyun.ext.ai2.constant.AiErrorCodes;
import com.moyun.ext.ai2.model.AiExecuteResponse;
import com.moyun.system.filter.SensitiveWordFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * AI 输出内容过滤器（v11.62 P1-3）
 *
 * <p>复用 {@link SensitiveWordFilter} 的 DFA 词树，对网关响应 data 的全部文本节点
 * 做敏感词脱敏（替换为 *，保留原文空白与格式）。依据《AI底座企业级评估-代码实测结论
 * 与改进清单》P1-3：输出过滤复用 DFA 词树，不另建检测体系。</p>
 *
 * <p>设计要点：
 * <ul>
 *   <li><b>类型保持</b>：Jackson 往返（valueToTree → mask 文本节点 → treeToValue 回原类型）。
 *       业务消费方存在 {@code resp.getData() instanceof XxxSceneData} 强转
 *       （PortalTopicServiceImpl / PortalJobTemplateServiceImpl 等），data 不能替换为 JsonNode。</li>
 *   <li><b>只改文本</b>：数值/布尔等非文本节点不动（金额、评分等业务字段零影响）；
 *       仅在命中时才做往返重建，无命中时原对象原样返回（零开销零风险）。</li>
 *   <li><b>失败放行</b>：DFA 内存操作稳定，异常仅可能来自 Jackson 序列化（极端 data 结构）——
 *       放行原文并 ERROR 留痕（过滤不可用不应阻断业务），与 Agent 人设注入的容错策略一致。</li>
 *   <li><b>位置约束</b>：须在缓存回写与执行日志之前调用——缓存与日志留痕的均为脱敏后内容；
 *       缓存命中路径同样调用（兜底过滤功能上线前的存量旧缓存）。</li>
 *   <li><b>已知局限</b>：流式（SSE）输出由 Handler 直发 emitter，逐 token 分片无法有效匹配
 *       跨片敏感词，本过滤器仅覆盖同步路径（与 TokenCostGuard 流式消费累计局限同类）。</li>
 * </ul>
 *
 * @author laomao
 * @since 2026-09-11
 */
@Slf4j
@Component
public class AiOutputFilter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final SensitiveWordFilter sensitiveWordFilter;

    public AiOutputFilter(SensitiveWordFilter sensitiveWordFilter) {
        this.sensitiveWordFilter = sensitiveWordFilter;
    }

    /**
     * 场景开关（ai_scene_config.enable_output_filter）
     */
    public boolean isEnabled(AiSceneConfig config) {
        return config != null && Boolean.TRUE.equals(config.getEnableOutputFilter());
    }

    /**
     * 对成功响应的 data 应用敏感词脱敏（任意深度的对象值/数组元素文本）
     *
     * @return 命中的敏感词（去重，保持命中顺序）；未命中/跳过/异常放行返回空列表
     */
    @SuppressWarnings("unchecked")
    public List<String> applyFilter(AiExecuteResponse<?> response) {
        if (response == null || response.getData() == null
                || response.getCode() == null || response.getCode() != AiErrorCodes.SUCCESS) {
            return List.of();
        }
        Object data = response.getData();
        try {
            JsonNode node = MAPPER.valueToTree(data);
            Set<String> hits = new LinkedHashSet<>();
            if (maskNode(node, hits)) {
                ((AiExecuteResponse<Object>) response).setData(
                        MAPPER.treeToValue(node, data.getClass()));
                log.warn("[ai2:输出过滤] 命中敏感词已脱敏: scene={}, words={}",
                        response.getSceneCode(), hits);
                return List.copyOf(hits);
            }
        } catch (Exception e) {
            log.error("[ai2:输出过滤] 脱敏异常（放行原文）: scene={}, {}",
                    response.getSceneCode(), e.getMessage(), e);
        }
        return List.of();
    }

    /**
     * 递归脱敏 JSON 树的全部文本节点（对象值/数组元素，任意深度）
     *
     * @return 是否发生替换（true 时才需要回写）
     */
    private boolean maskNode(JsonNode node, Set<String> hits) {
        boolean changed = false;
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            List<String> names = new ArrayList<>();
            obj.fieldNames().forEachRemaining(names::add);
            for (String name : names) {
                JsonNode child = obj.get(name);
                if (child.isTextual()) {
                    String masked = maskText(child.textValue(), hits);
                    if (masked != null) {
                        obj.put(name, masked);
                        changed = true;
                    }
                } else if (child.isContainerNode()) {
                    changed |= maskNode(child, hits);
                }
            }
        } else if (node.isArray()) {
            ArrayNode arr = (ArrayNode) node;
            for (int i = 0; i < arr.size(); i++) {
                JsonNode child = arr.get(i);
                if (child.isTextual()) {
                    String masked = maskText(child.textValue(), hits);
                    if (masked != null) {
                        arr.set(i, TextNode.valueOf(masked));
                        changed = true;
                    }
                } else if (child.isContainerNode()) {
                    changed |= maskNode(child, hits);
                }
            }
        }
        return changed;
    }

    /**
     * 单文本脱敏：无命中返回 null（调用方跳过，保持原节点），命中返回脱敏文本并登记命中词
     */
    private String maskText(String text, Set<String> hits) {
        List<String> words = sensitiveWordFilter.find(text);
        if (words.isEmpty()) {
            return null;
        }
        hits.addAll(words);
        return sensitiveWordFilter.mask(text);
    }
}
