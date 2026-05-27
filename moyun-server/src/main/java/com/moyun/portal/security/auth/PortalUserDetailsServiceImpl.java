package com.moyun.portal.security.auth;

import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.domain.model.PortalLoginUser;
import com.moyun.portal.service.IPortalUserService;
import com.moyun.util.string.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

/**
 * 门户用户验证处理
 *
 * @author moyun
 */
@Service("portalUserDetailsServiceImpl")
public class PortalUserDetailsServiceImpl implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(PortalUserDetailsServiceImpl.class);

    @Autowired
    @Qualifier("portalUserServiceImpl")
    private IPortalUserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        PortalUser user = userService.selectPortalUserByUsername(username);
        if (StringUtils.isNull(user)) {
            log.info("登录用户：{} 不存在.", username);
            throw new UsernameNotFoundException("登录用户：" + username + " 不存在");
        } else if ("2".equals(user.getDelFlag())) {
            log.info("登录用户：{} 已被删除.", username);
            throw new UsernameNotFoundException("登录用户：" + username + " 已被删除");
        } else if ("1".equals(user.getStatus())) {
            log.info("登录用户：{} 已被停用.", username);
            throw new UsernameNotFoundException("登录用户：" + username + " 已被停用");
        }

        return createLoginUser(user);
    }

    public UserDetails createLoginUser(PortalUser user) {
        PortalLoginUser loginUser = new PortalLoginUser();
        loginUser.setId(user.getId());
        loginUser.setUserId(user.getUserId());
        loginUser.setUser(user);
        
        // 设置角色（门户简单角色体系）
        Set<String> roles = new HashSet<>();
        if (StringUtils.isNotEmpty(user.getRole())) {
            roles.add(user.getRole());
        } else {
            roles.add("user"); // 默认角色
        }
        loginUser.setRoles(roles);
        
        return loginUser;
    }
}