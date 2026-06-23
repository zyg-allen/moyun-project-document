package com.moyun.portal.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 成就展示 VO（含用户达成状态）
 *
 * @author moyun
 */
@Data
public class AchievementVO {

    /** 成就ID */
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

    /** 达成奖励成长值 */
    private Integer growthReward;

    /** 排序 */
    private Integer sort;

    /** 当前用户是否已达成 */
    private Boolean earned;

    /** 达成时间（未达成为 null） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime earnedTime;
}
