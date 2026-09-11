package com.moyun.ext.ai.engine.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 工具参数 JSON Schema 校验器（v11.63 P1-2）
 *
 * <p>依据《AI底座企业级评估-代码实测结论与改进清单》P1-2：ai_agent_tool.parameters 已是
 * JSON Schema（种子数据 type/required/properties），但执行链零校验——LLM 生成的参数
 * 类型错误（如 days 传字符串）直接打入执行器，轻则执行失败重则 ClassCastException。</p>
 *
 * <p>支持子集（覆盖本项目全部工具 schema 实际用到的关键字）：
 * type（string/integer/number/boolean/array/object）、required、properties、
 * enum、minimum/maximum、items.type。</p>
 *
 * <p>设计要点：
 * <ul>
 *   <li><b>fail-open</b>：schema 为空/不可解析/type≠object 时跳过校验（兼容文本描述型
 *       存量数据与管理端手填脏数据——校验器不可用不应阻断工具执行，执行器自有兜底）。</li>
 *   <li><b>integer 宽容</b>：Integer/Long/整值 Double/BigDecimal 均视为 integer
 *       （LLM JSON 序列化常产出 3.0 形态）；字符串数字不宽容（应让 LLM 修正类型）。</li>
 *   <li><b>不改写参数</b>：纯校验，无副作用；类型转换由执行器 asInt/asString 既有兜底处理。</li>
 *   <li><b>错误可自纠</b>：错误文案为 LLM 可读的修正指令（期望类型 vs 实际值类型），
 *       经工具失败通道回传（chat 展示/工作流 NodeResult.fail），下一轮对话 LLM 可据此
 *       重新生成合规的 [TOOL_CALL]。</li>
 *   <li><b>未知类型跳过</b>：properties 中未知 type（如 null）跳过该项，不误杀。</li>
 * </ul>
 *
 * @author laomao
 * @since 2026-09-11
 */
public final class ToolParamValidator {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** object 类型 schema 下允许的属性 type 关键字（其余跳过校验） */
    private static final Set<String> KNOWN_TYPES = Set.of(
            "string", "integer", "number", "boolean", "array", "object");

    private ToolParamValidator() {
    }

    /**
     * 校验工具参数是否符合 JSON Schema
     *
     * @param schemaJson 工具参数 JSON Schema（executor.getParametersSchema() 或 ai_agent_tool.parameters）
     * @param params     LLM/调用方生成的参数（可为 null，视为空参）
     * @return 错误列表（空=通过或跳过）；每条为可直接回填 LLM 的中文修正指令
     */
    public static List<String> validate(String schemaJson, Map<String, Object> params) {
        List<String> errors = new ArrayList<>();
        if (schemaJson == null || schemaJson.isBlank()) {
            return errors; // 无 schema 不校验
        }
        JsonNode schema;
        try {
            schema = MAPPER.readTree(schemaJson);
        } catch (Exception e) {
            return errors; // schema 非法（文本描述型存量数据）不校验
        }
        if (!schema.isObject() || !"object".equals(schema.path("type").asText(null))) {
            return errors; // 仅支持 object 根 schema，其余形态不校验
        }

        Map<String, Object> safeParams = params != null ? params : Map.of();

        // 1. required 必填校验
        for (JsonNode req : schema.path("required")) {
            if (req.isTextual() && !safeParams.containsKey(req.asText())) {
                errors.add("缺少必填参数 \"" + req.asText() + "\"");
            }
        }

        // 2. properties 类型校验
        JsonNode properties = schema.path("properties");
        if (properties.isObject()) {
            for (var entry : properties.properties()) {
                String name = entry.getKey();
                JsonNode propSchema = entry.getValue();
                Object value = safeParams.get(name);
                if (value == null) {
                    continue; // 缺失已由 required 覆盖；可选参数缺省合法
                }
                checkProperty(name, propSchema, value, errors);
            }
        }
        return errors;
    }

    /**
     * 单属性校验：type / enum / minimum / maximum / items.type
     */
    private static void checkProperty(String name, JsonNode propSchema, Object value, List<String> errors) {
        String type = propSchema.path("type").asText(null);
        if (type == null || !KNOWN_TYPES.contains(type)) {
            return; // 未知/缺失类型不校验
        }

        // enum 枚举校验（优先于类型，提示更精准）
        JsonNode enumNode = propSchema.path("enum");
        if (enumNode.isArray() && !enumNode.isEmpty()) {
            boolean matched = false;
            for (JsonNode allowed : enumNode) {
                if (valueEqualsAllowed(value, allowed)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                errors.add("参数 \"" + name + "\" 的值 " + truncate(value)
                        + " 不在允许范围 " + enumNode.toPrettyString().replace("\n", "") + " 内");
                return;
            }
        }

        // type 类型校验
        if (!matchesType(type, value)) {
            errors.add("参数 \"" + name + "\" 应为 " + type + " 类型，实际收到 "
                    + valueType(value) + "：" + truncate(value) + "，请修正类型后重新调用");
            return;
        }

        // number/integer 范围校验
        if (value instanceof Number n && ("number".equals(type) || "integer".equals(type))) {
            BigDecimal dec = toBigDecimal(n);
            if (dec != null) {
                JsonNode min = propSchema.path("minimum");
                if (min.isNumber() && dec.compareTo(BigDecimal.valueOf(min.doubleValue())) < 0) {
                    errors.add("参数 \"" + name + "\" 的值 " + dec + " 小于最小值 " + min.asDouble());
                }
                JsonNode max = propSchema.path("maximum");
                if (max.isNumber() && dec.compareTo(BigDecimal.valueOf(max.doubleValue())) > 0) {
                    errors.add("参数 \"" + name + "\" 的值 " + dec + " 大于最大值 " + max.asDouble());
                }
            }
        }

        // array 元素类型校验
        if ("array".equals(type) && value instanceof List<?> list && !list.isEmpty()) {
            JsonNode items = propSchema.path("items");
            String itemType = items.isObject() ? items.path("type").asText(null) : null;
            if (itemType != null && KNOWN_TYPES.contains(itemType)) {
                for (Object item : list) {
                    if (!matchesType(itemType, item)) {
                        errors.add("参数 \"" + name + "\" 的数组元素应为 " + itemType
                                + " 类型，存在 " + valueType(item) + "：" + truncate(item));
                        break; // 每属性报一条，避免刷屏
                    }
                }
            }
        }
    }

    /**
     * Java 值是否匹配 JSON Schema 类型（integer 宽容整值浮点）
     */
    private static boolean matchesType(String type, Object value) {
        return switch (type) {
            case "string" -> value instanceof String;
            case "integer" -> isIntegerValue(value);
            case "number" -> value instanceof Number;
            case "boolean" -> value instanceof Boolean;
            case "array" -> value instanceof List;
            case "object" -> value instanceof Map;
            default -> true;
        };
    }

    /** integer：Integer/Long 直接过；Double/BigDecimal 需为整值（LLM JSON 常产出 3.0） */
    private static boolean isIntegerValue(Object value) {
        if (value instanceof Integer || value instanceof Long || value instanceof Short || value instanceof Byte) {
            return true;
        }
        if (value instanceof Double d) {
            return d == Math.floor(d) && !d.isInfinite() && !d.isNaN();
        }
        if (value instanceof BigDecimal bd) {
            return bd.stripTrailingZeros().scale() <= 0;
        }
        return false;
    }

    /** enum 匹配：字符串/数值/布尔分别比较（数值用 compareTo 避免整型/浮点表示差异） */
    private static boolean valueEqualsAllowed(Object value, JsonNode allowed) {
        if (allowed.isTextual()) {
            return value instanceof String s && s.equals(allowed.asText());
        }
        if (allowed.isBoolean()) {
            return value instanceof Boolean b && b == allowed.asBoolean();
        }
        if (allowed.isNumber()) {
            return value instanceof Number n && toBigDecimal(n) != null
                    && toBigDecimal(n).compareTo(BigDecimal.valueOf(allowed.doubleValue())) == 0;
        }
        return false;
    }

    private static BigDecimal toBigDecimal(Number n) {
        if (n instanceof BigDecimal bd) {
            return bd;
        }
        try {
            return new BigDecimal(n.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 值的 JSON 类型描述（错误文案用） */
    private static String valueType(Object value) {
        if (value instanceof String) return "string";
        if (value instanceof Boolean) return "boolean";
        if (value instanceof Integer || value instanceof Long) return "integer";
        if (value instanceof Number) return "number";
        if (value instanceof List) return "array";
        if (value instanceof Map) return "object";
        return value.getClass().getSimpleName();
    }

    /** 错误文案中的值截断（防长文本刷屏） */
    private static String truncate(Object value) {
        String s = String.valueOf(value);
        return s.length() > 50 ? "\"" + s.substring(0, 50) + "...\"" : "\"" + s + "\"";
    }
}
