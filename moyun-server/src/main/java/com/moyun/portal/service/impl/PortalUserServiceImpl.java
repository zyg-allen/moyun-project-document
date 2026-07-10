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
import com.moyun.util.security.SecurityUtils;
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
        // 防御性加密：确保密码不会以明文形式落库（已加密则跳过，避免双重加密）
        ensurePasswordEncoded(portalUser);
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
        // 防御性加密：Controller 已加密，此处兜底防止其他调用方未加密
        ensurePasswordEncoded(portalUser);
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
        // 防御性加密：若调用方在 update 时携带了明文密码，需先加密再写库
        ensurePasswordEncoded(portalUser);
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
        // 防御性加密：重置密码场景，确保新密码已 BCrypt 加密
        ensurePasswordEncoded(portalUser);
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
        // 防御性加密：若调用方传入明文密码，需先加密；已加密则跳过，避免双重加密
        String encoded = encodeIfRaw(password);
        return portalUserMapper.resetPortalUserPwd(username, encoded);
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

    /**
     * 防御性加密：仅在密码非空且尚未 BCrypt 加密时执行加密
     * <p>
     * BCrypt 哈希特征：以 "$2a$" / "$2b$" / "$2y$" 开头，长度 60。
     * 调用方（如 PortalLoginController.register）可能已加密，此处只对明文兜底，避免双重加密导致登录失败。
     * </p>
     *
     * @param portalUser 用户信息
     */
    private void ensurePasswordEncoded(PortalUser portalUser) {
        if (portalUser == null) {
            return;
        }
        String pwd = portalUser.getPassword();
        if (pwd == null || pwd.isEmpty()) {
            return;
        }
        portalUser.setPassword(encodeIfRaw(pwd));
    }

    /**
     * 若传入的是明文密码则做 BCrypt 加密；已是 BCrypt 哈希则原样返回
     *
     * @param password 待判定的密码字符串
     * @return 已加密后的密码
     */
    private String encodeIfRaw(String password) {
        if (password == null || password.isEmpty()) {
            return password;
        }
        if (isBcryptEncoded(password)) {
            return password;
        }
        return SecurityUtils.encryptPassword(password);
    }

    /**
     * 判断字符串是否已是 BCrypt 哈希（$2a$ / $2b$ / $2y$ 开头，长度 60）
     *
     * @param password 密码字符串
     * @return true 表示已是 BCrypt 哈希
     */
    private boolean isBcryptEncoded(String password) {
        return password != null
                && password.length() == 60
                && (password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$"));
    }
}
