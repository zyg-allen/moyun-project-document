package com.moyun.ext.aigateway.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.ext.aigateway.constant.AiErrorCodes;
import com.moyun.ext.aigateway.model.AiExecuteRequest;
import com.moyun.ext.aigateway.model.AiExecuteResponse;
import com.moyun.ext.aigateway.model.data.InterviewSceneData;
import com.moyun.ext.aigateway.service.AiGatewayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 场景结构化 JSON 调用客户端（业务收口配套）
 *
 * <p><b>职责边界（v12.2 统一入口原则）</b>：本类是<b>业务方外部入口</b>——业务 Service
 * 构造 input Map 后调用 {@code executeForJson(sceneCode, input, userId)}，经
 * {@link com.moyun.ext.aigateway.service.AiGatewayService} 统一网关（治理：限流/熔断/缓存/日志/降级）
 * 路由到对应 {@link com.moyun.ext.aigateway.handler.AiSceneHandler}。Handler 内部如需调底层 LLM
 * 用 {@link com.moyun.ext.aigateway.handler.AbstractAiSceneHandler#chatJson}（Handler 内部方法）。
 * 二者层次不同：本类=外部便捷入口，AbstractAiSceneHandler.chatJson=Handler 内部工具，不混用。</p>
 *
 * <p>业务服务（简历解析/优化/出题/语音面试等）收口统一网关后的便捷通道：构造请求 → 网关执行 →
 * 解包场景 Data 的 structured 载体为 {@link JsonNode}，业务侧直接
 * {@code path(...)} 导航或 {@code treeToValue} 反序列化为自有 VO。</p>
 *
 * <p><strong>失败语义</strong>：任何失败（场景未配置/限流/Token熔断/模型调用失败/JSON解析失败/
 * 降级兜底响应）统一返回 {@code null}——业务保留原有规则兜底逻辑（收口前 LLM 异常也是走兜底，
 * 行为语义不变）。需要区分失败原因的场景请直调 {@link AiGatewayService#execute}。</p>
 *
 * <p>输入约定：{@code input.task} 指定 Handler 子任务，{@code input.context} 为业务数据文本
 * （Handler 侧经 PromptInjectionGuard.wrapData 数据隔离）。userId 用于限流身份与日志归属，
 * 可空（匿名桶）。</p>
 *
 * <p><strong>解包泛化（2B.3 后口径）</strong>：配置驱动场景（DefaultSceneExecutor）统一返回
 * GenericSceneData.structured；InterviewSceneData（语音面试子任务，2B.5 前仍在）同样以
 * structured Map 透传。</p>
 *
 * @author laomao
 * @since 2026-09-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiSceneJsonClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AiGatewayService gateway;

    /**
     * 执行场景并返回结构化 JSON 结果
     *
     * @param sceneCode 场景代码（ai_scene_config.scene_code）
     * @param input     场景输入（task/context 及其他结构化参数）
     * @param userId    归属用户（限流身份/日志，可空）
     * @return 结构化 JSON；任何失败返回 null（业务走既有兜底）
     */
    public JsonNode executeForJson(String sceneCode, Map<String, Object> input, Long userId) {
        try {
            AiExecuteRequest request = new AiExecuteRequest();
            request.setSceneCode(sceneCode);
            request.setInput(input != null ? input : new HashMap<>());
            request.setUserId(userId);
            AiExecuteResponse<?> resp = gateway.execute(request);
            if (resp.getCode() == null || resp.getCode() != AiErrorCodes.SUCCESS) {
                log.warn("[aigateway:JsonClient] 场景执行未成功: scene={}, code={}, msg={}",
                        sceneCode, resp.getCode(), resp.getMsg());
                return null;
            }
            Map<String, Object> structured = unwrapStructured(resp.getData());
            if (structured == null || structured.isEmpty()) {
                log.warn("[aigateway:JsonClient] 场景执行未得结构化结果: scene={}, dataType={}",
                        sceneCode, resp.getData() == null ? "null" : resp.getData().getClass().getSimpleName());
                return null;
            }
            return MAPPER.valueToTree(structured);
        } catch (Exception e) {
            log.warn("[aigateway:JsonClient] 场景执行异常: scene={}, {}", sceneCode, e.getMessage());
            return null;
        }
    }

    /** 解包场景 Data 的 structured 载体（按场景 Data 类型分派，未承载结构化结果返回 null） */
    private Map<String, Object> unwrapStructured(Object data) {
        if (data instanceof InterviewSceneData interview && interview.getStructured() != null) {
            return interview.getStructured();
        }
        if (data instanceof com.moyun.ext.aigateway.model.data.GenericSceneData generic
                && generic.getStructured() != null) {
            return generic.getStructured();
        }
        return null;
    }
}
