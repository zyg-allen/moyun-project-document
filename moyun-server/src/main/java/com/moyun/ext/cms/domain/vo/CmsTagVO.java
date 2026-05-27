package com.moyun.ext.cms.domain.vo;

import com.moyun.core.base.BaseEntity;

/**
 * 标签视图对象
 *
 * @author moyun
 */
public class CmsTagVO extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 标签ID */
    private Long id;

    /** 标签名称 */
    private String name;

    /** 标签别名 */
    private String slug;

    /** 排序 */
    private Integer sort;

    /** 状态 */
    private String status;

    /** 文章数量 */
    private Integer articleCount;

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

    public Integer getSort()
    {
        return sort;
    }

    public void setSort(Integer sort)
    {
        this.sort = sort;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Integer getArticleCount()
    {
        return articleCount;
    }

    public void setArticleCount(Integer articleCount)
    {
        this.articleCount = articleCount;
    }
}
