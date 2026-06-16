package com.moyun.portal.controller;

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
import com.moyun.common.constant.Constants;
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

    /**
     * 登录方法
     */
    @Operation(summary = "用户登录", description = "用户登录获取Token")
    @PostMapping("/login")
    public AjaxResult login(
            @Parameter(description = "登录信息") @RequestBody LoginBody loginBody) {
        return portalLoginService.login(loginBody);
    }

    /**
     * 注册方法
     */
    @Operation(summary = "用户注册", description = "注册新门户用户")
    @PostMapping("/register")
    public AjaxResult register(
            @Parameter(description = "用户信息") @RequestBody PortalUser portalUser) {
        if (StringUtils.isEmpty(portalUser.getUsername()) || StringUtils.isEmpty(portalUser.getPassword())) {
            return AjaxResult.error("用户名或密码不能为空");
        }

        // 设置默认角色
        if (StringUtils.isEmpty(portalUser.getRole())) {
            portalUser.setRole("user");
        }

        // 设置默认状态
        portalUser.setStatus("0");

        boolean success = portalUserService.registerPortalUser(portalUser);
        if (success) {
            // 注册成功后，直接登录并返回 token
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(portalUser.getUsername(), portalUser.getPassword()));

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
     * 获取当前用户信息
     */
    @Operation(summary = "获取当前用户信息", description = "获取已登录用户的信息")
    @GetMapping("/user/info")
    public AjaxResult getInfo() {
        PortalLoginUser loginUser = getCurrentLoginUser();
        if (loginUser == null) {
            return AjaxResult.error("用户未登录");
        }
        return AjaxResult.success(toUserVo(loginUser.getUser()));
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
