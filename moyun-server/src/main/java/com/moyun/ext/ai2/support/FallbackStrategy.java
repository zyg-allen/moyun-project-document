package com.moyun.ext.ai2.support;

import com.moyun.ext.ai2.constant.AiErrorCodes;
import com.moyun.ext.ai2.model.AiExecuteResponse;
import com.moyun.ext.ai2.model.data.InterviewSceneData;
import com.moyun.ext.ai2.model.data.SensitiveWordSceneData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 降级策略（高可用兜底）
 *
 * <p>依据《AI能力统一接入层 — 完整方案文档》V2.0 §7.3。Handler 执行异常时按场景返回业务兜底，
 * 优先使用场景配置的 fallback_response，未配置时使用内置兜底。</p>
 *
 * @author laomao
 * @since 2026-09-09
 */
@Slf4j
@Component
public class FallbackStrategy {

    /**
     * 执行降级
     *
     * @param scene            场景代码
     * @param configuredFallback 场景配置的兜底回复（JSON，可为 null）
     * @param e                触发降级的异常
     */
    public AiExecuteResponse<?> executeFallback(String scene, String configuredFallback, Exception e) {
        log.warn("[ai2:fallback] 场景({})降级: {}", scene, e.getMessage());

        // 1. 场景配置的兜底回复优先
        if (configuredFallback != null && !configuredFallback.isBlank()) {
            AiExecuteResponse<Object> resp = new AiExecuteResponse<>();
            resp.setCode(AiErrorCodes.SUCCESS);
            resp.setMsg("fallback");
            resp.setData(parseFallbackData(configuredFallback));
            return resp;
        }

        // 2. 内置兜底（保证调用方拿到的结构可解析）
        return switch (scene) {
            case "voice_interview", "interview" -> {
                InterviewSceneData data = new InterviewSceneData();
                data.setNextAction("end");
                data.setEvaluation("AI服务暂时不可用，本次评估已跳过，请稍后重试");
                yield AiExecuteResponse.success(data);
            }
            case "sensitive_word" -> {
                SensitiveWordSceneData data = new SensitiveWordSceneData();
                data.setHasSensitive(false);
                data.setRiskLevel("low");
                data.setSuggestion("AI服务暂时不可用，已跳过检测");
                yield AiExecuteResponse.success(data);
            }
            default -> AiExecuteResponse.failure(AiErrorCodes.AI_CALL_FAILED,
                    "AI服务暂时不可用，请稍后重试");
        };
    }

    /**
     * 解析配置的兜底 JSON（非法 JSON 时原样作为文本返回）
     */
    private Object parseFallbackData(String fallbackJson) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(fallbackJson, Object.class);
        } catch (Exception e) {
            return fallbackJson;
        }
    }
}
