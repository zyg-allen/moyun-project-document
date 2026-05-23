package com.moyun.community.growth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.moyun.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cms_points_log")
public class CmsPointsLog extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long logId;

    private Long userId;

    private String type;

    private Integer points;

    private Integer balance;

    private String sourceType;

    private Long sourceId;

    private String reason;

    private String status;
}
