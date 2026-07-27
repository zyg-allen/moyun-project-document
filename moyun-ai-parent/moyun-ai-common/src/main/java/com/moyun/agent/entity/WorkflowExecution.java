package com.moyun.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 工作流执行记录实体
 *
 * @author laomao
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("workflow_execution")
public class WorkflowExecution {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工作流ID */
    private Long workflowId;

    /** 执行状态: running-执行中, completed-已完成, failed-失败, cancelled-已取消 */
    private String status;

    /** 输入参数(JSON) */
    private String inputData;

    /** 输出结果(JSON) */
    private String outputData;

    /** 执行日志(JSON数组，记录每个节点的执行情况) */
    private String executionLog;

    /** 错误信息 */
    private String errorMessage;

    /** 当前执行到的节点ID */
    private String currentNodeId;

    /** 执行耗时(毫秒) */
    private Long durationMs;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /**
     * Token使用记录实体
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @TableName("token_usage_log")
    public static class TokenUsageLog {

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
         * 工作流ID
         */
        private Long workflowId;

        /**
         * 工作流执行ID
         */
        private Long workflowExecutionId;

        /**
         * 工作流节点ID
         */
        private String workflowNodeId;

        /**
         * 用户ID
         */
        private String userId;

        /**
         * 模型名称
         */
        private String modelName;

        /**
         * 模型提供商
         */
        private String modelProvider;

        /**
         * 输入token数
         */
        private Integer inputTokens;

        /**
         * 输出token数
         */
        private Integer outputTokens;

        /**
         * 总token数
         */
        private Integer totalTokens;

        /**
         * 费用（元）
         */
        private BigDecimal cost;

        /**
         * 请求类型：chat/embedding/rerank/workflow_llm/workflow_classifier/workflow_extractor/workflow_question
         */
        private String requestType;

        /**
         * 创建时间
         */
        private LocalDateTime createTime;
    }

    /**
     * Token使用统计汇总实体
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @TableName("token_usage_summary")
    public static class TokenUsageSummary {

        @TableId(type = IdType.AUTO)
        private Long id;

        /**
         * 智能体ID
         */
        private Long agentId;

        /**
         * 用户ID
         */
        private String userId;

        /**
         * 统计日期
         */
        private LocalDate statDate;

        /**
         * 模型名称
         */
        private String modelName;

        /**
         * 请求次数
         */
        private Integer totalRequests;

        /**
         * 总输入token
         */
        private Long totalInputTokens;

        /**
         * 总输出token
         */
        private Long totalOutputTokens;

        /**
         * 总token
         */
        private Long totalTokens;

        /**
         * 总费用
         */
        private BigDecimal totalCost;

        /**
         * 创建时间
         */
        private LocalDateTime createTime;

        /**
         * 更新时间
         */
        private LocalDateTime updateTime;
    }
}
