package com.moyun.portal.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户视图对象
 *
 * @author moyun
 */
@Data
@Schema(description = "用户VO")
public class UserVO implements Serializable {

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
     * 角色
     */
    @Schema(description = "角色", example = "user")
    private String role;

    /**
     * VIP过期时间
     */
    @Schema(description = "VIP过期时间", example = "2025-12-31 23:59:59")
    private String vipExpireAt;

    /**
     * 帐号状态
     */
    @Schema(description = "帐号状态", example = "0")
    private String status;
}
