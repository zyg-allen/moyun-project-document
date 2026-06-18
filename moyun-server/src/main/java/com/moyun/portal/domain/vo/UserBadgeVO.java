package com.moyun.portal.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户徽章 VO
 *
 * @author moyun
 */
@Data
public class UserBadgeVO {

    /** 徽章记录ID */
    private Long id;

    /** 成就ID */
    private Long achievementId;

    /** 成就编码 */
    private String code;

    /** 成就名称 */
    private String name;

    /** 成就描述 */
    private String description;

    /** 图标URL */
    private String icon;

    /** 所属模块 */
    private String module;

    /** 达成奖励成长值 */
    private Integer growthReward;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
