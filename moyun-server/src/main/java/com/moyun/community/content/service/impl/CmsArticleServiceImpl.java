package com.moyun.community.content.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.community.content.entity.CmsArticle;
import com.moyun.community.content.mapper.CmsArticleMapper;
import com.moyun.community.content.service.ICmsArticleService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CmsArticleServiceImpl extends ServiceImpl<CmsArticleMapper, CmsArticle> implements ICmsArticleService {

    @Override
    public List<CmsArticle> selectArticleList(CmsArticle article) {
        return baseMapper.selectArticleList(article);
    }

    @Override
    public CmsArticle selectArticleById(Long articleId) {
        return baseMapper.selectArticleById(articleId);
    }

    @Override
    public List<CmsArticle> selectHomeRecommendList() {
        return baseMapper.selectHomeRecommendList();
    }

    @Override
    public List<CmsArticle> selectHotArticleList() {
        return baseMapper.selectHotArticleList();
    }

    @Override
    public List<CmsArticle> selectLatestArticleList() {
        return baseMapper.selectLatestArticleList();
    }

    @Override
    public List<CmsArticle> selectArticleListByCategory(Long categoryId) {
        return baseMapper.selectArticleListByCategory(categoryId);
    }

    @Override
    public List<CmsArticle> selectMyArticleList(Long authorId) {
        return baseMapper.selectMyArticleList(authorId);
    }

    @Override
    public List<CmsArticle> selectPendingAuditList() {
        return baseMapper.selectPendingAuditList();
    }

    @Override
    public boolean incrementViewCount(Long articleId) {
        return baseMapper.incrementViewCount(articleId) > 0;
    }

    @Override
    public boolean incrementLikeCount(Long articleId) {
        return baseMapper.incrementLikeCount(articleId) > 0;
    }

    @Override
    public boolean incrementCollectCount(Long articleId) {
        return baseMapper.incrementCollectCount(articleId) > 0;
    }

    @Override
    public boolean incrementCommentCount(Long articleId) {
        return baseMapper.incrementCommentCount(articleId) > 0;
    }

    @Override
    public boolean updateArticleStatus(Long articleId, String status) {
        return baseMapper.updateArticleStatus(articleId, status) > 0;
    }
}