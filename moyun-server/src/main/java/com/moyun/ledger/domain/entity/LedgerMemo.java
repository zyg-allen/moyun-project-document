package com.moyun.ledger.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 记账-备忘录（首页待办事项与备忘录模块共用一张表）
 *
 * <p>v11.34 增强：事项标题/内容/事项时间/是否提醒/提醒方式/重要程度。
 * 提醒由 LedgerMemoRemindTask 定时扫描 event_time - 提前量 到达的事项，
 * 经 INotificationService 发送站内通知，reminded 防重。
 *
 * @author moyun
 */
@Data
@TableName("ledger_memo")
public class LedgerMemo {

    /** 提醒方式：准时提醒 */
    public static final String RULE_ON_TIME = "on_time";
    /** 提醒方式：提前30分钟 */
    public static final String RULE_ADVANCE_30M = "advance_30m";
    /** 提醒方式：提前1小时 */
    public static final String RULE_ADVANCE_1H = "advance_1h";
    /** 提醒方式：提前2小时 */
    public static final String RULE_ADVANCE_2H = "advance_2h";
    /** 提醒方式：提前1天 */
    public static final String RULE_ADVANCE_1D = "advance_1d";
    /** 提醒方式：提前一天上午9点 */
    public static final String RULE_ADVANCE_1D_9AM = "advance_1d_9am";

    /** 重要程度：不重要 */
    public static final String IMPORTANCE_LOW = "low";
    /** 重要程度：一般 */
    public static final String IMPORTANCE_NORMAL = "normal";
    /** 重要程度：重要 */
    public static final String IMPORTANCE_HIGH = "high";
    /** 重要程度：紧急 */
    public static final String IMPORTANCE_URGENT = "urgent";

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 门户用户ID（portal_user.id） */
    private Long userId;

    /** 事项标题 */
    private String title;

    /** 待办内容 */
    private String content;

    /** 完成状态：1=已完成 0=未完成 */
    private Integer done;

    /** 事项时间（提醒基准时间） */
    private LocalDateTime eventTime;

    /** 是否提醒：1=是 0=否 */
    private Integer remindEnabled;

    /** 提醒方式：on_time/advance_30m/advance_1h/advance_2h/advance_1d/advance_1d_9am */
    private String remindRule;

    /** 重要程度：low/normal/high/urgent */
    private String importance;

    /** 提醒是否已发送：1=已发 0=未发（防重复） */
    private Integer reminded;

    /** 创建日期 */
    private LocalDate todoDate;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}