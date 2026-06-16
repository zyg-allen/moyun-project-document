package com.moyun.core.security.auth;

import com.moyun.core.base.entity.SysUser;
import com.moyun.core.base.model.LoginUser;
import com.moyun.system.service.ISysMenuService;
import com.moyun.system.service.ISysUserService;
import com.moyun.util.string.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

/**
 * 用户验证处理
 *
 * @author ruoyi
 */
@Service
@Primary
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    @Qualifier("sysUserServiceImpl")
    private ISysUserService userService;

    @Autowired
    @Qualifier("sysMenuServiceImpl")
    private ISysMenuService menuService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userService.selectUserByUserName(username);
        if (StringUtils.isNull(user)) {
            log.info("登录用户：{} 不存在.", username);
            throw new UsernameNotFoundException("登录用户：" + username + " 不存在");
        } else if ("1".equals(user.getDelFlag())) {
            log.info("登录用户：{} 已被删除.", username);
            throw new UsernameNotFoundException("登录用户：" + username + " 已被删除");
        } else if ("1".equals(user.getStatus())) {
            log.info("登录用户：{} 已被停用.", username);
            throw new UsernameNotFoundException("登录用户：" + username + " 已被停用");
        }

        return createLoginUser(user);
    }

    public UserDetails createLoginUser(SysUser user) {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getUserId());
        loginUser.setDeptId(user.getDeptId());
        //loginUser.setUsername(user.getUserName());
        loginUser.setUser(user);
        // 确保权限集合不为 null
        Set<String> permissions = menuService.selectMenuPermsByUserId(user.getUserId());
        if (permissions == null) {
            permissions = new HashSet<>();
        }
        loginUser.setPermissions(permissions);
        return loginUser;
    }
}