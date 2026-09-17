package com.moyun.vip.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 等级权益关联（类比 sys_role_menu）
 *
 * @author moyun
 */
@Data
@TableName("vip_tier_benefit")
public class VipTierBenefit {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 端代码 */
    private String platformCode;

    /** 等级代码 */
    private String tierCode;

    /** 权益代码 */
    private String benefitCode;

    /** 额度（unlimited 或数字） */
    private String benefitValue;

    /** 统计周期（day/month/year/unlimited） */
    private String period;

    private LocalDateTime createTime;
}
