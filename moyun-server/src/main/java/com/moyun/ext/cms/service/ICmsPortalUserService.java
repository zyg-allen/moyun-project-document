package com.moyun.ext.cms.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.moyun.ext.cms.domain.query.CmsPortalUserQuery;
import com.moyun.ext.cms.domain.vo.CmsPortalUserVO;
import com.moyun.portal.domain.entity.PortalUser;

/**
 * CMS门户用户服务接口
 *
 * @author moyun
 */
public interface ICmsPortalUserService
{
    /**
     * 查询门户用户列表
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return 门户用户列表
     */
    Page<CmsPortalUserVO> selectUserPage(Page<CmsPortalUserVO> page, CmsPortalUserQuery query);

    /**
     * 查询门户用户列表
     *
     * @param query 查询条件
     * @return 门户用户列表
     */
    List<PortalUser> selectUserList(CmsPortalUserQuery query);

    /**
     * 查询门户用户详情
     *
     * @param id 用户ID
     * @return 门户用户信息
     */
    PortalUser selectUserById(Long id);

    /**
     * 新增门户用户
     *
     * @param user 门户用户信息
     * @return 结果
     */
    int insertUser(PortalUser user);

    /**
     * 修改门户用户
     *
     * @param user 门户用户信息
     * @return 结果
     */
    int updateUser(PortalUser user);

    /**
     * 修改用户状态
     *
     * @param user 门户用户信息
     * @return 结果
     */
    int changeStatus(PortalUser user);

    /**
     * 批量删除门户用户
     *
     * @param ids 需要删除的用户ID
     * @return 结果
     */
    int deleteUserByIds(Long[] ids);

    /**
     * 重置用户密码
     *
     * @param user 用户信息
     * @return 结果
     */
    int resetUserPwd(PortalUser user);
}
