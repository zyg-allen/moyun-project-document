package com.moyun.system.service.impl;

import com.moyun.common.constant.CacheConstants;
import com.moyun.core.base.model.LoginUser;
import com.moyun.core.config.redis.RedisCache;
import com.moyun.system.domain.entity.SysUserOnline;
import com.moyun.util.string.StringUtils;
import com.moyun.system.service.ISysUserOnlineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 在线用户 服务层处理
 *
 * @author ruoyi
 */
@Service
public class SysUserOnlineServiceImpl implements ISysUserOnlineService {

    @Autowired
    private RedisCache redisCache;

    /**
     * 通过登录地址查询信息
     *
     * @param ipaddr 登录地址
     * @param user   用户信息
     * @return 在线用户信息
     */
    @Override
    public SysUserOnline selectOnlineByIpaddr(String ipaddr, LoginUser user) {
        if (StringUtils.equals(ipaddr, user.getIpaddr())) {
            return loginUserToUserOnline(user);
        }
        return null;
    }

    /**
     * 通过用户名称查询信息
     *
     * @param userName 用户名称
     * @param user     用户信息
     * @return 在线用户信息
     */
    @Override
    public SysUserOnline selectOnlineByUserName(String userName, LoginUser user) {
        if (StringUtils.equals(userName, user.getUsername())) {
            return loginUserToUserOnline(user);
        }
        return null;
    }

    /**
     * 通过登录地址/用户名称查询信息
     *
     * @param ipaddr   登录地址
     * @param userName 用户名称
     * @param user     用户信息
     * @return 在线用户信息
     */
    @Override
    public SysUserOnline selectOnlineByInfo(String ipaddr, String userName, LoginUser user) {
        if (StringUtils.equals(ipaddr, user.getIpaddr()) && StringUtils.equals(userName, user.getUsername())) {
            return loginUserToUserOnline(user);
        }
        return null;
    }

    /**
     * 设置在线用户信息
     *
     * @param user 用户信息
     * @return 在线用户
     */
    @Override
    public SysUserOnline loginUserToUserOnline(LoginUser user) {
        if (StringUtils.isNull(user) || StringUtils.isNull(user.getUser())) {
            return null;
        }
        SysUserOnline sysUserOnline = new SysUserOnline();
        sysUserOnline.setTokenId(user.getToken());
        sysUserOnline.setUserName(user.getUsername());
        sysUserOnline.setIpaddr(user.getIpaddr());
        sysUserOnline.setLoginLocation(user.getLoginLocation());
        sysUserOnline.setBrowser(user.getBrowser());
        sysUserOnline.setOs(user.getOs());
        sysUserOnline.setLoginTime(user.getLoginTime());
        if (StringUtils.isNotNull(user.getUser().getDept())) {
            sysUserOnline.setDeptName(user.getUser().getDept().getDeptName());
        }
        return sysUserOnline;
    }

    /**
     * 查询在线用户列表（从 Redis 读取所有 login_tokens:* 的 LoginUser）
     *
     * @param userName 用户名（不为空时过滤，区分大小写）
     * @param ipaddr   IP 地址（不为空时过滤）
     * @return 在线用户列表
     */
    @Override
    public Collection<SysUserOnline> selectUserOnlineList(String userName, String ipaddr) {
        Collection<String> keys = redisCache.keys(CacheConstants.LOGIN_TOKEN_KEY + "*");
        List<SysUserOnline> userOnlineList = new ArrayList<>();
        for (String key : keys) {
            LoginUser user = redisCache.getCacheObject(key);
            if (user == null || user.getUser() == null) {
                continue;
            }
            // 条件过滤
            if (StringUtils.isNotEmpty(userName) && !StringUtils.equals(userName, user.getUsername())) {
                continue;
            }
            if (StringUtils.isNotEmpty(ipaddr) && !StringUtils.equals(ipaddr, user.getIpaddr())) {
                continue;
            }
            userOnlineList.add(loginUserToUserOnline(user));
        }
        return userOnlineList;
    }

    /**
     * 强退指定用户：删除 Redis 中的 login_tokens:{tokenId}
     *
     * @param tokenId 令牌 id（login_tokens: 后的 key 值）
     */
    @Override
    public void forceLogout(String tokenId) {
        redisCache.deleteObject(CacheConstants.LOGIN_TOKEN_KEY + tokenId);
    }
}
