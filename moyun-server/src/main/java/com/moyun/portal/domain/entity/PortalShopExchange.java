package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.moyun.core.base.BaseEntity;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 积分兑换记录
 *
 * @author moyun
 */
@Data
@TableName("portal_shop_exchange")
public class PortalShopExchange extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 兑换用户ID */
    private Long userId;

    /** 商品ID */
    private Long itemId;

    /** 消耗积分（冗余，便于查询） */
    private Integer pointsCost;

    /** 状态 pending/fulfilled/failed */
    private String status;

    /** 收货地址（实物商品） */
    private String address;

    /** 兑换时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime exchangeTime;

    public PortalShopExchange() {
    }

    public PortalShopExchange(Long id) {
        this.id = id;
    }
}
