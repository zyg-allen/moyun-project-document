package com.moyun.community.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.moyun.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cms_banner")
public class CmsBanner extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long bannerId;

    private String title;

    private String imageUrl;

    private String jumpType;

    private String jumpValue;

    private String jumpParams;

    private Integer sort;

    private String status;

    private Date startTime;

    private Date endTime;

    private String position;

    private Long clickCount;

    private Long exposureCount;
}