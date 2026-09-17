package com.moyun.vip.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户会员卡（类比 sys_user_role，一端一卡，续费顺延）
 *
 * @author moyun
 */
@Data
@TableName("vip_user_card")
public class VipUserCard {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 端代码 */
    private String platformCode;

    /** 当前等级代码 */
    private String tierCode;

    /** 最近一次支付订单（pay_order.id） */
    private Long orderId;

    /** 生效时间 */
    private LocalDateTime startTime;

    /** 过期时间（永久为 NULL） */
    private LocalDateTime expireTime;

    /** 状态（1有效 0过期/作废） */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
