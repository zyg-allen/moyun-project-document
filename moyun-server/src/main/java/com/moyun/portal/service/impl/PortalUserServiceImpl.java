package com.moyun.portal.service.impl;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.domain.query.UserQuery;
import com.moyun.portal.mapper.PortalUserMapper;
import com.moyun.portal.service.IPortalUserService;
import com.moyun.util.string.StringUtils;

/**
 * 门户用户 业务层处理
 *
 * @author moyun
 */
@Service
public class PortalUserServiceImpl extends ServiceImpl<PortalUserMapper, PortalUser> implements IPortalUserService {

    @Autowired
    private PortalUserMapper portalUserMapper;

    /**
     * 根据条件分页查询用户列表
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return 分页结果
     */
    @Override
    public Page<PortalUser> selectPortalUserPage(Page<PortalUser> page, UserQuery query) {
        Page<PortalUser> result = baseMapper.selectPortalUserPage(page, query);
        // 双重防护：清空 password 字段，防止接口泄露
        result.getRecords().forEach(this::clearPassword);
        return result;
    }

    /**
     * 根据条件查询用户列表（不分页，用于导出等场景）
     *
     * @param query 查询条件
     * @return 用户信息集合
     */
    @Override
    public List<PortalUser> selectPortalUserList(UserQuery query) {
        List<PortalUser> list = baseMapper.selectPortalUserList(query);
        list.forEach(this::clearPassword);
        return list;
    }

    /**
     * 通过用户名查询用户（登录校验场景，需要 password）
     *
     * @param username 用户名
     * @return 用户对象信息
     */
    @Override
    public PortalUser selectPortalUserByUsername(String username) {
        return portalUserMapper.selectPortalUserByUsername(username);
    }

    /**
     * 通过用户ID查询用户
     *
     * @param id 用户ID
     * @return 用户对象信息
     */
    @Override
    public PortalUser selectPortalUserById(Long id) {
        PortalUser user = portalUserMapper.selectPortalUserById(id);
        clearPassword(user);
        return user;
    }

    /**
     * 校验用户名称是否唯一
     *
     * @param portalUser 用户信息
     * @return 结果
     */
    @Override
    public boolean checkPortalUserNameUnique(PortalUser portalUser) {
        Long userId = StringUtils.isNull(portalUser.getId()) ? -1L : portalUser.getId();
        PortalUser info = portalUserMapper.checkPortalUserNameUnique(portalUser.getUsername());
        if (StringUtils.isNotNull(info) && info.getId().longValue() != userId.longValue()) {
            return false;
        }
        return true;
    }

    /**
     * 校验邮箱是否唯一
     *
     * @param portalUser 用户信息
     * @return 结果
     */
    @Override
    public boolean checkPortalEmailUnique(PortalUser portalUser) {
        Long userId = StringUtils.isNull(portalUser.getId()) ? -1L : portalUser.getId();
        PortalUser info = portalUserMapper.checkPortalEmailUnique(portalUser.getEmail());
        if (StringUtils.isNotNull(info) && info.getId().longValue() != userId.longValue()) {
            return false;
        }
        return true;
    }

    /**
     * 新增用户信息
     *
     * @param portalUser 用户信息
     * @return 结果
     */
    @Override
    public int insertPortalUser(PortalUser portalUser) {
        return portalUserMapper.insertPortalUser(portalUser);
    }

    /**
     * 注册用户信息
     *
     * @param portalUser 用户信息
     * @return 结果
     */
    @Override
    public boolean registerPortalUser(PortalUser portalUser) {
        return portalUserMapper.insertPortalUser(portalUser) > 0;
    }

    /**
     * 修改用户信息
     *
     * @param portalUser 用户信息
     * @return 结果
     */
    @Override
    public int updatePortalUser(PortalUser portalUser) {
        return portalUserMapper.updatePortalUser(portalUser);
    }

    /**
     * 修改用户头像
     *
     * @param username 用户名
     * @param avatar   头像地址
     * @return 结果
     */
    @Override
    public boolean updatePortalUserAvatar(String username, String avatar) {
        return portalUserMapper.updatePortalUserAvatar(username, avatar) > 0;
    }

    /**
     * 重置用户密码
     *
     * @param portalUser 用户信息
     * @return 结果
     */
    @Override
    public int resetPortalUserPwd(PortalUser portalUser) {
        return portalUserMapper.updatePortalUser(portalUser);
    }

    /**
     * 重置用户密码
     *
     * @param username 用户名
     * @param password 密码
     * @return 结果
     */
    @Override
    public int resetPortalUserPwd(String username, String password) {
        return portalUserMapper.resetPortalUserPwd(username, password);
    }

    /**
     * 通过用户ID删除用户
     *
     * @param id 用户ID
     * @return 结果
     */
    @Override
    public int deletePortalUserById(Long id) {
        return portalUserMapper.deletePortalUserById(id);
    }

    /**
     * 批量删除用户信息
     *
     * @param ids 需要删除的用户ID
     * @return 结果
     */
    @Override
    public int deletePortalUserByIds(Long[] ids) {
        return portalUserMapper.deletePortalUserByIds(ids);
    }

    /**
     * 清空用户对象的 password 字段（双重防护，防止接口泄露密码哈希）
     * 配合 PortalUser 实体上的 @JsonProperty(access = WRITE_ONLY) 使用
     *
     * @param user 用户对象
     */
    private void clearPassword(PortalUser user) {
        if (user != null) {
            user.setPassword(null);
        }
    }
}
