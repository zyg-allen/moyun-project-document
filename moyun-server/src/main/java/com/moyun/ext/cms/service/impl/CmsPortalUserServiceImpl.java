package com.moyun.ext.cms.service.impl;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.exception.system.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.moyun.ext.cms.domain.query.CmsPortalUserQuery;
import com.moyun.ext.cms.domain.vo.CmsPortalUserProfileVO;
import com.moyun.ext.cms.domain.vo.CmsPortalUserVO;
import com.moyun.ext.cms.domain.vo.PortalUserBusinessStatsVO;
import com.moyun.ext.cms.service.ICmsPortalUserService;
import com.moyun.portal.domain.entity.PortalBookmark;
import com.moyun.portal.domain.entity.PortalBookshelf;
import com.moyun.portal.domain.entity.PortalFeedback;
import com.moyun.portal.domain.entity.PortalReport;
import com.moyun.portal.domain.entity.PortalTopicPost;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.domain.query.UserQuery;
import com.moyun.portal.domain.vo.UserStatsVO;
import com.moyun.portal.mapper.PortalBookmarkMapper;
import com.moyun.portal.mapper.PortalBookshelfMapper;
import com.moyun.portal.mapper.PortalFeedbackMapper;
import com.moyun.portal.mapper.PortalReportMapper;
import com.moyun.portal.mapper.PortalTopicPostMapper;
import com.moyun.portal.mapper.PortalUserMapper;
import com.moyun.portal.mapper.PortalUserResumeMapper;
import com.moyun.portal.service.IPortalGrowthService;
import com.moyun.portal.service.IPortalUserService;
import com.moyun.system.mapper.SysUserMapper;
import com.moyun.core.base.entity.SysUser;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CMS门户用户服务实现类
 *
 * @author moyun
 */
@Service
public class CmsPortalUserServiceImpl implements ICmsPortalUserService
{
    @Autowired
    private PortalUserMapper portalUserMapper;

    @Autowired
    private IPortalUserService portalUserService;

    @Autowired
    private IPortalGrowthService portalGrowthService;

    @Autowired
    private PortalTopicPostMapper portalTopicPostMapper;

    @Autowired
    private PortalBookmarkMapper portalBookmarkMapper;

    @Autowired
    private PortalBookshelfMapper portalBookshelfMapper;

    @Autowired
    private PortalFeedbackMapper portalFeedbackMapper;

    @Autowired
    private PortalReportMapper portalReportMapper;

    @Autowired
    private PortalUserResumeMapper portalUserResumeMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Override
    public Page<CmsPortalUserVO> selectUserPage(Page<CmsPortalUserVO> page, CmsPortalUserQuery query)
    {
        // 走自定义 XML 分页（selectPortalUserPage），绕开 @TableLogic 对 del_flag 的自动过滤：
        // 后台默认展示全部账号（含已注销 del_flag='2'），并顺带修复原先"全量加载后内存分页"的性能问题。
        UserQuery userQuery = new UserQuery();
        userQuery.setUsername(query.getUsername());
        userQuery.setNickname(query.getNickname());
        userQuery.setEmail(query.getEmail());
        userQuery.setPhone(query.getPhone());
        if ("2".equals(query.getStatus())) {
            // 注销账号：注销时 status 被置为 1、del_flag='2'，按 del_flag 过滤
            userQuery.setDelFlag("2");
        } else if (ObjectUtil.isNotEmpty(query.getStatus())) {
            // 正常(0)/停用(1)：仅从未注销账号中筛选
            userQuery.setStatus(query.getStatus());
            userQuery.setDelFlag("0");
        }
        // 不传 status 时不加任何过滤，默认显示所有（含注销、停用）

        Page<PortalUser> entityPage = portalUserMapper.selectPortalUserPage(
                new Page<>(page.getCurrent(), page.getSize()), userQuery);

        Page<CmsPortalUserVO> voPage = new Page<>(page.getCurrent(), page.getSize(), entityPage.getTotal());
        List<CmsPortalUserVO> voList = BeanUtil.copyToList(entityPage.getRecords(), CmsPortalUserVO.class);
        // 批量填充绑定的 sys_user 信息（列表展示"关联系统用户"列）
        fillSysUserInfo(voList);
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 批量填充 VO 中的 sysUserName/sysNickName（一次查询，避免 N+1）
     *
     * <p>注意：SysUser.delFlag 未加 @TableLogic，MyBatis-Plus 的 selectBatchIds 不会过滤已删除用户，
     * 这里显式加 del_flag='0' 条件，避免列表展示已删除的后台账号。</p>
     *
     * @param voList 当前页的门户用户 VO 列表
     */
    private void fillSysUserInfo(List<CmsPortalUserVO> voList) {
        if (voList == null || voList.isEmpty()) {
            return;
        }
        Set<Long> sysUserIds = voList.stream()
                .map(CmsPortalUserVO::getUserId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (sysUserIds.isEmpty()) {
            return;
        }
        // 显式过滤已删除用户（del_flag='0' 为正常）
        LambdaQueryWrapper<SysUser> uw = new LambdaQueryWrapper<SysUser>()
                .in(SysUser::getUserId, sysUserIds)
                .eq(SysUser::getDelFlag, "0");
        List<SysUser> sysUsers = sysUserMapper.selectList(uw);
        if (sysUsers == null || sysUsers.isEmpty()) {
            return;
        }
        Map<Long, SysUser> sysUserMap = sysUsers.stream()
                .collect(Collectors.toMap(SysUser::getUserId, u -> u, (a, b) -> a));
        for (CmsPortalUserVO vo : voList) {
            if (vo.getUserId() == null) {
                continue;
            }
            SysUser sysUser = sysUserMap.get(vo.getUserId());
            if (sysUser != null) {
                vo.setSysUserName(sysUser.getUserName());
                vo.setSysNickName(sysUser.getNickName());
            }
        }
    }

    @Override
    public List<PortalUser> selectUserList(CmsPortalUserQuery query)
    {
        return portalUserMapper.selectList(buildQueryWrapper(query));
    }

    @Override
    public PortalUser selectUserById(Long id)
    {
        // 后台可查看已注销（del_flag='2'）账号详情，绕开 @TableLogic
        return portalUserMapper.selectCmsUserById(id);
    }

    @Override
    public CmsPortalUserProfileVO selectUserProfile(Long id)
    {
        // 后台可查看已注销（del_flag='2'）账号画像，绕开 @TableLogic
        PortalUser user = portalUserMapper.selectCmsUserById(id);
        if (user == null) {
            return null;
        }

        CmsPortalUserProfileVO profile = new CmsPortalUserProfileVO();

        // 1. 用户完整画像
        CmsPortalUserVO userVO = BeanUtil.copyProperties(user, CmsPortalUserVO.class);
        profile.setUser(userVO);

        // 2. 业务统计：复用成长体系已有的聚合（文章/读书/面试/粉丝/关注/签到等）
        PortalUserBusinessStatsVO stats = new PortalUserBusinessStatsVO();
        profile.setStats(stats);
        try {
            UserStatsVO userStats = portalGrowthService.getUserStats(id);
            if (userStats != null) {
                stats.setArticles(userStats.getArticles());
                stats.setViews(userStats.getViews());
                stats.setLikes(userStats.getLikes());
                stats.setBookmarks(userStats.getBookmarks());
                stats.setWordCount(userStats.getWordCount());
                stats.setBookFinished(userStats.getBookFinished());
                stats.setBooklistCount(userStats.getBooklistCount());
                stats.setQuoteCount(userStats.getQuoteCount());
                stats.setReadingMinutes(userStats.getReadingMinutes());
                stats.setQuestionSolved(userStats.getQuestionSolved());
                stats.setNoteCount(userStats.getNoteCount());
                stats.setExperienceCount(userStats.getExperienceCount());
                stats.setNoteAdopted(userStats.getNoteAdopted());
                stats.setFollowers(userStats.getFollowers());
                stats.setFollowing(userStats.getFollowing());
                stats.setComments(userStats.getComments());
                stats.setTotalLikes(userStats.getTotalLikes());
                stats.setCheckinStreak(userStats.getCheckinStreak());
            }
        } catch (Exception e) {
            // 成长体系聚合失败不阻断画像展示，仅留空
        }

        // 3. 补充统计：本服务直接 COUNT（每项独立 try-catch，避免单表异常拖累整体）
        stats.setTopicPosts(safeCount(() -> portalTopicPostMapper.selectCount(
                new LambdaQueryWrapper<PortalTopicPost>()
                        .eq(PortalTopicPost::getUserId, id)
                        .eq(PortalTopicPost::getDelFlag, "0"))));
        stats.setBookmarksArticle(safeCount(() -> portalBookmarkMapper.selectCount(
                new LambdaQueryWrapper<PortalBookmark>()
                        .eq(PortalBookmark::getUserId, id))));
        stats.setBookshelfCount(safeCount(() -> portalBookshelfMapper.selectCount(
                new LambdaQueryWrapper<PortalBookshelf>()
                        .eq(PortalBookshelf::getUserId, id))));
        stats.setResumeCount(safeCount(() -> portalUserResumeMapper.countByUserId(id)));
        stats.setFeedbackCount(safeCount(() -> portalFeedbackMapper.selectCount(
                new LambdaQueryWrapper<PortalFeedback>()
                        .eq(PortalFeedback::getUserId, id))));
        stats.setFeedbackPending(safeCount(() -> portalFeedbackMapper.selectCount(
                new LambdaQueryWrapper<PortalFeedback>()
                        .eq(PortalFeedback::getUserId, id)
                        .eq(PortalFeedback::getStatus, "pending"))));
        stats.setReportAsReporter(safeCount(() -> portalReportMapper.selectCount(
                new LambdaQueryWrapper<PortalReport>()
                        .eq(PortalReport::getUserId, id))));
        stats.setReportAsTarget(safeCount(() -> portalReportMapper.selectCount(
                new LambdaQueryWrapper<PortalReport>()
                        .eq(PortalReport::getTargetId, id)
                        .eq(PortalReport::getTargetType, "user"))));

        // 4. 快速跳转入口（带用户筛选参数，跳到对应菜单页）
        List<CmsPortalUserProfileVO.ProfileQuickLink> links = new ArrayList<>();
        links.add(new CmsPortalUserProfileVO.ProfileQuickLink(
                "/cms/article", "查看文章", stats.getArticles(), "authorId", id, "Document"));
        links.add(new CmsPortalUserProfileVO.ProfileQuickLink(
                "/cms/comment", "查看评论", stats.getComments(), "authorId", id, "ChatDotRound"));
        links.add(new CmsPortalUserProfileVO.ProfileQuickLink(
                "/cms/feedback", "查看反馈", stats.getFeedbackCount(), "userId", id, "Suggestion"));
        links.add(new CmsPortalUserProfileVO.ProfileQuickLink(
                "/cms/report", "查看举报记录", stats.getReportAsReporter(), "userId", id, "Warning"));
        profile.setLinks(links);

        return profile;
    }

    /**
     * 安全计数：单个表查询异常时返回 0，不阻断整体画像聚合
     */
    private Integer safeCount(java.util.function.Supplier<Object> supplier) {
        try {
            Object result = supplier.get();
            if (result == null) {
                return 0;
            }
            return ((Number) result).intValue();
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public int insertUser(PortalUser user)
    {
        // 业务唯一性校验（username/phone/email 含注销账号，与唯一索引同源）：
        // 拦截在落库前，避免唯一索引冲突触发 DuplicateKey 500
        String conflict = portalUserService.checkUniqueBusinessKeys(user);
        if (conflict != null) {
            throw new ServiceException(conflict);
        }
        return portalUserMapper.insert(user);
    }

    @Override
    public int updateUser(PortalUser user)
    {
        // 业务唯一性校验（排除自身 id，含注销账号，与唯一索引同源）
        String conflict = portalUserService.checkUniqueBusinessKeys(user);
        if (conflict != null) {
            throw new ServiceException(conflict);
        }
        // 保护字段置空，不允许被编辑表单覆写：
        // 密码走 resetUserPwd 专用入口；注销标志/创建审计/登录信息由系统维护
        user.setPassword(null);
        user.setDelFlag(null);
        user.setCreateBy(null);
        user.setCreateTime(null);
        user.setUpdateBy(null);
        user.setUpdateTime(null);
        user.setLoginIp(null);
        user.setLoginDate(null);
        // updateById 受 @TableLogic 影响会拼 AND del_flag='0'，已注销账号（del_flag='2'）无法命中
        // （日志表现：UPDATE ... WHERE id=? AND del_flag='0' → Updates: 0）。
        // 改用自定义 XML updatePortalUser（where id=#{id}），注销账号也可修改。
        return portalUserMapper.updatePortalUser(user);
    }

    @Override
    public int changeStatus(PortalUser user)
    {
        PortalUser updateUser = new PortalUser();
        updateUser.setId(user.getId());
        updateUser.setStatus(user.getStatus());
        return portalUserMapper.updateById(updateUser);
    }

    @Override
    public int deleteUserByIds(Long[] ids)
    {
        return portalUserMapper.deleteBatchIds(Arrays.asList(ids));
    }

    @Override
    public int restoreUser(Long id)
    {
        // 绕 @TableLogic 查询（注销账号 del_flag='2' 会被自动过滤，查不到）
        PortalUser exist = portalUserMapper.selectCmsUserById(id);
        if (exist == null || !"2".equals(exist.getDelFlag()))
        {
            throw new ServiceException("账号不存在或未处于注销状态，无需恢复");
        }
        // 恢复前唯一键校验：注销期间其用户名/手机号/邮箱若被其他账号占用（含注销记录），不允许恢复，
        // 否则 del_flag '2'->'0' 后将直接撞 uk_username/uk_phone/uk_email 唯一索引
        PortalUser check = new PortalUser();
        check.setId(id);
        check.setUsername(exist.getUsername());
        check.setPhone(exist.getPhone());
        check.setEmail(exist.getEmail());
        String conflict = portalUserService.checkUniqueBusinessKeys(check);
        if (conflict != null)
        {
            throw new ServiceException("恢复失败：" + conflict + "，请先处理冲突账号");
        }
        // 复活：del_flag '2'->'0'，状态一并恢复正常（updatePortalUser 绕 TableLogic 可命中注销记录）
        PortalUser restore = new PortalUser();
        restore.setId(id);
        restore.setDelFlag("0");
        restore.setStatus("0");
        restore.setUpdateTime(java.time.LocalDateTime.now());
        return portalUserMapper.updatePortalUser(restore);
    }

    @Override
    public int resetUserPwd(PortalUser user)
    {
        // 委托给前台用户服务：复用其防御性 BCrypt 加密逻辑，避免明文入库导致前台登录失败
        // 修复：原实现直接 updateById 写入 user.getPassword()，未做 BCrypt 加密，
        // 与前台注册（PortalUserServiceImpl.registerPortalUser）加密方式不一致，
        // 导致后台改密后前台用户无法登录
        return portalUserService.resetPortalUserPwd(user);
    }

    // ========================================================================
    // 系统用户绑定（身份桥接入口）
    //   关系：sys_user 1 : N portal_user（一个后台账号可绑多个门户身份）
    //        portal_user 端为 1:1（同一门户用户只能被一个 sys_user 绑定）
    //   场景：后台管理员绑定门户作者身份后，可在后台私信中心查看/回复发给该作者的私信
    // ========================================================================
    @Override
    public int bindSysUser(Long portalUserId, Long sysUserId) {
        if (portalUserId == null || sysUserId == null) {
            throw new ServiceException("门户用户ID与系统用户ID均不能为空");
        }
        PortalUser portalUser = portalUserMapper.selectById(portalUserId);
        if (portalUser == null) {
            throw new ServiceException("门户用户不存在");
        }
        // 已绑定同一 sys_user，幂等直接返回成功
        if (sysUserId.equals(portalUser.getUserId())) {
            return 1;
        }
        // portal_user 端一对一校验：已绑其他 sys_user 则拒绝（需先解绑）
        if (portalUser.getUserId() != null) {
            throw new ServiceException(
                    "该门户用户已绑定其他系统用户，请先解绑后再绑定");
        }
        // 校验 sys_user 存在且未删除（selectById 不过滤 del_flag，需显式查询）
        SysUser sysUser = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUserId, sysUserId)
                        .eq(SysUser::getDelFlag, "0")
        );
        if (sysUser == null) {
            throw new ServiceException("系统用户不存在或已删除");
        }
        PortalUser update = new PortalUser();
        update.setId(portalUserId);
        update.setUserId(sysUserId);
        return portalUserMapper.updateById(update);
    }

    @Override
    public int unbindSysUser(Long portalUserId) {
        if (portalUserId == null) {
            throw new ServiceException("门户用户ID不能为空");
        }
        PortalUser portalUser = portalUserMapper.selectById(portalUserId);
        if (portalUser == null) {
            throw new ServiceException("门户用户不存在");
        }
        if (portalUser.getUserId() == null) {
            // 未绑定，幂等返回
            return 1;
        }
        // MyBatis-Plus updateById 默认不更新 null 字段，这里用 LambdaUpdateWrapper 显式置 null
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<PortalUser> uw =
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
        uw.eq(PortalUser::getId, portalUserId)
          .set(PortalUser::getUserId, null);
        return portalUserMapper.update(null, uw);
    }

    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<PortalUser> buildQueryWrapper(CmsPortalUserQuery query)
    {
        LambdaQueryWrapper<PortalUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(ObjectUtil.isNotEmpty(query.getUsername()), PortalUser::getUsername, query.getUsername());
        wrapper.like(ObjectUtil.isNotEmpty(query.getNickname()), PortalUser::getNickname, query.getNickname());
        wrapper.like(ObjectUtil.isNotEmpty(query.getEmail()), PortalUser::getEmail, query.getEmail());
        wrapper.like(ObjectUtil.isNotEmpty(query.getPhone()), PortalUser::getPhone, query.getPhone());
        wrapper.eq(ObjectUtil.isNotEmpty(query.getStatus()), PortalUser::getStatus, query.getStatus());
        wrapper.orderByDesc(PortalUser::getCreateTime);
        return wrapper;
    }
}
