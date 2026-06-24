package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.moyun.core.base.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 帮助中心分类对象 portal_help_category
 *
 * @author moyun
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("portal_help_category")
public class PortalHelpCategory extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 分类ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分类名称 */
    @NotBlank(message = "分类名称不能为空")
    @Size(min = 0, max = 100, message = "分类名称长度不能超过100个字符")
    private String name;

    /** 图标（lucide 图标名） */
    @Size(min = 0, max = 100, message = "图标长度不能超过100个字符")
    private String icon;

    /** 分类描述 */
    @Size(min = 0, max = 500, message = "分类描述长度不能超过500个字符")
    private String description;

    /** 排序 */
    private Integer sort;

    /** 状态：active/inactive */
    @Size(min = 0, max = 20, message = "状态长度不能超过20个字符")
    private String status;

    public PortalHelpCategory() {}

    public PortalHelpCategory(Long id) {
        this.id = id;
    }
}
