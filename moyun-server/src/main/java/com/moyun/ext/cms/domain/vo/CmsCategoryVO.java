package com.moyun.ext.cms.domain.vo;

import java.util.List;

import lombok.Data;

import com.moyun.core.base.BaseEntity;

/**
 * 分类视图对象
 *
 * @author moyun
 */
@Data
public class CmsCategoryVO extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 分类ID */
    private Long id;

    /** 分类名称 */
    private String name;

    /** 分类别名 */
    private String slug;

    /** 分类描述 */
    private String description;

    /** 图标URL */
    private String icon;

    /** 排序 */
    private Integer sort;

    /** 父分类ID */
    private Long parentId;

    /** 状态（0正常 1停用） */
    private String status;

    /** 文章数量 */
    private Integer articleCount;

    /** 子分类 */
    private List<CmsCategoryVO> children;
}
