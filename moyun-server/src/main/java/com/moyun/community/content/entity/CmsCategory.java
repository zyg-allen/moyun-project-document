package com.moyun.community.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.moyun.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cms_category")
public class CmsCategory extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long categoryId;

    private Long parentId;

    private String ancestors;

    private String categoryName;

    private String categoryCode;

    private String categoryType;

    private String icon;

    private String coverImage;

    private String description;

    private Integer sort;

    private String status;

    private Integer isShow;

    private Integer isRecommend;

    private String seoTitle;

    private String seoKeywords;

    private String seoDescription;
}