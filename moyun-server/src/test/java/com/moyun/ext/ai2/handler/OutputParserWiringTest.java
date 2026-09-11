package com.moyun.ext.ai2.handler;

import com.moyun.ext.ai.entity.AiSceneConfig;
import com.moyun.ext.ai2.handler.impl.DailyTopicHandler;
import com.moyun.ext.ai2.model.AiExecuteRequest;
import com.moyun.ext.ai2.model.AiExecuteResponse;
import com.moyun.ext.ai2.model.ChatOutcome;
import com.moyun.ext.ai2.model.data.TopicSceneData;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * output_parser 配置接线单元测试（v11.66 P1-5）
 *
 * <p>覆盖：parseOutput 分派分支（null config 默认 json / json / markdown / text 原文包装 /
 * 未知值容错）+ DailyTopicHandler 双参升级后的配置驱动回归（默认 json 行为不变、
 * 改 parser=markdown 后配置真实生效——此前配置可编辑零消费）。</p>
 */
class OutputParserWiringTest {

    /** 暴露 protected parseOutput 的测试桩 */
    private static class StubHandler extends AbstractAiSceneHandler {
        @Override
        public String getSceneCode() {
            return "stub";
        }

        Map<String, Object> callParseOutput(String raw, AiSceneConfig config) {
            return parseOutput(raw, config);
        }
    }

    private final StubHandler stub = new StubHandler();

    private AiSceneConfig configWithParser(String parser) {
        AiSceneConfig config = new AiSceneConfig();
        config.setOutputParser(parser);
        return config;
    }

    // ==================== parseOutput 分派分支 ====================

    @Test
    void parseOutput_nullConfig_defaultsToJson() {
        Map<String, Object> parsed = stub.callParseOutput(
                "```json\n{\"title\":\"T\",\"category\":\"tech\"}\n```", null);
        assertEquals("T", parsed.get("title"));
        assertEquals("tech", parsed.get("category"));
    }

    @Test
    void parseOutput_blankParser_defaultsToJson() {
        Map<String, Object> parsed = stub.callParseOutput(
                "前缀杂文本 {\"score\": 88} 后缀", configWithParser(" "));
        assertEquals(88, parsed.get("score"), "围栏/杂文本容错提取不变");
    }

    @Test
    void parseOutput_json_explicit_behavesSame() {
        Map<String, Object> parsed = stub.callParseOutput(
                "{\"a\": 1}", configWithParser("json"));
        assertEquals(1, parsed.get("a"));
    }

    @Test
    void parseOutput_markdown_wrapsCleanedContent() {
        Map<String, Object> parsed = stub.callParseOutput(
                "# 标题\n\n这是自由文本回答。", configWithParser("markdown"));
        assertEquals("# 标题\n\n这是自由文本回答。", parsed.get("content"),
                "原文清洗后包装为 content 键");
        assertEquals(1, parsed.size());
    }

    @Test
    void parseOutput_text_alias_sameAsMarkdown() {
        Map<String, Object> parsed = stub.callParseOutput(
                "纯文本综述内容", configWithParser("text"));
        assertEquals("纯文本综述内容", parsed.get("content"));
    }

    @Test
    void parseOutput_unknownParser_fallsBackToJson() {
        Map<String, Object> parsed = stub.callParseOutput(
                "{\"x\": true}", configWithParser("xml"));
        assertEquals(Boolean.TRUE, parsed.get("x"), "未知值 WARN 留痕 + json 容错");
    }

    // ==================== DailyTopicHandler 双参配置驱动回归 ====================

    /** 覆写 chatDetailed 打桩 LLM（chat → chatDetailed） */
    private DailyTopicHandler topicHandlerWithLlm(String llmRaw) {
        return new DailyTopicHandler() {
            @Override
            protected ChatOutcome chatDetailed(String sceneCode, String systemPrompt, String userPrompt) {
                ChatOutcome outcome = new ChatOutcome();
                outcome.setText(llmRaw);
                return outcome;
            }
        };
    }

    @Test
    void dailyTopic_defaultJson_behaviorUnchanged() {
        DailyTopicHandler handler = topicHandlerWithLlm(
                "{\"title\":\"架构话题\",\"description\":\"描述\",\"category\":\"技术\"}");
        AiExecuteRequest request = new AiExecuteRequest();
        request.setSceneCode("daily_topic");

        AiExecuteResponse<?> resp = handler.execute(request, configWithParser("json"));

        TopicSceneData data = assertInstanceOf(TopicSceneData.class, resp.getData());
        assertEquals("架构话题", data.getTitle(), "存量 json 配置行为零变化");
    }

    @Test
    void dailyTopic_markdownParser_configTakesEffect() {
        // 此前改 parser 无效果（零消费）；接线后 markdown 输出包装为 content，类型化字段为空
        DailyTopicHandler handler = topicHandlerWithLlm("这是自由格式的运营文案");
        AiExecuteRequest request = new AiExecuteRequest();
        request.setSceneCode("daily_topic");

        AiExecuteResponse<?> resp = handler.execute(request, configWithParser("markdown"));

        assertEquals(com.moyun.ext.ai2.constant.AiErrorCodes.SUCCESS, resp.getCode());
        TopicSceneData data = assertInstanceOf(TopicSceneData.class, resp.getData());
        assertNull(data.getTitle(), "markdown 解析无 title 字段——配置真实生效（消费方读 structured/content）");
    }

    @Test
    void dailyTopic_nullConfig_forwardedFromBase_stillJson() {
        DailyTopicHandler handler = topicHandlerWithLlm("{\"title\":\"T\"}");
        AiExecuteRequest request = new AiExecuteRequest();
        request.setSceneCode("daily_topic");

        AiExecuteResponse<?> resp = handler.execute(request, null);

        TopicSceneData data = assertInstanceOf(TopicSceneData.class, resp.getData());
        assertEquals("T", data.getTitle(), "config=null 时 parseOutput 内部默认 json");
    }
}
