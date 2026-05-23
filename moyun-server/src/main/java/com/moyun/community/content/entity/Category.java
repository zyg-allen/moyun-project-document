package com.moyun.community.content.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("cms_category")
public class Category implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "category_id", type = IdType.AUTO)
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

    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private String updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private String remark;

    @TableField(exist = false)
    private List<Category> children;
}
