package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户成长值总表
 *
 * @author moyun
 */
@Data
@TableName("portal_user_growth")
public class PortalUserGrowth {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 门户用户ID */
    private Long userId;

    /** 成长值（累计） */
    private Integer growthValue;

    /** 当前等级 */
    private Integer level;

    /** 当前头衔 */
    private String title;

    /** 本季成长值 */
    private Integer seasonValue;

    /** 补签卡数量（每月赠送1张，补签消耗） */
    private Integer supplementCardCount;

    /** 最后赠送补签卡月份（YYYY-MM，幂等控制每月只赠送1张） */
    private String lastCardGrantMonth;

    /** 积分余额（可消耗，与成长值解耦，用于积分商城） */
    private Long points;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
