package com.moyun.agent.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.agent.entity.SysUser;

/**
 * 系统用户服务接口
 *
 * <p>提供用户查询、密码验证、密码加密等功能</p>
 *
 * @author laomao
 * @time 2025/11/25
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户实体，不存在返回null
     */
    SysUser getByUsername(String username);

    /**
     * 验证密码是否匹配
     *
     * @param rawPassword 明文密码
     * @param encodedPassword 加密后的密码
     * @return 是否匹配
     */
    boolean checkPassword(String rawPassword, String encodedPassword);

    /**
     * 加密密码
     *
     * <p>使用BCrypt算法加密</p>
     *
     * @param rawPassword 明文密码
     * @return 加密后的密码
     */
    String encodePassword(String rawPassword);

    /**
     * 更新用户最后登录信息
     *
     * @param userId 用户ID
     * @param ip 登录IP地址
     */
    void updateLoginInfo(Long userId, String ip);
}
