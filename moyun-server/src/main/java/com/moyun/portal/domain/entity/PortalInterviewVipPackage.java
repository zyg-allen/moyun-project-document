package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 面试会员套餐（平台直收类）
 *
 * <p>价格/时长后台可配置（骨架阶段提供默认套餐，后续调整无需改代码）。
 * 购买走公共支付通道：portal_interview_vip_order（bizType=interview_vip，platform=portal）。
 *
 * @author moyun
 */
@Data
@TableName("portal_interview_vip_package")
public class PortalInterviewVipPackage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 套餐名（如：月度会员/年度会员） */
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
