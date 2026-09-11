package com.moyun.portal.controller;

/**
 * 文件变更说明：
 * 已删除 GET /portal/user/info (getInfo) 接口，该接口为死接口，
 * 前端已改用 /portal/user/me 获取当前用户信息。
 * 本次仅清理 Controller 层方法，对应 Service/Mapper/XML 实现保持不变。
 */

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.moyun.common.annotation.Anonymous;
import com.moyun.common.annotation.RateLimiter;
import com.moyun.common.constant.Constants;
import com.moyun.common.enums.LimitType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.model.LoginBody;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.domain.model.PortalLoginUser;
import com.moyun.portal.security.auth.PortalTokenService;
import com.moyun.portal.service.IPortalUserService;
import com.moyun.portal.service.impl.PortalLoginServiceImpl;
import com.moyun.util.security.SecurityUtils;
import com.moyun.util.string.StringUtils;

/**
 * 门户登录验证
 *
 * @author moyun
 */
@Anonymous
@Tag(name = "门户登录", description = "门户用户登录注册相关接口")
@RestController
@RequestMapping("/portal")
public class PortalLoginController {

    @Autowired
    @Qualifier("portalAuthenticationManager")
    private AuthenticationManager authenticationManager;

    @Autowired
    private PortalTokenService portalTokenService;

    @Autowired
    private IPortalUserService portalUserService;

    @Autowired
    private PortalLoginServiceImpl portalLoginService;

    @Autowired
    private com.moyun.portal.service.PortalEmailService portalEmailService;

    @org.springframework.beans.factory.annotation.Autowired
    private com.moyun.core.sms.SmsCodeService smsCodeService;

    @org.springframework.beans.factory.annotation.Autowired
    private com.moyun.portal.mapper.PortalUserMapper portalUserMapper;

    /**
     * 登录方法
     */
    @Operation(summary = "用户登录", description = "用户登录获取Token")
    @RateLimiter(time = 60, count = 5, limitType = LimitType.IP)
    @PostMapping("/login")
    public AjaxResult login(
            @Parameter(description = "登录信息") @RequestBody LoginBody loginBody) {
        return portalLoginService.login(loginBody);
    }

    /**
     * 注册方法
     * <p>v11.42：人机校验已前移至发送短信/邮箱验证码时的图形码弹窗（一次性作废），
     * 注册提交不再校验图形码，由短信/邮箱验证码（一次性消费）+ IP 限流保护。
     */
    @Operation(summary = "用户注册", description = "注册新门户用户")
    // v11.42：NAT 共享 IP（校园网/公司）下同 IP 众多真实用户，原 3次/小时 会误伤；
    // 放宽为 20次/10分钟 防脚本轰炸，真实用户几乎无感（有人机校验+短信/邮箱验证码兜底）
    @RateLimiter(time = 600, count = 20, limitType = LimitType.IP)
    @PostMapping("/register")
    public AjaxResult register(
            @Parameter(description = "用户信息") @RequestBody PortalUser portalUser) {

        if (StringUtils.isEmpty(portalUser.getUsername()) || StringUtils.isEmpty(portalUser.getPassword())) {
            return AjaxResult.error("用户名或密码不能为空");
        }

        // v11.35：注册方式双轨——手机短信 或 邮箱验证码（二选一）
        if (StringUtils.isNotEmpty(portalUser.getPhone())) {
            // 手机号注册：短信验证码校验（verifyCode 通过即一次性消费）
            if (StringUtils.isEmpty(portalUser.getSmsCode())) {
                return AjaxResult.error("请获取短信验证码");
            }
            if (!smsCodeService.verifyCode(portalUser.getPhone(), "register", portalUser.getSmsCode())) {
                return AjaxResult.error("短信验证码错误或已过期");
            }
            // 手机号占用校验（同一手机号不可重复注册账号）
            Long phoneCount = portalUserMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PortalUser>()
                            .eq(PortalUser::getPhone, portalUser.getPhone()));
            if (phoneCount != null && phoneCount > 0) {
                return AjaxResult.error("该手机号已注册，请直接登录");
            }
        } else {
            // 邮箱注册：验证码校验保证邮箱真实可用（一次性消费，校验后失效）
            if (StringUtils.isEmpty(portalUser.getEmail()) || StringUtils.isEmpty(portalUser.getEmailCode())) {
                return AjaxResult.error("请填写邮箱并获取邮箱验证码");
            }
            if (!portalEmailService.verifyCode(portalUser.getEmail(), portalUser.getEmailCode(), "register")) {
                return AjaxResult.error("邮箱验证码错误或已过期");
            }
            // v11.42：verifyCode 改为校验通过即一次性消费（与短信一致），无需再补 consumeCode
        }

        // 设置默认角色
        if (StringUtils.isEmpty(portalUser.getRole())) {
            portalUser.setRole("user");
        }

        // 设置默认状态
        portalUser.setStatus("0");

        // 保留明文密码，用于注册成功后立即登录认证
        String rawPassword = portalUser.getPassword();

        // 密码 BCrypt 加密后再落库（避免明文存储），Service 层亦有兜底加密
        portalUser.setPassword(SecurityUtils.encryptPassword(rawPassword));

        boolean success = portalUserService.registerPortalUser(portalUser);
        if (success) {
            // 注册成功后，直接登录并返回 token（验证码已在上方校验时一次性消费）
            // 注意：此处使用注册前的明文密码做认证，BCryptPasswordEncoder.matches 会自动完成校验
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(portalUser.getUsername(), rawPassword));

            // 生成token
            PortalLoginUser loginUser = (PortalLoginUser) authentication.getPrincipal();
            String token = portalTokenService.createToken(loginUser);

            // 构建响应数据
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put(Constants.TOKEN, token);
            data.put("user", toUserVo(loginUser.getUser()));

            return AjaxResult.success("注册成功", data);
        } else {
            return AjaxResult.error("注册失败");
        }
    }

    /**
     * 退出登录
     */
    @Operation(summary = "退出登录", description = "清除登录状态")
    @PostMapping("/logout")
    public AjaxResult logout() {
        PortalLoginUser loginUser = getCurrentLoginUser();
        if (loginUser != null) {
            portalTokenService.delLoginUser(loginUser.getToken());
        }
        return AjaxResult.success("退出成功");
    }

    /**
     * 获取当前登录用户
     */
    private PortalLoginUser getCurrentLoginUser() {
        try {
            Authentication authentication = SecurityUtils.getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof PortalLoginUser) {
                return (PortalLoginUser) authentication.getPrincipal();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 转换为用户VO（隐藏敏感信息）
     */
    private UserVo toUserVo(PortalUser user) {
        UserVo vo = new UserVo();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setRole(user.getRole());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    /**
     * 用户VO类
     */
    public static class UserVo {
        private Long id;
        private String username;
        private String nickname;
        private String avatar;
        private String email;
        private String phone;
        private String role;
        private LocalDateTime createTime;

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }

        public String getAvatar() { return avatar; }
        public void setAvatar(String avatar) { this.avatar = avatar; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public LocalDateTime getCreateTime() { return createTime; }
        public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    }
}
