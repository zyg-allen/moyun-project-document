package com.moyun.portal.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 微信小程序绑定：一次性 code */
@Data
public class WechatBindDTO {

    @NotBlank(message = "微信授权 code 不能为空")
    @Size(max = 128, message = "微信授权 code 过长")
    private String code;
}
