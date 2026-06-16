package com.moyun.portal.domain.entity;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import com.moyun.core.base.BaseEntity;

@Data
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
}
