package com.moyun.system.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户视图对象
 *
 * @author ruoyi
 */
@Data
@Schema(description = "用户VO")
public class UserVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    @Schema(description = "用户ID")
    private Long userId;

    /** 部门ID */
    @Schema(description = "部门ID")
    private Long deptId;

    /** 用户账号 */
    @Schema(description = "用户账号")
    private String userName;

    /** 用户昵称 */
    @Schema(description = "用户昵称")
    private String nickName;

    /** 用户邮箱 */
    @Schema(description = "用户邮箱")
    private String email;

    /** 手机号码 */
    @Schema(description = "手机号码")
    private String phonenumber;

    /** 用户性别 */
    @Schema(description = "用户性别")
    private String sex;

    /** 用户头像 */
    @Schema(description = "用户头像")
    private String avatar;

    /** 帐号状态（0正常 1停用） */
    @Schema(description = "帐号状态")
    private String status;

    /** 最后登录IP */
    @Schema(description = "最后登录IP")
    private String loginIp;

    /** 最后登录时间 */
    @Schema(description = "最后登录时间")
    private LocalDateTime loginDate;

    /** 部门名称 */
    @Schema(description = "部门名称")
    private String deptName;

    /** 角色名称 */
    @Schema(description = "角色名称")
    private String roleName;

    /** 角色ID */
    @Schema(description = "角色ID")
    private Long roleId;

    /** 角色组 */
    @Schema(description = "角色组")
    private List<Long> roleIds;

    /** 岗位组 */
    @Schema(description = "岗位组")
    private List<Long> postIds;

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
