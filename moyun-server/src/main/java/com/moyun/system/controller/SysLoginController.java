package com.moyun.system.controller;

import com.moyun.common.constant.Constants;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.entity.SysMenu;
import com.moyun.core.base.entity.SysUser;
import com.moyun.core.base.model.LoginBody;
import com.moyun.core.base.model.LoginUser;
import com.moyun.core.security.auth.SysLoginService;
import com.moyun.core.security.auth.TokenService;
import com.moyun.core.security.permission.SysPermissionService;
import com.moyun.system.service.ISysConfigService;
import com.moyun.system.service.ISysMenuService;
import com.moyun.util.security.SecurityUtils;
import com.moyun.util.string.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

/**
 * 登录验证
 *
 * @author ruoyi
 */
@Tag(name = "认证管理", description = "系统登录、注销等认证相关接口")
@RestController
public class SysLoginController {
    @Autowired
    private SysLoginService loginService;

    @Autowired
    private ISysMenuService menuService;

    @Autowired
    private SysPermissionService permissionService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private ISysConfigService configService;

    /**
     * 登录方法
     *
     * @param loginBody 登录信息
     * @return 结果
     */
    @Operation(summary = "用户登录", description = "通过用户名密码进行登录认证，返回JWT Token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "登录成功"),
            @ApiResponse(responseCode = "401", description = "认证失败")
    })
    @PostMapping("/login")
    public AjaxResult login(
            @Parameter(description = "登录信息", required = true)
            @RequestBody LoginBody loginBody) {
        AjaxResult ajax = AjaxResult.success();
        // 生成令牌
        String token = loginService.login(loginBody.getUsername(), loginBody.getPassword(), loginBody.getCode(),
                loginBody.getUuid());
        ajax.put(Constants.TOKEN, token);
        return ajax;
    }

    /**
     * 获取用户信息
     *
     * @return 用户信息
     */
    @Operation(summary = "获取用户信息", description = "获取当前登录用户的详细信息、角色和权限列表")
    @GetMapping("getInfo")
    public AjaxResult getInfo() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        SysUser user = loginUser.getUser();
        // 角色集合
        Set<String> roles = permissionService.getRolePermission(user);
        // 权限集合
        Set<String> permissions = permissionService.getMenuPermission(user);
        if (!loginUser.getPermissions().equals(permissions)) {
            loginUser.setPermissions(permissions);
            tokenService.refreshToken(loginUser);
        }
        AjaxResult ajax = AjaxResult.success();
        ajax.put("user", user);
        ajax.put("roles", roles);
        ajax.put("permissions", permissions);
//        ajax.put("isDefaultModifyPwd", initPasswordIsModify(user.getPwdUpdateDate()));
//        ajax.put("isPasswordExpired", passwordIsExpiration(user.getPwdUpdateDate()));
        return ajax;
    }

    /**
     * 获取路由信息
     *
     * @return 路由信息
     */
    @Operation(summary = "获取路由信息", description = "获取当前登录用户的菜单路由信息，用于前端动态生成菜单")
    @SecurityRequirement(name = "Authorization")
    @GetMapping("getRouters")
    public AjaxResult getRouters() {
        Long userId = SecurityUtils.getUserId();
        List<SysMenu> menus = menuService.selectMenuTreeByUserId(userId);
        return AjaxResult.success(menuService.buildMenus(menus));
    }

    /**
     * 检查初始密码是否提醒修改
     *
     * @param pwdUpdateDate 密码最后更新时间
     * @return 是否需要提醒修改
     */
    public boolean initPasswordIsModify(LocalDateTime pwdUpdateDate) {
        String configValue = configService.selectConfigByKey("sys.account.initPasswordModify");
        Integer initPasswordModify = parseConfigValue(configValue);
        return initPasswordModify != null && initPasswordModify == 1 && pwdUpdateDate == null;
    }

    /**
     * 检查密码是否过期
     *
     * @param pwdUpdateDate 密码最后更新时间
     * @return 密码是否过期
     */
    public boolean passwordIsExpiration(LocalDateTime pwdUpdateDate) {
        String configValue = configService.selectConfigByKey("sys.account.passwordValidateDays");
        Integer passwordValidateDays = parseConfigValue(configValue);
        if (passwordValidateDays != null && passwordValidateDays > 0) {
            if (pwdUpdateDate == null) {
                return true;
            }
            long days = ChronoUnit.DAYS.between(pwdUpdateDate, LocalDateTime.now());
            return days > passwordValidateDays;
        }
        return false;
    }

    /**
     * 安全解析配置值为整数
     *
     * @param configValue 配置值字符串
     * @return 解析后的整数，解析失败返回null
     */
    private Integer parseConfigValue(String configValue) {
        if (StringUtils.isEmpty(configValue)) {
            return null;
        }
        try {
            return Integer.valueOf(configValue.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}