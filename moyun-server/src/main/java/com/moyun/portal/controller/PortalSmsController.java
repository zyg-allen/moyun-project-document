package com.moyun.portal.controller;

import com.moyun.common.annotation.Anonymous;
import com.moyun.common.constant.Constants;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.sms.SmsCodeService;
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
 * 门户短信验证码控制器
 *
 * <p>发送方为当前登录用户（手机号即接收人，从请求体传入但服务端绑定场景校验），
 * 敏感操作（银行卡绑定等）在业务层调用 {@code SmsCodeService.verifyCode} 完成闭环。
 *
 * <p>场景白名单：bankcard=银行卡绑定；member 预留给后期会员支付开通；
 * register=注册（匿名+图形码）；reset_password=找回密码（匿名+图形码，要求手机号已注册）。
 *
 * @author moyun
 */
@RestController
@RequestMapping("/portal/sms")
public class PortalSmsController {

    /** 允许的业务场景白名单（防任意 scene 写爆 Redis） */
    private static final java.util.Set<String> ALLOWED_SCENES =
            java.util.Set.of("bankcard", "member", "register", "reset_password");

    @Autowired
    private SmsCodeService smsCodeService;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private com.moyun.portal.mapper.PortalUserMapper portalUserMapper;

    @Autowired
    private com.moyun.portal.service.IPortalUserService portalUserService;

    /**
     * 发送验证码（bankcard/member 场景需登录；register/reset_password 场景匿名可发——
     * 注册/找回密码时用户尚未登录，服务层已有 60s 间隔 + 日限额 + IP 层 @RateLimiter 防轰炸）
     * <p>方法级 @Anonymous 放行安全链（场景级登录校验由下方 if 兜底，
     * 非 register/reset_password 场景未登录返回 401 业务错误）
     * <p>register/reset_password 场景发送前强制图形验证码人机校验（跟随 sys.account.captchaEnabled
     * 开关，开启时前端弹窗输入，验证码一次性作废防重放）；reset_password 额外要求手机号已注册
     */
    @Anonymous
    @PostMapping("/code/send")
    public AjaxResult sendCode(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        String scene = body.get("scene");
        if (scene == null || !ALLOWED_SCENES.contains(scene)) {
            return AjaxResult.error("不支持的业务场景");
        }
        boolean cancelled = false;
        if ("register".equals(scene) || "reset_password".equals(scene)) {
            // 匿名场景：图形验证码人机校验（开关开启时）
            AjaxResult captchaError = validateCaptcha(body);
            if (captchaError != null) {
                return captchaError;
            }
            // 找回密码：手机号必须已注册（含注销账号——注销账号通过找回密码自助恢复，无需找管理员）
            if ("reset_password".equals(scene)) {
                com.moyun.portal.domain.entity.PortalUser exist = portalUserMapper.selectPortalUserByPhoneAny(phone);
                if (exist == null) {
                    return AjaxResult.error("该手机号未注册");
                }
                cancelled = "2".equals(exist.getDelFlag());
            }
        } else {
            Long userId = PortalSecurityUtils.getUserId();
            if (userId == null) {
                return AjaxResult.error(401, "登录已过期，请重新登录");
            }
        }
        try {
            smsCodeService.sendCode(phone, scene);
            // 注销账号发码成功时明确提示：完成验证将自动恢复，避免用户误以为账号废弃
            return cancelled
                    ? AjaxResult.success("验证码已发送，该账号已注销，完成验证后将自动恢复")
                    : AjaxResult.success("验证码已发送");
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

    /**
     * 找回密码：短信验证码重置密码（手机号注册用户专用）
     * <p>流程与 /portal/email/reset-password 完全对称：
     * 图形码人机校验在发送短信环节完成（弹窗一次性作废），本端点校验短信验证码
     * （verifyCode 通过即一次性消费）+ 重置密码，幂等无副作用。
     * <p>仅正常在用账号（del_flag='0'）可找回；注销账号需重新注册复活。
     */
    @Anonymous
    @com.moyun.common.annotation.RateLimiter(time = 60, count = 5, limitType = com.moyun.common.enums.LimitType.IP)
    @PostMapping("/reset-password")
    public AjaxResult resetPassword(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        String code = body.get("code");
        String newPassword = body.get("newPassword");
        String confirmPassword = body.get("confirmPassword");
        if (phone == null || phone.isBlank() || code == null || code.isBlank()) {
            return AjaxResult.error("请填写手机号和短信验证码");
        }
        // 清单 P2：原先只判"长度不少于 6 位"，既不校验上限也不校验复杂度，
        // 而邮箱通道（PortalEmailServiceImpl#resetPassword）要求 **6-20 位且含大小写字母和数字**。
        // 同一账号体系两条找回路径策略必须一致，否则短信通道成为弱口令入口。
        if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 20) {
            return AjaxResult.error("密码长度必须为 6-20 位");
        }
        if (!newPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$")) {
            return AjaxResult.error("密码必须包含大小写字母和数字");
        }
        if (confirmPassword != null && !newPassword.equals(confirmPassword)) {
            return AjaxResult.error("两次输入的密码不一致");
        }
        // 账号存在性校验（含注销账号：注销用户通过本流程重置密码并自动恢复）
        com.moyun.portal.domain.entity.PortalUser user = portalUserMapper.selectPortalUserByPhoneAny(phone);
        if (user == null) {
            return AjaxResult.error("该手机号未注册");
        }
        // 短信验证码校验（一次性消费，防重放）
        if (!smsCodeService.verifyCode(phone, "reset_password", code)) {
            return AjaxResult.error("短信验证码错误或已过期");
        }
        // 注销账号：重置密码的同时自动恢复（del_flag '2'->'0'）；恢复前做唯一键防御校验
        boolean cancelled = "2".equals(user.getDelFlag());
        if (cancelled) {
            com.moyun.portal.domain.entity.PortalUser check = new com.moyun.portal.domain.entity.PortalUser();
            check.setId(user.getId());
            check.setUsername(user.getUsername());
            check.setPhone(user.getPhone());
            check.setEmail(user.getEmail());
            String conflict = portalUserService.checkUniqueBusinessKeys(check);
            if (conflict != null) {
                return AjaxResult.error("密码未重置，账号恢复受阻：" + conflict + "，请联系客服处理");
            }
        }
        com.moyun.portal.domain.entity.PortalUser update = new com.moyun.portal.domain.entity.PortalUser();
        update.setId(user.getId());
        // BCrypt 加密后落库（resetPortalUserPwd 内部也是防御性加密，此处因需同时恢复 del_flag 改用一条 update）
        update.setPassword(com.moyun.util.security.SecurityUtils.encryptPassword(newPassword));
        if (cancelled) {
            update.setDelFlag("0");
            update.setStatus("0");
        }
        // updatePortalUser 绕 @TableLogic（where 仅 id），注销账号也可命中
        int rows = portalUserMapper.updatePortalUser(update);
        if (rows > 0) {
            return cancelled
                    ? AjaxResult.success("密码重置成功，账号已恢复，请使用新密码登录")
                    : AjaxResult.success("密码重置成功，请使用新密码登录");
        }
        return AjaxResult.error("密码重置失败");
    }
}
