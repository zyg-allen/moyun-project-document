package com.moyun.community.content.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.community.content.entity.CmsComment;
import com.moyun.community.content.mapper.CmsCommentMapper;
import com.moyun.community.content.service.ICmsCommentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CmsCommentServiceImpl extends ServiceImpl<CmsCommentMapper, CmsComment> implements ICmsCommentService {

    @Override
    public List<CmsComment> selectCommentListByArticle(Long articleId) {
        return baseMapper.selectCommentListByArticle(articleId);
    }

    @Override
    public List<CmsComment> selectCommentListByUser(Long userId) {
        return baseMapper.selectCommentListByUser(userId);
    }

    @Override
    @Transactional
    public boolean addComment(CmsComment comment) {
        comment.setStatus("0");
        comment.setLikeCount(0);
        return save(comment);
    }

    @Override
    @Transactional
    public boolean replyComment(CmsComment comment) {
        comment.setStatus("0");
        comment.setLikeCount(0);
        return save(comment);
    }

    @Override
    @Transactional
    public boolean deleteComment(Long commentId) {
        CmsComment comment = getById(commentId);
        if (comment == null) {
            return false;
        }
        comment.setStatus("2");
        return updateById(comment);
    }

    @Override
    @Transactional
    public boolean toggleLike(Long commentId) {
        return baseMapper.incrementLikeCount(commentId) > 0;
    }
}