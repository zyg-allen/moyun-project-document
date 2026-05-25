package com.moyun.system.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 角色数据传输对象
 *
 * @author ruoyi
 */
@Data
@Schema(description = "角色DTO")
public class RoleDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 角色ID */
    @Schema(description = "角色ID")
    private Long roleId;

    /** 角色名称 */
    @NotBlank(message = "角色名称不能为空")
    @Size(min = 0, max = 30, message = "角色名称长度不能超过30个字符")
    @Schema(description = "角色名称")
    private String roleName;

    /** 角色权限 */
    @NotBlank(message = "权限字符不能为空")
    @Size(min = 0, max = 100, message = "权限字符长度不能超过100个字符")
    @Schema(description = "角色权限")
    private String roleKey;

    /** 角色排序 */
    @NotNull(message = "显示顺序不能为空")
    @Schema(description = "角色排序")
    private Integer roleSort;

    /** 数据范围（1：所有数据权限；2：自定义数据权限；3：本部门数据权限；4：本部门及以下数据权限；5：仅本人数据权限） */
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

    /** 菜单组 */
    @Schema(description = "菜单组")
    private Long[] menuIds;

    /** 部门组（数据权限） */
    @Schema(description = "部门组")
    private Long[] deptIds;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;
}
