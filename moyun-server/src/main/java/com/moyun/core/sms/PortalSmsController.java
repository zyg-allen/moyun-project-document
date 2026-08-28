package com.moyun.core.sms;

import com.moyun.core.base.AjaxResult;
import com.moyun.portal.util.PortalSecurityUtils;
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
    private static final java.util.Set<String> ALLOWED_SCENES = java.util.Set.of("bankcard", "member");

    @Autowired
    private SmsCodeService smsCodeService;

    /** 发送验证码（需登录；接收手机号即请求体手机号，频控在服务层） */
    @PostMapping("/code/send")
    public AjaxResult sendCode(@RequestBody Map<String, String> body) {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error(401, "登录已过期，请重新登录");
        }
        String phone = body.get("phone");
        String scene = body.get("scene");
        if (scene == null || !ALLOWED_SCENES.contains(scene)) {
            return AjaxResult.error("不支持的业务场景");
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
