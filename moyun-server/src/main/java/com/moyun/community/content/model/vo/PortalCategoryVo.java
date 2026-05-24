package com.moyun.community.content.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 前台分类VO
 *
 * @author moyun
 */
@Data
public class PortalCategoryVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String slug;
    private String description;
    private String icon;
    private Integer articleCount;
    private Integer sortOrder;
    private String color;
    private String createdAt;
}
