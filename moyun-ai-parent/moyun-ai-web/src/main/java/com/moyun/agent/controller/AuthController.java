package com.moyun.agent.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.moyun.agent.common.ApiResponse;
import com.moyun.agent.dto.CaptchaResponse;
import com.moyun.agent.dto.LoginRequest;
import com.moyun.agent.dto.LoginResponse;
import com.moyun.agent.entity.SysUser;
import com.moyun.agent.service.CaptchaService;
import com.moyun.agent.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 *
 * <p>提供登录、登出、验证码等认证相关接口</p>
 *
 * @author laomao
 * @time 2025/11/25
 */
@Slf4j
@Tag(name = "认证管理")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private SysUserService userService;

    @Autowired
    private CaptchaService captchaService;

    /**
     * 获取验证码
     */
    @Operation(summary = "获取验证码", description = "获取图形验证码，用于登录")
    @GetMapping("/captcha")
    public ResponseEntity<ApiResponse<CaptchaResponse>> getCaptcha() {
        try {
            CaptchaResponse captcha = captchaService.generateCaptcha();
            return ResponseEntity.ok(ApiResponse.success(captcha));
        } catch (Exception e) {
            log.error("获取验证码失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取验证码失败"));
        }
    }

    /**
     * 用户登录
     */
    @Operation(summary = "用户登录", description = "使用用户名、密码和验证码登录")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        try {
            // 1. 验证验证码
            if (!captchaService.verifyCaptcha(request.getCaptchaKey(), request.getCaptcha())) {
                return ResponseEntity.ok(ApiResponse.error("验证码错误或已过期"));
            }

            // 2. 查询用户
            SysUser user = userService.getByUsername(request.getUsername());
            if (user == null) {
                return ResponseEntity.ok(ApiResponse.error("用户名或密码错误"));
            }

            // 3. 检查用户状态
            if (user.getStatus() != 1) {
                return ResponseEntity.ok(ApiResponse.error("账号已被禁用"));
            }

            // 4. 验证密码
            if (!userService.checkPassword(request.getPassword(), user.getPassword())) {
                return ResponseEntity.ok(ApiResponse.error("用户名或密码错误"));
            }

            // 5. 登录成功，生成Token
            StpUtil.login(user.getId());
            String token = StpUtil.getTokenValue();

            // 6. 更新登录信息
            String ip = getClientIp(httpRequest);
            userService.updateLoginInfo(user.getId(), ip);

            // 7. 构建响应
            LoginResponse response = new LoginResponse();
            response.setToken(token);
            response.setUserId(user.getId());
            response.setUsername(user.getUsername());
            response.setNickname(user.getNickname());
            response.setAvatar(user.getAvatar());

            log.info("用户登录成功: username={}, ip={}", user.getUsername(), ip);
            return ResponseEntity.ok(ApiResponse.success("登录成功", response));

        } catch (Exception e) {
            log.error("登录失败", e);
            return ResponseEntity.ok(ApiResponse.error("登录失败: " + e.getMessage()));
        }
    }

    /**
     * 用户登出
     */
    @Operation(summary = "用户登出", description = "退出登录，清除Token")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        try {
            StpUtil.logout();
            return ResponseEntity.ok(ApiResponse.success("登出成功"));
        } catch (Exception e) {
            log.error("登出失败", e);
            return ResponseEntity.ok(ApiResponse.error("登出失败"));
        }
    }

    /**
     * 获取当前登录用户信息
     */
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息")
    @GetMapping("/userinfo")
    public ResponseEntity<ApiResponse<LoginResponse>> getUserInfo() {
        try {
            if (!StpUtil.isLogin()) {
                return ResponseEntity.ok(ApiResponse.error("未登录"));
            }

            Long userId = StpUtil.getLoginIdAsLong();
            SysUser user = userService.getById(userId);

            if (user == null) {
                return ResponseEntity.ok(ApiResponse.error("用户不存在"));
            }

            LoginResponse response = new LoginResponse();
            response.setToken(StpUtil.getTokenValue());
            response.setUserId(user.getId());
            response.setUsername(user.getUsername());
            response.setNickname(user.getNickname());
            response.setAvatar(user.getAvatar());

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("获取用户信息失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取用户信息失败"));
        }
    }

    /**
     * 检查登录状态
     */
    @Operation(summary = "检查登录状态", description = "检查当前Token是否有效")
    @GetMapping("/check")
    public ResponseEntity<ApiResponse<Boolean>> checkLogin() {
        return ResponseEntity.ok(ApiResponse.success(StpUtil.isLogin()));
    }

    /**
     * 修改密码
     */
    @Operation(summary = "修改密码", description = "修改当前登录用户的密码")
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@RequestBody java.util.Map<String, String> request) {
        try {
            if (!StpUtil.isLogin()) {
                return ResponseEntity.ok(ApiResponse.error("未登录"));
            }

            String newPassword = request.get("newPassword");
            if (newPassword == null || newPassword.length() < 6) {
                return ResponseEntity.ok(ApiResponse.error("密码长度不能少于6位"));
            }

            Long userId = StpUtil.getLoginIdAsLong();
            SysUser user = new SysUser();
            user.setId(userId);
            user.setPassword(userService.encodePassword(newPassword));
            userService.updateById(user);

            // 修改密码后登出，要求重新登录
            StpUtil.logout();

            log.info("用户修改密码成功: userId={}", userId);
            return ResponseEntity.ok(ApiResponse.success("密码修改成功"));
        } catch (Exception e) {
            log.error("修改密码失败", e);
            return ResponseEntity.ok(ApiResponse.error("修改密码失败"));
        }
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理时，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
