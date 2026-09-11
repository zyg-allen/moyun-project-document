package com.moyun.ext.ai.engine.tool;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 工具参数 JSON Schema 校验器单元测试（v11.63 P1-2）
 *
 * <p>覆盖：跳过分支（空/非法/非 object schema——fail-open）、必填缺失、
 * 类型不匹配（含错误文案自纠指令断言）、integer 宽容（整值 Double/BigDecimal）、
 * enum/minimum/maximum/items 数组元素、全合规通过、未知类型跳过。</p>
 *
 * <p>Schema 样例取自 ai_agent_tool 种子数据真实形态（weather_query/calculator）。</p>
 */
class ToolParamValidatorTest {

    /** 天气工具 schema（种子数据 id=3 原文） */
    private static final String WEATHER_SCHEMA = """
            {"type": "object", "required": ["city"], "properties": {
              "city": {"type": "string", "description": "城市名称"},
              "days": {"type": "integer", "default": 1, "description": "预报天数1-7"}}}
            """;

    // ==================== 跳过分支（fail-open） ====================

    @Test
    void validate_nullOrBlankSchema_shouldSkip() {
        assertTrue(ToolParamValidator.validate(null, Map.of("city", 123)).isEmpty());
        assertTrue(ToolParamValidator.validate("  ", Map.of()).isEmpty());
    }

    @Test
    void validate_illegalJsonSchema_shouldSkip() {
        // 文本描述型存量数据（非 JSON）→ 跳过不误杀
        assertTrue(ToolParamValidator.validate("city: 城市名，days: 天数", Map.of("city", 123)).isEmpty());
    }

    @Test
    void validate_nonObjectRootSchema_shouldSkip() {
        assertTrue(ToolParamValidator.validate("{\"type\": \"array\"}", Map.of()).isEmpty());
        assertTrue(ToolParamValidator.validate("{\"required\": [\"city\"]}", Map.of()).isEmpty());
    }

    @Test
    void validate_nullParams_requiredMissingStillReported() {
        List<String> errors = ToolParamValidator.validate(WEATHER_SCHEMA, null);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("city"));
    }

    // ==================== 必填与类型 ====================

    @Test
    void validate_missingRequired_shouldReport() {
        List<String> errors = ToolParamValidator.validate(WEATHER_SCHEMA, Map.of("days", 3));
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("缺少必填参数"));
        assertTrue(errors.get(0).contains("city"));
    }

    @Test
    void validate_wrongType_shouldReportWithSelfCorrectionHint() {
        // days 传字符串（LLM 常见错误）→ 拒绝且文案含期望类型与实际类型
        List<String> errors = ToolParamValidator.validate(WEATHER_SCHEMA,
                Map.of("city", "北京", "days", "3"));
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("days"));
        assertTrue(errors.get(0).contains("integer"));
        assertTrue(errors.get(0).contains("string"));
    }

    @Test
    void validate_stringPassedAsNumber_shouldReport() {
        List<String> errors = ToolParamValidator.validate(WEATHER_SCHEMA,
                Map.of("city", 123));
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("city"));
        assertTrue(errors.get(0).contains("string"));
    }

    @Test
    void validate_validParams_shouldPass() {
        assertTrue(ToolParamValidator.validate(WEATHER_SCHEMA,
                Map.of("city", "北京", "days", 3)).isEmpty());
        // 可选参数缺省合法
        assertTrue(ToolParamValidator.validate(WEATHER_SCHEMA, Map.of("city", "北京")).isEmpty());
    }

    @Test
    void validate_integerLeniency_doubleWithWholeValueShouldPass() {
        // LLM JSON 常产出 3.0 形态，整值 Double 视为 integer
        assertTrue(ToolParamValidator.validate(WEATHER_SCHEMA,
                Map.of("city", "北京", "days", 3.0)).isEmpty());
        assertTrue(ToolParamValidator.validate(WEATHER_SCHEMA,
                Map.of("city", "北京", "days", 3L)).isEmpty());
        // 非整值浮点仍拒绝
        List<String> errors = ToolParamValidator.validate(WEATHER_SCHEMA,
                Map.of("city", "北京", "days", 3.5));
        assertEquals(1, errors.size());
    }

    // ==================== enum / 范围 / 数组 ====================

    @Test
    void validate_enumMismatch_shouldReport() {
        String schema = """
                {"type": "object", "required": ["lang"], "properties": {
                  "lang": {"type": "string", "enum": ["zh", "en", "ja"]}}}
                """;
        assertTrue(ToolParamValidator.validate(schema, Map.of("lang", "zh")).isEmpty());
        List<String> errors = ToolParamValidator.validate(schema, Map.of("lang", "fr"));
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("fr"));
        assertTrue(errors.get(0).contains("zh"));
    }

    @Test
    void validate_minMaxRange_shouldReport() {
        String schema = """
                {"type": "object", "properties": {
                  "days": {"type": "integer", "minimum": 1, "maximum": 7}}}
                """;
        assertTrue(ToolParamValidator.validate(schema, Map.of("days", 4)).isEmpty());
        assertTrue(ToolParamValidator.validate(schema, Map.of()).isEmpty());
        assertEquals(1, ToolParamValidator.validate(schema, Map.of("days", 0)).size());
        assertEquals(1, ToolParamValidator.validate(schema, Map.of("days", 8)).size());
    }

    @Test
    void validate_arrayItemType_shouldReport() {
        String schema = """
                {"type": "object", "properties": {
                  "keywords": {"type": "array", "items": {"type": "string"}}}}
                """;
        assertTrue(ToolParamValidator.validate(schema,
                Map.of("keywords", List.of("java", "spring"))).isEmpty());
        List<String> errors = ToolParamValidator.validate(schema,
                Map.of("keywords", List.of("java", 123)));
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("数组元素"));
    }

    // ==================== 兼容与边界 ====================

    @Test
    void validate_unknownPropertyType_shouldSkip() {
        // 未知 type（如 null/自定义）跳过该项，不误杀
        String schema = """
                {"type": "object", "properties": {
                  "anything": {"type": null},
                  "count": {"type": "integer"}}}
                """;
        assertTrue(ToolParamValidator.validate(schema,
                Map.of("anything", "任意值", "count", 5)).isEmpty());
    }

    @Test
    void validate_extraParamsNotInSchema_shouldPass() {
        // Schema 未声明的多余参数不报错（additionalProperties 默认宽容，由执行器忽略）
        assertTrue(ToolParamValidator.validate(WEATHER_SCHEMA,
                Map.of("city", "北京", "extra", "多余参数")).isEmpty());
    }

    @Test
    void validate_booleanType_shouldWork() {
        String schema = """
                {"type": "object", "required": ["need_analysis"], "properties": {
                  "need_analysis": {"type": "boolean"}}}
                """;
        assertTrue(ToolParamValidator.validate(schema, Map.of("need_analysis", true)).isEmpty());
        assertEquals(1, ToolParamValidator.validate(schema, Map.of("need_analysis", "true")).size());
    }

    @Test
    void validate_multipleErrors_allReported() {
        // 必填缺失 + 类型错误同时报
        List<String> errors = ToolParamValidator.validate(WEATHER_SCHEMA,
                Map.of("days", "abc"));
        assertEquals(2, errors.size());
    }
}
