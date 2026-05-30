package com.moyun.ext.cms.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.moyun.core.base.BaseEntity;

import java.time.LocalDateTime;

public class CmsFriendLinkVO extends BaseEntity
{
    private Long id;
    private String name;
    private String url;
    private String description;
    private String logo;
    private Integer sort;
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

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

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getLogo()
    {
        return logo;
    }

    public void setLogo(String logo)
    {
        this.logo = logo;
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

    @Override
    public LocalDateTime getCreateTime()
    {
        return createTime;
    }

    @Override
    public void setCreateTime(LocalDateTime createTime)
    {
        this.createTime = createTime;
    }
}
