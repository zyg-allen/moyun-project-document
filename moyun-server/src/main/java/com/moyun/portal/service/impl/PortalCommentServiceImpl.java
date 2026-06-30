package com.moyun.portal.service.impl;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moyun.common.exception.system.ServiceException;
import com.moyun.portal.domain.entity.PortalArticle;
import com.moyun.portal.domain.entity.PortalComment;
import com.moyun.portal.domain.entity.PortalCommentLike;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.domain.query.CommentQuery;
import com.moyun.portal.domain.vo.CommentVO;
import com.moyun.portal.mapper.PortalArticleMapper;
import com.moyun.portal.mapper.PortalCommentLikeMapper;
import com.moyun.portal.mapper.PortalCommentMapper;
import com.moyun.portal.mapper.PortalUserMapper;
import com.moyun.portal.service.IPortalCommentService;
import com.moyun.portal.service.IPortalGrowthService;
import com.moyun.portal.util.PortalSecurityUtils;

/**
 * 门户评论 业务层处理
 *
 * @author moyun
 */
@Slf4j
@Service
public class PortalCommentServiceImpl extends ServiceImpl<PortalCommentMapper, PortalComment> implements IPortalCommentService {

    @Autowired
    private PortalCommentMapper portalCommentMapper;

    @Autowired
    private PortalUserMapper portalUserMapper;

    @Autowired
    private PortalArticleMapper portalArticleMapper;

    @Autowired
    private IPortalGrowthService portalGrowthService;

    @Autowired
    private PortalCommentLikeMapper portalCommentLikeMapper;

    /**
     * 根据条件分页查询评论列表
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return 分页结果
     */
    @Override
    public Page<PortalComment> selectPortalCommentPage(Page<PortalComment> page, CommentQuery query) {
        return baseMapper.selectPortalCommentPage(page, query);
    }

    /**
     * 根据条件查询评论列表（不分页，用于导出等场景）
     *
     * @param query 查询条件
     * @return 评论信息集合
     */
    @Override
    public List<PortalComment> selectPortalCommentList(CommentQuery query) {
        return baseMapper.selectPortalCommentList(query);
    }

    /**
     * 通过评论ID查询评论
     *
     * @param id 评论ID
     * @return 评论对象信息
     */
    @Override
    public PortalComment selectPortalCommentById(Long id) {
        return portalCommentMapper.selectPortalCommentById(id);
    }

    /**
     * 新增评论信息
     *
     * @param portalComment 评论信息
     * @return 结果
     */
    @Override
    public int insertPortalComment(PortalComment portalComment) {
        // 自动填充评论者ID
        if (portalComment.getAuthorId() == null) {
            portalComment.setAuthorId(PortalSecurityUtils.getUserId());
        }
        // 设置默认状态：0-待审核，1-已发布
        if (portalComment.getStatus() == null) {
            portalComment.setStatus("1");
        }
        // 处理评论逻辑
        if (portalComment.getParentId() == null || portalComment.getParentId() == 0) {
            // 一级评论
            portalComment.setParentId(0L);
            portalComment.setRootId(0L);
            portalComment.setReplyTo(null);
            portalComment.setReplyToContent("");
        } else {
            // 回复评论：查询父评论
            PortalComment parentComment = portalCommentMapper.selectPortalCommentById(portalComment.getParentId());
            if (parentComment == null) {
                throw new RuntimeException("父评论不存在");
            }
            
            // 设置 root_id
            if (parentComment.getParentId() == 0) {
                portalComment.setRootId(parentComment.getId());  // 父是一级评论
            } else {
                portalComment.setRootId(parentComment.getRootId()); // 父是回复
            }
            
            // 设置被回复的用户ID
            portalComment.setReplyTo(parentComment.getAuthorId());
            
            // 冗余被回复的内容摘要（前50字）
            String replyContent = parentComment.getContent();
            if (replyContent.length() > 50) {
                replyContent = replyContent.substring(0, 50) + "...";
            }
            portalComment.setReplyToContent(replyContent);
        }
        
        // 初始化点赞数
        if (portalComment.getLikeCount() == null) {
            portalComment.setLikeCount(0L);
        }
        
        int rows = portalCommentMapper.insertPortalComment(portalComment);

        // 同步增加文章评论数（原子更新，仅一级评论计入文章评论数）
        if (rows > 0 && portalComment.getArticleId() != null
                && (portalComment.getParentId() == null || portalComment.getParentId() == 0)) {
            portalArticleMapper.incrementComments(portalComment.getArticleId(), 1);

            // 为文章作者记录被评论成长事件
            PortalArticle article = portalArticleMapper.selectById(portalComment.getArticleId());
            if (article != null && article.getAuthorId() != null
                    && !article.getAuthorId().equals(portalComment.getAuthorId())) {
                portalGrowthService.recordEventWithTarget("article", "receive_comment",
                        article.getAuthorId(), portalComment.getAuthorId(),
                        "article", portalComment.getArticleId());
            }
        }

        return rows;
    }

    /**
     * 修改评论信息
     *
     * @param portalComment 评论信息
     * @return 结果
     */
    @Override
    public int updatePortalComment(PortalComment portalComment) {
        return portalCommentMapper.updatePortalComment(portalComment);
    }

    /**
     * 通过评论ID删除评论
     *
     * @param id 评论ID
     * @return 结果
     */
    @Override
    public int deletePortalCommentById(Long id) {
        // 删除前查询评论，用于同步减少文章评论数
        PortalComment comment = portalCommentMapper.selectPortalCommentById(id);
        int rows = portalCommentMapper.deletePortalCommentById(id);

        // 同步减少文章评论数（仅一级评论）
        if (rows > 0 && comment != null && comment.getArticleId() != null
                && (comment.getParentId() == null || comment.getParentId() == 0)) {
            portalArticleMapper.incrementComments(comment.getArticleId(), -1);
        }

        return rows;
    }

    /**
     * 批量删除评论信息
     *
     * @param ids 需要删除的评论ID
     * @return 结果
     */
    @Override
    public int deletePortalCommentByIds(Long[] ids) {
        return portalCommentMapper.deletePortalCommentByIds(ids);
    }

    /**
     * 获取文章的评论列表（含回复）
     *
     * @param articleId 文章ID
     * @return 评论VO列表
     */
    @Override
    public List<CommentVO> getCommentsByArticle(Long articleId) {
        // 1. 查询所有一级评论（按时间倒序，最新的在上）
        List<PortalComment> rootComments = baseMapper.selectList(
                new LambdaQueryWrapper<PortalComment>()
                        .eq(PortalComment::getArticleId, articleId)
                        .eq(PortalComment::getStatus, "1")
                        .eq(PortalComment::getParentId, 0)
                        .orderByDesc(PortalComment::getCreateTime)
        );

        if (rootComments.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 查询所有回复评论（按时间正序，保持对话逻辑）
        List<Long> rootIds = rootComments.stream()
                .map(PortalComment::getId)
                .collect(Collectors.toList());

        List<PortalComment> allReplies = baseMapper.selectList(
                new LambdaQueryWrapper<PortalComment>()
                        .eq(PortalComment::getArticleId, articleId)
                        .eq(PortalComment::getStatus, "1")
                        .ne(PortalComment::getParentId, 0)
                        .in(PortalComment::getRootId, rootIds)
                        .orderByAsc(PortalComment::getCreateTime)
        );

        // 3. 合并一级评论和回复，用于批量查询用户信息
        List<PortalComment> allComments = new ArrayList<>();
        allComments.addAll(rootComments);
        allComments.addAll(allReplies);

        // 4. 批量查询用户信息
        Set<Long> userIds = allComments.stream()
                .map(PortalComment::getAuthorId)
                .collect(Collectors.toSet());

        // 添加被回复用户的ID
        allReplies.stream()
                .filter(c -> c.getReplyTo() != null)
                .forEach(c -> userIds.add(c.getReplyTo()));

        List<PortalUser> users = portalUserMapper.selectBatchIds(userIds);
        Map<Long, PortalUser> userMap = users.stream()
                .collect(Collectors.toMap(PortalUser::getId, u -> u));

        // 5. 组装一级评论为VO（已按倒序排序）
        List<CommentVO> roots = new ArrayList<>();
        Map<Long, List<CommentVO>> replyMap = new HashMap<>();

        for (PortalComment rootComment : rootComments) {
            CommentVO vo = convertToVO(rootComment, userMap);
            roots.add(vo);
            replyMap.put(rootComment.getId(), new ArrayList<>());
        }

        // 6. 组装回复为VO，并挂载到对应的一级评论下（回复按正序）
        for (PortalComment reply : allReplies) {
            CommentVO vo = convertToVO(reply, userMap);
            Long rootId = reply.getRootId();
            if (rootId != null && rootId > 0) {
                replyMap.computeIfAbsent(rootId, k -> new ArrayList<>()).add(vo);
            } else {
                // 如果rootId不存在，尝试使用parentId查找
                replyMap.computeIfAbsent(reply.getParentId(), k -> new ArrayList<>()).add(vo);
            }
        }

        // 7. 将回复挂载到对应的一级评论下
        for (CommentVO root : roots) {
            List<CommentVO> replies = replyMap.get(root.getId());
            if (replies != null && !replies.isEmpty()) {
                root.setReplies(replies);
            }
        }

        // 8. 一级评论已经按倒序从数据库取出，无需再次排序
        return roots;
    }
    
    /**
     * 转换评论实体为VO
     *
     * @param comment 评论实体
     * @param userMap 用户信息Map
     * @return 评论VO
     */
    private CommentVO convertToVO(PortalComment comment, Map<Long, PortalUser> userMap) {
        CommentVO vo = new CommentVO();
        BeanUtils.copyProperties(comment, vo);
        
        // 设置作者信息
        PortalUser author = userMap.get(comment.getAuthorId());
        if (author != null) {
            vo.setAuthorUsername(author.getUsername());
            vo.setAuthorNickname(author.getNickname());
            vo.setAuthorAvatar(author.getAvatar());
        }
        
        // 设置被回复人的昵称
        if (comment.getReplyTo() != null && comment.getReplyTo() > 0) {
            PortalUser replyUser = userMap.get(comment.getReplyTo());
            if (replyUser != null) {
                vo.setReplyToUsername(replyUser.getUsername());
                vo.setReplyToNickname(replyUser.getNickname());
            }
        }
        
        return vo;
    }

    /**
     * 获取文章的评论列表（含回复）- 分页版本（行业标准：一级评论倒序，回复正序）
     *
     * @param articleId 文章ID
     * @param pageNum 页码，从1开始
     * @param pageSize 每页数量
     * @return 包含评论列表和分页信息的Map
     */
    @Override
    public Map<String, Object> getCommentsByArticle(Long articleId, Integer pageNum, Integer pageSize) {
        // 1. 查询一级评论总数（用于分页）
        Long totalCount = baseMapper.selectCount(
                new LambdaQueryWrapper<PortalComment>()
                        .eq(PortalComment::getArticleId, articleId)
                        .eq(PortalComment::getStatus, "1")
                        .eq(PortalComment::getParentId, 0)
        );

        int total = totalCount.intValue();
        int pages = (int) Math.ceil((double) total / pageSize);

        if (total == 0) {
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("list", new ArrayList<>());
            emptyResult.put("total", 0);
            emptyResult.put("pageNum", pageNum);
            emptyResult.put("pageSize", pageSize);
            emptyResult.put("pages", 0);
            emptyResult.put("hasMore", false);
            return emptyResult;
        }

        // 2. 分页查询一级评论（按时间倒序，最新的在上）
        Page<PortalComment> rootPage = new Page<>(pageNum, pageSize);
        Page<PortalComment> rootResultPage = baseMapper.selectPage(
                rootPage,
                new LambdaQueryWrapper<PortalComment>()
                        .eq(PortalComment::getArticleId, articleId)
                        .eq(PortalComment::getStatus, "1")
                        .eq(PortalComment::getParentId, 0)
                        .orderByDesc(PortalComment::getCreateTime)
        );

        List<PortalComment> rootComments = rootResultPage.getRecords();
        if (rootComments.isEmpty()) {
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("list", new ArrayList<>());
            emptyResult.put("total", total);
            emptyResult.put("pageNum", pageNum);
            emptyResult.put("pageSize", pageSize);
            emptyResult.put("pages", pages);
            emptyResult.put("hasMore", pageNum < pages);
            return emptyResult;
        }

        // 3. 查询这些一级评论的所有回复（按时间正序，保持对话逻辑）
        List<Long> rootIds = rootComments.stream()
                .map(PortalComment::getId)
                .collect(Collectors.toList());

        List<PortalComment> allReplies = baseMapper.selectList(
                new LambdaQueryWrapper<PortalComment>()
                        .eq(PortalComment::getArticleId, articleId)
                        .eq(PortalComment::getStatus, "1")
                        .ne(PortalComment::getParentId, 0)
                        .in(PortalComment::getRootId, rootIds)
                        .orderByAsc(PortalComment::getCreateTime)
        );

        // 4. 合并一级评论和回复，用于批量查询用户信息
        List<PortalComment> allComments = new ArrayList<>();
        allComments.addAll(rootComments);
        allComments.addAll(allReplies);

        // 5. 批量查询用户信息
        Set<Long> userIds = allComments.stream()
                .map(PortalComment::getAuthorId)
                .collect(Collectors.toSet());

        // 添加被回复用户的ID
        allReplies.stream()
                .filter(c -> c.getReplyTo() != null)
                .forEach(c -> userIds.add(c.getReplyTo()));

        List<PortalUser> users = portalUserMapper.selectBatchIds(userIds);
        Map<Long, PortalUser> userMap = users.stream()
                .collect(Collectors.toMap(PortalUser::getId, u -> u));

        // 6. 组装一级评论为VO
        List<CommentVO> roots = new ArrayList<>();
        Map<Long, List<CommentVO>> replyMap = new HashMap<>();

        for (PortalComment rootComment : rootComments) {
            CommentVO vo = convertToVO(rootComment, userMap);
            roots.add(vo);
            replyMap.put(rootComment.getId(), new ArrayList<>());
        }

        // 7. 组装回复为VO，并挂载到对应的一级评论下
        for (PortalComment reply : allReplies) {
            CommentVO vo = convertToVO(reply, userMap);
            Long rootId = reply.getRootId();
            if (rootId != null && rootId > 0) {
                replyMap.computeIfAbsent(rootId, k -> new ArrayList<>()).add(vo);
            } else {
                // 如果rootId不存在，尝试使用parentId查找
                replyMap.computeIfAbsent(reply.getParentId(), k -> new ArrayList<>()).add(vo);
            }
        }

        // 8. 将回复挂载到对应的一级评论下（回复按时间正序，保持对话逻辑）
        for (CommentVO root : roots) {
            List<CommentVO> replies = replyMap.get(root.getId());
            if (replies != null && !replies.isEmpty()) {
                root.setReplies(replies);
            }
        }

        // 9. 构建返回结果（一级评论已经按倒序从数据库取出，无需再次排序）
        Map<String, Object> result = new HashMap<>();
        result.put("list", roots);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        result.put("pages", pages);
        result.put("hasMore", pageNum < pages);

        return result;
    }

    /**
     * 切换评论点赞状态（点赞 / 取消点赞，幂等 toggle）
     *
     * <p>返回字段契约与 PortalArticleController.toggleLikeArticle 保持一致：
     * {@code isLiked}（当前是否已赞）+ {@code likeCount}（评论最新点赞数）。
     * 面试模块的 toggleCommentLike 使用 {@code liked} 字段属独立契约，不在此统一范围内。</p>
     *
     * @param commentId 评论ID
     * @param userId    当前登录用户ID
     * @return 含 isLiked 和 likeCount
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleLike(Long commentId, Long userId) {
        if (userId == null) {
            throw new ServiceException("请登录后操作");
        }
        PortalCommentLike exist = portalCommentLikeMapper.selectLike(commentId, userId);
        PortalComment comment = portalCommentMapper.selectById(commentId);
        if (comment == null) {
            throw new ServiceException("评论不存在");
        }
        Map<String, Object> result = new HashMap<>();
        boolean isLiked;
        if (exist != null) {
            // 已赞 -> 取消
            portalCommentLikeMapper.deleteById(exist.getId());
            comment.setLikeCount(Math.max(0L, (comment.getLikeCount() == null ? 0L : comment.getLikeCount()) - 1));
            isLiked = false;
        } else {
            // 未赞 -> 点赞
            PortalCommentLike like = new PortalCommentLike();
            like.setCommentId(commentId);
            like.setUserId(userId);
            like.setCreateTime(LocalDateTime.now());
            try {
                portalCommentLikeMapper.insert(like);
            } catch (DuplicateKeyException e) {
                // 并发场景：另一个事务已先插入（uk_comment_user 唯一索引触发）
                // 此时按"已赞→取消"语义处理，保证幂等
                log.info("评论点赞并发冲突，转为取消点赞：commentId={}, userId={}", commentId, userId);
                PortalCommentLike conflict = portalCommentLikeMapper.selectLike(commentId, userId);
                if (conflict != null) {
                    portalCommentLikeMapper.deleteById(conflict.getId());
                }
                comment.setLikeCount(Math.max(0L, (comment.getLikeCount() == null ? 0L : comment.getLikeCount()) - 1));
                portalCommentMapper.updateById(comment);
                result.put("isLiked", false);
                result.put("likeCount", comment.getLikeCount());
                return result;
            }
            comment.setLikeCount((comment.getLikeCount() == null ? 0L : comment.getLikeCount()) + 1);
            isLiked = true;

            // 为评论作者记录"评论被点赞"成长事件（与文章点赞/收藏保持一致）
            if (comment.getAuthorId() != null && !comment.getAuthorId().equals(userId)) {
                portalGrowthService.recordEventWithTarget("comment", "receive_like",
                        comment.getAuthorId(), userId, "comment", commentId);
            }
        }
        portalCommentMapper.updateById(comment);
        result.put("isLiked", isLiked);
        result.put("likeCount", comment.getLikeCount());
        return result;
    }
}
