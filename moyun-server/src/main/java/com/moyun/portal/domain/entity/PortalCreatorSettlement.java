package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.moyun.core.base.BaseEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创作者分成结算单
 *
 * <p>状态机：pending（待确认）-> confirmed（已确认）-> paid（已打款）。
 * 每位创作者每月唯一一条结算单（uk_creator_period）。
 * 收入聚合来源：
 *   tip_income        portal_tip_order（target_type=article/column, status=paid, paid_time 在周期内）
 *   paid_read_income  portal_tip_order（target_type=article_paid, status=paid, paid_time 在周期内）
 *   column_income     portal_column_subscribe + portal_order（专栏订阅付费，简化：取 portal_tip_order target_type=column_subscribe）
 * </p>
 *
 * @author moyun
 */
@Data
@TableName("portal_creator_settlement")
public class PortalCreatorSettlement extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 创作者用户ID */
    private Long creatorId;

    /** 结算周期，格式 yyyy-MM，如 2026-07 */
    private String period;

    /** 打赏收入（当月已支付打赏总额） */
    private BigDecimal tipIncome;

    /** 付费阅读收入（当月已支付购买总额） */
    private BigDecimal paidReadIncome;

    /** 专栏订阅收入（当月已支付订阅总额） */
    private BigDecimal columnIncome;

    /** 总收入（三项之和） */
    private BigDecimal totalIncome;

    /** 平台抽成（total_income * platform_fee_rate） */
    private BigDecimal platformFee;

    /** 创作者实得（total_income - platform_fee） */
    private BigDecimal creatorIncome;

    /** 状态 pending/confirmed/paid */
    private String status;

    /** 打款时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime paidTime;

    // createTime / updateTime 由 BaseEntity 提供，portal_creator_settlement 表的 create_time / update_time 列与之对应
    // BaseEntity 公共字段 create_by / update_by / remark 在本表不存在，排除 MyBatis-Plus 映射，避免 SELECT/INSERT 报未知列
    @TableField(exist = false)
    private String createBy;
    @TableField(exist = false)
    private String updateBy;
    @TableField(exist = false)
    private String remark;

    // ========== JOIN 查询时填充 ==========

    /** 创作者昵称（JOIN portal_user 时填充） */
    @TableField(exist = false)
    private String creatorNickname;

    /** 创作者头像（JOIN portal_user 时填充） */
    @TableField(exist = false)
    private String creatorAvatar;
}
