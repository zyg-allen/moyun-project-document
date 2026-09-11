package com.moyun.core.security.auth;

import com.moyun.common.constant.CacheConstants;
import com.moyun.common.constant.Constants;
import com.moyun.common.constant.UserConstants;
import com.moyun.common.exception.business.user.*;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.core.base.entity.SysUser;
import com.moyun.core.base.model.LoginUser;
import com.moyun.core.config.redis.RedisCache;
import com.moyun.core.manager.AsyncManager;
import com.moyun.core.manager.factory.AsyncFactory;
import com.moyun.core.security.context.AuthenticationContextHolder;
import com.moyun.system.service.ISysConfigService;
import com.moyun.system.service.ISysUserService;
import com.moyun.util.ip.IpUtils;
import com.moyun.util.string.MessageUtils;
import com.moyun.util.string.StringUtils;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 登录校验方法
 *
 * <p>v11.32 验证码风控：
 * <ul>
 *   <li>全局开关：sys_config sys.account.captchaEnabled（缓存带 TTL，v11.32）</li>
 *   <li>风险触发（即使全局关闭也强制验证）：密码错误 ≥{@code captcha.riskFailThreshold} 次（复用 pwd_err_cnt），
 *       或距上次成功登录 ≥{@code captcha.riskInactiveDays} 天（含从未登录）</li>
 *   <li>密码错误计数/锁定（user.password.maxRetryCount/lockTime）自 v11.32 起真正接入登录链路</li>
 * </ul>
 *
 * @author allen-zyg
 */
@Component
public class SysLoginService {
    @Autowired
    private TokenService tokenService;

    @Resource
    private AuthenticationManager authenticationManager;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private SysPasswordService passwordService;

    /** 风险验证码阈值：密码错误达到该次数即要求验证码（低于 maxRetryCount 锁定线） */
    @Value("${captcha.riskFailThreshold:2}")
    private int riskFailThreshold;

    /** 风险验证码阈值：距上次成功登录超过该天数即要求验证码 */
    @Value("${captcha.riskInactiveDays:30}")
    private int riskInactiveDays;

    /**
     * 登录验证
     *
     * @param username 用户名
     * @param password 密码
     * @param code     验证码
     * @param uuid     唯一标识
     * @return 结果
     */
    public String login(String username, String password, String code, String uuid) {
        // 验证码校验（全局开关 || 风险触发）
        validateCaptcha(username, code, uuid);
        // 登录前置校验
        loginPreCheck(username, password);
        // 用户验证
        Authentication authentication = null;
        try {
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, password);
            AuthenticationContextHolder.setContext(authenticationToken);
            // v11.32：密码错误计数/锁定（pwd_err_cnt）。原 SysPasswordService.validate 从未接入登录链路（死代码），
            // 此处补接，同时为风险验证码判定提供错误次数数据源
            SysUser user = userService.selectUserByUserName(username);
            if (user != null && !"1".equals(user.getDelFlag()) && !"1".equals(user.getStatus())) {
                passwordService.validate(user);
            }
            // 该方法会去调用UserDetailsServiceImpl.loadUserByUsername
            authentication = authenticationManager.authenticate(authenticationToken);
        } catch (BadCredentialsException | UserPasswordNotMatchException e) {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.password.not.match")));
            throw new UserPasswordNotMatchException();
        } catch (UserPasswordRetryLimitExceedException e) {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, e.getMessage()));
            throw e;
        } catch (Exception e) {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, e.getMessage()));
            throw new ServiceException(e.getMessage());
        } finally {
            AuthenticationContextHolder.clearContext();
        }
        AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_SUCCESS, MessageUtils.message("user.login.success")));
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        recordLoginInfo(loginUser.getUserId());
        // 生成token
        return tokenService.createToken(loginUser);
    }

    /**
     * 校验验证码
     *
     * <p>v11.32：要求验证码 = 全局开关开启 || 风险触发（密码错误过多 / 长时间未登录）。
     * 风险判定在服务端强制执行，前端仅负责展示验证码输入框，绕过前端无法跳过校验。
     *
     * @param username 用户名
     * @param code     验证码
     * @param uuid     唯一标识
     */
    public void validateCaptcha(String username, String code, String uuid) {
        boolean captchaRequired = configService.selectCaptchaEnabled() || isRiskCaptchaRequired(username);
        if (captchaRequired) {
            String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + StringUtils.nvl(uuid, "");
            String captcha = redisCache.getCacheObject(verifyKey);
            if (captcha == null) {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.expire")));
                throw new CaptchaExpireException();
            }
            redisCache.deleteObject(verifyKey);
            if (!code.equalsIgnoreCase(captcha)) {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.error")));
                throw new CaptchaException();
            }
        }
    }

    /**
     * 风险验证码判定（v11.32）
     *
     * <p>任一条件命中即要求验证码（即使全局开关关闭）：
     * <ol>
     *   <li>密码错误次数 ≥ riskFailThreshold（30 分钟滑动窗口内，复用 pwd_err_cnt）</li>
     *   <li>距上次成功登录 ≥ riskInactiveDays 天，或从未登录过（防异地/隔久爆破）</li>
     * </ol>
     *
     * @param username 用户名（可空，空时视为无风险——如登录页初次加载尚未输入用户名）
     * @return true 表示该账号需要验证码
     */
    public boolean isRiskCaptchaRequired(String username) {
        if (StringUtils.isEmpty(username)) {
            return false;
        }
        // 1. 密码错误次数（pwd_err_cnt，TTL=lockTime 分钟）
        Integer errCount = redisCache.getCacheObject(CacheConstants.PWD_ERR_CNT_KEY + username);
        if (errCount != null && errCount >= riskFailThreshold) {
            return true;
        }
        // 2. 长时间未登录（含从未登录）
        SysUser user = userService.selectUserByUserName(username);
        if (user != null && !"1".equals(user.getDelFlag()) && !"1".equals(user.getStatus())) {
            LocalDateTime lastLogin = user.getLoginDate();
            if (lastLogin == null || ChronoUnit.DAYS.between(lastLogin, LocalDateTime.now()) >= riskInactiveDays) {
                return true;
            }
        }
        return false;
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
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("not.null")));
            throw new UserNotExistsException();
        }
        // 密码如果不在指定范围内 错误
        if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
                || password.length() > UserConstants.PASSWORD_MAX_LENGTH) {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.password.not.match")));
            throw new UserPasswordNotMatchException();
        }
        // 用户名不在指定范围内 错误
        if (username.length() < UserConstants.USERNAME_MIN_LENGTH
                || username.length() > UserConstants.USERNAME_MAX_LENGTH) {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.password.not.match")));
            throw new UserPasswordNotMatchException();
        }
        // IP黑名单校验
        String blackStr = configService.selectConfigByKey("sys.login.blackIPList");
        if (IpUtils.isMatchedIp(blackStr, IpUtils.getIpAddr())) {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("login.blocked")));
            throw new BlackListException();
        }
    }

    /**
     * 记录登录信息
     *
     * @param userId 用户ID
     */
    public void recordLoginInfo(Long userId) {
        SysUser sysUser = new SysUser();
        sysUser.setUserId(userId);
        sysUser.setLoginIp(IpUtils.getIpAddr());
        sysUser.setLoginDate(LocalDateTime.now());
        userService.updateUserProfile(sysUser);
    }
}