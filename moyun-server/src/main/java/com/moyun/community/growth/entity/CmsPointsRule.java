package com.moyun.community.growth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.moyun.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cms_points_rule")
public class CmsPointsRule extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long ruleId;

    private String ruleName;

    private String ruleCode;

    private Integer points;

    private String triggerType;

    private String description;

    private Integer dailyLimit;

    private Integer monthlyLimit;

    private String status;
}
