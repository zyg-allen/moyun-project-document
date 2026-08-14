package com.moyun.portal.controller;

import com.moyun.common.annotation.Anonymous;
import com.moyun.common.annotation.RateLimiter;
import com.moyun.common.enums.LimitType;
import com.moyun.core.base.AjaxResult;
import com.moyun.portal.service.PortalEmailService;
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

    /**
     * 发送邮箱验证码
     * <p>
     * 场景：
     * <ul>
     *     <li>register：注册时校验邮箱真实性，且要求邮箱未注册</li>
     *     <li>reset_password：找回密码，要求邮箱已注册</li>
     * </ul>
     */
    @Operation(summary = "发送邮箱验证码", description = "注册/找回密码场景")
    @RateLimiter(time = 60, count = 3, limitType = LimitType.IP)
    @PostMapping("/code")
    public AjaxResult sendCode(@Parameter(description = "邮箱与场景") @RequestBody Map<String, String> body) {
        String email = body.get("email");
        String type = body.get("type");
        return portalEmailService.sendCode(email, type);
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
