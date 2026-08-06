package com.moyun.ext.ai.engine.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工具执行上下文
 *
 * <p>包含工具执行时需要的上下文信息</p>
 *
 * @author laomao
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ToolContext {

    /**
     * 智能体ID
     */
    private Long agentId;

    /**
     * 会话ID
     */
    private Long conversationId;

    /**
     * 消息ID
     */
    private Long messageId;

    /**
     * 用户ID（预留）
     */
    private String userId;

    /**
     * 用户原始问题
     */
    private String userQuery;

    /**
     * 工作流ID（工作流执行时使用）
     */
    private Long workflowId;
}
