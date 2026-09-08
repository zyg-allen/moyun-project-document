package com.moyun.ledger.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 记账-存钱流水（每个计划的期次存入记录）
 *
 * <p>status 说明：0=待存 1=成功 2=失败（如余额不足）。
 * 明确记录每次存入是否成功或失败，失败时 fail_reason 记录原因。
 *
 * @author moyun
 */
@Data
@TableName("ledger_saving_record")
public class LedgerSavingRecord {

    /** 状态：待存 */
    public static final int STATUS_PENDING = 0;
    /** 状态：成功 */
    public static final int STATUS_SUCCESS = 1;
    /** 状态：失败 */
    public static final int STATUS_FAILED = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 计划ID（ledger_saving_plan.id） */
    private Long planId;

    /** 门户用户ID（冗余，数据隔离校验用） */
    private Long userId;

    /** 期次序号（第几期/第几周） */
    private Integer periodIndex;

    /** 本期应存金额（元） */
    private BigDecimal targetAmount;

    /** 实际存入金额（元；成功时记录） */
    private BigDecimal amount;

    /** 状态：0=待存 1=成功 2=失败 */
    private Integer status;

    /** 失败原因（如余额不足） */
    private String failReason;

    /** 备注 */
    private String remark;

    /** 存入/失败日期 */
    private LocalDate recordDate;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
