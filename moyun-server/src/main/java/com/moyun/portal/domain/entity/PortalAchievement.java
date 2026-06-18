package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 成就定义表
 *
 * @author moyun
 */
@Data
@TableName("portal_achievement")
public class PortalAchievement {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 成就编码 */
    private String code;

    /** 成就名称 */
    private String name;

    /** 成就描述 */
    private String description;

    /** 图标URL */
    private String icon;

    /** 所属模块: article/reading/interview/all */
    private String module;

    /** 达成条件JSON */
    private String conditionJson;

    /** 达成奖励成长值 */
    private Integer growthReward;

    /** 排序 */
    private Integer sort;

    /** 状态（0启用 1停用） */
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
