package com.moyun.agent.service;

import com.moyun.agent.dto.CaptchaResponse;

/**
 * 验证码服务接口
 *
 * <p>生成图形验证码并存储到Redis</p>
 *
 * @author laomao
 */
public interface CaptchaService {

    /**
     * 生成验证码
     *
     * @return 包含验证码key和Base64图片的响应对象
     */
    CaptchaResponse generateCaptcha();

    /**
     * 验证验证码
     *
     * <p>验证成功后会自动删除Redis中的验证码，防止重复使用</p>
     *
     * @param captchaKey 验证码key
     * @param captchaCode 用户输入的验证码
     * @return 验证是否通过
     */
    boolean verifyCaptcha(String captchaKey, String captchaCode);
}
