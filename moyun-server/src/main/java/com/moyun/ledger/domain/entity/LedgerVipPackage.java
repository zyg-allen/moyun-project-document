package com.moyun.ledger.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 记账VIP套餐（v11.81 平台直收类）
 *
 * <p>价格/时长后台可配置（骨架阶段提供默认套餐，后续调整无需改代码）。
 * 购买走公共支付通道：ledger_vip_order（bizType=ledger_vip）。
 *
 * @author moyun
 */
@Data
@TableName("ledger_vip_package")
public class LedgerVipPackage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 套餐名（如：月度VIP/年度VIP） */
    private String name;

    /** 售价（元） */
    private BigDecimal price;

    /** 划线原价（元，可空） */
    private BigDecimal originalPrice;

    /** 时长（天） */
    private Integer durationDays;

    /** 权益说明 */
    private String description;

    /** 是否推荐（1=推荐展示） */
    private Boolean popular;

    /** 排序（小在前） */
    private Integer sort;

    /** 0=下架 1=上架 */
    private Boolean status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
