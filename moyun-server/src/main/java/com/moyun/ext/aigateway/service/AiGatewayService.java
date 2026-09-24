package com.moyun.ext.aigateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.ext.ai.constant.RedisKeys;
import com.moyun.ext.ai.entity.Agent;
import com.moyun.ext.ai.entity.AiSceneConfig;
import com.moyun.ext.ai.mapper.AgentMapper;
import com.moyun.ext.ai.service.impl.AiSceneConfigVersionService;
import com.moyun.ext.aigateway.constant.AiErrorCodes;
import com.moyun.ext.aigateway.handler.AiSceneHandler;
import com.moyun.ext.aigateway.model.AiExecuteRequest;
import com.moyun.ext.aigateway.model.AiExecuteResponse;
import com.moyun.ext.aigateway.model.AiMetadata;
import com.moyun.ext.aigateway.model.ConversationStreamCommand;
import com.moyun.ext.aigateway.registry.AiSceneRegistry;
import com.moyun.ext.aigateway.support.AgentModelRouter;
import com.moyun.ext.aigateway.support.AiExecuteLogService;
import com.moyun.ext.aigateway.support.AiOutputFilter;
import com.moyun.ext.aigateway.support.ContextManager;
import com.moyun.ext.aigateway.support.FallbackStrategy;
import com.moyun.ext.aigateway.support.IntentClassifier;
import com.moyun.ext.aigateway.support.PromptInjectionGuard;
import com.moyun.ext.aigateway.support.SceneRateLimiter;
import com.moyun.ext.aigateway.support.SemanticCache;
import com.moyun.ext.aigateway.support.TokenCostGuard;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AI统一网关编排服务
 *
 * <p>依据《AI能力统一接入层 — 完整方案文档》V2.0 §5.3。五层编排：
 * 意图判断 → 缓存检查 → 场景路由（限流/配置合并）→ Handler执行（异常降级）→ 响应输出（缓存回写/日志/指标）。</p>
 *
 * @author laomao
 * @since 2026-09-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiGatewayService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AiSceneRegistry registry;
    private final IntentClassifier intentClassifier;
    private final SemanticCache semanticCache;
    private final SceneRateLimiter rateLimiter;
    private final FallbackStrategy fallbackStrategy;
    private final AiExecuteLogService executeLogService;
    /** 场景日 Token 成本熔断（ai_scene_config.daily_token_limit） */
    private final TokenCostGuard tokenCostGuard;
    /** 输出内容过滤（ai_scene_config.enable_output_filter，复用 DFA 词树脱敏） */
    private final AiOutputFilter outputFilter;
    /** Agent 人设注入（ai_scene_config.agent_id → ai_agent.system_prompt） */
    private final AgentMapper agentMapper;
    /** 会话上下文管理（滑窗 + 摘要 + 瞬态指令装配） */
    private final ContextManager contextManager;
    /** per-agent 模型路由（agent 绑定链，含流式自动路由） */
    private final AgentModelRouter agentModelRouter;
    /** 场景配置版本服务（会话版本锁：进行中会话按锁定版本读快照） */
    private final AiSceneConfigVersionService sceneConfigVersionService;
    /** Redis（会话配置版本锁 chat:memory:session:{sessionId}） */
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 同步执行（统一入口核心编排）
     */
    public AiExecuteResponse<?> execute(AiExecuteRequest request) {
        request.setRequestId(UUID.randomUUID().toString().replace("-", ""));
        long startTime = System.currentTimeMillis();
        String sceneCode = request.getSceneCode();
        log.info("[aigateway:网关] 请求: scene={}, requestId={}", sceneCode, request.getRequestId());

        AiSceneConfig config = null;
        AiSceneHandler handler = null;
        try {
            // 1. 场景配置（未配置/未启用的场景不对外服务）
            config = registry.getConfig(sceneCode);
            if (config == null) {
                return failure(request, AiErrorCodes.SCENE_NOT_FOUND,
                        "场景未注册或未启用: " + sceneCode, 0, "scene_not_found");
            }
            handler = registry.getHandler(sceneCode);

            // 1.2 输出模式路由校验（output_mode 配置接线——此前配置可编辑零消费）。
            //     显式 stream-only 场景拒绝同步入口；both/null 放行（Handler 能力校验在流式侧兜底）
            if ("stream".equals(config.getOutputMode())) {
                return failure(request, AiErrorCodes.INVALID_REQUEST,
                        "场景 [" + sceneCode + "] 仅支持流式输出，请调用流式端点 /api/ai/execute/stream",
                        0, "stream_only_scene");
            }

            // 1.5 Agent 人设注入（ai_scene_config.agent_id 绑定智能体时，其 system_prompt
            //     渲染 {{占位符}} 后以 agentPersona 注入 input，Handler 构建系统提示词时统一前置。
            //     注入先于缓存键计算——人设变更自动不脏读缓存）
            String agentName = injectAgentPersona(request, config);

            // 1.6 Prompt 注入防护：指令通道（顶层 userInput）统一清洗+扫描。
            //     DANGEROUS（指令覆盖/提示词探取）直接拒绝；SUSPECT（角色扮演）放行由数据隔离兜底——
            //     业务存在合法角色扮演场景。input Map 的字符串值做字符级清洗（不拦截，防误杀数据）。
            sanitizeInputChannel(request);
            String userInput = request.getUserInput();
            if (userInput != null && !userInput.isBlank()) {
                PromptInjectionGuard.ScanResult guard = PromptInjectionGuard.scan(userInput);
                if (guard.isDangerous()) {
                    log.warn("[aigateway:网关] 注入防护拦截: scene={}, requestId={}, pattern={}",
                            sceneCode, request.getRequestId(), guard.getPattern());
                    executeLogService.record(request.getRequestId(), request.getUserId(), sceneCode,
                            handler.getClass().getSimpleName(), resolveBindType(config), null,
                            null, null, "fail", "prompt_injection_blocked",
                            System.currentTimeMillis() - startTime);
                    return failure(request, AiErrorCodes.INPUT_REJECTED,
                            "输入包含不允许的指令内容", System.currentTimeMillis() - startTime, "prompt_injection");
                }
            }

            // 2. 意图判断（消费顶层 userInput 字段——用户自由文本触发分类路由；
            //    结构化参数场景（如 finance_analysis 传 userId/range）不传 userInput，自然跳过。
            //    会话模式（sessionId 非空）跳过——会话已绑定场景，分类是多余且有误打断风险。
            //    当前主要预留对象：chat 收口进网关后，对话消息即 userInput，此分支成为场景路由器）
            if (userInput != null && !userInput.isBlank() && request.getSessionId() == null) {
                IntentClassifier.IntentResult intent = intentClassifier.classify(userInput, sceneCode);
                if (intent.getConfidence() < 0.6) {
                    AiExecuteResponse<Object> resp = AiExecuteResponse.clarification(
                            "未能理解您的意图，能否补充说明一下您想做什么？");
                    fillCommon(resp, request, System.currentTimeMillis() - startTime);
                    return resp;
                }
                if (intent.getSuggestedScene() != null
                        && registry.getConfig(intent.getSuggestedScene()) != null) {
                    request.setSceneCode(intent.getSuggestedScene());
                    sceneCode = request.getSceneCode();
                }
            }

            // 3. 缓存检查
            String inputKey = canonicalInputKey(request);
            String inputText = primaryInputText(request);
            if (semanticCache.isEnabled(Boolean.TRUE.equals(config.getEnableCache()) ? 1 : 0)) {
                AiExecuteResponse<Object> cached = semanticCache.get(sceneCode, inputKey, inputText);
                if (cached != null) {
                    // 输出过滤：命中路径同样过滤——兜底过滤功能上线前的存量旧缓存
                    if (outputFilter.isEnabled(config)) {
                        outputFilter.applyFilter(cached);
                    }
                    fillCommon(cached, request, System.currentTimeMillis() - startTime);
                    log.info("[aigateway:网关] 缓存命中: scene={}, requestId={}", sceneCode, request.getRequestId());
                    return cached;
                }
            }

            // 4. 限流（场景 × 用户）
            String identity = request.getUserId() != null
                    ? String.valueOf(request.getUserId()) : "anonymous";
            int limit = config.getRateLimitCount() != null ? config.getRateLimitCount() : 100;
            int window = config.getRateLimitTime() != null ? config.getRateLimitTime() : 60;
            SceneRateLimiter.RateResult rate = rateLimiter.tryAcquire(sceneCode, identity, limit, window);
            if (!rate.allowed()) {
                executeLogService.record(request.getRequestId(), request.getUserId(), sceneCode,
                    handler.getClass().getSimpleName(), resolveBindType(config), null,
                    inputKey, null, "fail", "rate_limited",
                    System.currentTimeMillis() - startTime);
                return failure(request, AiErrorCodes.RATE_LIMITED,
                        "请求过于频繁，请稍后再试", System.currentTimeMillis() - startTime, "rate_limited");
            }

            // 4.5 成本熔断：场景日 Token 累计超 daily_token_limit → 拒绝。
            //     场景级配额（全体用户共享），保护平台总成本；null/0=不限。
            TokenCostGuard.QuotaResult quota = tokenCostGuard.checkQuota(sceneCode, config.getDailyTokenLimit());
            if (!quota.allowed()) {
                executeLogService.record(request.getRequestId(), request.getUserId(), sceneCode,
                    handler.getClass().getSimpleName(), resolveBindType(config), null,
                    inputKey, null, "fail", "token_limit_exceeded",
                    System.currentTimeMillis() - startTime);
                log.warn("[aigateway:网关] Token配额熔断: scene={}, requestId={}, used={}/{}",
                        sceneCode, request.getRequestId(), quota.todayUsed(), quota.limit());
                return failure(request, AiErrorCodes.AI_TOKEN_LIMIT_EXCEEDED,
                        "当前场景今日AI额度已用完，请明天再试", System.currentTimeMillis() - startTime, "token_limit");
            }

            // 5. 参数校验
            handler.validate(request);

            // 6. 执行（配置随调用下发，Handler 提示词/输出结构读配置即时生效，无静态 ThreadLocal）
            AiExecuteResponse<?> response = handler.execute(request, config);

            // 6.5 输出内容过滤：场景开启 enable_output_filter 时，复用 DFA 词树
            //     对响应 data 的全部文本节点脱敏。位于缓存回写/执行日志之前——缓存与日志留痕的
            //     均为脱敏后内容（命中路径见步骤 3，兜底存量旧缓存）。
            if (outputFilter.isEnabled(config)) {
                outputFilter.applyFilter(response);
            }

            // 7. 填充通用字段 + 回写缓存 + 记录日志
            long elapsed = System.currentTimeMillis() - startTime;
            fillCommon(response, request, elapsed);
            fillAgentMetadata(response, agentName);
            // 按实际消耗累计场景日 Token（未回传 token 不计）
            if (response.getMetadata() != null && response.getMetadata().getTokenUsed() != null) {
                tokenCostGuard.consume(sceneCode, response.getMetadata().getTokenUsed());
            }
            if (semanticCache.isEnabled(Boolean.TRUE.equals(config.getEnableCache()) ? 1 : 0)
                    && response.getCode() != null && response.getCode() == AiErrorCodes.SUCCESS) {
                semanticCache.put(sceneCode, inputKey, inputText, response, config.getCacheTtl());
            }
            executeLogService.record(request.getRequestId(), request.getUserId(), sceneCode,
                    handler.getClass().getSimpleName(), resolveBindType(config), response.getMetadata(),
                    inputKey, summarizeOutput(response), "success", null, elapsed);
            log.info("[aigateway:网关] 成功: scene={}, requestId={}, elapsed={}ms",
                    sceneCode, request.getRequestId(), elapsed);
            return response;

        } catch (Exception e) {
            // 8. 降级兜底
            long elapsed = System.currentTimeMillis() - startTime;
            AiExecuteResponse<?> fallback = fallbackStrategy.executeFallback(sceneCode,
                    config != null ? config.getFallbackResponse() : null, e);
            fillCommon(fallback, request, elapsed);
            executeLogService.record(request.getRequestId(), request.getUserId(), sceneCode,
                    handler != null ? handler.getClass().getSimpleName() : null,
                    config != null ? resolveBindType(config) : null, null,
                    canonicalInputKey(request), null, "fail", e.getMessage(), elapsed);
            return fallback;
        }
    }

    /**
     * 流式执行（SSE）—— 内部创建 emitter
     */
    public SseEmitter executeStream(AiExecuteRequest request) {
        SseEmitter emitter = new SseEmitter(60_000L);
        executeStream(request, emitter);
        return emitter;
    }

    /**
     * 流式执行（SSE）—— 外部传入 emitter（适配 DiagramChatService 等已有 emitter 的场景）
     */
    public void executeStream(AiExecuteRequest request, SseEmitter emitter) {
        request.setRequestId(UUID.randomUUID().toString().replace("-", ""));
        long startTime = System.currentTimeMillis();
        String scene = request.getSceneCode();
        log.info("[aigateway:网关] 流式请求: scene={}, requestId={}", scene, request.getRequestId());

        try {
            AiSceneConfig config = registry.getConfig(scene);
            if (config == null) {
                sendErrorAndComplete(emitter, "场景未注册或未启用: " + scene);
            }
            AiSceneHandler handler = registry.getHandler(scene);
            injectAgentPersona(request, config);

            // Prompt 注入防护：流式路径同样清洗+拦截
            sanitizeInputChannel(request);
            if (request.getUserInput() != null && !request.getUserInput().isBlank()) {
                PromptInjectionGuard.ScanResult guard = PromptInjectionGuard.scan(request.getUserInput());
                if (guard.isDangerous()) {
                    log.warn("[aigateway:网关] 流式注入拦截: scene={}, requestId={}, pattern={}",
                            scene, request.getRequestId(), guard.getPattern());
                    sendErrorAndComplete(emitter, "输入包含不允许的指令内容");
                }
            }

            // 流式支持校验（Handler 能力 + 场景配置双保险，output_mode='sync' 显式拒绝流式）
            String supported = handler.getSupportedOutputMode();
            if (!"stream".equals(supported) && !"both".equals(supported)) {
                sendErrorAndComplete(emitter, "场景 [" + scene + "] 不支持流式输出");
            }
            if ("sync".equals(config.getOutputMode())) {
                sendErrorAndComplete(emitter, "场景 [" + scene + "] 配置为仅同步输出（output_mode=sync）");
            }

            // 限流
            String identity = request.getUserId() != null
                    ? String.valueOf(request.getUserId()) : "anonymous";
            int limit = config.getRateLimitCount() != null ? config.getRateLimitCount() : 100;
            int window = config.getRateLimitTime() != null ? config.getRateLimitTime() : 60;
            if (!rateLimiter.tryAcquire(scene, identity, limit, window).allowed()) {
                sendErrorAndComplete(emitter, "请求过于频繁，请稍后再试");
            }
            // 成本熔断：流式路径同样前置配额检查；
            // 消费累计依赖响应 metadata，流式由 Handler 直发 emitter 无汇总——记为已知局限
            if (!tokenCostGuard.checkQuota(scene, config.getDailyTokenLimit()).allowed()) {
                sendErrorAndComplete(emitter, "当前场景今日AI额度已用完，请明天再试");
            }

            handler.validate(request);
            handler.executeStream(request, emitter);
            executeLogService.record(request.getRequestId(), request.getUserId(), scene,
                    handler.getClass().getSimpleName(), resolveBindType(config), null,
                    canonicalInputKey(request), null, "success", null,
                    System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            log.error("[aigateway:网关] 流式失败: scene={}, requestId={}", scene, request.getRequestId(), e);
            executeLogService.record(request.getRequestId(), request.getUserId(), scene, null, null, null,
                    canonicalInputKey(request), null, "fail", e.getMessage(),
                    System.currentTimeMillis() - startTime);
            sendErrorAndComplete(emitter, e.getMessage());
        }
    }

    // ==================== 内部实现 ====================

    /**
     * 会话流式执行（回调式）：治理前置（限流/Token熔断/注入防护/执行日志）
     * + ContextManager 记忆注入（滑窗+摘要+瞬态指令，消息列表直传模型）
     * + per-agent 模型路由 + SSE 完成回调 Token 累计。
     *
     * <p>与 {@link #executeStream} 的区别：本通道供业务 Service 直接调用（如语音面试主干），
     * SSE 事件协议（事件名/载荷）由调用方在回调中自定义，业务编排留在业务侧；
     * 网关负责公共治理与 LLM 调用收口。无意图分类（会话已绑定场景）。</p>
     *
     * @param onToken    增量文本回调（回调内异常由网关吞掉记日志，不影响后续回调）
     * @param onComplete 完成回调（参数为全文；记忆写回与治理记账已由网关完成）
     * @param onError    失败回调（治理拒绝/模型失败；网关已完成失败日志记录）
     */
    public void executeConversationStream(ConversationStreamCommand cmd,
                                          java.util.function.Consumer<String> onToken,
                                          java.util.function.Consumer<String> onComplete,
                                          java.util.function.Consumer<Throwable> onError) {
        String requestId = UUID.randomUUID().toString().replace("-", "");
        long startTime = System.currentTimeMillis();
        String scene = cmd.getSceneCode();
        String sessionId = cmd.getSessionId();
        AtomicBoolean done = new AtomicBoolean(false);

        try {
            // 1. 场景治理配置（限流/熔断参数来源；会话模式按首轮锁定版本读快照——
            //    配置回滚只影响新会话，进行中会话不跨版本混跑）
            AiSceneConfig config = resolveSessionConfig(scene, sessionId);
            if (config == null) {
                onError.accept(new IllegalStateException("场景未注册或未启用: " + scene));
                return;
            }

            // 2. 注入防护：本轮用户输入清洗+扫描（会话场景由数据隔离兜底，同主网关口径）
            String userInput = PromptInjectionGuard.sanitizeAndCap(cmd.getUserInput());
            if (userInput != null && !userInput.isBlank()) {
                PromptInjectionGuard.ScanResult guard = PromptInjectionGuard.scan(userInput);
                if (guard.isDangerous()) {
                    log.warn("[aigateway:网关] 会话流式注入拦截: scene={}, requestId={}, pattern={}",
                            scene, requestId, guard.getPattern());
                    executeLogService.record(requestId, cmd.getUserId(), scene,
                            "conversationStream", "agent", null, sessionId, null,
                            "fail", "prompt_injection_blocked", 0);
                    onError.accept(new IllegalStateException("输入包含不允许的指令内容"));
                    return;
                }
            }

            // 3. 限流（场景 × 用户）
            String identity = cmd.getUserId() != null ? String.valueOf(cmd.getUserId()) : "anonymous";
            int limit = config.getRateLimitCount() != null ? config.getRateLimitCount() : 100;
            int window = config.getRateLimitTime() != null ? config.getRateLimitTime() : 60;
            if (!rateLimiter.tryAcquire(scene, identity, limit, window).allowed()) {
                executeLogService.record(requestId, cmd.getUserId(), scene,
                        "conversationStream", "agent", null, sessionId, null,
                        "fail", "rate_limited", 0);
                onError.accept(new IllegalStateException("请求过于频繁，请稍后再试"));
                return;
            }

            // 4. 成本熔断（场景日 Token 配额）
            TokenCostGuard.QuotaResult quota = tokenCostGuard.checkQuota(scene, config.getDailyTokenLimit());
            if (!quota.allowed()) {
                log.warn("[aigateway:网关] 会话流式Token配额熔断: scene={}, requestId={}, used={}/{}",
                        scene, requestId, quota.todayUsed(), quota.limit());
                executeLogService.record(requestId, cmd.getUserId(), scene,
                        "conversationStream", "agent", null, sessionId, null,
                        "fail", "token_limit_exceeded", 0);
                onError.accept(new IllegalStateException("当前场景今日AI额度已用完，请明天再试"));
                return;
            }

            // 5. per-agent 模型路由（agent 绑定链 + 流式自动路由）
            Agent agent = cmd.getAgentId() != null ? agentMapper.selectById(cmd.getAgentId()) : null;
            if (agent == null || Boolean.FALSE.equals(agent.getEnabled())) {
                onError.accept(new IllegalStateException("智能体不存在或未启用"));
                return;
            }
            StreamingChatLanguageModel model = agentModelRouter.createStreamingModel(agent);
            if (model == null) {
                onError.accept(new IllegalStateException("无可用流式模型，请联系管理员配置"));
                return;
            }

            // 6. 上下文装配：当前输入入滑窗 → 滑窗 + 摘要 + 瞬态指令（消息列表直传模型）
            List<ChatMessage> messages = contextManager.buildTurnMessages(
                    sessionId, cmd.getMaxMessages(), userInput, cmd.getDirectives());

            // 7. 流式调用（完成回调补 Token 累计 + 记忆写回 + 执行日志）
            final StringBuilder buffer = new StringBuilder();
            model.chat(messages, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    if (partialResponse == null || partialResponse.isEmpty()) {
                        return;
                    }
                    buffer.append(partialResponse);
                    try {
                        onToken.accept(partialResponse);
                    } catch (Exception e) {
                        log.error("[aigateway:网关] 会话流式 onToken 回调异常: requestId={}", requestId, e);
                    }
                }

                @Override
                public void onCompleteResponse(ChatResponse response) {
                    if (!done.compareAndSet(false, true)) {
                        return;
                    }
                    AiMetadata metadata = new AiMetadata();
                    Integer tokenUsed = null;
                    try {
                        if (response != null && response.tokenUsage() != null
                                && response.tokenUsage().totalTokenCount() != null) {
                            tokenUsed = response.tokenUsage().totalTokenCount();
                            metadata.setTokenUsed(tokenUsed);
                        }
                        if (response != null && response.metadata() != null
                                && response.metadata().modelName() != null) {
                            metadata.setModelUsed(response.metadata().modelName());
                        }
                    } catch (Exception ignored) {
                    }
                    // Token 累计（流式消费补缺：完成回调汇总 tokenUsage）
                    if (tokenUsed != null) {
                        try {
                            tokenCostGuard.consume(scene, tokenUsed);
                        } catch (Exception e) {
                            log.warn("[aigateway:网关] Token累计失败（不影响业务）: {}", e.getMessage());
                        }
                    }
                    // AI 回复入滑窗（超窗时异步预生成摘要，不阻塞）
                    contextManager.recordAiReply(sessionId, cmd.getMaxMessages(), buffer.toString());
                    executeLogService.record(requestId, cmd.getUserId(), scene,
                            "conversationStream", "agent", metadata, sessionId,
                            buffer.toString(), "success", null,
                            System.currentTimeMillis() - startTime);
                    try {
                        onComplete.accept(buffer.toString());
                    } catch (Exception e) {
                        log.error("[aigateway:网关] 会话流式 onComplete 回调异常: requestId={}", requestId, e);
                    }
                }

                @Override
                public void onError(Throwable error) {
                    if (!done.compareAndSet(false, true)) {
                        return;
                    }
                    executeLogService.record(requestId, cmd.getUserId(), scene,
                            "conversationStream", "agent", null, sessionId,
                            buffer.toString(), "fail", error.getMessage(),
                            System.currentTimeMillis() - startTime);
                    onError.accept(error);
                }
            });
        } catch (Exception e) {
            log.error("[aigateway:网关] 会话流式失败: scene={}, requestId={}", scene, requestId, e);
            if (done.compareAndSet(false, true)) {
                executeLogService.record(requestId, cmd.getUserId(), scene,
                        "conversationStream", "agent", null, sessionId, null,
                        "fail", e.getMessage(), System.currentTimeMillis() - startTime);
                onError.accept(e);
            }
        }
    }

    // ==================== 输入清洗与记忆 ====================

    /**
     * 会话配置版本锁：会话首轮将当前 config_version 写入 Redis
     * （chat:memory:session:{sessionId}，30 天与记忆同过期），此后每轮校验——
     * 版本未变直用当前配置；版本已变（管理端保存/回滚）则按锁定版本读
     * ai_scene_config_history 快照。非会话模式（无 sessionId）始终读当前配置。
     */
    private AiSceneConfig resolveSessionConfig(String scene, String sessionId) {
        AiSceneConfig current = registry.getConfig(scene);
        if (current == null || sessionId == null || sessionId.isBlank()) {
            return current;
        }
        String lockKey = RedisKeys.chatMemorySession(sessionId);
        try {
            String locked = redisTemplate.opsForValue().get(lockKey);
            if (locked == null) {
                redisTemplate.opsForValue().set(lockKey,
                        String.valueOf(current.getConfigVersion() != null ? current.getConfigVersion() : 1),
                        RedisKeys.CHAT_MEMORY_EXPIRE_DAYS, TimeUnit.DAYS);
                return current;
            }
            int lockedVersion = Integer.parseInt(locked);
            int currentVersion = current.getConfigVersion() != null ? current.getConfigVersion() : 1;
            if (lockedVersion == currentVersion) {
                return current;
            }
            AiSceneConfig snapshot = sceneConfigVersionService.loadSnapshot(scene, lockedVersion);
            if (snapshot != null) {
                log.info("[aigateway:网关] 会话按锁定版本读快照: scene={}, sessionId={}, locked=v{}, current=v{}",
                        scene, sessionId, lockedVersion, currentVersion);
                return snapshot;
            }
            // 快照缺失（历史数据无快照）：回落当前配置，不阻断会话
            log.warn("[aigateway:网关] 会话锁定版本无快照，回落当前配置: scene={}, locked=v{}", scene, lockedVersion);
            return current;
        } catch (Exception e) {
            log.warn("[aigateway:网关] 会话版本锁读取失败（回落当前配置）: sessionId={}: {}", sessionId, e.getMessage());
            return current;
        }
    }

    /**
     * 输入通道清洗：顶层 userInput 走 sanitizeAndCap（含长度截断）；
     * input Map 的字符串值仅做字符级清洗（数据通道不拦截——误杀简历/文档类数据代价高于收益，
     * 由 Handler 侧 wrapData 数据隔离兜底）。
     *
     * <p>根因修复：业务侧可能传入不可变 Map（{@code Map.of(...)}），原地
     * {@code entry.setValue} 会抛 {@code UnsupportedOperationException("not supported")}
     * （JDK 21 不可变集合语义），曾导致 voice_interview 逐题分析 100% 走 50 分兜底。
     * 此处不再原地改写，重建可变 LinkedHashMap 整体替换。</p>
     */
    private void sanitizeInputChannel(AiExecuteRequest request) {
        request.setUserInput(PromptInjectionGuard.sanitizeAndCap(request.getUserInput()));
        if (request.getInput() != null) {
            Map<String, Object> sanitized = new java.util.LinkedHashMap<>(request.getInput().size());
            for (Map.Entry<String, Object> entry : request.getInput().entrySet()) {
                sanitized.put(entry.getKey(), entry.getValue() instanceof String s
                        ? PromptInjectionGuard.sanitize(s) : entry.getValue());
            }
            request.setInput(sanitized);
        }
    }

    /**
     * Agent 人设注入：ai_scene_config.agent_id 绑定智能体时，读取 ai_agent.system_prompt，
     * 以 request.input 渲染 {{占位符}} 后注入 input.agentPersona。Agent 禁用/无提示词/加载失败均静默跳过
     * （场景按无人设执行，不阻断）。注入位于缓存键计算之前——人设变更自动失效缓存。
     *
     * @return 绑定的 Agent 名称（未绑定/加载失败返回 null，供 metadata.agentUsed 补充）
     */
    private String injectAgentPersona(AiExecuteRequest request, AiSceneConfig config) {
        if (config == null || config.getAgentId() == null) {
            return null;
        }
        try {
            Agent agent = agentMapper.selectById(config.getAgentId());
            if (agent == null || Boolean.FALSE.equals(agent.getEnabled())
                    || agent.getSystemPrompt() == null || agent.getSystemPrompt().isBlank()) {
                return null;
            }
            if (request.getInput() == null) {
                request.setInput(new java.util.LinkedHashMap<>());
            }
            String persona = agent.getSystemPrompt();
            for (Map.Entry<String, Object> entry : request.getInput().entrySet()) {
                persona = persona.replace("{{" + entry.getKey() + "}}",
                        entry.getValue() != null ? String.valueOf(entry.getValue()) : "");
            }
            request.getInput().put("agentPersona", persona);
            log.info("[aigateway:网关] Agent人设注入: scene={}, agent={}({})",
                    request.getSceneCode(), agent.getName(), agent.getId());
            return agent.getName();
        } catch (Exception e) {
            log.warn("[aigateway:网关] Agent人设注入失败（忽略，按无人设执行）: agentId={}, {}",
                    config.getAgentId(), e.getMessage());
            return null;
        }
    }

    /**
     * 响应元数据补充 Agent 名称（可观测性）：Handler 已填 modelUsed/tokenUsed 等时仅补
     * agentUsed 空位；未填时创建。fromCache 由 SemanticCache 独立标记，此处不触碰。
     */
    private void fillAgentMetadata(AiExecuteResponse<?> response, String agentName) {
        if (response == null || agentName == null) {
            return;
        }
        com.moyun.ext.aigateway.model.AiMetadata metadata = response.getMetadata();
        if (metadata == null) {
            metadata = new com.moyun.ext.aigateway.model.AiMetadata();
            response.setMetadata(metadata);
        }
        if (metadata.getAgentUsed() == null) {
            metadata.setAgentUsed(agentName);
        }
    }

    /**
     * 从场景配置推导绑定类型（ai_scene_config 无 bind_type 冗余列，运行时推导）
     */
    private String resolveBindType(AiSceneConfig config) {
        if (config == null) return null;
        if (config.getAgentId() != null) return "agent";
        if (config.getModelConfigId() != null) return "model";
        if (config.getWorkflowId() != null) return "workflow";
        return "empty";
    }

    private void fillCommon(AiExecuteResponse<?> response, AiExecuteRequest request, long elapsed) {
        response.setRequestId(request.getRequestId());
        response.setSceneCode(request.getSceneCode());
        response.setElapsedMs(elapsed);
    }

    private AiExecuteResponse<?> failure(AiExecuteRequest request, int code, String msg,
                                         long elapsed, String logError) {
        AiExecuteResponse<Object> resp = AiExecuteResponse.failure(code, msg);
        fillCommon(resp, request, elapsed);
        return resp;
    }

    /**
     * 缓存键：input 的规范化 JSON（TreeMap 保证键序稳定）
     */
    private String canonicalInputKey(AiExecuteRequest request) {
        if (request.getInput() == null || request.getInput().isEmpty()) {
            return "{}";
        }
        try {
            return MAPPER.writeValueAsString(new TreeMap<>(request.getInput()));
        } catch (Exception e) {
            return String.valueOf(request.getInput());
        }
    }

    /**
     * 语义比对主文本：优先顶层 userInput，其次 input 内字符串参数拼接
     */
    private String primaryInputText(AiExecuteRequest request) {
        String preferred = request.getUserInput();
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        if (request.getInput() == null || request.getInput().isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (Object value : request.getInput().values()) {
            if (value instanceof String s) {
                sb.append(s).append(' ');
            }
        }
        return sb.isEmpty() ? null : sb.toString().trim();
    }

    private String summarizeOutput(AiExecuteResponse<?> response) {
        if (response == null || response.getData() == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(response.getData());
        } catch (Exception e) {
            return String.valueOf(response.getData());
        }
    }

    private void sendErrorAndComplete(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().name("error").data(
                    Map.of("error", message != null ? message : "unknown error")));
            emitter.complete();
        } catch (Exception ignored) {
        }
    }
}
