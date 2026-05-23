package com.moyun.community.growth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.moyun.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cms_level_config")
public class CmsLevelConfig extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long levelId;

    private Integer level;

    private String levelName;

    private Long minPoints;

    private Long maxPoints;

    private String description;

    private String icon;

    private String color;

    private String privileges;

    private Integer sort;

    private String status;
}
