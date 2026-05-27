package com.moyun.ext.cms.domain.vo;

import com.moyun.core.base.BaseEntity;

import java.util.List;

/**
 * 分类视图对象
 *
 * @author moyun
 */
public class CmsCategoryVO extends BaseEntity
{
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

    /** 子分类 */
    private List<CmsCategoryVO> children;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getSlug()
    {
        return slug;
    }

    public void setSlug(String slug)
    {
        this.slug = slug;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getIcon()
    {
        return icon;
    }

    public void setIcon(String icon)
    {
        this.icon = icon;
    }

    public Integer getSort()
    {
        return sort;
    }

    public void setSort(Integer sort)
    {
        this.sort = sort;
    }

    public Long getParentId()
    {
        return parentId;
    }

    public void setParentId(Long parentId)
    {
        this.parentId = parentId;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public List<CmsCategoryVO> getChildren()
    {
        return children;
    }

    public void setChildren(List<CmsCategoryVO> children)
    {
        this.children = children;
    }
}
