package com.moyun.ledger.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 记账-打赏记录（V11.80 接入公共支付通道）
 *
 * <p>链路：落 pending 单 → payGateway 统一下单(bizType=ledger_tip, platform=ledger_app)
 * → 收银台（扫码 / mock 模拟支付）→ 回调置 paid + 平台全额分账。
 *
 * <p>v11.79 全平台支付状态统一：业务订单 status 统一字符串枚举 pending/paid/refunded/closed，
 * 支付渠道字段统一 pay_channel（wechat/alipay），pay_no 关联公共通道单据。
 *
 * @author moyun
 */
@Data
@TableName("ledger_tip_order")
public class LedgerTipOrder {

    /** 打赏对象：开发者 */
    public static final String TARGET_DEVELOPER = "developer";
    /** 打赏对象：平台 */
    public static final String TARGET_PLATFORM = "platform";

    /** 状态：待支付 */
    public static final String STATUS_PENDING = "pending";
    /** 状态：已支付（成功） */
    public static final String STATUS_PAID = "paid";
    /** 状态：已退款 */
    public static final String STATUS_REFUNDED = "refunded";
    /** 状态：已关闭 */
    public static final String STATUS_CLOSED = "closed";

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 门户用户ID（portal_user.id） */
    private Long userId;

    /** 打赏金额（元） */
    private BigDecimal amount;

    /** 打赏对象：developer=开发者 platform=平台 */
    private String target;

    /** 打赏理由（可选） */
    private String reason;

    /** 支付渠道：wechat/alipay（v11.79 与 portal_tip_order.pay_channel 统一命名） */
    private String payChannel;

    /** 关联公共通道单据号（pay_order.pay_no） */
    private String payNo;

    /** 客户端幂等号（防重复提交，UNIQUE；V11.80 对齐记一笔 clientUuid 机制） */
    private String clientUuid;

    /** 状态：pending/paid/refunded/closed（v11.79 统一字符串枚举） */
    private String status;

    /** 下单时间 */
    private LocalDateTime createTime;

    /** 支付完成时间（网关回调置 paid 时写入，V11.80） */
    private LocalDateTime paidTime;
}
