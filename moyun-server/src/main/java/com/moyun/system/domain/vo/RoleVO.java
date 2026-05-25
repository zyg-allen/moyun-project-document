package com.moyun.system.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * 角色视图对象
 *
 * @author ruoyi
 */
@Data
@Schema(description = "角色VO")
public class RoleVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 角色ID */
    @Schema(description = "角色ID")
    private Long roleId;

    /** 角色名称 */
    @Schema(description = "角色名称")
    private String roleName;

    /** 角色权限 */
    @Schema(description = "角色权限")
    private String roleKey;

    /** 角色排序 */
    @Schema(description = "角色排序")
    private Integer roleSort;

    /** 数据范围 */
    @Schema(description = "数据范围")
    private String dataScope;

    /** 菜单树选择项是否关联显示 */
    @Schema(description = "菜单树选择项是否关联显示")
    private boolean menuCheckStrictly;

    /** 部门树选择项是否关联显示 */
    @Schema(description = "部门树选择项是否关联显示")
    private boolean deptCheckStrictly;

    /** 角色状态（0正常 1停用） */
    @Schema(description = "角色状态")
    private String status;

    /** 用户是否存在此角色标识 */
    @Schema(description = "用户是否存在此角色标识")
    private boolean flag;

    /** 角色菜单权限 */
    @Schema(description = "角色菜单权限")
    private Set<String> permissions;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;
}
