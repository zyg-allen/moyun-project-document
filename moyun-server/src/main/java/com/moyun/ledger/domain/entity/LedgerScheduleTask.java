package com.moyun.ledger.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 记账-定时记账任务
 *
 * <p>cycle 说明：daily=每天；weekly=每周几（day_of_week 1-7）；
 * monthly=每月几号（day_of_month 1-28）；interval=每N天一次（interval_days）。
 *
 * <p>next_exec_date 由定时任务扫描推进；end_date 到期后自动停用（enabled=0）。
 *
 * @author moyun
 */
@Data
@TableName("ledger_schedule_task")
public class LedgerScheduleTask {

    /** 周期：每天 */
    public static final String CYCLE_DAILY = "daily";
    /** 周期：每周几 */
    public static final String CYCLE_WEEKLY = "weekly";
    /** 周期：每月几号 */
    public static final String CYCLE_MONTHLY = "monthly";
    /** 周期：每N天一次 */
    public static final String CYCLE_INTERVAL = "interval";

    /** 类型：收入 */
    public static final String TYPE_INCOME = "income";
    /** 类型：支出 */
    public static final String TYPE_EXPENSE = "expense";

    /** 状态：正常 */
    public static final int STATUS_NORMAL = 1;
    /** 状态：已删除 */
    public static final int STATUS_DELETED = 0;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 门户用户ID（portal_user.id） */
    private Long userId;

    /** 任务名称 */
    private String name;

    /** 记账类型：income/expense */
    private String type;

    /** 金额（元） */
    private BigDecimal amount;

    /** 分类ID（ledger_category） */
    private Long categoryId;

    /** 关联资产账户ID（支出扣款/收入入账） */
    private Long accountId;

    /** 备注 */
    private String description;

    /** 执行周期：daily/weekly/monthly/interval */
    private String cycle;

    /** 每周几（1-7，weekly 用） */
    private Integer dayOfWeek;

    /** 每月几号（1-28，monthly 用） */
    private Integer dayOfMonth;

    /** 间隔天数（interval 用） */
    private Integer intervalDays;

    /** 执行时间 HH:mm */
    private String execTime;

    /** 开始日期 */
    private LocalDate startDate;

    /** 结束日期（可选，到期自动停用） */
    private LocalDate endDate;

    /** 下次执行日期 */
    private LocalDate nextExecDate;

    /** 启用状态：1=启用 0=停用 */
    private Integer enabled;

    /** 状态：1=正常 0=已删除 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    // ---------------- 列表展示用冗余字段（非表字段，查询后填充） ----------------

    /** 分类名称（展示用） */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String categoryName;

    /** 账户名称（展示用） */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String accountName;
}
