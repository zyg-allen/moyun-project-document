package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.moyun.core.base.BaseEntity;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.math.BigDecimal;

@TableName("portal_vip_package")
public class PortalVipPackage extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "套餐名称不能为空")
    @Size(min = 0, max = 100, message = "套餐名称长度不能超过100个字符")
    private String name;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.00", message = "价格不能小于0")
    private BigDecimal price;

    private BigDecimal originalPrice;

    @NotNull(message = "有效期不能为空")
    private Integer duration;

    @Size(min = 0, max = 500, message = "套餐描述长度不能超过500个字符")
    private String description;

    private String features;

    private Boolean popular;

    private Integer sort;

    @Size(min = 0, max = 20, message = "状态长度不能超过20个字符")
    private String status;

    public PortalVipPackage()
    {
    }

    public PortalVipPackage(Long id)
    {
        this.id = id;
    }

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

    public BigDecimal getPrice()
    {
        return price;
    }

    public void setPrice(BigDecimal price)
    {
        this.price = price;
    }

    public BigDecimal getOriginalPrice()
    {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice)
    {
        this.originalPrice = originalPrice;
    }

    public Integer getDuration()
    {
        return duration;
    }

    public void setDuration(Integer duration)
    {
        this.duration = duration;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getFeatures()
    {
        return features;
    }

    public void setFeatures(String features)
    {
        this.features = features;
    }

    public Boolean getPopular()
    {
        return popular;
    }

    public void setPopular(Boolean popular)
    {
        this.popular = popular;
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
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("name", getName())
            .append("price", getPrice())
            .append("originalPrice", getOriginalPrice())
            .append("duration", getDuration())
            .append("description", getDescription())
            .append("features", getFeatures())
            .append("popular", getPopular())
            .append("sort", getSort())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
