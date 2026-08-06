package com.moyun.ext.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 工具调用日志实体类
 *
 * <p>对应数据库表 tool_call_log，记录工具调用历史</p>
 *
 * @author laomao
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("tool_call_log")
public class ToolCallLog {
    
    /**
     * 日志ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会话ID
     */
    private Long conversationId;

    /**
     * 消息ID
     */
    private Long messageId;

    /**
     * 智能体ID
     */
    private Long agentId;

    /**
     * 工具ID
     */
    private Long toolId;

    /**
     * 工具名称
     */
    private String toolName;

    /**
     * 输入参数（JSON格式）
     */
    private String inputParams;

    /**
     * 输出结果
     */
    private String outputResult;

    /**
     * 状态：pending/running/success/failed/timeout
     */
    private String status;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 执行耗时（毫秒）
     */
    private Integer durationMs;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
