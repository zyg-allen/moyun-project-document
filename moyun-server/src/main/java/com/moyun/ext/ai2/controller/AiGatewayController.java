package com.moyun.ext.ai2.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.ext.ai2.constant.AiErrorCodes;
import com.moyun.ext.ai2.model.AiExecuteRequest;
import com.moyun.ext.ai2.model.AiExecuteResponse;
import com.moyun.ext.ai2.registry.AiSceneRegistry;
import com.moyun.ext.ai2.service.AiGatewayService;
import com.moyun.util.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * AI能力统一接入层-网关Controller
 *
 * <p>统一AI执行入口。依据《AI能力统一接入层 — 完整方案文档》V2.0 §5.3。</p>
 *
 * <p>所有AI场景（面试/简历/出题/财务分析/敏感词/今日主题等）统一走
 * {@code POST /api/ai/execute}（同步）与 {@code POST /api/ai/execute/stream}（流式），
 * 场景由 sceneCode 参数路由到对应 Handler。接口经全局安全链鉴权（anyRequest().authenticated()）。</p>
 *
 * @author laomao
 * @since 2026-09-09
 */
@Slf4j
@Tag(name = "AI能力统一接入层")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiGatewayController {

    private final AiGatewayService gatewayService;
    private final AiSceneRegistry registry;

    /**
     * 统一AI执行入口（同步）
     */
    @Operation(summary = "统一AI执行（同步）", description = "所有AI场景统一入口，按scene路由到对应Handler")
    @PostMapping(value = "/execute", produces = MediaType.APPLICATION_JSON_VALUE)
    public AiExecuteResponse<?> execute(@RequestBody @Valid AiExecuteRequest request) {
        injectContext(request);
        AiExecuteResponse<?> rejected = rejectIfNotOpen(request.getSceneCode(), request.getRequestId());
        if (rejected != null) {
            return rejected;
        }
        return gatewayService.execute(request);
    }

    /**
     * 统一AI执行入口（流式 SSE）
     */
    @Operation(summary = "统一AI执行（流式SSE）", description = "事件流：chunk(增量文本) / done(完成) / error(错误)")
    @PostMapping(value = "/execute/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter executeStream(@RequestBody @Valid AiExecuteRequest request) {
        injectContext(request);
        SseEmitter emitter = new SseEmitter(60_000L);
        if (rejectIfNotOpen(request.getSceneCode(), request.getRequestId()) != null) {
            try {
                emitter.send(SseEmitter.event().name("error").data(
                        Map.of("code", AiErrorCodes.SCENE_NOT_OPEN, "error", "场景未开放通用入口调用")));
                emitter.complete();
            } catch (Exception ignored) {
            }
            return emitter;
        }
        return gatewayService.executeStream(request);
    }

    /**
     * 通用入口开放校验（v11.51 断点⑤）：ai_scene_config.open_api=0 的场景（业务内部链路专用，
     * 如 finance_analysis 需查用户库聚合指标）不得经 /api/ai/execute 外部调用——防越权绕过业务
     * Controller 的鉴权/快照/落表编排。业务 Service 直接调 AiGatewayService 不经此层，不受限。
     *
     * @return 拒绝响应；null=放行
     */
    private AiExecuteResponse<?> rejectIfNotOpen(String sceneCode, String requestId) {
        com.moyun.ext.ai.entity.AiSceneConfig config = registry.getConfig(sceneCode);
        if (config == null || !Boolean.TRUE.equals(config.getOpenApi())) {
            AiExecuteResponse<Object> resp = AiExecuteResponse.failure(
                    config == null ? AiErrorCodes.SCENE_NOT_FOUND : AiErrorCodes.SCENE_NOT_OPEN,
                    config == null ? "场景未注册或未启用: " + sceneCode : "场景未开放通用入口调用: " + sceneCode);
            resp.setRequestId(requestId);
            resp.setSceneCode(sceneCode);
            return resp;
        }
        return null;
    }

    /**
     * 已注册场景总览（调试/管理）
     */
    @Operation(summary = "已注册AI场景列表")
    @GetMapping("/scenes")
    public AjaxResult listScenes() {
        List<Map<String, Object>> scenes = registry.listScenes();
        return AjaxResult.success(scenes);
    }

    /**
     * 刷新场景注册中心（Handler重注册+配置重加载）
     */
    @Operation(summary = "刷新场景注册中心", description = "ai_scene_config 配置变更后调用，无需重启服务")
    @PostMapping("/refresh")
    public AjaxResult refresh() {
        registry.refresh();
        return AjaxResult.success("场景注册中心已刷新");
    }

    /**
     * 注入调用上下文（当前用户）
     */
    private void injectContext(AiExecuteRequest request) {
        if (request.getUserId() == null) {
            try {
                request.setUserId(SecurityUtils.getUserId());
            } catch (Exception ignored) {
                // 未登录上下文（如匿名调试），保持 null，限流按 anonymous 处理
            }
        }
    }
}
