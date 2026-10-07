package com.moyun.ext.aigateway.service;

import com.moyun.ext.ai.entity.AiSceneConfig;
import com.moyun.ext.aigateway.model.ConversationStreamCommand;
import com.moyun.ext.aigateway.registry.AiSceneRegistry;
import com.moyun.ext.aigateway.support.AgentModelRouter;
import com.moyun.ext.aigateway.support.AgentPlanExecutor;
import com.moyun.ext.aigateway.support.AiExecuteLogService;
import com.moyun.ext.aigateway.support.AiOutputFilter;
import com.moyun.ext.aigateway.support.ContextManager;
import com.moyun.ext.aigateway.support.FallbackStrategy;
import com.moyun.ext.aigateway.support.IntentClassifier;
import com.moyun.ext.aigateway.support.SceneRateLimiter;
import com.moyun.ext.aigateway.support.SemanticCache;
import com.moyun.ext.aigateway.support.TokenCostGuard;
import com.moyun.ext.aigateway.support.TokenMeter;
import com.moyun.ext.ai.service.impl.AiSceneConfigVersionService;
import com.moyun.ext.ai.entity.Agent;
import com.moyun.ext.ai.mapper.AgentMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 会话流式（语音面试主干）Token 计量的**接线**回归测试
 *
 * <h3>被验证的缺陷（P1 · 成本可观测性与熔断）</h3>
 * <p>langchain4j {@code 1.0.0-beta3} 的流式模型既不下发 {@code stream_options.include_usage}，
 * builder 也无该选项（已用 {@code javap} + class 常量池检索证实）→ OpenAI 兼容端点的流式回调里
 * {@code ChatResponse.tokenUsage()} 恒为 {@code null}。
 * 而原实现只在 {@code tokenUsage != null} 时才 {@code tokenCostGuard.consume(...)}：</p>
 * <ul>
 *   <li>流式请求 Token <b>全部漏计</b> → 场景日配额（成本熔断）被绕过；</li>
 *   <li>{@code ai_execute_log} 里流式场景 {@code token_used} 恒为 0 → 成本看板失真。</li>
 * </ul>
 *
 * <p>本测试构造一个"**服务端不回 usage** 的流式模型"（{@code ChatResponse} 不含 tokenUsage），
 * 断言网关仍然调用了 {@code tokenCostGuard.consume(scene, >0)} —— 即估算兜底真的生效。
 * {@code TokenMeter} 用**真实实现**（jtokkit 分词），不 mock，以证明估算链路可用。</p>
 *
 * @author moyun
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiGatewayStreamTokenAccountingTest {

    private static final String SCENE = "voice_interview";

    @Mock private AiSceneRegistry registry;
    @Mock private IntentClassifier intentClassifier;
    @Mock private SemanticCache semanticCache;
    @Mock private SceneRateLimiter rateLimiter;
    @Mock private FallbackStrategy fallbackStrategy;
    @Mock private AiExecuteLogService executeLogService;
    @Mock private TokenCostGuard tokenCostGuard;
    @Mock private AiOutputFilter outputFilter;
    @Mock private AgentMapper agentMapper;
    @Mock private ContextManager contextManager;
    @Mock private AgentModelRouter agentModelRouter;
    @Mock private AiSceneConfigVersionService sceneConfigVersionService;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private AgentPlanExecutor agentPlanExecutor;
    @Mock private WorkflowSceneExecutor workflowSceneExecutor;
    @Mock private StreamingChatLanguageModel streamingModel;

    private AiGatewayService gateway;

    @BeforeEach
    void setUp() {
        // 构造参数顺序须与 @RequiredArgsConstructor 的字段声明顺序一致
        // （v14.72 末两位为三通道分派的规划/工作流执行器，会话流式路径不触及，mock 占位）
        gateway = new AiGatewayService(registry, intentClassifier, semanticCache, rateLimiter,
                fallbackStrategy, executeLogService, tokenCostGuard, new TokenMeter(), outputFilter,
                agentMapper, contextManager, agentModelRouter, sceneConfigVersionService, redisTemplate,
                agentPlanExecutor, workflowSceneExecutor);

        AiSceneConfig config = new AiSceneConfig();
        config.setSceneCode(SCENE);
        config.setEnabled(true);
        when(registry.getConfig(SCENE)).thenReturn(config);
        when(rateLimiter.tryAcquire(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(new SceneRateLimiter.RateResult(true, 100, 0, 60));
        when(tokenCostGuard.checkQuota(anyString(), any())).thenReturn(new TokenCostGuard.QuotaResult(true, 100, 0L));
    }

    @Test
    @DisplayName("服务端未回传 usage 的流式响应：仍必须按本地估算累计 Token（否则流式绕过日配额）")
    void conversationStream_withoutProviderUsage_stillConsumesTokens() {
        Agent agent = new Agent();
        agent.setId(9L);
        agent.setEnabled(true);
        when(agentMapper.selectById(9L)).thenReturn(agent);
        when(agentModelRouter.createStreamingModel(agent)).thenReturn(streamingModel);
        when(contextManager.buildTurnMessages(any(), any(), any(), any()))
                .thenReturn(List.of(UserMessage.from("你好，请用一句话介绍你自己，并说明你最擅长的技术方向。")));

        // 关键：ChatResponse 不带 tokenUsage（模拟 langchain4j 流式路径的真实情况）
        doAnswer(inv -> {
            StreamingChatResponseHandler handler = inv.getArgument(1);
            handler.onPartialResponse("我是本次的面试官，");
            handler.onPartialResponse("我们先从你的项目经历聊起。");
            handler.onCompleteResponse(ChatResponse.builder()
                    .aiMessage(AiMessage.from("我是本次的面试官，我们先从你的项目经历聊起。"))
                    .build());
            return null;
        }).when(streamingModel).chat(anyList(), any(StreamingChatResponseHandler.class));

        gateway.executeConversationStream(cmd(), t -> { }, full -> { }, err -> { });

        ArgumentCaptor<Integer> captured = ArgumentCaptor.forClass(Integer.class);
        verify(tokenCostGuard).consume(eq(SCENE), captured.capture());
        assertTrue(captured.getValue() != null && captured.getValue() > 0,
                "服务端未回传 usage 时必须用本地分词估算并累计，实际=" + captured.getValue());
    }

    private static ConversationStreamCommand cmd() {
        ConversationStreamCommand cmd = new ConversationStreamCommand();
        cmd.setSceneCode(SCENE);
        cmd.setUserId(1L);
        cmd.setAgentId(9L);
        cmd.setUserInput("你好");
        // sessionId 留空：跳过 Redis 版本锁分支，专注 Token 计量
        return cmd;
    }
}
