package com.moyun.portal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.portal.domain.entity.PortalComment;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.domain.query.CommentQuery;
import com.moyun.portal.domain.vo.CommentVO;
import com.moyun.portal.mapper.PortalCommentMapper;
import com.moyun.portal.mapper.PortalUserMapper;
import com.moyun.portal.service.IPortalCommentService;
import com.moyun.portal.util.PortalSecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

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
        
        return portalCommentMapper.insertPortalComment(portalComment);
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
        return portalCommentMapper.deletePortalCommentById(id);
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
        // 1. 一次性查出所有评论
        List<PortalComment> allComments = baseMapper.selectList(
                new LambdaQueryWrapper<PortalComment>()
                        .eq(PortalComment::getArticleId, articleId)
                        .eq(PortalComment::getStatus, "1")
                        .orderByAsc(PortalComment::getCreateTime)
        );
        
        if (allComments.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 2. 批量查询用户信息
        Set<Long> userIds = allComments.stream()
                .map(PortalComment::getAuthorId)
                .collect(Collectors.toSet());
        
        // 添加被回复用户的ID
        allComments.stream()
                .filter(c -> c.getReplyTo() != null)
                .forEach(c -> userIds.add(c.getReplyTo()));
        
        List<PortalUser> users = portalUserMapper.selectBatchIds(userIds);
        Map<Long, PortalUser> userMap = users.stream()
                .collect(Collectors.toMap(PortalUser::getId, u -> u));
        
        // 3. 组装 VO
        List<CommentVO> roots = new ArrayList<>();
        Map<Long, List<CommentVO>> replyMap = new HashMap<>();
        
        for (PortalComment comment : allComments) {
            CommentVO vo = convertToVO(comment, userMap);
            
            if (comment.getParentId() == 0) {
                roots.add(vo);
                replyMap.put(comment.getId(), new ArrayList<>());
            } else {
                Long rootId = comment.getRootId();
                if (rootId != null && rootId > 0) {
                    replyMap.computeIfAbsent(rootId, k -> new ArrayList<>()).add(vo);
                } else {
                    // 如果rootId不存在，尝试使用parentId查找
                    replyMap.computeIfAbsent(comment.getParentId(), k -> new ArrayList<>()).add(vo);
                }
            }
        }
        
        // 4. 将回复挂载到对应的一级评论下
        for (CommentVO root : roots) {
            List<CommentVO> replies = replyMap.get(root.getId());
            if (replies != null && !replies.isEmpty()) {
                root.setReplies(replies);
            }
        }
        
        // 5. 一级评论按时间倒序（最新的在上）
        roots.sort((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()));
        
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
}
