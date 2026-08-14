package com.moyun.portal.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.moyun.common.constant.Constants;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.model.LoginBody;
import com.moyun.core.config.redis.RedisCache;
import com.moyun.core.manager.AsyncManager;
import com.moyun.core.manager.factory.AsyncFactory;
import com.moyun.portal.controller.PortalLoginController;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.domain.model.PortalLoginUser;
import com.moyun.portal.security.auth.PortalTokenService;
import com.moyun.portal.service.IPortalUserService;
import com.moyun.system.service.ISysConfigService;
import com.moyun.util.http.ServletUtils;
import com.moyun.util.ip.IpUtils;
import com.moyun.util.string.StringUtils;

import java.time.LocalDateTime;

/**
 * 门户登录服务
 *
 * @author moyun
 */
@Slf4j
@Service
public class PortalLoginServiceImpl {

    @Autowired
    @Qualifier("portalAuthenticationManager")
    private AuthenticationManager authenticationManager;

    @Autowired
    @Qualifier("portalTokenService")
    private PortalTokenService portalTokenService;

    @Autowired
    @Qualifier("portalUserServiceImpl")
    private IPortalUserService portalUserService;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private ISysConfigService configService;

    /**
     * 登录验证
     *
     * @param loginBody 登录信息
     * @return 结果
     */
    public AjaxResult login(LoginBody loginBody) {
        String username = loginBody.getUsername();
        String password = loginBody.getPassword();

        // 验证码校验（受 sys.account.captchaEnabled 开关控制，关闭时跳过，登录流程不受影响）
        String captchaError = validateCaptcha(loginBody.getCode(), loginBody.getUuid());
        if (captchaError != null) {
            return AjaxResult.error(captchaError);
        }

        // 登录前置校验
        loginPreCheck(username, password);

        // 用户验证
        Authentication authentication = null;
        try {
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(username, password);
            // 该方法会去调用PortalUserDetailsServiceImpl.loadUserByUsername
            authentication = authenticationManager.authenticate(authenticationToken);
        } catch (Exception e) {
            String errMsg;
            if (e instanceof BadCredentialsException) {
                log.info("门户登录用户：{} 密码错误.", username);
                errMsg = "用户名或密码错误";
            } else {
                log.info("门户登录用户：{} 验证失败: {}", username, e.getMessage());
                errMsg = e.getMessage();
            }
            // 记录门户登录失败日志（user_type=portal，供后台首页登录趋势/今日登录统计使用）
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, errMsg, "portal"));
            return AjaxResult.error(errMsg);
        }

        log.info("门户登录用户：{} 成功.", username);

        // 获取登录用户信息
        PortalLoginUser loginUser = (PortalLoginUser) authentication.getPrincipal();
        PortalUser portalUser = loginUser.getUser();

        // 记录门户登录成功日志（user_type=portal）
        AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_SUCCESS, "门户登录成功", "portal"));

        // 更新 portal_user 最后登录IP/时间（修复：原为死字段，门户登录不维护）
        try {
            String ip = IpUtils.getIpAddr(ServletUtils.getRequest());
            portalUser.setLoginIp(ip);
            portalUser.setLoginDate(LocalDateTime.now());
            PortalUser update = new PortalUser();
            update.setId(portalUser.getId());
            update.setLoginIp(ip);
            update.setLoginDate(LocalDateTime.now());
            portalUserService.updatePortalUser(update);
        } catch (Exception ex) {
            // 更新登录信息失败不影响登录主流程
            log.warn("更新门户用户登录信息失败: {}", ex.getMessage());
        }

        // 生成token
        String token = portalTokenService.createToken(loginUser);

        // 构建响应数据
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put(Constants.TOKEN, token);
        data.put("user", toUserVo(portalUser));

        return AjaxResult.success(data);
    }

    /**
     * 登录前置校验
     *
     * @param username 用户名
     * @param password 用户密码
     */
    public void loginPreCheck(String username, String password) {
        // 用户名或密码为空 错误
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            throw new RuntimeException("用户名或密码不能为空");
        }
        // 密码长度检查
        if (password.length() < 6 || password.length() > 50) {
            throw new RuntimeException("密码长度必须在6-50个字符之间");
        }
        // 用户名长度检查
        if (username.length() < 2 || username.length() > 50) {
            throw new RuntimeException("用户名长度必须在2-50个字符之间");
        }
    }

    /**
     * 校验图形验证码
     * <p>
     * 与后台登录保持一致：受 sys.account.captchaEnabled 开关控制。
     * 开关关闭时直接放行（返回 null），不影响现有登录/注册流程；
     * 开关开启时按 Redis 中 captcha_codes:{uuid} 的值做一次性校验。
     *
     * @param code 用户输入的验证码
     * @param uuid 验证码唯一标识
     * @return 校验失败时的错误提示；成功返回 null
     */
    public String validateCaptcha(String code, String uuid) {
        if (!configService.selectCaptchaEnabled()) {
            return null;
        }
        String verifyKey = Constants.CAPTCHA_CODE_KEY + StringUtils.nvl(uuid, "");
        String captcha = redisCache.getCacheObject(verifyKey);
        // 一次性使用，无论成功失败都清除
        redisCache.deleteObject(verifyKey);
        if (captcha == null) {
            return "验证码已失效，请重新获取";
        }
        if (StringUtils.isEmpty(code) || !code.equalsIgnoreCase(captcha)) {
            return "验证码错误";
        }
        return null;
    }

    /**
     * 转换为用户VO（隐藏敏感信息）
     */
    private PortalLoginController.UserVo toUserVo(PortalUser user) {
        PortalLoginController.UserVo vo = new PortalLoginController.UserVo();
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
}
