package com.moyun.system.service;

import com.moyun.core.base.model.LoginUser;
import com.moyun.system.domain.entity.SysUserOnline;

import java.util.Collection;

/**
 * 在线用户 服务层
 *
 * @author ruoyi
 */
public interface ISysUserOnlineService {
    /**
     * 通过登录地址查询信息
     *
     * @param ipaddr 登录地址
     * @param user   用户信息
     * @return 在线用户信息
     */
    public SysUserOnline selectOnlineByIpaddr(String ipaddr, LoginUser user);

    /**
     * 通过用户名称查询信息
     *
     * @param userName 用户名称
     * @param user     用户信息
     * @return 在线用户信息
     */
    public SysUserOnline selectOnlineByUserName(String userName, LoginUser user);

    /**
     * 通过登录地址/用户名称查询信息
     *
     * @param ipaddr   登录地址
     * @param userName 用户名称
     * @param user     用户信息
     * @return 在线用户信息
     */
    public SysUserOnline selectOnlineByInfo(String ipaddr, String userName, LoginUser user);

    /**
     * 设置在线用户信息
     *
     * @param user 用户信息
     * @return 在线用户
     */
    public SysUserOnline loginUserToUserOnline(LoginUser user);

    /**
     * 查询在线用户列表（从 Redis 遍历 login_tokens:*）
     *
     * @param userName 登录用户名称（模糊匹配）
     * @param ipaddr   登录地址（模糊匹配）
     * @return 在线用户列表
     */
    public Collection<SysUserOnline> selectUserOnlineList(String userName, String ipaddr);

    /**
     * 强退指定用户（删除 Redis 中的 login_tokens:{tokenId}）
     *
     * @param tokenId 令牌 id（login_tokens: 后的 key 值）
     */
    public void forceLogout(String tokenId);
}
