package com.moyun.util.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * LLM JSON 提取：全项目唯一实现的能力并集（v13.19 收敛自 4 处同族实现）
 *
 * <p>收敛前各实现的差异（本测试逐条锁定）：</p>
 * <ul>
 *   <li>死代码版：只处理"文本以围栏开头"（围栏在第二行就失效），且不支持数组；</li>
 *   <li>{@code AbstractAiSceneHandler} 版：围栏任意位置 + 对象/数组谁先取谁，但用
 *       {@code lastIndexOf} 切尾（内容里含 {@code }} 会切错）；</li>
 *   <li>{@code VoiceInterviewServiceImpl} 版：只有括号切片、无围栏处理；</li>
 *   <li>{@code WorkflowGeneratorServiceImpl} 版：什么都不匹配时返回 null。</li>
 * </ul>
 *
 * @author moyun
 */
class LlmJsonExtractorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    @DisplayName("纯 JSON / 夹说明文字 / 围栏（任意位置）都能取到本体")
    void extractCommonShapes() {
        assertEquals("{\"a\":1}", LlmJsonExtractor.extract("{\"a\":1}"));
        assertEquals("{\"a\":1}", LlmJsonExtractor.extract("好的，结果如下：{\"a\":1}\n希望对你有帮助"));
        assertEquals("{\"a\":1}", LlmJsonExtractor.extract("```json\n{\"a\":1}\n```"));
        assertEquals("{\"a\":1}", LlmJsonExtractor.extract("```\n{\"a\":1}\n```"));
        assertEquals("{\"a\":1}", LlmJsonExtractor.extract("说明在前\n```json\n{\"a\":1}\n```\n说明在后"),
                "围栏出现在第二行也必须能剥（死代码版此处失效）");
    }

    @Test
    @DisplayName("数组本体 / 对象与数组混排：取先出现的那个")
    void extractArrayAndMixedShapes() {
        assertEquals("[\"Java\",\"MySQL\"]", LlmJsonExtractor.extract("```json\n[\"Java\",\"MySQL\"]\n```"));
        assertEquals("[\"a\"]", LlmJsonExtractor.extract("[\"a\"]"));
        assertEquals("{\"a\":1}", LlmJsonExtractor.extract("先给对象 {\"a\":1} 再给数组 [1,2]"));
        assertEquals("[1,2]", LlmJsonExtractor.extract("先给数组 [1,2] 再给对象 {\"a\":1}"));
    }

    @Test
    @DisplayName("括号配平：字符串里的花括号/转义不破坏截取（lastIndexOf 版会切错）")
    void balancedScanIgnoresBracesInsideStrings() {
        String raw = "{\"tpl\":\"为 {name} 庆祝\",\"brace\":\"}{\"}";
        assertEquals(raw, LlmJsonExtractor.extract(raw), "字符串内的 { } 不得影响配平");

        String withTrailingJunk = "{\"a\":\"}\"} 后面还有说明 {不是JSON}";
        assertEquals("{\"a\":\"}\"}", LlmJsonExtractor.extract(withTrailingJunk),
                "首个配平对象之后的内容必须被丢弃");

        String nested = "{\"a\":{\"b\":[1,{\"c\":2}]}}";
        assertEquals(nested, LlmJsonExtractor.extract("前缀 " + nested + " 后缀"));
    }

    @Test
    @DisplayName("不闭合（被截断）返回空串；完全没有 JSON 也返回空串（永不返回 null）")
    void fallbackAndEmpty() {
        assertEquals("", LlmJsonExtractor.extract("{\"a\":1"),
                "括号未配平（max_tokens 截断）时返回空串：半截 JSON 无法解析，交由调用方走降级分支");
        assertEquals("{\"a\":1}", LlmJsonExtractor.extract("前缀 {\"a\":1} 后缀"),
                "有成对括号时正常截取");
        assertEquals("", LlmJsonExtractor.extract("完全没有 JSON 的一段话"));
        assertEquals("", LlmJsonExtractor.extract(""));
        assertEquals("", LlmJsonExtractor.extract("   "));
        assertEquals("", LlmJsonExtractor.extract(null), "null 输入返回空串，调用方无需判空");
    }

    @Test
    @DisplayName("extractNode：解析成功返回节点，失败返回 null 且不抛异常")
    void extractNodeParsesOrReturnsNull() {
        JsonNode node = LlmJsonExtractor.extractNode(MAPPER, "```json\n{\"a\":1,\"b\":[2]}\n```");
        assertNotNull(node);
        assertEquals(1, node.path("a").asInt());
        assertEquals(2, node.path("b").get(0).asInt());

        JsonNode arr = LlmJsonExtractor.extractNode(MAPPER, "结果：[\"x\",\"y\"]");
        assertNotNull(arr);
        assertTrue(arr.isArray());
        assertEquals("x", arr.get(0).asText());

        assertNull(LlmJsonExtractor.extractNode(MAPPER, "不是 JSON"));
        assertNull(LlmJsonExtractor.extractNode(MAPPER, ""));
        assertNull(LlmJsonExtractor.extractNode(MAPPER, null));
        assertNull(LlmJsonExtractor.extractNode(MAPPER, "{不是合法JSON}"), "非法 JSON 不得抛异常");
    }

    @Test
    @DisplayName("stripCodeFence：只去围栏，不对散文做 JSON 抠取")
    void stripCodeFenceKeepsProse() {
        assertEquals("你好，这是一段文案。", LlmJsonExtractor.stripCodeFence("```\n你好，这是一段文案。\n```"));
        assertEquals("你好，这是一段文案。", LlmJsonExtractor.stripCodeFence("```markdown\n你好，这是一段文案。\n```"));
        assertEquals("纯文本无围栏", LlmJsonExtractor.stripCodeFence("  纯文本无围栏  "));
        assertEquals("", LlmJsonExtractor.stripCodeFence(null));
        // 与 extract 的区别：散文里的 { } 不应被当成 JSON
        assertEquals("含 {a} 的散文", LlmJsonExtractor.stripCodeFence("含 {a} 的散文"));
        assertEquals("{a}", LlmJsonExtractor.extract("含 {a} 的散文"), "extract 会切出括号片段——所以散文场景必须用 stripCodeFence");
    }
}
