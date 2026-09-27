package com.moyun.ext.aigateway.support;

import com.moyun.ext.aigateway.entity.AiExecuteLog;
import com.moyun.ext.aigateway.mapper.AiExecuteLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * AI执行日志服务（可观测性）
 *
 * <p>异步落库 ai_execute_log，为统一网关提供全链路追踪数据。
 * 依据《AI能力统一接入层 — 完整方案文档》V2.0 §3.2 / Phase 5。</p>
 *
 * <p>日志写入失败不影响业务主流程（吞异常记 warn）。</p>
 *
 * @author laomao
 * @since 2026-09-09
 */
@Slf4j
@Service
public class AiExecuteLogService {

    @Autowired
    private AiExecuteLogMapper executeLogMapper;

    /** 成本核算复用老链路口径（ai_model_config 单价 + 价格缓存），保证与 Token 统计页一致 */
    @Autowired(required = false)
    private com.moyun.ext.ai.service.TokenUsageService tokenUsageService;

    /**
     * 异步记录执行日志（响应 metadata 的模型/Agent/token 同步落 ai_execute_log，
     * 与响应可观测性闭环——日志表 model_used/agent_used/token_used 列自此有数据；
     * cost_yuan 按 metadata 细分 token × 模型单价核算落库）
     *
     * <p>带用户维度（网关 {@code request.getUserId()} 直取，支撑 AI 消费按用户统计；
     * 系统内部调用 userId 为空）。</p>
     *
     * <p><b>v13.4 清理</b>：原有一个不带 userId 的 10 参重载，其方法体内直接调用本方法
     * —— 属"同类自调用"，会绕过 Spring 代理使 {@code @Async} 静默失效；且该重载**
     * 已无任何调用方**（全部调用点都传 userId），故连同自调用陷阱一并删除。
     * 结构守卫：{@code AsyncSelfInvocationGuardTest} 会禁止任何 {@code @Async} 方法被同类自调用。</p>
     */
    @Async
    public void record(String requestId, Long userId, String sceneCode, String handlerName, String bindType,
                       com.moyun.ext.aigateway.model.AiMetadata metadata,
                       String inputSummary, String outputSummary,
                       String status, String errorMsg, long elapsedMs) {
        try {
            AiExecuteLog logEntry = new AiExecuteLog();
            logEntry.setRequestId(requestId);
            logEntry.setUserId(userId);
            logEntry.setSceneCode(sceneCode);
            logEntry.setHandlerName(handlerName);
            logEntry.setBindType(bindType);
            if (metadata != null) {
                logEntry.setModelUsed(metadata.getModelUsed());
                logEntry.setAgentUsed(metadata.getAgentUsed());
                logEntry.setTokenUsed(metadata.getTokenUsed());
                logEntry.setInputTokens(metadata.getInputTokens());
                logEntry.setOutputTokens(metadata.getOutputTokens());
                // 估算标记（v13.3）：区分"服务端真实 usage"与"本地分词估算"，报表/看板不得混用
                logEntry.setTokenEstimated(Boolean.TRUE.equals(metadata.getTokenEstimated()) ? 1 : 0);
                logEntry.setCostYuan(calculateCostYuan(metadata));
            }
            logEntry.setInputSummary(abbreviate(inputSummary, 500));
            logEntry.setOutputSummary(abbreviate(outputSummary, 500));
            logEntry.setStatus(status);
            logEntry.setErrorMsg(abbreviate(errorMsg, 2000));
            logEntry.setElapsedMs(elapsedMs);
            logEntry.setCreateTime(LocalDateTime.now());
            executeLogMapper.insert(logEntry);
        } catch (Exception e) {
            log.warn("[aigateway:log] 执行日志落库失败（不影响业务）: {}", e.getMessage());
        }
    }

    /**
     * 单次调用成本（元）：细分 token 按单价核算；仅有合计时保守按输出单价估算（上界）；
     * 无 token / 核算服务不可用返回 null（留空不误导）
     */
    private java.math.BigDecimal calculateCostYuan(com.moyun.ext.aigateway.model.AiMetadata metadata) {
        if (tokenUsageService == null || metadata.getModelUsed() == null) {
            return null;
        }
        try {
            Integer in = metadata.getInputTokens();
            Integer out = metadata.getOutputTokens();
            if (in != null || out != null) {
                return tokenUsageService.calculateCost(metadata.getModelUsed(),
                        in != null ? in : 0, out != null ? out : 0);
            }
            // 部分供应商仅回传合计：按输出单价保守估算（输出价通常高于输入价，估上界）
            if (metadata.getTokenUsed() != null) {
                return tokenUsageService.calculateCost(metadata.getModelUsed(), 0, metadata.getTokenUsed());
            }
            return null;
        } catch (Exception e) {
            log.warn("[aigateway:log] 成本核算失败（留空）: {}", e.getMessage());
            return null;
        }
    }

    private String abbreviate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }
}
