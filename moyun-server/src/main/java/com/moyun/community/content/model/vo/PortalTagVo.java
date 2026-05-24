package com.moyun.community.content.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 前台标签VO
 *
 * @author moyun
 */
@Data
public class PortalTagVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String slug;
    private String color;
    private String icon;
    private Integer articleCount;
    private Integer usageCount;
    private String createdAt;
}
