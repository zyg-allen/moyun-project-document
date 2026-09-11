package com.moyun.ext.ai2.support;

import com.moyun.ext.ai.entity.AiSceneConfig;
import com.moyun.ext.ai2.constant.AiErrorCodes;
import com.moyun.ext.ai2.model.AiExecuteResponse;
import com.moyun.system.domain.entity.SysSensitiveWord;
import com.moyun.system.filter.SensitiveWordFilter;
import com.moyun.system.mapper.SysSensitiveWordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * AI 输出内容过滤单元测试（v11.62 P1-3）
 *
 * <p>覆盖：场景开关判定、未命中零侵入（原引用返回）、Map/类型化对象/嵌套结构脱敏
 * 与类型保持（业务侧 instanceof 强转不受影响）、非成功响应跳过、DFA 异常放行原文。</p>
 *
 * <p>词库经 mock mapper 真实加载（真实 DFA 集成验证，非 mock find/mask 行为）。</p>
 */
class AiOutputFilterTest {

    private AiOutputFilter filter;

    @BeforeEach
    void setUp() {
        SysSensitiveWordMapper mapper = Mockito.mock(SysSensitiveWordMapper.class);
        SysSensitiveWord word = new SysSensitiveWord();
        word.setWord("炸弹");
        word.setStatus("0");
        when(mapper.selectList(any())).thenReturn(List.of(word));
        SensitiveWordFilter wordFilter = new SensitiveWordFilter();
        ReflectionTestUtils.setField(wordFilter, "sensitiveWordMapper", mapper);
        wordFilter.reload();
        filter = new AiOutputFilter(wordFilter);
    }

    // ==================== isEnabled：场景开关 ====================

    @Test
    void isEnabled_nullOrUnset_shouldReturnFalse() {
        assertFalse(filter.isEnabled(null));
        AiSceneConfig config = new AiSceneConfig();
        assertFalse(filter.isEnabled(config));
        config.setEnableOutputFilter(Boolean.FALSE);
        assertFalse(filter.isEnabled(config));
    }

    @Test
    void isEnabled_enabled_shouldReturnTrue() {
        AiSceneConfig config = new AiSceneConfig();
        config.setEnableOutputFilter(Boolean.TRUE);
        assertTrue(filter.isEnabled(config));
    }

    // ==================== applyFilter：跳过分支 ====================

    @Test
    void applyFilter_nullResponseOrNullData_shouldSkip() {
        assertTrue(filter.applyFilter(null).isEmpty());

        AiExecuteResponse<Object> resp = AiExecuteResponse.success(null);
        assertTrue(filter.applyFilter(resp).isEmpty());
    }

    @Test
    void applyFilter_failureCode_shouldSkip() {
        AiExecuteResponse<Object> resp = AiExecuteResponse.failure(9999, "error");
        resp.setData(Map.of("text", "炸弹内容"));
        assertTrue(filter.applyFilter(resp).isEmpty());
        assertEquals("炸弹内容", ((Map<?, ?>) resp.getData()).get("text"));
    }

    // ==================== applyFilter：未命中（零侵入） ====================

    @Test
    void applyFilter_noHit_shouldKeepOriginalReference() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("summary", "正常的财务分析内容");
        data.put("serial", "12345");
        AiExecuteResponse<Map<String, Object>> resp = AiExecuteResponse.success(data);
        resp.setSceneCode("finance_analysis");

        assertTrue(filter.applyFilter(resp).isEmpty());
        assertSame(data, resp.getData(), "未命中时应原引用返回，不做 Jackson 往返重建");
    }

    // ==================== applyFilter：命中脱敏（Map，缓存命中同型） ====================

    @Test
    void applyFilter_hitOnMap_shouldMaskTextOnly() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("summary", "这是一个炸弹计划");
        data.put("score", 88);
        data.put("amount", new BigDecimal("123.45"));
        AiExecuteResponse<Map<String, Object>> resp = AiExecuteResponse.success(data);
        resp.setSceneCode("finance_analysis");

        List<String> hits = filter.applyFilter(resp);

        assertEquals(List.of("炸弹"), hits);
        Map<?, ?> masked = (Map<?, ?>) resp.getData();
        String summary = (String) masked.get("summary");
        assertFalse(summary.contains("炸") || summary.contains("弹"), "敏感词应被替换");
        assertTrue(summary.contains("*"));
        assertEquals(88, masked.get("score"), "数值字段不应受影响");
        assertEquals(new BigDecimal("123.45"), masked.get("amount"), "金额字段不应受影响");
    }

    // ==================== applyFilter：命中脱敏（类型化对象，类型保持） ====================

    @Test
    void applyFilter_hitOnTypedData_shouldPreserveType() {
        SampleReport data = new SampleReport();
        data.setSummary("面试者谈到炸弹威胁");
        data.setScore(72);
        AiExecuteResponse<SampleReport> resp = AiExecuteResponse.success(data);
        resp.setSceneCode("voice_interview");

        List<String> hits = filter.applyFilter(resp);

        assertEquals(List.of("炸弹"), hits);
        SampleReport masked = assertInstanceOf(SampleReport.class, resp.getData(),
                "业务侧存在 instanceof 强转，data 类型必须保持");
        assertFalse(masked.getSummary().contains("炸弹"));
        assertTrue(masked.getSummary().contains("*"));
        assertEquals(72, masked.getScore());
    }

    // ==================== applyFilter：嵌套结构（List<Map<List<String>>>） ====================

    @Test
    void applyFilter_hitOnNestedStructure_shouldMaskRecursively() {
        List<Object> inner = new ArrayList<>(List.of("第一点含炸弹", "第二点正常"));
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("points", inner);
        row.put("title", "评估结论");
        List<Object> data = new ArrayList<>(List.of(row, "尾部炸弹"));
        AiExecuteResponse<List<Object>> resp = AiExecuteResponse.success(data);
        resp.setSceneCode("resume_optimize");

        List<String> hits = filter.applyFilter(resp);

        assertEquals(List.of("炸弹"), hits);
        List<?> maskedList = (List<?>) resp.getData();
        Map<?, ?> maskedRow = (Map<?, ?>) maskedList.get(0);
        List<?> maskedInner = (List<?>) maskedRow.get("points");
        String point1 = (String) maskedInner.get(0);
        String point2 = (String) maskedInner.get(1);
        assertFalse(point1.contains("炸弹"));
        assertTrue(point1.contains("*"));
        assertEquals("第二点正常", point2, "未命中文本应原样保留");
        assertFalse(((String) maskedList.get(1)).contains("炸弹"));
    }

    @Test
    void applyFilter_hitWithWhitespaceBypass_shouldStillMask() {
        // DFA 跳空白匹配："炸 弹"（插入空白绕过）仍应命中并脱敏
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("text", "提到炸 弹两个字");
        AiExecuteResponse<Map<String, Object>> resp = AiExecuteResponse.success(data);

        List<String> hits = filter.applyFilter(resp);

        assertEquals(List.of("炸弹"), hits);
        String masked = (String) ((Map<?, ?>) resp.getData()).get("text");
        assertFalse(masked.contains("炸"));
        assertFalse(masked.contains("弹"));
    }

    // ==================== applyFilter：异常放行 ====================

    @Test
    void applyFilter_dfaError_shouldPassThroughOriginal() {
        SensitiveWordFilter throwing = Mockito.mock(SensitiveWordFilter.class);
        when(throwing.find(any())).thenThrow(new RuntimeException("dfa boom"));
        AiOutputFilter failing = new AiOutputFilter(throwing);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("text", "包含炸弹的内容");
        AiExecuteResponse<Map<String, Object>> resp = AiExecuteResponse.success(data);

        assertTrue(failing.applyFilter(resp).isEmpty(), "异常放行返回空命中列表");
        assertSame(data, resp.getData(), "异常时应放行原文，不重建不替换");
        assertEquals("包含炸弹的内容", ((Map<?, ?>) resp.getData()).get("text"));
    }

    @Test
    void applyFilter_unserializableData_shouldPassThroughOriginal() {
        // Jackson 无法序列化的 data（无 getter 的私有字段对象）→ 放行原文不抛出
        AiExecuteResponse<OpaqueData> resp = AiExecuteResponse.success(new OpaqueData());
        assertTrue(filter.applyFilter(resp).isEmpty());
        assertNotNull(resp.getData());
    }

    /** 类型化场景数据样例（模拟 TopicSceneData 等业务消费方 instanceof 目标） */
    static class SampleReport {
        private String summary;
        private Integer score;

        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        public Integer getScore() { return score; }
        public void setScore(Integer score) { this.score = score; }
    }

    /** 无 getter/公有字段的不可序列化对象（Jackson valueToTree 抛 InvalidDefinitionException → 走异常放行分支） */
    static class OpaqueData {
        private final String value = "炸弹";
    }
}
