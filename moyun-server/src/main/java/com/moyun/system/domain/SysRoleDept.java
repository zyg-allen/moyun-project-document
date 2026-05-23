package com.moyun.system.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 角色和部门关联表 sys_role_dept
 *
 * @author ruoyi
 */
@Data
@TableName("sys_role_dept")
public class SysRoleDept implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 角色ID */
    private Long roleId;

    /** 部门ID */
    private Long deptId;
}