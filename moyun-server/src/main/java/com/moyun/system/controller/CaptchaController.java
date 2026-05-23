package com.moyun.system.controller;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

import com.moyun.common.constant.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.FastByteArrayOutputStream;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.google.code.kaptcha.Producer;
import com.moyun.common.config.RuoYiConfig;
import com.moyun.common.constant.CacheConstants;
import com.moyun.common.core.domain.AjaxResult;
import com.moyun.common.core.redis.RedisCache;
import com.moyun.common.utils.sign.Base64;
import com.moyun.common.utils.uuid.IdUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 验证码操作处理
 * 
 * @author ruoyi
 */
@Tag(name = "验证码管理", description = "验证码生成和获取接口")
@RestController
public class CaptchaController {

    @Autowired
    private Producer captchaProducer;

    @Autowired
    private RedisCache redisCache;

    // 验证码类型
    @Value("${captcha.type:math}")
    private String captchaType;

    // 验证码有效期（分钟）
    @Value("${captcha.expireTime:2}")
    private int expireTime;

    /**
     * 生成验证码
     */
    @Operation(summary = "生成验证码", description = "生成图形验证码并返回Base64编码的图片")
    @GetMapping("/captchaImage")
    public AjaxResult getCode() throws IOException {
        AjaxResult ajax = AjaxResult.success();
        boolean captchaEnabled = true;
        ajax.put("captchaEnabled", captchaEnabled);
        if (!captchaEnabled) {
            return ajax;
        }

        // 保存验证码信息
        String uuid = IdUtils.simpleUUID();
        String verifyKey = Constants.CAPTCHA_CODE_KEY + uuid;

        String capStr = null, code = null;
        BufferedImage image = null;

        // 生成验证码
        if ("math".equals(captchaType)) {
            String capText = captchaProducer.createText();
            int atIndex = capText.lastIndexOf("@");
            if (atIndex == -1) {
                // 如果没有@符号，说明不是数学验证码格式，直接使用生成的文本
                capStr = capText;
                code = capText;
            } else {
                capStr = capText.substring(0, atIndex);
                code = capText.substring(atIndex + 1);
            }
            image = captchaProducer.createImage(capStr);
        } else if ("char".equals(captchaType)) {
            capStr = code = captchaProducer.createText();
            image = captchaProducer.createImage(capStr);
        }

        redisCache.setCacheObject(verifyKey, code, expireTime, TimeUnit.MINUTES);
        // 转换流信息写出
        FastByteArrayOutputStream os = new FastByteArrayOutputStream();
        try {
            ImageIO.write(image, "jpg", os);
        } catch (IOException e) {
            return AjaxResult.error(e.getMessage());
        }

        ajax.put("capStr", capStr);
        ajax.put("uuid", uuid);
        ajax.put("img", Base64.encode(os.toByteArray()));
        return ajax;
    }
}