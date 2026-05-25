package com.moyun.portal.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户资料视图对象
 *
 * @author moyun
 */
@Data
@Schema(description = "用户资料VO")
public class UserProfileVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @Schema(description = "用户ID", example = "1")
    private Long id;

    /**
     * 用户名
     */
    @Schema(description = "用户名", example = "john_doe")
    private String username;

    /**
     * 昵称
     */
    @Schema(description = "昵称", example = "John Doe")
    private String nickname;

    /**
     * 邮箱
     */
    @Schema(description = "邮箱", example = "john@example.com")
    private String email;

    /**
     * 手机号
     */
    @Schema(description = "手机号", example = "138****8000")
    private String phone;

    /**
     * 头像URL
     */
    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    private String avatar;

    /**
     * 个人简介
     */
    @Schema(description = "个人简介", example = "热爱技术，喜欢分享")
    private String bio;

    /**
     * 职位
     */
    @Schema(description = "职位", example = "Java Developer")
    private String position;

    /**
     * 微信号
     */
    @Schema(description = "微信号", example = "john_doe")
    private String wechat;

    /**
     * 角色
     */
    @Schema(description = "角色", example = "user")
    private String role;

    /**
     * VIP过期时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "VIP过期时间", example = "2025-12-31 23:59:59")
    private LocalDateTime vipExpireAt;

    /**
     * 是否已验证手机号
     */
    @Schema(description = "是否已验证手机号", example = "true")
    private Boolean isPhoneVerified;

    /**
     * 是否已验证微信
     */
    @Schema(description = "是否已验证微信", example = "false")
    private Boolean isWechatVerified;

    /**
     * 是否开启两步验证
     */
    @Schema(description = "是否开启两步验证", example = "false")
    private Boolean twoFactorEnabled;

    /**
     * 帐号状态
     */
    @Schema(description = "帐号状态", example = "0")
    private String status;

    /**
     * 最后登录IP
     */
    @Schema(description = "最后登录IP", example = "127.0.0.1")
    private String loginIp;

    /**
     * 最后登录时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最后登录时间", example = "2024-01-01 12:00:00")
    private LocalDateTime loginDate;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间", example = "2024-01-01 12:00:00")
    private LocalDateTime createTime;
}
