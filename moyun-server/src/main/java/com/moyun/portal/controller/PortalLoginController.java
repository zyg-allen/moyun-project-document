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
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
     * <p>双重人机校验：① 发送短信/邮箱验证码前的图形码弹窗（一次性作废）；
     * ② 注册提交时的图形码校验（本方法，开关控制、一次性作废），防止脚本批量注册。</p>
     * <p>幂等保护：图形码与短信/邮箱验证码均为一次性消费；同一手机号/邮箱已存在时
     * 不允许出现第二条 portal_user 记录——正常在用账号直接拒绝，
     * 已注销账号（del_flag='2'）复活原记录（沿用原 id，保留历史数据关联）。</p>
     */
    @Operation(summary = "用户注册", description = "注册新门户用户")
    // NAT 共享 IP（校园网/公司）下同 IP 众多真实用户，原 3次/小时 会误伤；
    // 放宽为 20次/10分钟 防脚本轰炸，真实用户几乎无感（有双重人机校验+短信/邮箱验证码兜底）
    @RateLimiter(time = 600, count = 20, limitType = LimitType.IP)
    @PostMapping("/register")
    public AjaxResult register(
            @Parameter(description = "用户信息") @RequestBody PortalUser portalUser) {

        if (StringUtils.isEmpty(portalUser.getUsername()) || StringUtils.isEmpty(portalUser.getPassword())) {
            return AjaxResult.error("用户名或密码不能为空");
        }

        // 注册提交的图形验证码人机校验（sys.account.captchaEnabled 开关控制，一次性作废）
        String captchaError = portalLoginService.validateCaptcha(portalUser.getCode(), portalUser.getUuid());
        if (captchaError != null) {
            return AjaxResult.error(captchaError);
        }

        // 注册方式双轨——手机短信 或 邮箱验证码（二选一）
        boolean revive = false;
        Long reviveId = null;
        // 用户名查重（不过滤 del_flag：uk_username 唯一索引对注销账号同样生效）
        PortalUser existUsername = portalUserMapper.selectPortalUserByUsernameAny(portalUser.getUsername());

        if (StringUtils.isNotEmpty(portalUser.getPhone())) {
            // 手机号占用校验（含注销账号；@TableLogic 会漏查 del_flag='2'，故用自定义查询）
            PortalUser existPhone = portalUserMapper.selectPortalUserByPhoneAny(portalUser.getPhone());
            if (existPhone != null && "0".equals(existPhone.getDelFlag())) {
                return AjaxResult.error("该手机号已注册，请直接登录");
            }
            if (existPhone != null) {
                // 已注销账号复活：沿用原记录 id，不新增 portal_user
                revive = true;
                reviveId = existPhone.getId();
                // 新用户名若被其他账号占用（含注销账号）则拒绝
                if (existUsername != null && !existUsername.getId().equals(reviveId)) {
                    return AjaxResult.error("用户名已被其他账号占用（含已注销账号），请更换");
                }
            } else if (existUsername != null) {
                return AjaxResult.error("用户名已被占用（含已注销账号），如需找回原账号请使用原手机号/邮箱注册");
            }
            // 短信验证码校验（verifyCode 通过即一次性消费；放在查重之后，避免查重失败烧掉验证码）
            if (StringUtils.isEmpty(portalUser.getSmsCode())) {
                return AjaxResult.error("请获取短信验证码");
            }
            if (!smsCodeService.verifyCode(portalUser.getPhone(), "register", portalUser.getSmsCode())) {
                return AjaxResult.error("短信验证码错误或已过期");
            }
        } else {
            // 邮箱占用校验（含注销账号）
            if (StringUtils.isEmpty(portalUser.getEmail()) || StringUtils.isEmpty(portalUser.getEmailCode())) {
                return AjaxResult.error("请填写邮箱并获取邮箱验证码");
            }
            PortalUser existEmail = portalUserMapper.selectPortalUserByEmailAny(portalUser.getEmail());
            if (existEmail != null && "0".equals(existEmail.getDelFlag())) {
                return AjaxResult.error("该邮箱已注册，请直接登录");
            }
            if (existEmail != null) {
                // 已注销账号复活：沿用原记录 id，不新增 portal_user
                revive = true;
                reviveId = existEmail.getId();
                if (existUsername != null && !existUsername.getId().equals(reviveId)) {
                    return AjaxResult.error("用户名已被其他账号占用（含已注销账号），请更换");
                }
            } else if (existUsername != null) {
                return AjaxResult.error("用户名已被占用（含已注销账号），如需找回原账号请使用原手机号/邮箱注册");
            }
            // 邮箱验证码校验（一次性消费；放在查重之后，避免查重失败烧掉验证码）
            if (!portalEmailService.verifyCode(portalUser.getEmail(), portalUser.getEmailCode(), "register")) {
                return AjaxResult.error("邮箱验证码错误或已过期");
            }
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

        // 复活：更新原记录注册凭据并恢复 del_flag='0'；否则插入新记录
        // 并发兜底：两请求同时通过查重时会撞唯一索引，捕获后返回友好提示（幂等，不产生脏数据）
        boolean success;
        try {
            success = revive
                    ? portalUserService.revivePortalUser(reviveId, portalUser)
                    : portalUserService.registerPortalUser(portalUser);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            log.warn("注册并发唯一键冲突: username={}, phone={}, email={}",
                    portalUser.getUsername(), portalUser.getPhone(), portalUser.getEmail());
            return AjaxResult.error("注册冲突：该用户名/手机号/邮箱刚被其他用户注册，请更换后重试");
        }
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

            return AjaxResult.success(revive ? "注册成功（已恢复原账号历史数据）" : "注册成功", data);
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
            log.error("获取当前登录用户异常", e);
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
