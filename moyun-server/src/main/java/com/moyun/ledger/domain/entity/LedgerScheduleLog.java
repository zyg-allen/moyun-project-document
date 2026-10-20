package com.moyun.ledger.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 记账-定时记账执行日志（成功生成流水 / 失败留痕，支持手动重试）
 *
 * @author moyun
 */
@Data
@TableName("ledger_schedule_log")
public class LedgerScheduleLog {

    /** 状态：成功 */
    public static final int STATUS_SUCCESS = 1;
    /** 状态：失败 */
    public static final int STATUS_FAILED = 2;

    /** 触发来源：自动调度 */
    public static final int TRIGGER_AUTO = 1;
    /** 触发来源：页面主动执行（立即执行 / 重试） */
    public static final int TRIGGER_MANUAL = 2;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 任务ID（ledger_schedule_task.id） */
    private Long taskId;

    /** 门户用户ID（冗余） */
    private Long userId;

    /** 执行日期 */
    private LocalDate execDate;

    /** 金额（元） */
    private BigDecimal amount;

    /** 状态：1=成功 2=失败 */
    private Integer status;

    /** 失败原因（如余额不足） */
    private String failReason;

    /** 生成的流水ID（ledger_transaction.id） */
    private Long transactionId;

    /** 重试次数 */
    private Integer retryCount;

    /**
     * 触发来源：1=自动调度到点执行，2=页面主动「立即执行」。
     * 两者在业务上含义不同（主动执行是用户当场补记），日志需要分开呈现。
     */
    private Integer triggerType;

    private LocalDateTime createTime;
}
