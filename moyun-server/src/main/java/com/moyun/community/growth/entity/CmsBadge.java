package com.moyun.community.growth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.moyun.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cms_badge")
public class CmsBadge extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long badgeId;

    private String badgeName;

    private String badgeCode;

    private String description;

    private String icon;

    private String color;

    private String conditionType;

    private String conditionValue;

    private String rarity;

    private String status;
}
