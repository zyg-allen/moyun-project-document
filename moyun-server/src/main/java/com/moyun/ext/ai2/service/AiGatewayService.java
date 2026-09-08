package com.moyun.ext.ai2.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.ext.ai2.constant.AiErrorCodes;
import com.moyun.ext.ai2.entity.AiSceneRegistryConfig;
import com.moyun.ext.ai2.handler.AiSceneHandler;
import com.moyun.ext.ai2.model.AiExecuteRequest;
import com.moyun.ext.ai2.model.AiExecuteResponse;
import com.moyun.ext.ai2.registry.AiSceneRegistry;
import com.moyun.ext.ai2.support.AiExecuteLogService;
import com.moyun.ext.ai2.support.FallbackStrategy;
import com.moyun.ext.ai2.support.IntentClassifier;
import com.moyun.ext.ai2.support.SceneRateLimiter;
import com.moyun.ext.ai2.support.SemanticCache;
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

    /**
     * 同步执行（统一入口核心编排）
     */
    public AiExecuteResponse<?> execute(AiExecuteRequest request) {
        request.setRequestId(UUID.randomUUID().toString().replace("-", ""));
        long startTime = System.currentTimeMillis();
        String scene = request.getScene();
        log.info("[ai2:网关] 请求: scene={}, requestId={}", scene, request.getRequestId());

        AiSceneRegistryConfig config = null;
        AiSceneHandler handler = null;
        try {
            // 1. 场景配置（未配置/未启用的场景不对外服务）
            config = registry.getConfig(scene);
            if (config == null) {
                return failure(request, AiErrorCodes.SCENE_NOT_FOUND,
                        "场景未注册或未启用: " + scene, 0, "scene_not_found");
            }
            handler = registry.getHandler(scene);

            // 2. 意图判断（仅当输入包含 userInput 时；低置信度触发追问）
            String userInput = getStringInput(request, "userInput");
            if (userInput != null && !userInput.isBlank()) {
                IntentClassifier.IntentResult intent = intentClassifier.classify(userInput, scene);
                if (intent.getConfidence() < 0.6) {
                    AiExecuteResponse<Object> resp = AiExecuteResponse.clarification(
                            "未能理解您的意图，能否补充说明一下您想做什么？");
                    fillCommon(resp, request, System.currentTimeMillis() - startTime);
                    return resp;
                }
                if (intent.getSuggestedScene() != null
                        && registry.getConfig(intent.getSuggestedScene()) != null) {
                    request.setScene(intent.getSuggestedScene());
                    scene = request.getScene();
                }
            }

            // 3. 缓存检查
            String inputKey = canonicalInputKey(request);
            String inputText = primaryInputText(request);
            if (semanticCache.isEnabled(config.getEnableCache())) {
                AiExecuteResponse<Object> cached = semanticCache.get(scene, inputKey, inputText);
                if (cached != null) {
                    fillCommon(cached, request, System.currentTimeMillis() - startTime);
                    log.info("[ai2:网关] 缓存命中: scene={}, requestId={}", scene, request.getRequestId());
                    return cached;
                }
            }

            // 4. 限流（场景 × 用户）
            String identity = request.getUserId() != null
                    ? String.valueOf(request.getUserId()) : "anonymous";
            int limit = config.getRateLimitCount() != null ? config.getRateLimitCount() : 100;
            int window = config.getRateLimitTime() != null ? config.getRateLimitTime() : 60;
            SceneRateLimiter.RateResult rate = rateLimiter.tryAcquire(scene, identity, limit, window);
            if (!rate.allowed()) {
                executeLogService.record(request.getRequestId(), scene,
                        handler.getClass().getSimpleName(), config.getBindType(), null,
                        inputKey, null, "fail", "rate_limited",
                        System.currentTimeMillis() - startTime);
                return failure(request, AiErrorCodes.RATE_LIMITED,
                        "请求过于频繁，请稍后再试", System.currentTimeMillis() - startTime, "rate_limited");
            }

            // 5. 参数校验
            handler.validate(request);

            // 6. 执行
            AiExecuteResponse<?> response = handler.execute(request);

            // 7. 填充通用字段 + 回写缓存 + 记录日志
            long elapsed = System.currentTimeMillis() - startTime;
            fillCommon(response, request, elapsed);
            if (semanticCache.isEnabled(config.getEnableCache())
                    && response.getCode() != null && response.getCode() == AiErrorCodes.SUCCESS) {
                semanticCache.put(scene, inputKey, inputText, response, config.getCacheTtl());
            }
            executeLogService.record(request.getRequestId(), scene,
                    handler.getClass().getSimpleName(), config.getBindType(), null,
                    inputKey, summarizeOutput(response), "success", null, elapsed);
            log.info("[ai2:网关] 成功: scene={}, requestId={}, elapsed={}ms",
                    scene, request.getRequestId(), elapsed);
            return response;

        } catch (Exception e) {
            // 8. 降级兜底
            long elapsed = System.currentTimeMillis() - startTime;
            AiExecuteResponse<?> fallback = fallbackStrategy.executeFallback(scene,
                    config != null ? config.getFallbackResponse() : null, e);
            fillCommon(fallback, request, elapsed);
            executeLogService.record(request.getRequestId(), scene,
                    handler != null ? handler.getClass().getSimpleName() : null,
                    config != null ? config.getBindType() : null, null,
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
        String scene = request.getScene();
        log.info("[ai2:网关] 流式请求: scene={}, requestId={}", scene, request.getRequestId());

        try {
            AiSceneRegistryConfig config = registry.getConfig(scene);
            if (config == null) {
                sendErrorAndComplete(emitter, "场景未注册或未启用: " + scene);
                return emitter;
            }
            AiSceneHandler handler = registry.getHandler(scene);

            // 流式支持校验
            String supported = handler.getSupportedOutputMode();
            if (!"stream".equals(supported) && !"both".equals(supported)) {
                sendErrorAndComplete(emitter, "场景 [" + scene + "] 不支持流式输出");
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

            handler.validate(request);
            handler.executeStream(request, emitter);
            executeLogService.record(request.getRequestId(), scene,
                    handler.getClass().getSimpleName(), config.getBindType(), null,
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

    private void fillCommon(AiExecuteResponse<?> response, AiExecuteRequest request, long elapsed) {
        response.setRequestId(request.getRequestId());
        response.setScene(request.getScene());
        response.setElapsedMs(elapsed);
    }

    private AiExecuteResponse<?> failure(AiExecuteRequest request, int code, String msg,
                                         long elapsed, String logError) {
        AiExecuteResponse<Object> resp = AiExecuteResponse.failure(code, msg);
        fillCommon(resp, request, elapsed);
        return resp;
    }

    private String getStringInput(AiExecuteRequest request, String key) {
        Object value = request.getInput() != null ? request.getInput().get(key) : null;
        return value != null ? String.valueOf(value) : null;
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
     * 语义比对主文本：优先 userInput/text，其次所有字符串参数拼接
     */
    private String primaryInputText(AiExecuteRequest request) {
        if (request.getInput() == null || request.getInput().isEmpty()) {
            return null;
        }
        String preferred = getStringInput(request, "userInput");
        if (preferred == null) {
            preferred = getStringInput(request, "text");
        }
        if (preferred != null) {
            return preferred;
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
