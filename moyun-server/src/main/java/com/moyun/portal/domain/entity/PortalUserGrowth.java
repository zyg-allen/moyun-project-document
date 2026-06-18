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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
