package com.moyun.ext.ai2.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.ext.ai.entity.Agent;
import com.moyun.ext.ai.entity.AiSceneConfig;
import com.moyun.ext.ai.mapper.AgentMapper;
import com.moyun.ext.ai2.constant.AiErrorCodes;
import com.moyun.ext.ai2.handler.AiSceneHandler;
import com.moyun.ext.ai2.model.AiExecuteRequest;
import com.moyun.ext.ai2.model.AiExecuteResponse;
import com.moyun.ext.ai2.registry.AiSceneRegistry;
import com.moyun.ext.ai2.support.AiExecuteLogService;
import com.moyun.ext.ai2.support.AiOutputFilter;
import com.moyun.ext.ai2.support.FallbackStrategy;
import com.moyun.ext.ai2.support.IntentClassifier;
import com.moyun.ext.ai2.support.PromptInjectionGuard;
import com.moyun.ext.ai2.support.SceneRateLimiter;
import com.moyun.ext.ai2.support.SemanticCache;
import com.moyun.ext.ai2.support.TokenCostGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

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
    /** v11.57 P0-2：场景日 Token 成本熔断（ai_scene_config.daily_token_limit） */
    private final TokenCostGuard tokenCostGuard;
    /** v11.62 P1-3：输出内容过滤（ai_scene_config.enable_output_filter，复用 DFA 词树脱敏） */
    private final AiOutputFilter outputFilter;
    /** v11.49：Agent 人设注入（ai_scene_config.agent_id → ai_agent.system_prompt） */
    private final AgentMapper agentMapper;

    /**
     * 同步执行（统一入口核心编排）
     */
    public AiExecuteResponse<?> execute(AiExecuteRequest request) {
        request.setRequestId(UUID.randomUUID().toString().replace("-", ""));
        long startTime = System.currentTimeMillis();
        String sceneCode = request.getSceneCode();
        log.info("[ai2:网关] 请求: scene={}, requestId={}", sceneCode, request.getRequestId());

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

            // 1.2 输出模式路由校验（v11.66 P1-5：output_mode 配置接线——此前配置可编辑零消费）。
            //     显式 stream-only 场景拒绝同步入口；both/null 放行（Handler 能力校验在流式侧兜底）
            if ("stream".equals(config.getOutputMode())) {
                return failure(request, AiErrorCodes.INVALID_REQUEST,
                        "场景 [" + sceneCode + "] 仅支持流式输出，请调用流式端点 /api/ai/execute/stream",
                        0, "stream_only_scene");
            }

            // 1.5 Agent 人设注入（v11.49：ai_scene_config.agent_id 绑定智能体时，其 system_prompt
            //     渲染 {{占位符}} 后以 agentPersona 注入 input，Handler 构建系统提示词时统一前置。
            //     注入先于缓存键计算——人设变更自动不脏读缓存）
            String agentName = injectAgentPersona(request, config);

            // 1.6 Prompt 注入防护（v11.57 P0-1）：指令通道（顶层 userInput）统一清洗+扫描。
            //     DANGEROUS（指令覆盖/提示词探取）直接拒绝；SUSPECT（角色扮演）放行由数据隔离兜底——
            //     业务存在合法角色扮演场景。input Map 的字符串值做字符级清洗（不拦截，防误杀数据）。
            sanitizeInputChannel(request);
            String userInput = request.getUserInput();
            if (userInput != null && !userInput.isBlank()) {
                PromptInjectionGuard.ScanResult guard = PromptInjectionGuard.scan(userInput);
                if (guard.isDangerous()) {
                    log.warn("[ai2:网关] 注入防护拦截: scene={}, requestId={}, pattern={}",
                            sceneCode, request.getRequestId(), guard.getPattern());
                    executeLogService.record(request.getRequestId(), sceneCode,
                            handler.getClass().getSimpleName(), resolveBindType(config), null,
                            null, null, "fail", "prompt_injection_blocked",
                            System.currentTimeMillis() - startTime);
                    return failure(request, AiErrorCodes.INPUT_REJECTED,
                            "输入包含不允许的指令内容", System.currentTimeMillis() - startTime, "prompt_injection");
                }
            }

            // 2. 意图判断（v11.52：消费顶层 userInput 字段——用户自由文本触发分类路由；
            //    结构化参数场景（如 finance_analysis 传 userId/range）不传 userInput，自然跳过。
            //    当前主要预留对象：chat 收口进网关后，对话消息即 userInput，此分支成为场景路由器）
            if (userInput != null && !userInput.isBlank()) {
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
                    // 输出过滤（v11.62 P1-3）：命中路径同样过滤——兜底过滤功能上线前的存量旧缓存
                    if (outputFilter.isEnabled(config)) {
                        outputFilter.applyFilter(cached);
                    }
                    fillCommon(cached, request, System.currentTimeMillis() - startTime);
                    log.info("[ai2:网关] 缓存命中: scene={}, requestId={}", sceneCode, request.getRequestId());
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
                executeLogService.record(request.getRequestId(), sceneCode,
                    handler.getClass().getSimpleName(), resolveBindType(config), null,
                    inputKey, null, "fail", "rate_limited",
                    System.currentTimeMillis() - startTime);
                return failure(request, AiErrorCodes.RATE_LIMITED,
                        "请求过于频繁，请稍后再试", System.currentTimeMillis() - startTime, "rate_limited");
            }

            // 4.5 成本熔断（v11.57 P0-2）：场景日 Token 累计超 daily_token_limit → 拒绝。
            //     场景级配额（全体用户共享），保护平台总成本；null/0=不限。
            TokenCostGuard.QuotaResult quota = tokenCostGuard.checkQuota(sceneCode, config.getDailyTokenLimit());
            if (!quota.allowed()) {
                executeLogService.record(request.getRequestId(), sceneCode,
                    handler.getClass().getSimpleName(), resolveBindType(config), null,
                    inputKey, null, "fail", "token_limit_exceeded",
                    System.currentTimeMillis() - startTime);
                log.warn("[ai2:网关] Token配额熔断: scene={}, requestId={}, used={}/{}",
                        sceneCode, request.getRequestId(), quota.todayUsed(), quota.limit());
                return failure(request, AiErrorCodes.AI_TOKEN_LIMIT_EXCEEDED,
                        "当前场景今日AI额度已用完，请明天再试", System.currentTimeMillis() - startTime, "token_limit");
            }

            // 5. 参数校验
            handler.validate(request);

            // 6. 执行（v11.48：配置随调用下发，Handler 提示词/输出结构读配置即时生效，无静态 ThreadLocal）
            AiExecuteResponse<?> response = handler.execute(request, config);

            // 6.5 输出内容过滤（v11.62 P1-3）：场景开启 enable_output_filter 时，复用 DFA 词树
            //     对响应 data 的全部文本节点脱敏。位于缓存回写/执行日志之前——缓存与日志留痕的
            //     均为脱敏后内容（命中路径见步骤 3，兜底存量旧缓存）。
            if (outputFilter.isEnabled(config)) {
                outputFilter.applyFilter(response);
            }

            // 7. 填充通用字段 + 回写缓存 + 记录日志
            long elapsed = System.currentTimeMillis() - startTime;
            fillCommon(response, request, elapsed);
            fillAgentMetadata(response, agentName);
            // v11.57 P0-2：按实际消耗累计场景日 Token（未回传 token 不计）
            if (response.getMetadata() != null && response.getMetadata().getTokenUsed() != null) {
                tokenCostGuard.consume(sceneCode, response.getMetadata().getTokenUsed());
            }
            if (semanticCache.isEnabled(Boolean.TRUE.equals(config.getEnableCache()) ? 1 : 0)
                    && response.getCode() != null && response.getCode() == AiErrorCodes.SUCCESS) {
                semanticCache.put(sceneCode, inputKey, inputText, response, config.getCacheTtl());
            }
            executeLogService.record(request.getRequestId(), sceneCode,
                    handler.getClass().getSimpleName(), resolveBindType(config), response.getMetadata(),
                    inputKey, summarizeOutput(response), "success", null, elapsed);
            log.info("[ai2:网关] 成功: scene={}, requestId={}, elapsed={}ms",
                    sceneCode, request.getRequestId(), elapsed);
            return response;

        } catch (Exception e) {
            // 8. 降级兜底
            long elapsed = System.currentTimeMillis() - startTime;
            AiExecuteResponse<?> fallback = fallbackStrategy.executeFallback(sceneCode,
                    config != null ? config.getFallbackResponse() : null, e);
            fillCommon(fallback, request, elapsed);
            executeLogService.record(request.getRequestId(), sceneCode,
                    handler != null ? handler.getClass().getSimpleName() : null,
                    config != null ? resolveBindType(config) : null, null,
                    canonicalInputKey(request), null, "fail", e.getMessage(), elapsed);
            return fallback;
        }
    }

    /**
     * 流式执行（SSE）
     */
    public SseEmitter executeStream(AiExecuteRequest request) {
        request.setRequestId(UUID.randomUUID().toString().replace("-", ""));
        SseEmitter emitter = new SseEmitter(60_000L);
        long startTime = System.currentTimeMillis();
        String scene = request.getSceneCode();
        log.info("[ai2:网关] 流式请求: scene={}, requestId={}", scene, request.getRequestId());

        try {
            AiSceneConfig config = registry.getConfig(scene);
            if (config == null) {
                sendErrorAndComplete(emitter, "场景未注册或未启用: " + scene);
                return emitter;
            }
            AiSceneHandler handler = registry.getHandler(scene);
            injectAgentPersona(request, config);

            // Prompt 注入防护（v11.57 P0-1）：流式路径同样清洗+拦截
            sanitizeInputChannel(request);
            if (request.getUserInput() != null && !request.getUserInput().isBlank()) {
                PromptInjectionGuard.ScanResult guard = PromptInjectionGuard.scan(request.getUserInput());
                if (guard.isDangerous()) {
                    log.warn("[ai2:网关] 流式注入拦截: scene={}, requestId={}, pattern={}",
                            scene, request.getRequestId(), guard.getPattern());
                    sendErrorAndComplete(emitter, "输入包含不允许的指令内容");
                    return emitter;
                }
            }

            // 流式支持校验（Handler 能力 + 场景配置双保险，v11.66：output_mode='sync' 显式拒绝流式）
            String supported = handler.getSupportedOutputMode();
            if (!"stream".equals(supported) && !"both".equals(supported)) {
                sendErrorAndComplete(emitter, "场景 [" + scene + "] 不支持流式输出");
                return emitter;
            }
            if ("sync".equals(config.getOutputMode())) {
                sendErrorAndComplete(emitter, "场景 [" + scene + "] 配置为仅同步输出（output_mode=sync）");
                return emitter;
            }

            // 限流
            String identity = request.getUserId() != null
                    ? String.valueOf(request.getUserId()) : "anonymous";
            int limit = config.getRateLimitCount() != null ? config.getRateLimitCount() : 100;
            int window = config.getRateLimitTime() != null ? config.getRateLimitTime() : 60;
            if (!rateLimiter.tryAcquire(scene, identity, limit, window).allowed()) {
                sendErrorAndComplete(emitter, "请求过于频繁，请稍后再试");
                return emitter;
            }
            // 成本熔断（v11.57 P0-2）：流式路径同样前置配额检查；
            // 消费累计依赖响应 metadata，流式由 Handler 直发 emitter 无汇总——记为已知局限
            if (!tokenCostGuard.checkQuota(scene, config.getDailyTokenLimit()).allowed()) {
                sendErrorAndComplete(emitter, "当前场景今日AI额度已用完，请明天再试");
                return emitter;
            }

            handler.validate(request);
            handler.executeStream(request, emitter);
            executeLogService.record(request.getRequestId(), scene,
                    handler.getClass().getSimpleName(), resolveBindType(config), null,
                    canonicalInputKey(request), null, "success", null,
                    System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            log.error("[ai2:网关] 流式失败: scene={}, requestId={}", scene, request.getRequestId(), e);
            executeLogService.record(request.getRequestId(), scene, null, null, null,
                    canonicalInputKey(request), null, "fail", e.getMessage(),
                    System.currentTimeMillis() - startTime);
            sendErrorAndComplete(emitter, e.getMessage());
        }
        return emitter;
    }

    // ==================== 内部实现 ====================

    /**
     * 输入通道清洗（v11.57 P0-1）：顶层 userInput 走 sanitizeAndCap（含长度截断）；
     * input Map 的字符串值仅做字符级清洗（数据通道不拦截——误杀简历/文档类数据代价高于收益，
     * 由 Handler 侧 wrapData 数据隔离兜底）。
     */
    private void sanitizeInputChannel(AiExecuteRequest request) {
        request.setUserInput(PromptInjectionGuard.sanitizeAndCap(request.getUserInput()));
        if (request.getInput() != null) {
            for (Map.Entry<String, Object> entry : request.getInput().entrySet()) {
                if (entry.getValue() instanceof String s) {
                    entry.setValue(PromptInjectionGuard.sanitize(s));
                }
            }
        }
    }

    /**
     * Agent 人设注入（v11.49）：ai_scene_config.agent_id 绑定智能体时，读取 ai_agent.system_prompt，
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
            log.info("[ai2:网关] Agent人设注入: scene={}, agent={}({})",
                    request.getSceneCode(), agent.getName(), agent.getId());
            return agent.getName();
        } catch (Exception e) {
            log.warn("[ai2:网关] Agent人设注入失败（忽略，按无人设执行）: agentId={}, {}",
                    config.getAgentId(), e.getMessage());
            return null;
        }
    }

    /**
     * 响应元数据补充 Agent 名称（v11.51 可观测性）：Handler 已填 modelUsed/tokenUsed 等时仅补
     * agentUsed 空位；未填时创建。fromCache 由 SemanticCache 独立标记，此处不触碰。
     */
    private void fillAgentMetadata(AiExecuteResponse<?> response, String agentName) {
        if (response == null || agentName == null) {
            return;
        }
        com.moyun.ext.ai2.model.AiMetadata metadata = response.getMetadata();
        if (metadata == null) {
            metadata = new com.moyun.ext.ai2.model.AiMetadata();
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
