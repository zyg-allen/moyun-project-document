package com.moyun.ext.ai2.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI统一接入层-执行日志实体
 *
 * <p>对应表 ai_execute_log。依据《AI能力统一接入层 — 完整方案文档》V2.0 §3.2。</p>
 *
 * @author laomao
 * @since 2026-09-09
 */
@Data
@TableName("ai_execute_log")
public class AiExecuteLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 请求ID */
    @TableField("request_id")
    private String requestId;

    /** 场景代码 */
    @TableField("scene_code")
    private String sceneCode;

    /** Handler名称 */
    @TableField("handler_name")
    private String handlerName;

    /** 绑定类型 */
    @TableField("bind_type")
    private String bindType;

    /** 使用的模型 */
    @TableField("model_used")
    private String modelUsed;

    /** 使用的Agent */
    @TableField("agent_used")
    private String agentUsed;

    /** Token消耗 */
    @TableField("token_used")
    private Integer tokenUsed;

    /** 本次调用成本（元，v11.57 P0-2：metadata 细分 token × 模型单价，6位小数；模型未回传 token 时为 null） */
    @TableField("cost_yuan")
    private java.math.BigDecimal costYuan;

    /** 工具调用记录（JSON） */
    @TableField("tool_calls")
    private String toolCalls;

    /** 输入摘要 */
    @TableField("input_summary")
    private String inputSummary;

    /** 输出摘要 */
    @TableField("output_summary")
    private String outputSummary;

    /** 状态: success/fail/timeout */
    @TableField("status")
    private String status;

    /** 错误信息 */
    @TableField("error_msg")
    private String errorMsg;

    /** 耗时（毫秒） */
    @TableField("elapsed_ms")
    private Long elapsedMs;

    /** 创建时间（v11.49.1 修复：对齐 DDL 列名 create_time，原 created_at 与表结构不符导致落库失败） */
    @TableField("create_time")
    private LocalDateTime createTime;
}
