package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.moyun.core.base.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * 门户分类对象 portal_category
 *
 * @author moyun
 */
@TableName("portal_category")
public class PortalCategory extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 分类ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分类名称 */
    @NotBlank(message = "分类名称不能为空")
    @Size(min = 0, max = 100, message = "分类名称长度不能超过100个字符")
    private String name;

    /** 分类别名 */
    @Size(min = 0, max = 100, message = "分类别名长度不能超过100个字符")
    private String slug;

    /** 分类描述 */
    @Size(min = 0, max = 500, message = "分类描述长度不能超过500个字符")
    private String description;

    /** 图标URL */
    @Size(min = 0, max = 500, message = "图标URL长度不能超过500个字符")
    private String icon;

    /** 排序 */
    private Integer sort;

    /** 父分类ID */
    private Long parentId;

    /** 状态（0正常 1停用） */
    private String status;

    /** 子分类列表（不映射到数据库字段） */
    @TableField(exist = false)
    private List<PortalCategory> children = new ArrayList<>();

    public PortalCategory()
    {

    }

    public PortalCategory(Long id)
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

    public List<PortalCategory> getChildren()
    {
        return children;
    }

    public void setChildren(List<PortalCategory> children)
    {
        this.children = children;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("name", getName())
            .append("slug", getSlug())
            .append("description", getDescription())
            .append("icon", getIcon())
            .append("sort", getSort())
            .append("parentId", getParentId())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
