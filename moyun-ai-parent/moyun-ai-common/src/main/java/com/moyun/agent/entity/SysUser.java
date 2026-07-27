package com.moyun.agent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户实体
 *
 * <p>对应数据库表 sys_user，存储系统用户信息</p>
 *
 * @author laomao
 * @time 2025/11/25
 */
@Data
@TableName("sys_user")
public class SysUser {

    /** 用户ID，自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名，唯一 */
    private String username;

    /** 密码，BCrypt加密存储 */
    private String password;

    /** 用户昵称 */
    private String nickname;

    /** 邮箱地址 */
    private String email;

    /** 手机号码 */
    private String phone;

    /** 头像URL */
    private String avatar;

    /** 状态：0-禁用，1-正常 */
    private Integer status;

    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;

    /** 最后登录IP地址 */
    private String lastLoginIp;

    /** 创建时间，自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间，自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
