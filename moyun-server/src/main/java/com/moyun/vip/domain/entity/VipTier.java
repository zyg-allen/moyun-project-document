package com.moyun.vip.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * VIP 等级（一端一套，类比 sys_role）
 *
 * @author moyun
 */
@Data
@TableName("vip_tier")
public class VipTier {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 端代码（sys_platform.platform_code） */
    private String platformCode;

    /** 等级代码（free/monthly/yearly/permanent…） */
    private String tierCode;

    /** 等级名称 */
    private String tierName;

    /** 有效天数（-1=永久，0=免费tier） */
    private Integer durationDays;

    /** 价格（元，DECIMAL(18,2)） */
    private BigDecimal price;

    /** 划线原价（元，可空） */
    private BigDecimal originalPrice;

    /** 是否推荐（1=售卖页推荐展示） */
    private Integer popular;

    /** 等级说明 */
    private String description;

    /** 排序 */
    private Integer sortOrder;

    /** 状态（1上架 0下架） */
    private Integer status;

    private LocalDateTime createTime;
}
