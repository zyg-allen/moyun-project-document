package com.moyun.system.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 角色和菜单关联表 sys_role_menu
 *
 * @author ruoyi
 */
@Data
@TableName("sys_role_menu")
public class SysRoleMenu implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 角色ID */
    private Long roleId;

    /** 菜单ID */
    private Long menuId;
}
