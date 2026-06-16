package com.moyun.ext.cms.domain.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import com.moyun.core.base.BaseEntity;

/**
 * 门户用户视图对象
 *
 * @author moyun
 */
@Data
public class CmsPortalUserVO extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long id;

    /** 用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 头像URL */
    private String avatar;

    /** 个人简介 */
    private String bio;

    /** 职位 */
    private String position;

    /** 角色 */
    private String role;

    /** VIP过期时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime vipExpireAt;

    /** 帐号状态（0正常 1停用） */
    private String status;

    /** 最后登录IP */
    private String loginIp;

    /** 最后登录时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime loginDate;
}
