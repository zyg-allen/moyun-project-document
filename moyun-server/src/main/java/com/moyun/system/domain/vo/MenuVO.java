package com.moyun.system.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 菜单视图对象
 *
 * @author ruoyi
 */
@Data
@Schema(description = "菜单VO")
public class MenuVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 菜单ID */
    @Schema(description = "菜单ID")
    private Long menuId;

    /** 菜单名称 */
    @Schema(description = "菜单名称")
    private String menuName;

    /** 父菜单名称 */
    @Schema(description = "父菜单名称")
    private String parentName;

    /** 父菜单ID */
    @Schema(description = "父菜单ID")
    private Long parentId;

    /** 显示顺序 */
    @Schema(description = "显示顺序")
    private Integer orderNum;

    /** 路由地址 */
    @Schema(description = "路由地址")
    private String path;

    /** 组件路径 */
    @Schema(description = "组件路径")
    private String component;

    /** 路由参数 */
    @Schema(description = "路由参数")
    private String query;

    /** 路由名称 */
    @Schema(description = "路由名称")
    private String routeName;

    /** 是否为外链 */
    @Schema(description = "是否为外链")
    private String isFrame;

    /** 是否缓存 */
    @Schema(description = "是否缓存")
    private String isCache;

    /** 类型（M目录 C菜单 F按钮） */
    @Schema(description = "类型")
    private String menuType;

    /** 显示状态（0显示 1隐藏） */
    @Schema(description = "显示状态")
    private String visible;

    /** 菜单状态（0正常 1停用） */
    @Schema(description = "菜单状态")
    private String status;

    /** 权限字符串 */
    @Schema(description = "权限字符串")
    private String perms;

    /** 菜单图标 */
    @Schema(description = "菜单图标")
    private String icon;

    /** 子菜单 */
    @Schema(description = "子菜单")
    private List<MenuVO> children = new ArrayList<>();
}
