package com.moyun.ext.aigateway.support;

import com.moyun.ext.ai.entity.Agent;
import com.moyun.ext.ai.entity.AiSceneConfig;
import com.moyun.ext.ai.mapper.AgentMapper;
import com.moyun.ext.ai.service.AiSceneResolver;
import com.moyun.ext.ai.service.ToolCallingService;
import com.moyun.ext.aigateway.model.AiMetadata;
import com.moyun.ext.aigateway.model.ConversationStreamCommand;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 受限自主规划执行器 · 会话流式通道单元测试（v14.72 P2-2）
 *
 * <p>覆盖：READY 协议（工具轮输出不转发前端，终答流式增量下发 + 计量元数据）、
 * 模型不守约直接给出完整答案（一次性转发，不二次调用）两条主路径。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgentPlanExecutorStreamTest {

    private static final String SCENE = "default_chat";

    @Mock private AgentMapper agentMapper;
    @Mock private AiSceneResolver sceneResolver;
    @Mock private AgentModelRouter agentModelRouter;
    @Mock private ObjectProvider<ToolCallingService> toolCallingProvider;
    @Mock private ToolCallingService toolCalling;
    @Mock private ChatLanguageModel planModel;
    @Mock private StreamingChatLanguageModel streamingModel;

    private AgentPlanExecutor executor;

    private final AtomicReference<String> tokenSink = new AtomicReference<>("");

    @BeforeEach
    void setUp() {
        executor = new AgentPlanExecutor(agentMapper, sceneResolver, new TokenMeter(),
                agentModelRouter, toolCallingProvider);

        Agent agent = new Agent();
        agent.setId(5L);
        agent.setEnabled(true);
        agent.setName("规划助手");
        agent.setSystemPrompt("你是任务规划助手");
        when(agentMapper.selectById(5L)).thenReturn(agent);
        when(toolCallingProvider.getObject()).thenReturn(toolCalling);
        when(toolCalling.buildToolPrompt(5L)).thenReturn("");
        when(sceneResolver.resolveChatModel(SCENE)).thenReturn(planModel);
        when(agentModelRouter.createStreamingModel(agent)).thenReturn(streamingModel);
        tokenSink.set("");
    }

    private AiSceneConfig config() {
        AiSceneConfig config = new AiSceneConfig();
        config.setSceneCode(SCENE);
        config.setAgentId(5L);
        return config;
    }

    private ConversationStreamCommand cmd() {
        ConversationStreamCommand cmd = new ConversationStreamCommand();
        cmd.setSceneCode(SCENE);
        cmd.setUserId(1L);
        cmd.setUserInput("帮我查一下今天北京的天气并给出出行建议");
        return cmd;
    }

    @Test
    @DisplayName("READY 协议：工具轮不转发前端，终答流式增量下发，计量入元数据")
    void readyProtocol_toolRoundSuppressed_finalAnswerStreamed() {
        // 规划轮1：工具调用；规划轮2：READY（终答由系统另行安排）
        ChatResponse round1 = ChatResponse.builder()
                .aiMessage(AiMessage.from("[TOOL_CALL]{\"tool\":\"weather\",\"params\":{\"city\":\"北京\"}}[/TOOL_CALL]"))
                .build();
        ChatResponse round2 = ChatResponse.builder()
                .aiMessage(AiMessage.from("[READY]"))
                .build();
        when(planModel.chat(anyList())).thenReturn(round1, round2);

        ToolCallingService.ToolCallResult call = ToolCallingService.ToolCallResult.builder()
                .hasToolCall(true)
                .toolName("weather")
                .build();
        when(toolCalling.detectAndExecute(anyString(), any())).thenReturn(call, null);

        // 流式终答：两段增量 + 无 usage 完成回调（TokenMeter 本地估算兜底）
        doAnswer(inv -> {
            StreamingChatResponseHandler handler = inv.getArgument(1);
            handler.onPartialResponse("今天北京晴，");
            handler.onPartialResponse("适合出行。");
            handler.onCompleteResponse(ChatResponse.builder()
                    .aiMessage(AiMessage.from("今天北京晴，适合出行。"))
                    .build());
            return null;
        }).when(streamingModel).chat(anyList(), any(StreamingChatResponseHandler.class));

        AtomicReference<AiMetadata> metaRef = new AtomicReference<>();
        AtomicReference<String> fullRef = new AtomicReference<>();
        executor.executeConversationStream(cmd(), config(),
                List.of(UserMessage.from("帮我查一下今天北京的天气并给出出行建议")),
                t -> tokenSink.set(tokenSink.get() + t),
                (full, meta) -> {
                    fullRef.set(full);
                    metaRef.set(meta);
                },
                err -> { });

        // 终答完整下发（增量拼接）
        assertEquals("今天北京晴，适合出行。", fullRef.get());
        assertEquals("今天北京晴，适合出行。", tokenSink.get());
        // 工具调用标记与 READY 不泄露给前端
        assertFalse(tokenSink.get().contains("TOOL_CALL"));
        assertFalse(tokenSink.get().contains("[READY]"));

        AiMetadata metadata = metaRef.get();
        assertNotNull(metadata);
        assertEquals("规划助手", metadata.getAgentUsed());
        assertEquals(1, metadata.getToolCalls().size());
        assertEquals("weather", metadata.getToolCalls().get(0).getToolName());
        assertTrue(metadata.getTokenUsed() != null && metadata.getTokenUsed() > 0,
                "规划轮+终答 token 必须计量（否则绕过场景日配额）");
        assertTrue(Boolean.TRUE.equals(metadata.getTokenEstimated()));
        // 规划循环执行了 2 轮（工具轮 + READY 轮）
        assertEquals(2, metadata.getRetryCount());
        // 流式模型被调用一次（终答下发）
        verify(streamingModel).chat(anyList(), any(StreamingChatResponseHandler.class));
    }

    @Test
    @DisplayName("模型不守约直接给出完整答案：按终答接受一次性转发，不二次调用流式模型")
    void directAnswer_forwardedWithoutSecondCall() {
        ChatResponse round1 = ChatResponse.builder()
                .aiMessage(AiMessage.from("今天北京晴，适合出行，建议带伞备用。"))
                .build();
        when(planModel.chat(anyList())).thenReturn(round1);
        when(toolCalling.detectAndExecute(anyString(), any())).thenReturn(null);

        AtomicReference<String> fullRef = new AtomicReference<>();
        AtomicReference<AiMetadata> metaRef = new AtomicReference<>();
        executor.executeConversationStream(cmd(), config(),
                List.of(UserMessage.from("帮我查一下今天北京的天气并给出出行建议")),
                t -> tokenSink.set(tokenSink.get() + t),
                (full, meta) -> {
                    fullRef.set(full);
                    metaRef.set(meta);
                },
                err -> { });

        assertEquals("今天北京晴，适合出行，建议带伞备用。", fullRef.get());
        assertEquals("今天北京晴，适合出行，建议带伞备用。", tokenSink.get());
        assertNotNull(metaRef.get());
        assertTrue(metaRef.get().getTokenUsed() != null && metaRef.get().getTokenUsed() > 0);
        // 不发生终答二次调用（成本不翻倍）
        verify(streamingModel, org.mockito.Mockito.never())
                .chat(anyList(), any(StreamingChatResponseHandler.class));
    }

    @Test
    @DisplayName("智能体未启用：onError 下发，不进入规划循环")
    void disabledAgent_routesError() {
        Agent disabled = new Agent();
        disabled.setId(6L);
        disabled.setEnabled(false);
        when(agentMapper.selectById(6L)).thenReturn(disabled);
        AiSceneConfig cfg = config();
        cfg.setAgentId(6L);

        AtomicReference<Throwable> errRef = new AtomicReference<>();
        executor.executeConversationStream(cmd(), cfg, List.of(),
                t -> { }, (full, meta) -> { }, errRef::set);

        assertNotNull(errRef.get());
        assertTrue(errRef.get().getMessage().contains("智能体不存在或未启用"));
    }
}
