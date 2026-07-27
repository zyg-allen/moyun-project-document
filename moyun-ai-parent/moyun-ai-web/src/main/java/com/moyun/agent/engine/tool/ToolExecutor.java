package com.moyun.agent.engine.tool;

import java.util.Map;

/**
 * 工具执行器接口
 *
 * <p>所有内置工具都需要实现此接口</p>
 *
 * @author laomao
 */
public interface ToolExecutor {

    /**
     * 获取工具名称（唯一标识）
     *
     * @return 工具名称
     */
    String getName();

    /**
     * 获取工具描述（给LLM理解用）
     *
     * @return 工具描述
     */
    String getDescription();

    /**
     * 获取参数定义（JSON Schema格式）
     *
     * @return 参数定义JSON
     */
    String getParametersSchema();

    /**
     * 执行工具
     *
     * @param context 执行上下文
     * @param params 调用参数
     * @return 执行结果
     */
    ToolResult execute(ToolContext context, Map<String, Object> params);

    /**
     * 是否支持异步执行
     *
     * @return 默认false
     */
    default boolean isAsync() {
        return false;
    }

    /**
     * 获取超时时间（秒）
     *
     * @return 默认30秒
     */
    default int getTimeoutSeconds() {
        return 30;
    }
}
