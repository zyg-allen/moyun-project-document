package com.moyun.vip.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * VIP 权益定义（类比 sys_menu，统一 {action} 命名，端级隔离）
 *
 * @author moyun
 */
@Data
@TableName("vip_benefit")
public class VipBenefit {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 端代码 */
    private String platformCode;

    /** 权益代码（interview_unlimited/resume_optimize/bill_parse…） */
    private String benefitCode;

    /** 权益名称 */
    private String benefitName;

    /** 描述 */
    private String description;

    /** 排序 */
    private Integer sortOrder;

    private LocalDateTime createTime;
}
