package com.moyun.portal.controller;

import com.moyun.common.annotation.Anonymous;
import com.moyun.common.annotation.RateLimiter;
import com.moyun.common.constant.Constants;
import com.moyun.common.enums.LimitType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.config.redis.RedisCache;
import com.moyun.portal.service.PortalEmailService;
import com.moyun.system.service.ISysConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 门户邮箱服务
 * <p>
 * 提供注册邮箱验证码、找回密码（邮箱验证码重置密码）能力。
 * 匿名访问（@Anonymous），限流走 IP 维度防止刷短信/邮件。
 *
 * @author moyun
 */
@Anonymous
@Tag(name = "门户邮箱服务", description = "邮箱验证码 / 找回密码")
@RestController
@RequestMapping("/portal/email")
public class PortalEmailController {

    @Autowired
    private PortalEmailService portalEmailService;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private ISysConfigService configService;

    /**
     * 发送邮箱验证码
     * <p>
     * 场景：
     * <ul>
     *     <li>register：注册时校验邮箱真实性，且要求邮箱未注册</li>
     *     <li>reset_password：找回密码，要求邮箱已注册</li>
     * </ul>
     * <p>v11.42：发送前强制图形验证码人机校验（跟随 sys.account.captchaEnabled 开关，
     * 与短信 /portal/sms/code/send 同一套弹窗交互，验证码一次性作废防重放）
     */
    @Operation(summary = "发送邮箱验证码", description = "注册/找回密码场景")
    @RateLimiter(time = 60, count = 3, limitType = LimitType.IP)
    @PostMapping("/code")
    public AjaxResult sendCode(@Parameter(description = "邮箱与场景") @RequestBody Map<String, String> body) {
        // 图形验证码人机校验（开关开启时；逻辑与 PortalSmsController 一致）
        AjaxResult captchaError = validateCaptcha(body);
        if (captchaError != null) {
            return captchaError;
        }
        String email = body.get("email");
        String type = body.get("type");
        return portalEmailService.sendCode(email, type);
    }

    /**
     * 图形验证码校验（发送邮箱验证码前的人机校验）
     *
     * @return 校验失败返回错误 AjaxResult；通过（或开关关闭）返回 null
     */
    private AjaxResult validateCaptcha(Map<String, String> body) {
        if (!configService.selectCaptchaEnabled()) {
            return null; // 全局开关关闭，依靠 IP 限流 + 服务层频控兜底
        }
        String code = body.get("code");
        String uuid = body.get("uuid");
        if (code == null || code.isBlank() || uuid == null || uuid.isBlank()) {
            return AjaxResult.error("请输入图形验证码");
        }
        String verifyKey = Constants.CAPTCHA_CODE_KEY + uuid;
        String captcha = redisCache.getCacheObject(verifyKey);
        // 一次性作废：无论对错都删除，防止同一 uuid 重放撞库
        redisCache.deleteObject(verifyKey);
        if (captcha == null) {
            return AjaxResult.error("验证码已过期，请刷新后重试");
        }
        if (!code.trim().equalsIgnoreCase(captcha)) {
            return AjaxResult.error("图形验证码错误");
        }
        return null;
    }

    /**
     * 找回密码：通过邮箱验证码重置密码
     */
    @Operation(summary = "找回密码", description = "邮箱+验证码+新密码重置密码")
    @RateLimiter(time = 60, count = 5, limitType = LimitType.IP)
    @PostMapping("/reset-password")
    public AjaxResult resetPassword(@Parameter(description = "重置密码参数") @RequestBody Map<String, String> body) {
        String email = body.get("email");
        String code = body.get("code");
        String newPassword = body.get("newPassword");
        return portalEmailService.resetPassword(email, code, newPassword);
    }
}
