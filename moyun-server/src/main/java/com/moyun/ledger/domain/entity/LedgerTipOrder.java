package com.moyun.ledger.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 记账-打赏记录（演示性质：模拟支付成功即落库）
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

    /** 支付方式：微信 */
    public static final String PAY_WECHAT = "wechat";
    /** 支付方式：支付宝 */
    public static final String PAY_ALIPAY = "alipay";

    /** 状态：成功 */
    public static final int STATUS_SUCCESS = 1;
    /** 状态：已撤销 */
    public static final int STATUS_CANCELLED = 0;

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

    /** 支付方式：wechat/alipay */
    private String payWay;

    /** 状态：1=成功 0=已撤销 */
    private Integer status;

    /** 打赏时间 */
    private LocalDateTime createTime;
}
