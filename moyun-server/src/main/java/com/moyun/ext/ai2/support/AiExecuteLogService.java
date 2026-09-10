package com.moyun.ext.ai2.support;

import com.moyun.ext.ai2.entity.AiExecuteLog;
import com.moyun.ext.ai2.mapper.AiExecuteLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * AI执行日志服务（可观测性）
 *
 * <p>异步落库 ai2_execute_log，为统一网关提供全链路追踪数据。
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

    /**
     * 异步记录执行日志（v11.51：响应 metadata 的模型/Agent/token 同步落 ai_execute_log，
     * 与响应可观测性闭环——日志表 model_used/agent_used/token_used 列自此有数据）
     */
    @Async
    public void record(String requestId, String sceneCode, String handlerName, String bindType,
                       com.moyun.ext.ai2.model.AiMetadata metadata,
                       String inputSummary, String outputSummary,
                       String status, String errorMsg, long elapsedMs) {
        try {
            AiExecuteLog logEntry = new AiExecuteLog();
            logEntry.setRequestId(requestId);
            logEntry.setSceneCode(sceneCode);
            logEntry.setHandlerName(handlerName);
            logEntry.setBindType(bindType);
            if (metadata != null) {
                logEntry.setModelUsed(metadata.getModelUsed());
                logEntry.setAgentUsed(metadata.getAgentUsed());
                logEntry.setTokenUsed(metadata.getTokenUsed());
            }
            logEntry.setInputSummary(abbreviate(inputSummary, 500));
            logEntry.setOutputSummary(abbreviate(outputSummary, 500));
            logEntry.setStatus(status);
            logEntry.setErrorMsg(abbreviate(errorMsg, 2000));
            logEntry.setElapsedMs(elapsedMs);
            logEntry.setCreateTime(LocalDateTime.now());
            executeLogMapper.insert(logEntry);
        } catch (Exception e) {
            log.warn("[ai2:log] 执行日志落库失败（不影响业务）: {}", e.getMessage());
        }
    }

    private String abbreviate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }
}
