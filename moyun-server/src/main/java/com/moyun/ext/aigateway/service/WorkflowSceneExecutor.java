package com.moyun.ext.aigateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.ext.ai.engine.workflow.WorkflowEngine;
import com.moyun.ext.ai.entity.AiSceneConfig;
import com.moyun.ext.ai.service.WorkflowService;
import com.moyun.ext.aigateway.constant.AiErrorCodes;
import com.moyun.ext.aigateway.model.AiExecuteRequest;
import com.moyun.ext.aigateway.model.AiExecuteResponse;
import com.moyun.ext.aigateway.model.AiMetadata;
import com.moyun.ext.aigateway.model.data.GenericSceneData;
import com.moyun.ext.aigateway.support.TokenMeter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * 工作流场景执行器（v14.72 统一收口 · workflow 固定路线通道）
 *
 * <p><b>定位</b>：统一网关的第二条执行通道（与模型直连、agent 自主规划并列）——
 * 场景行绑定 {@code workflow_id} 时，网关把请求分派给 {@link WorkflowService}
 * 执行固定编排路线。适合<b>执行路线固定、输出结构由节点保证</b>的场景：成本可
 * 预算（节点数固定，无自主规划的不确定轮次），管理端改图即改逻辑。</p>
 *
 * <p><b>与 agent 自主规划的分工</b>（调度三选一，由
 * {@code AiSceneConfigController.validate} 保存时强制互斥）：</p>
 * <ul>
 *   <li>固定执行路线 / 固定输出格式 → 绑<b>工作流</b>（本通道，成本可预算）；</li>
 *   <li>固定模型单发 → 绑<b>模型</b>（Handler 配置驱动直连通道）；</li>
 *   <li>开放任务（路线无法预先固化）→ 绑<b>智能体</b>并开启 autonomousPlanning
 *       （{@code AgentPlanExecutor}，maxIterations + tokenBudget 双硬顶限损）。</li>
 * </ul>
 *
 * <p><b>治理分层</b>：本通道在网关编排内运行——外层限流/Token 熔断/执行日志
 * 对整个工作流请求生效；工作流内部的 LLM 节点经场景 {@code workflow_agent_node}
 * 独立计量（内层治理）。外层 Token 为<b>信封估算</b>（tokenEstimated=true，
 * 工作流多节点 usage 无法在网关层聚合），用于外层场景配额与成本可见性，
 * 与内层逐节点真实计量互不抵扣。</p>
 *
 * <p><b>Bean 循环防护</b>：{@code WorkflowService → WorkflowEngine → List<NodeExecutor>
 * → AgentNodeExecutor → AiSceneJsonClient → AiGatewayService → 本执行器}，用
 * {@link ObjectProvider} 延迟解析打破环。</p>
 *
 * @author laomao
 * @since 2026-10-05
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowSceneExecutor {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Token 信封估算的输入/输出截断（估算口径，防超长日志文本拖慢计量） */
    private static final int METER_TEXT_CAP = 4_000;

    private final TokenMeter tokenMeter;
    /** ObjectProvider 延迟解析：工作流引擎节点依赖链含网关（Agent 节点收口），防 Bean 循环 */
    private final ObjectProvider<WorkflowService> workflowServiceProvider;

    /**
     * 执行绑定工作流：input 全量透传 + userInput/userId 固定键注入（工作流节点
     * 变量引用契约），输出按 GenericSceneData 包装（String→content，Map→structured）。
     */
    public AiExecuteResponse<?> execute(AiExecuteRequest request, AiSceneConfig config) {
        long start = System.currentTimeMillis();
        String sceneCode = config.getSceneCode();
        WorkflowService workflowService = workflowServiceProvider.getIfAvailable();
        if (workflowService == null) {
            return AiExecuteResponse.failure(AiErrorCodes.AI_CALL_FAILED, "工作流服务不可用");
        }

        Map<String, Object> wfInput = buildWorkflowInput(request);

        WorkflowEngine.WorkflowResult result;
        try {
            result = workflowService.execute(config.getWorkflowId(), wfInput);
        } catch (Exception e) {
            log.error("[aigateway:workflow] 工作流执行异常: scene={}, workflowId={}, elapsed={}ms",
                    sceneCode, config.getWorkflowId(), System.currentTimeMillis() - start, e);
            return AiExecuteResponse.failure(AiErrorCodes.AI_CALL_FAILED,
                    "工作流执行失败: " + e.getMessage());
        }
        long elapsed = System.currentTimeMillis() - start;

        if (result == null || !result.isSuccess()) {
            String err = result != null && result.getErrorMessage() != null
                    ? result.getErrorMessage() : "未知原因";
            log.warn("[aigateway:workflow] 工作流执行失败: scene={}, workflowId={}, err={}, elapsed={}ms",
                    sceneCode, config.getWorkflowId(), err, elapsed);
            return AiExecuteResponse.failure(AiErrorCodes.AI_CALL_FAILED, "工作流执行失败: " + err);
        }

        GenericSceneData data = wrapOutput(result.getOutput());
        AiExecuteResponse<GenericSceneData> response = AiExecuteResponse.success(data);
        response.setMetadata(envelopeMetering(request, result));
        log.info("[aigateway:workflow] 完成: scene={}, workflowId={}, executionId={}, nodes={}, duration={}ms, elapsed={}ms",
                sceneCode, config.getWorkflowId(), result.getExecutionId(),
                result.getNodeLogs() != null ? result.getNodeLogs().size() : 0,
                result.getDurationMs(), elapsed);
        return response;
    }

    /**
     * 工作流输入组装：input 全量透传（模板/变量引用）+ userInput（自由文本契约键）
     * + userId（归属注入——内部 Agent 节点经它落执行日志与限流身份）。
     * 已有同名键不覆盖（调用方显式传参优先）。
     */
    private Map<String, Object> buildWorkflowInput(AiExecuteRequest request) {
        Map<String, Object> wfInput = new LinkedHashMap<>();
        if (request.getInput() != null) {
            wfInput.putAll(request.getInput());
        }
        if (request.getUserInput() != null && !request.getUserInput().isBlank()) {
            wfInput.putIfAbsent("userInput", request.getUserInput());
        }
        if (request.getUserId() != null) {
            wfInput.putIfAbsent("userId", request.getUserId());
        }
        return wfInput;
    }

    /**
     * 工作流输出包装：String → content（文本消费方经 AiSceneJsonClient.unwrapContent
     * 透明读取）；Map → structured（结构化消费方经 unwrapStructured 读取，若含
     * content 字符串键则同时填 content）；其余类型 → String.valueOf 兜底 content。
     */
    @SuppressWarnings("unchecked")
    private GenericSceneData wrapOutput(Object output) {
        GenericSceneData data = new GenericSceneData();
        data.setSource("workflow");
        if (output instanceof Map<?, ?> map) {
            data.setStructured((Map<String, Object>) map);
            if (map.get("content") instanceof String content && !content.isBlank()) {
                data.setContent(content);
            }
        } else if (output != null) {
            data.setContent(String.valueOf(output));
        }
        return data;
    }

    /**
     * 信封估算计量：工作流多节点（LLM/代码/条件分支）的真实 usage 在网关层无法
     * 聚合（内部 LLM 节点经 workflow_agent_node 场景独立计量），外层按
     * 输入/输出信封本地估算并打 tokenEstimated=true——外层场景日配额（成本熔断）
     * 与成本报表依赖该值，避免"工作流场景静默 0 Token"失真。
     */
    private AiMetadata envelopeMetering(AiExecuteRequest request, WorkflowEngine.WorkflowResult result) {
        AiMetadata metadata = new AiMetadata();
        metadata.setTokenEstimated(true);
        try {
            String inputText = serializeCapped(request.getInput());
            String outputText = result.getOutput() != null
                    ? cap(String.valueOf(result.getOutput())) : "";
            TokenMeter.Metered metered = tokenMeter.meterTexts(inputText, outputText);
            metadata.setInputTokens(metered.inputTokens());
            metadata.setOutputTokens(metered.outputTokens());
            metadata.setTokenUsed(metered.totalTokens());
        } catch (Exception e) {
            log.warn("[aigateway:workflow] 信封估算失败（不影响业务）: {}", e.getMessage());
        }
        return metadata;
    }

    /** input 规范化 JSON（TreeMap 稳定键序），超长截断（仅计量口径） */
    private String serializeCapped(Map<String, Object> input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        try {
            return cap(MAPPER.writeValueAsString(new TreeMap<>(input)));
        } catch (Exception e) {
            return cap(String.valueOf(input));
        }
    }

    private String cap(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > METER_TEXT_CAP ? text.substring(0, METER_TEXT_CAP) : text;
    }
}
