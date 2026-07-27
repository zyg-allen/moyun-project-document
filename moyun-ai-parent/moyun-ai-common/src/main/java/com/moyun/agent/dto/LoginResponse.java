package com.moyun.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 登录响应DTO
 *
 * <p>登录成功后返回的用户信息和Token</p>
 *
 * @author laomao
 * @time 2025/11/25
 */
@Data
@Schema(description = "登录响应")
public class LoginResponse {

    /** 访问令牌，后续请求需要在Header中携带 */
    @Schema(description = "访问令牌")
    private String token;

    /** 用户ID */
    @Schema(description = "用户ID")
    private Long userId;

    /** 用户名 */
    @Schema(description = "用户名")
    private String username;

    /** 用户昵称 */
    @Schema(description = "昵称")
    private String nickname;

    /** 用户头像URL */
    @Schema(description = "头像URL")
    private String avatar;
}
