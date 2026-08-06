package com.moyun.ext.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 验证码响应DTO
 *
 * <p>获取验证码接口返回的数据，包含验证码key和Base64编码的图片</p>
 *
 * @author laomao
 * @time 2025/11/25
 */
@Data
@Schema(description = "验证码响应")
public class CaptchaResponse {

    /** 验证码Key，登录时需要携带此key进行校验 */
    @Schema(description = "验证码Key，用于登录时校验")
    private String captchaKey;

    /** 验证码图片，Base64编码的PNG图片，可直接用于img标签的src属性 */
    @Schema(description = "验证码图片（Base64编码）")
    private String captchaImage;
}
