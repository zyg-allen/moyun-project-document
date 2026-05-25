package com.moyun.system.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户更新数据传输对象
 *
 * @author ruoyi
 */
@Data
@Schema(description = "用户更新DTO")
public class UserUpdateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    @Schema(description = "用户ID")
    private Long userId;

    /** 部门ID */
    @Schema(description = "部门ID")
    private Long deptId;

    /** 用户账号 */
    @Size(min = 0, max = 30, message = "用户账号长度不能超过30个字符")
    @Schema(description = "用户账号")
    private String userName;

    /** 用户昵称 */
    @Size(min = 0, max = 30, message = "用户昵称长度不能超过30个字符")
    @Schema(description = "用户昵称")
    private String nickName;

    /** 用户邮箱 */
    @Email(message = "邮箱格式不正确")
    @Size(min = 0, max = 50, message = "邮箱长度不能超过50个字符")
    @Schema(description = "用户邮箱")
    private String email;

    /** 手机号码 */
    @Size(min = 0, max = 11, message = "手机号码长度不能超过11个字符")
    @Schema(description = "手机号码")
    private String phonenumber;

    /** 用户性别 */
    @Schema(description = "用户性别")
    private String sex;

    /** 用户头像 */
    @Schema(description = "用户头像")
    private String avatar;

    /** 密码 */
    @Size(min = 0, max = 100, message = "密码长度不能超过100个字符")
    @Schema(description = "密码")
    private String password;

    /** 帐号状态（0正常 1停用） */
    @Schema(description = "帐号状态")
    private String status;

    /** 角色组 */
    @Schema(description = "角色组")
    private Long[] roleIds;

    /** 岗位组 */
    @Schema(description = "岗位组")
    private Long[] postIds;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;
}
