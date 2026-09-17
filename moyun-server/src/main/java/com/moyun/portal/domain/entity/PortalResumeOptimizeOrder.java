package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 简历优化会员订单（v11.83 接入公共支付通道，bizType=resume_optimize 平台直收类）
 *
 * <p>链路：简历优化工作台选套餐下单（快照 name/duration，clientUuid 幂等）→ 落 pending 单 →
 * payGateway 统一下单（platform=portal）→ 收银台（扫码/mock）→ 回调置 paid +
 * 权益顺延（vip_expire 从 max(now, 现有到期) 顺延 duration_days）+ settlePlatform
 * 平台全额分账。
 *
 * <p>状态枚举 v11.79 统一：pending/paid/refunded/closed。
 *
 * @author moyun
 */
@Data
@TableName("portal_resume_optimize_order")
public class PortalResumeOptimizeOrder {

    /** 状态：待支付 */
    public static final String STATUS_PENDING = "pending";
    /** 状态：已支付（权益已发放） */
    public static final String STATUS_PAID = "paid";
    /** 状态：已退款 */
    public static final String STATUS_REFUNDED = "refunded";
    /** 状态：已关闭 */
    public static final String STATUS_CLOSED = "closed";

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 门户用户ID（portal_user.id） */
    private Long userId;

    /** 套餐ID（portal_resume_optimize_package.id） */
    private Long packageId;

    /** 套餐名快照（下单时冻结，套餐改名不影响历史订单） */
    private String packageName;

    /** 时长快照（天） */
    private Integer durationDays;

    /** 支付金额（元） */
    private BigDecimal amount;

    /** 支付渠道：wechat/alipay */
    private String payChannel;

    /** 关联公共通道单据号（pay_order.pay_no） */
    private String payNo;

    /** 客户端幂等号（防重复提交，UNIQUE） */
    private String clientUuid;

    /** 状态：pending/paid/refunded/closed */
    private String status;

    /** 权益起始时间（回调置 paid 时计算） */
    private LocalDateTime vipStart;

    /** 权益到期时间（现有时长顺延：max(now, 现有到期) + duration_days） */
    private LocalDateTime vipExpire;

    /** 下单时间 */
    private LocalDateTime createTime;

    /** 支付完成时间（网关回调置 paid 时写入） */
    private LocalDateTime paidTime;
}
