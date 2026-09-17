package com.moyun.vip.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 权益使用记录（Redis 计数异步落库，统计口径）
 *
 * @author moyun
 */
@Data
@TableName("vip_benefit_usage")
public class VipBenefitUsage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 端代码 */
    private String platformCode;

    /** 权益代码 */
    private String benefitCode;

    /** 当日累计使用次数 */
    private Integer usageCount;

    /** 使用日期 */
    private LocalDate usageDate;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
