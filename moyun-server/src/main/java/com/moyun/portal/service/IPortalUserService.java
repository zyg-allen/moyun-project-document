package com.moyun.portal.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.domain.query.UserQuery;

/**
 * 门户用户 业务层
 *
 * @author moyun
 */
public interface IPortalUserService {

    /**
     * 根据条件分页查询用户列表
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return 分页结果
     */
    Page<PortalUser> selectPortalUserPage(Page<PortalUser> page, UserQuery query);

    /**
     * 根据条件查询用户列表（不分页，用于导出等场景）
     *
     * @param query 查询条件
     * @return 用户信息集合
     */
    List<PortalUser> selectPortalUserList(UserQuery query);

    /**
     * 通过用户名查询用户
     *
     * @param username 用户名
     * @return 用户对象信息
     */
    public PortalUser selectPortalUserByUsername(String username);

    /**
     * 通过用户ID查询用户
     *
     * @param id 用户ID
     * @return 用户对象信息
     */
    public PortalUser selectPortalUserById(Long id);

    /**
     * 校验用户名称是否唯一
     *
     * @param portalUser 用户信息
     * @return 结果
     */
    public boolean checkPortalUserNameUnique(PortalUser portalUser);

    /**
     * 校验邮箱是否唯一
     *
     * @param portalUser 用户信息
     * @return 结果
     */
    public boolean checkPortalEmailUnique(PortalUser portalUser);

    /**
     * 新增用户信息
     *
     * @param portalUser 用户信息
     * @return 结果
     */
    public int insertPortalUser(PortalUser portalUser);

    /**
     * 注册用户信息
     *
     * @param portalUser 用户信息
     * @return 结果
     */
    public boolean registerPortalUser(PortalUser portalUser);

    /**
     * 修改用户信息
     *
     * @param portalUser 用户信息
     * @return 结果
     */
    public int updatePortalUser(PortalUser portalUser);

    /**
     * 修改用户头像
     *
     * @param username 用户名
     * @param avatar   头像地址
     * @return 结果
     */
    public boolean updatePortalUserAvatar(String username, String avatar);

    /**
     * 重置用户密码
     *
     * @param portalUser 用户信息
     * @return 结果
     */
    public int resetPortalUserPwd(PortalUser portalUser);

    /**
     * 重置用户密码
     *
     * @param username 用户名
     * @param password 密码
     * @return 结果
     */
    public int resetPortalUserPwd(String username, String password);

    /**
     * 通过用户ID删除用户
     *
     * @param id 用户ID
     * @return 结果
     */
    public int deletePortalUserById(Long id);

    /**
     * 批量删除用户信息
     *
     * @param ids 需要删除的用户ID
     * @return 结果
     */
    public int deletePortalUserByIds(Long[] ids);
}
