package com.moyun.ledger.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 记账-存钱计划
 *
 * <p>method 说明：
 * 52week=52周存钱法（第n周存10n元，52周累计13780元）；
 * fixed=固定周期存钱（每期固定金额）；monthly=每月固定存；custom=自定义递增规则。
 *
 * <p>status 说明：1=进行中 2=成功 3=失败 0=已删除。
 * 计划支持手动标记成功/失败；失败后可重新开始（重置回进行中）或调整计划。
 *
 * @author moyun
 */
@Data
@TableName("ledger_saving_plan")
public class LedgerSavingPlan {

    /** 存钱方式：52周存钱法 */
    public static final String METHOD_52WEEK = "52week";
    /** 存钱方式：固定周期存钱 */
    public static final String METHOD_FIXED = "fixed";
    /** 存钱方式：每月固定存 */
    public static final String METHOD_MONTHLY = "monthly";
    /** 存钱方式：自定义递增规则 */
    public static final String METHOD_CUSTOM = "custom";

    /** 状态：进行中 */
    public static final int STATUS_RUNNING = 1;
    /** 状态：成功 */
    public static final int STATUS_SUCCESS = 2;
    /** 状态：失败 */
    public static final int STATUS_FAILED = 3;
    /** 状态：已删除 */
    public static final int STATUS_DELETED = 0;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 门户用户ID（portal_user.id） */
    private Long userId;

    /** 计划名称 */
    private String name;

    /** 存钱方式：52week/fixed/monthly/custom */
    private String method;

    /** 目标金额（元） */
    private BigDecimal targetAmount;

    /** 当前已存金额（元） */
    private BigDecimal currentAmount;

    /** 每期金额（元；fixed/monthly 用） */
    private BigDecimal periodAmount;

    /** 总期数（custom 自定义规则期数） */
    private Integer periodCount;

    /** 每期递增金额（元；custom 自定义递增规则用） */
    private BigDecimal increaseStep;

    /** 开始日期 */
    private LocalDate startDate;

    /** 结束日期（可选） */
    private LocalDate endDate;

    /** 备注 */
    private String remark;

    /** 状态：1=进行中 2=成功 3=失败 0=已删除 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
