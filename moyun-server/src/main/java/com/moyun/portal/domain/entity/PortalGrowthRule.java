package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 成长规则配置表
 *
 * @author moyun
 */
@Data
@TableName("portal_growth_rule")
public class PortalGrowthRule {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 模块: article/reading/interview/all */
    private String module;

    /** 行为编码 */
    private String action;

    /** 成长值 */
    private Integer growthDelta;

    /** 每日上限（0=不限） */
    private Integer dailyLimit;

    /** 描述 */
    private String description;

    /** 状态（0启用 1停用） */
    private String status;

    /** 排序 */
    private Integer sort;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
