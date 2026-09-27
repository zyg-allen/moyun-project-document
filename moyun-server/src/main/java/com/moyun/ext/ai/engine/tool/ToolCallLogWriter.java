package com.moyun.ext.ai.engine.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.ext.ai.entity.ToolCallLog;
import com.moyun.ext.ai.mapper.ToolCallLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 工具调用日志写入器（{@code @Async} 必须在本类——独立 Bean 里才能生效）
 *
 * <h3>为什么单独一个类</h3>
 * <p>该写入原先位于 {@code ToolRegistry#logToolCallAsync}，并被 {@code ToolRegistry.executeTool}
 * **同类内部调用**。Spring 的 {@code @Async} 依赖 AOP 代理，同类自调用会绕过代理 →
 * 方法**退化为同步执行**（不报错、无日志，只是默默把一次 DB 插入放回请求线程），
 * 而类头注释、方法名与 {@code AsyncConfig}/{@code AsyncTaskConfig} 的说明都宣称它是异步的。</p>
 *
 * <p>把 {@code @Async} 方法抽到独立 Bean 由调用方注入调用，是本项目已确立的修法
 * （参见 {@code AiTaskAsyncExecutor} 的类注释）。结构守卫：
 * {@code AsyncSelfInvocationGuardTest} 会扫描全部源码，禁止任何 {@code @Async} 方法被同类自调用。</p>
 *
 * @author laomao
 */
@Slf4j
@Component
public class ToolCallLogWriter {

    @Autowired
    private ToolCallLogMapper toolCallLogMapper;

    /** JSON 解析器（工具参数序列化） */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 异步记录工具调用日志（不阻塞工具执行主流程）
     *
     * <p>写库失败只记 debug：工具调用日志是旁路可观测性数据，不得影响业务结果。</p>
     */
    @Async
    public void logToolCallAsync(ToolContext context, String toolName,
                                 Map<String, Object> params, ToolResult result,
                                 String status, String errorMessage) {
        try {
            ToolCallLog callLog = ToolCallLog.builder()
                    .conversationId(context.getConversationId())
                    .messageId(context.getMessageId())
                    .agentId(context.getAgentId())
                    .toolName(toolName)
                    .inputParams(objectMapper.writeValueAsString(params))
                    .outputResult(result.getContent())
                    .status(status)
                    .errorMessage(errorMessage)
                    .durationMs((int) result.getDurationMs())
                    .createTime(LocalDateTime.now())
                    .build();

            toolCallLogMapper.insert(callLog);
        } catch (Exception e) {
            // 日志记录失败不影响主流程
            log.debug("工具调用日志记录失败: {}", e.getMessage());
        }
    }
}
