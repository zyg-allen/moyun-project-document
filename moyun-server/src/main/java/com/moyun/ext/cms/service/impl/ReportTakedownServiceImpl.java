package com.moyun.ext.cms.service.impl;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.moyun.ext.cms.service.IFeedService;
import com.moyun.ext.cms.service.IReportTakedownService;
import com.moyun.portal.domain.entity.PortalArticle;
import com.moyun.portal.domain.entity.PortalColumn;
import com.moyun.portal.domain.entity.PortalComment;
import com.moyun.portal.domain.entity.PortalTopic;
import com.moyun.portal.domain.entity.PortalTopicComment;
import com.moyun.portal.domain.entity.PortalTopicPost;
import com.moyun.portal.mapper.PortalArticleMapper;
import com.moyun.portal.mapper.PortalColumnMapper;
import com.moyun.portal.mapper.PortalCommentMapper;
import com.moyun.portal.mapper.PortalTopicCommentMapper;
import com.moyun.portal.mapper.PortalTopicMapper;
import com.moyun.portal.mapper.PortalTopicPostMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * 举报联动下架服务实现
 *
 * @author moyun
 */
@Slf4j
@Service
public class ReportTakedownServiceImpl implements IReportTakedownService {

    @Autowired
    private PortalArticleMapper articleMapper;

    @Autowired
    private PortalCommentMapper commentMapper;

    @Autowired
    private PortalTopicMapper topicMapper;

    @Autowired
    private PortalTopicPostMapper topicPostMapper;

    @Autowired
    private PortalTopicCommentMapper topicCommentMapper;

    @Autowired
    private PortalColumnMapper columnMapper;

    @Autowired(required = false)
    private IFeedService feedService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean takedown(String targetType, Long targetId, String handler) {
        if (targetType == null || targetId == null) {
            return false;
        }
        try {
            switch (targetType) {
                case "article":
                    return takedownArticle(targetId, handler);
                case "comment":
                    return takedownComment(targetId, handler);
                case "topic":
                    return takedownTopic(targetId, handler);
                case "topic_post":
                    return takedownTopicPost(targetId, handler);
                case "topic_comment":
                    return takedownTopicComment(targetId, handler);
                case "column":
                    return takedownColumn(targetId, handler);
                default:
                    log.info("举报联动下架：目标类型 {} 暂不支持自动下架，跳过（targetId={}）", targetType, targetId);
                    return false;
            }
        } catch (Exception e) {
            log.error("举报联动下架失败：targetType={}, targetId={}, handler={}, err={}",
                    targetType, targetId, handler, e.getMessage(), e);
            // 抛出以触发事务回滚，避免出现"举报已解决但内容未下架"的不一致
            throw new RuntimeException("举报联动下架失败：" + e.getMessage(), e);
        }
    }

    /**
     * 文章下架：status → rejected（沿用审核驳回状态）
     * 同时清理 Feed 事件，避免下架文章继续在 Feed 流曝光
     */
    private boolean takedownArticle(Long articleId, String handler) {
        PortalArticle existing = articleMapper.selectById(articleId);
        if (existing == null) {
            log.warn("举报联动下架-文章不存在：articleId={}", articleId);
            return false;
        }
        LambdaUpdateWrapper<PortalArticle> uw = new LambdaUpdateWrapper<>();
        uw.eq(PortalArticle::getId, articleId)
                .set(PortalArticle::getStatus, "rejected")
                .set(PortalArticle::getAuditRemark, "举报成立，自动下架（处理人：" + handler + "）")
                .set(PortalArticle::getAuditTime, LocalDateTime.now());
        int rows = articleMapper.update(null, uw);
        // 清理 Feed 事件
        if (feedService != null) {
            try {
                feedService.deleteEvent("article", articleId);
            } catch (Exception e) {
                log.warn("举报下架-清理文章 Feed 事件失败：articleId={}, err={}", articleId, e.getMessage());
            }
        }
        log.info("举报联动下架-文章：articleId={}, rows={}, handler={}", articleId, rows, handler);
        return rows > 0;
    }

    /**
     * 通用评论下架：物理软删（del_flag，沿用 BaseMapper 逻辑删除）
     */
    private boolean takedownComment(Long commentId, String handler) {
        PortalComment existing = commentMapper.selectById(commentId);
        if (existing == null) {
            log.warn("举报联动下架-评论不存在：commentId={}", commentId);
            return false;
        }
        int rows = commentMapper.deleteById(commentId);
        log.info("举报联动下架-评论：commentId={}, rows={}, handler={}", commentId, rows, handler);
        return rows > 0;
    }

    /**
     * 话题下架：status → archived（归档，从 active 列表移除，保留数据可申诉恢复）
     * 同时清理 Feed 事件
     */
    private boolean takedownTopic(Long topicId, String handler) {
        PortalTopic existing = topicMapper.selectById(topicId);
        if (existing == null) {
            log.warn("举报联动下架-话题不存在：topicId={}", topicId);
            return false;
        }
        LambdaUpdateWrapper<PortalTopic> uw = new LambdaUpdateWrapper<>();
        uw.eq(PortalTopic::getId, topicId)
                .set(PortalTopic::getStatus, "archived")
                .set(PortalTopic::getAuditRemark, "举报成立，自动归档下架（处理人：" + handler + "）")
                .set(PortalTopic::getAuditTime, LocalDateTime.now());
        int rows = topicMapper.update(null, uw);
        if (feedService != null) {
            try {
                feedService.deleteEvent("topic", topicId);
            } catch (Exception e) {
                log.warn("举报下架-清理话题 Feed 事件失败：topicId={}, err={}", topicId, e.getMessage());
            }
        }
        log.info("举报联动下架-话题：topicId={}, rows={}, handler={}", topicId, rows, handler);
        return rows > 0;
    }

    /**
     * 话题观点下架：is_deleted=1（软删）
     */
    private boolean takedownTopicPost(Long postId, String handler) {
        PortalTopicPost existing = topicPostMapper.selectById(postId);
        if (existing == null) {
            log.warn("举报联动下架-话题观点不存在：postId={}", postId);
            return false;
        }
        LambdaUpdateWrapper<PortalTopicPost> uw = new LambdaUpdateWrapper<>();
        uw.eq(PortalTopicPost::getId, postId)
                .set(PortalTopicPost::getIsDeleted, 1)
                .set(PortalTopicPost::getUpdatedTime, LocalDateTime.now());
        int rows = topicPostMapper.update(null, uw);
        log.info("举报联动下架-话题观点：postId={}, rows={}, handler={}", postId, rows, handler);
        return rows > 0;
    }

    /**
     * 话题评论下架：is_deleted=1（软删）
     */
    private boolean takedownTopicComment(Long commentId, String handler) {
        PortalTopicComment existing = topicCommentMapper.selectById(commentId);
        if (existing == null) {
            log.warn("举报联动下架-话题评论不存在：commentId={}", commentId);
            return false;
        }
        LambdaUpdateWrapper<PortalTopicComment> uw = new LambdaUpdateWrapper<>();
        uw.eq(PortalTopicComment::getId, commentId)
                .set(PortalTopicComment::getIsDeleted, 1)
                .set(PortalTopicComment::getUpdatedTime, LocalDateTime.now());
        int rows = topicCommentMapper.update(null, uw);
        log.info("举报联动下架-话题评论：commentId={}, rows={}, handler={}", commentId, rows, handler);
        return rows > 0;
    }

    /**
     * 专栏下架：status → archived（归档，从公开列表移除）
     * 同时清理 Feed 事件
     */
    private boolean takedownColumn(Long columnId, String handler) {
        PortalColumn existing = columnMapper.selectById(columnId);
        if (existing == null) {
            log.warn("举报联动下架-专栏不存在：columnId={}", columnId);
            return false;
        }
        LambdaUpdateWrapper<PortalColumn> uw = new LambdaUpdateWrapper<>();
        uw.eq(PortalColumn::getId, columnId)
                .set(PortalColumn::getStatus, "archived")
                .set(PortalColumn::getAuditRemark, "举报成立，自动归档下架（处理人：" + handler + "）")
                .set(PortalColumn::getAuditTime, LocalDateTime.now())
                .set(PortalColumn::getUpdatedTime, LocalDateTime.now());
        int rows = columnMapper.update(null, uw);
        if (feedService != null) {
            try {
                feedService.deleteEvent("column", columnId);
            } catch (Exception e) {
                log.warn("举报下架-清理专栏 Feed 事件失败：columnId={}, err={}", columnId, e.getMessage());
            }
        }
        log.info("举报联动下架-专栏：columnId={}, rows={}, handler={}", columnId, rows, handler);
        return rows > 0;
    }
}
