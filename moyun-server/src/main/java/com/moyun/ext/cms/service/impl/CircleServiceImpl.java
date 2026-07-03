package com.moyun.ext.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.core.base.page.PageDomain;
import com.moyun.ext.cms.domain.query.CircleQuery;
import com.moyun.ext.cms.domain.vo.CircleListItemVO;
import com.moyun.ext.cms.domain.vo.CircleMemberVO;
import com.moyun.ext.cms.domain.vo.CirclePostVO;
import com.moyun.ext.cms.domain.vo.CircleVO;
import com.moyun.ext.cms.service.ICircleService;
import com.moyun.portal.domain.entity.PortalCircle;
import com.moyun.portal.domain.entity.PortalCircleMember;
import com.moyun.portal.domain.entity.PortalCirclePost;
import com.moyun.portal.mapper.PortalCircleMapper;
import com.moyun.portal.mapper.PortalCircleMemberMapper;
import com.moyun.portal.mapper.PortalCirclePostMapper;
import com.moyun.util.bean.PageUtils;
import com.moyun.util.string.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 圈子/兴趣小组 Service 实现（社交深化与商业化 4.1）
 *
 * @author moyun
 */
@Service
public class CircleServiceImpl implements ICircleService {

    /** 单用户加入圈子数量上限 */
    private static final int MAX_CIRCLE_PER_USER = 50;

    /** 详情页展示的成员数量上限 */
    private static final int DETAIL_MEMBER_LIMIT = 10;

    @Autowired private PortalCircleMapper circleMapper;
    @Autowired private PortalCircleMemberMapper memberMapper;
    @Autowired private PortalCirclePostMapper postMapper;

    // ========================================================================
    // 列表 / 详情
    // ========================================================================
    @Override
    public Page<CircleListItemVO> listCircles(CircleQuery query) {
        Page<CircleListItemVO> page = PageUtils.buildPage(query);
        return circleMapper.selectListPage(page, query);
    }

    @Override
    public CircleVO getCircleDetail(Long id, Long currentUserId) {
        CircleVO vo = circleMapper.selectDetailById(id);
        if (vo == null) {
            return null;
        }
        // 当前用户视角
        if (currentUserId != null) {
            PortalCircleMember member = memberMapper.selectByCircleAndUser(id, currentUserId);
            if (member != null) {
                vo.setIsJoined(true);
                vo.setMyRole(member.getRole());
            } else {
                vo.setIsJoined(false);
                vo.setMyRole(null);
            }
        } else {
            vo.setIsJoined(false);
            vo.setMyRole(null);
        }
        // 成员列表（前 N 个，按角色优先）
        List<CircleMemberVO> members = circleMapper.selectMembersByCircle(id, DETAIL_MEMBER_LIMIT);
        vo.setMembers(members);
        return vo;
    }

    // ========================================================================
    // 创建 / 修改 / 删除
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCircle(CircleVO vo, Long userId) {
        if (userId == null) {
            throw new ServiceException("请登录后操作");
        }
        if (vo == null || StringUtils.isEmpty(vo.getName())) {
            throw new ServiceException("圈子名称不能为空");
        }
        PortalCircle entity = new PortalCircle();
        entity.setOwnerId(userId);
        entity.setName(vo.getName());
        entity.setDescription(vo.getDescription());
        entity.setCover(vo.getCover());
        entity.setCategory(vo.getCategory());
        entity.setStatus("active");
        entity.setMemberCount(0);
        entity.setPostCount(0);
        entity.setCreatedTime(LocalDateTime.now());
        circleMapper.insert(entity);
        // 创建者自动成为 owner 成员
        PortalCircleMember member = new PortalCircleMember();
        member.setCircleId(entity.getId());
        member.setUserId(userId);
        member.setRole("owner");
        member.setJoinedTime(LocalDateTime.now());
        try {
            memberMapper.insert(member);
        } catch (DuplicateKeyException e) {
            // 并发兜底：忽略
        }
        // 成员数 +1
        circleMapper.updateMemberCount(entity.getId(), 1);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateCircle(CircleVO vo, Long userId) {
        PortalCircle entity = mustOwnCircle(vo.getId(), userId);
        if (StringUtils.isNotEmpty(vo.getName())) {
            entity.setName(vo.getName());
        }
        entity.setDescription(vo.getDescription());
        entity.setCover(vo.getCover());
        if (vo.getCategory() != null) {
            entity.setCategory(vo.getCategory());
        }
        return circleMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteCircle(Long id, Long userId) {
        PortalCircle entity = mustOwnCircle(id, userId);
        // 级联删除成员与帖子
        LambdaQueryWrapper<PortalCircleMember> memberQw = Wrappers.<PortalCircleMember>lambdaQuery()
                .eq(PortalCircleMember::getCircleId, id);
        memberMapper.delete(memberQw);

        LambdaQueryWrapper<PortalCirclePost> postQw = Wrappers.<PortalCirclePost>lambdaQuery()
                .eq(PortalCirclePost::getCircleId, id);
        postMapper.delete(postQw);

        return circleMapper.deleteById(entity.getId());
    }

    // ========================================================================
    // 加入 / 退出
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean joinCircle(Long circleId, Long userId) {
        if (userId == null) {
            throw new ServiceException("请登录后操作");
        }
        PortalCircle circle = circleMapper.selectById(circleId);
        if (circle == null) {
            throw new ServiceException("圈子不存在");
        }
        if (!"active".equals(circle.getStatus())) {
            throw new ServiceException("圈子已停用，无法加入");
        }
        // 校验加入数量上限
        int joinedCount = memberMapper.countByUserId(userId);
        if (joinedCount >= MAX_CIRCLE_PER_USER) {
            throw new ServiceException("加入圈子数量已达上限（" + MAX_CIRCLE_PER_USER + " 个）");
        }
        PortalCircleMember existing = memberMapper.selectByCircleAndUser(circleId, userId);
        if (existing != null) {
            return true;
        }
        PortalCircleMember member = new PortalCircleMember();
        member.setCircleId(circleId);
        member.setUserId(userId);
        member.setRole("member");
        member.setJoinedTime(LocalDateTime.now());
        try {
            memberMapper.insert(member);
        } catch (DuplicateKeyException e) {
            // 并发兜底：已加入
            return true;
        }
        circleMapper.updateMemberCount(circleId, 1);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean leaveCircle(Long circleId, Long userId) {
        if (userId == null) {
            throw new ServiceException("请登录后操作");
        }
        PortalCircle circle = circleMapper.selectById(circleId);
        if (circle == null) {
            throw new ServiceException("圈子不存在");
        }
        PortalCircleMember member = memberMapper.selectByCircleAndUser(circleId, userId);
        if (member == null) {
            return false;
        }
        if ("owner".equals(member.getRole())) {
            throw new ServiceException("圈主不能退出，请先转让或解散圈子");
        }
        memberMapper.deleteById(member.getId());
        circleMapper.updateMemberCount(circleId, -1);
        return true;
    }

    // ========================================================================
    // 帖子
    // ========================================================================
    @Override
    public Page<CirclePostVO> listCirclePosts(Long circleId, PageDomain query) {
        Page<CirclePostVO> page = PageUtils.buildPage(query);
        return postMapper.selectPostsByCircle(page, circleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPost(Long circleId, CirclePostVO post, Long userId) {
        if (userId == null) {
            throw new ServiceException("请登录后操作");
        }
        if (post == null || StringUtils.isEmpty(post.getTitle())) {
            throw new ServiceException("帖子标题不能为空");
        }
        PortalCircle circle = circleMapper.selectById(circleId);
        if (circle == null) {
            throw new ServiceException("圈子不存在");
        }
        // 仅成员可发帖
        PortalCircleMember member = memberMapper.selectByCircleAndUser(circleId, userId);
        if (member == null) {
            throw new ServiceException("请先加入圈子后再发帖");
        }
        PortalCirclePost entity = new PortalCirclePost();
        entity.setCircleId(circleId);
        entity.setUserId(userId);
        entity.setTitle(post.getTitle());
        entity.setContent(post.getContent());
        entity.setViewCount(0);
        entity.setLikeCount(0);
        entity.setReplyCount(0);
        entity.setStatus("active");
        entity.setCreatedTime(LocalDateTime.now());
        postMapper.insert(entity);
        circleMapper.updatePostCount(circleId, 1);
        return entity.getId();
    }

    // ========================================================================
    // 后台管理
    // ========================================================================
    @Override
    public Page<CircleListItemVO> cmsListCircles(CircleQuery query) {
        Page<CircleListItemVO> page = PageUtils.buildPage(query);
        return circleMapper.selectCmsListPage(page, query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cmsAuditCircle(Long id, String status) {
        if (!"active".equals(status) && !"disabled".equals(status) && !"pending".equals(status)) {
            throw new ServiceException("非法状态");
        }
        PortalCircle entity = circleMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException("圈子不存在");
        }
        entity.setStatus(status);
        return circleMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cmsDeleteCircle(Long id) {
        LambdaQueryWrapper<PortalCircleMember> memberQw = Wrappers.<PortalCircleMember>lambdaQuery()
                .eq(PortalCircleMember::getCircleId, id);
        memberMapper.delete(memberQw);

        LambdaQueryWrapper<PortalCirclePost> postQw = Wrappers.<PortalCirclePost>lambdaQuery()
                .eq(PortalCirclePost::getCircleId, id);
        postMapper.delete(postQw);

        return circleMapper.deleteById(id);
    }

    @Override
    public Page<CirclePostVO> cmsListPosts(Long circleId, String keyword, String status, PageDomain query) {
        Page<CirclePostVO> page = PageUtils.buildPage(query);
        return postMapper.selectCmsPostsPage(page, circleId, keyword, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cmsDeletePost(Long id) {
        PortalCirclePost post = postMapper.selectById(id);
        if (post == null) {
            return 0;
        }
        int rows = postMapper.deleteById(id);
        if (rows > 0) {
            circleMapper.updatePostCount(post.getCircleId(), -1);
        }
        return rows;
    }

    // ========================================================================
    // 内部工具
    // ========================================================================

    /**
     * 校验圈子存在且归属当前用户（圈主），返回实体
     */
    private PortalCircle mustOwnCircle(Long circleId, Long userId) {
        if (userId == null) {
            throw new ServiceException("请登录后操作");
        }
        PortalCircle entity = circleMapper.selectById(circleId);
        if (entity == null) {
            throw new ServiceException("圈子不存在");
        }
        if (entity.getOwnerId() == null || !entity.getOwnerId().equals(userId)) {
            throw new ServiceException("无权操作该圈子");
        }
        return entity;
    }
}
