package com.moyun.portal.domain.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import com.moyun.core.base.BaseEntity;

/**
 * 门户分类对象 portal_category
 *
 * @author moyun
 */
@Data
@TableName("portal_category")
public class PortalCategory extends BaseEntity {
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

    /** 是否在头部栏目展示（0否/1是） */
    private Integer showInNav;

    /** 路由类型（home/category/static/external） */
    @Size(min = 0, max = 20, message = "路由类型长度不能超过20个字符")
    private String navRouteType;

    /** 静态/外链路由路径（仅 static/external 类型使用） */
    @Size(min = 0, max = 200, message = "路由路径长度不能超过200个字符")
    private String navRoutePath;

    /** 栏目内容类型（article=文章栏目可发布文章 special=特殊页面不发布文章） */
    @Size(min = 0, max = 20, message = "栏目内容类型长度不能超过20个字符")
    private String categoryType;

    /** 是否需要登录（0否/1是） */
    private Integer requiresAuth;

    /** 文章数量（非持久字段，查询时统计填充） */
    @TableField(exist = false)
    private Integer articleCount;

    /** 子分类列表（不映射到数据库字段） */
    @TableField(exist = false)
    private List<PortalCategory> children = new ArrayList<>();

    public PortalCategory() {
    }

    public PortalCategory(Long id) {
        this.id = id;
    }
}
