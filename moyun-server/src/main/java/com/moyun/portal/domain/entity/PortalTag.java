package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import com.moyun.core.base.BaseEntity;

@Data
@TableName("portal_tag")
public class PortalTag extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "标签名称不能为空")
    @Size(min = 0, max = 100, message = "标签名称长度不能超过100个字符")
    private String name;

    @Size(min = 0, max = 100, message = "标签别名长度不能超过100个字符")
    private String slug;

    private Integer sort;

    private String status;

    @Size(min = 0, max = 50, message = "所属模块长度不能超过50个字符")
    private String module;

    private Long referenceCount = 0L;

    /** 文章数量（非持久字段，查询时统计填充） */
    @TableField(exist = false)
    private Integer articleCount;

    public PortalTag()
    {
    }

    public PortalTag(Long id)
    {
        this.id = id;
    }
}
