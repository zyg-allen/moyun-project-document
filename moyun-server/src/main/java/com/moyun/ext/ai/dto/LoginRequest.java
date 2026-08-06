package com.moyun.ext.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 登录请求DTO
 *
 * <p>用于接收前端登录表单提交的数据</p>
 *
 * @author laomao
 * @time 2025/11/25
 */
@Data
@Schema(description = "登录请求")
public class LoginRequest {

    /** 用户名 */
    @Schema(description = "用户名", required = true, example = "laomao")
    private String username;

    /** 密码 */
    @Schema(description = "密码", required = true, example = "laomao123456")
    private String password;

    /** 验证码 */
    @Schema(description = "验证码", required = true, example = "A3B5")
    private String captcha;

    /** 验证码Key，从获取验证码接口返回 */
    @Schema(description = "验证码Key", required = true)
    private String captchaKey;
}
