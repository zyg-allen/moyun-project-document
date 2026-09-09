package com.moyun.core.sms;

import com.moyun.common.annotation.Anonymous;
import com.moyun.common.constant.Constants;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.config.redis.RedisCache;
import com.moyun.portal.util.PortalSecurityUtils;
import com.moyun.system.service.ISysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 门户短信验证码控制器（V11.1）
 *
 * <p>发送方为当前登录用户（手机号即接收人，从请求体传入但服务端绑定场景校验），
 * 敏感操作（银行卡绑定等）在业务层调用 {@code SmsCodeService.verifyCode} 完成闭环。
 *
 * <p>场景白名单：bankcard=银行卡绑定；member 预留给后期会员支付开通。
 *
 * @author moyun
 */
@RestController
@RequestMapping("/portal/sms")
public class PortalSmsController {

    /** 允许的业务场景白名单（防任意 scene 写爆 Redis） */
    private static final java.util.Set<String> ALLOWED_SCENES = java.util.Set.of("bankcard", "member", "register");

    @Autowired
    private SmsCodeService smsCodeService;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private ISysConfigService configService;

    /**
     * 发送验证码（bankcard/member 场景需登录；register 场景匿名可发——注册时用户尚未登录，
     * 服务层已有 60s 间隔 + 日限额 + IP 层 @RateLimiter 防轰炸）
     * <p>v11.41：方法级 @Anonymous 放行安全链（场景级登录校验由下方 if 兜底，
     * 非 register 场景未登录返回 401 业务错误）
     * <p>v11.42：register 场景发送前强制图形验证码人机校验（跟随 sys.account.captchaEnabled 开关，
     * 开启时前端弹窗输入，验证码一次性作废防重放）
     */
    @Anonymous
    @PostMapping("/code/send")
    public AjaxResult sendCode(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        String scene = body.get("scene");
        if (scene == null || !ALLOWED_SCENES.contains(scene)) {
            return AjaxResult.error("不支持的业务场景");
        }
        if (!"register".equals(scene)) {
            Long userId = PortalSecurityUtils.getUserId();
            if (userId == null) {
                return AjaxResult.error(401, "登录已过期，请重新登录");
            }
        } else {
            // register 匿名场景：图形验证码人机校验（开关开启时）
            AjaxResult captchaError = validateCaptcha(body);
            if (captchaError != null) {
                return captchaError;
            }
        }
        try {
            smsCodeService.sendCode(phone, scene);
            return AjaxResult.success("验证码已发送");
        } catch (IllegalArgumentException e) {
            return AjaxResult.error(e.getMessage());
        } catch (IllegalStateException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 图形验证码校验（register 场景专用）
     *
     * @return 校验失败返回错误 AjaxResult；通过（或开关关闭）返回 null
     */
    private AjaxResult validateCaptcha(Map<String, String> body) {
        if (!configService.selectCaptchaEnabled()) {
            return null; // 全局开关关闭，依靠 60s 间隔 + 日限额 + IP 限流
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
     * 校验验证码（前端主动校验用；银行卡绑定等敏感操作由服务端在业务流程内二次校验，
     * 本端点仅作交互辅助，不替代业务侧校验）
     */
    @PostMapping("/code/verify")
    public AjaxResult verifyCode(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        String scene = body.get("scene");
        String code = body.get("code");
        if (scene == null || !ALLOWED_SCENES.contains(scene)) {
            return AjaxResult.error("不支持的业务场景");
        }
        boolean ok = smsCodeService.verifyCode(phone, scene, code);
        return ok ? AjaxResult.success("校验通过") : AjaxResult.error("验证码错误或已过期");
    }
}
