package com.moyun.portal.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.domain.query.UserQuery;

/**
 * 门户用户表 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalUserMapper extends BaseMapper<PortalUser> {

    /**
     * 根据条件分页查询用户列表
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return 用户信息集合信息
     */
    Page<PortalUser> selectPortalUserPage(Page<PortalUser> page, @Param("params") UserQuery query);

    /**
     * 根据条件查询用户列表（不分页，用于导出等场景）
     *
     * @param query 查询条件
     * @return 用户信息集合信息
     */
    List<PortalUser> selectPortalUserList(@Param("params") UserQuery query);

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
     * 新增用户信息
     *
     * @param portalUser 用户信息
     * @return 结果
     */
    public int insertPortalUser(PortalUser portalUser);

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
    public int updatePortalUserAvatar(@Param("username") String username, @Param("avatar") String avatar);

    /**
     * 重置用户密码
     *
     * @param username 用户名
     * @param password 密码
     * @return 结果
     */
    public int resetPortalUserPwd(@Param("username") String username, @Param("password") String password);

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

    /**
     * 校验用户名称是否唯一
     *
     * @param username 用户名称
     * @return 结果
     */
    public PortalUser checkPortalUserNameUnique(String username);

    /**
     * 校验邮箱是否唯一
     *
     * @param email 用户邮箱
     * @return 结果
     */
    public PortalUser checkPortalEmailUnique(String email);

    /**
     * 查询所有绑定了系统用户的前台用户ID列表
     * 用于待办通知定向发送
     *
     * @return 前台用户ID列表
     */
    List<Long> selectBoundPortalUserIds();

    /**
     * 查询「名家录」展示用户（用于首页 /authors 列表）
     *
     * <p>三个硬性条件（用户指令）：
     * <ol>
     *   <li>已开启公开主页（privacy_profile = 1）</li>
     *   <li>已认证创作者且审核通过（is_certified_creator = 1）</li>
     *   <li>至少发布过 1 篇已发布文章（EXISTS 子查询 portal_article status='published'）</li>
     * </ol>
     * 另加常规状态过滤：账号正常（status='0'）+ 未删除（del_flag='0'）。
     *
     * <p>排序：按发布文章数倒序，让高产作者靠前；同数量按注册时间倒序。
     * 文章数等统计字段由 Controller 调用 batchSelectAuthorArticleStats 批量聚合填充，
     * 此处仅返回 PortalUser 基础字段，避免返回非实体列导致映射混乱。
     *
     * @param limit 取前 N 条（前端首页传 10，/authors 页传 100）
     * @return 符合条件的用户列表
     */
    @Select("SELECT u.* " +
            "FROM portal_user u " +
            "WHERE u.status = '0' " +
            "  AND u.del_flag = '0' " +
            "  AND u.is_certified_creator = 1 " +
            "  AND u.privacy_profile = 1 " +
            "  AND EXISTS (" +
            "    SELECT 1 FROM portal_article a " +
            "    WHERE a.author_id = u.id AND a.status = 'published'" +
            "  ) " +
            "ORDER BY (SELECT COUNT(*) FROM portal_article a WHERE a.author_id = u.id AND a.status = 'published') DESC, u.create_time DESC " +
            "LIMIT #{limit}")
    List<PortalUser> selectAuthors(@Param("limit") int limit);
}
