package com.moyun.ext.cms.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.moyun.ext.cms.domain.query.CmsPortalUserQuery;
import com.moyun.ext.cms.domain.vo.CmsPortalUserProfileVO;
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
     * 查询门户用户画像（含完整画像 + 业务统计 + 快速跳转入口）
     *
     * <p>用于后台"用户画像"抽屉展示，让管理员快速掌握客户状态。</p>
     *
     * @param id 用户ID
     * @return 用户画像聚合对象
     */
    CmsPortalUserProfileVO selectUserProfile(Long id);

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

    /**
     * 绑定系统用户（把 sys_user.user_id 写入 portal_user.user_id）
     *
     * <p>关系约束：sys_user ↔ portal_user 为一对多（一个后台账号可绑定多个门户身份），
     * 但 portal_user 端为一对一——同一门户用户只能被一个系统用户绑定。
     * 若该门户用户已绑定其他 sys_user，需先解绑再绑定。</p>
     *
     * <p>用于后台私信身份桥接：绑定后，该门户用户收到的私信可由绑定的系统用户在后台查看/回复。</p>
     *
     * @param portalUserId 门户用户ID
     * @param sysUserId     后台用户ID（sys_user.user_id）
     * @return 受影响行数
     * @throws com.moyun.common.exception.system.ServiceException 门户用户不存在/已绑其他账号/后台用户不存在时抛出
     */
    int bindSysUser(Long portalUserId, Long sysUserId);

    /**
     * 解绑系统用户（清空 portal_user.user_id）
     *
     * <p>解绑后该门户用户成为独立身份（如邮箱投稿作者），不再受后台私信桥接。</p>
     *
     * @param portalUserId 门户用户ID
     * @return 受影响行数
     * @throws com.moyun.common.exception.system.ServiceException 门户用户不存在时抛出
     */
    int unbindSysUser(Long portalUserId);
}
