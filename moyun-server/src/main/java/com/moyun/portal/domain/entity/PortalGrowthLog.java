package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 成长事件流水表
 *
 * @author moyun
 */
@Data
@TableName("portal_growth_log")
public class PortalGrowthLog {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 获得成长值的用户ID */
    private Long userId;

    /** 目标用户ID（如被点赞的内容作者） */
    private Long targetUserId;

    /** 来源模块: article/reading/interview/all */
    private String module;

    /** 行为编码 */
    private String action;

    /** 实体类型 */
    private String entityType;

    /** 实体ID */
    private Long entityId;

    /** 成长值变化 */
    private Integer growthDelta;

    /** 描述 */
    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
