package com.moyun.community.auth.controller.app;

import com.moyun.community.auth.model.dto.LoginDto;
import com.moyun.community.auth.model.dto.RegisterDto;
import com.moyun.community.auth.model.vo.UserVo;
import com.moyun.community.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户认证Controller
 *
 * @author moyun
 * @date 2026-04-21
 */
@Tag(name = "用户认证", description = "用户登录、注册、登出等认证接口")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     */
    @Operation(summary = "用户登录", description = "使用账号密码或手机号登录，返回Token和用户信息")
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @Parameter(description = "登录信息", required = true)
            @RequestBody LoginDto dto) {
        return ResponseEntity.ok(authService.login(dto));
    }

    /**
     * 用户注册
     */
    @Operation(summary = "用户注册", description = "新用户注册账号")
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(
            @Parameter(description = "注册信息", required = true)
            @RequestBody RegisterDto dto) {
        return ResponseEntity.ok(authService.register(dto));
    }

    /**
     * 用户登出
     */
    @Operation(summary = "用户登出", description = "退出登录，清除Token")
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Boolean>> logout() {
        authService.logout();
        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * 获取当前用户信息
     */
    @Operation(summary = "获取当前用户", description = "获取当前登录用户的详细信息")
    @GetMapping("/me")
    public ResponseEntity<UserVo> getCurrentUser() {
        return ResponseEntity.ok(authService.getCurrentUser());
    }
}
