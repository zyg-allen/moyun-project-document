package com.moyun.community.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.community.content.entity.CmsArticle;
import com.moyun.community.content.entity.CmsCollect;
import com.moyun.community.content.mapper.CmsArticleMapper;
import com.moyun.community.content.mapper.CmsCollectMapper;
import com.moyun.community.content.service.ICmsCollectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CmsCollectServiceImpl extends ServiceImpl<CmsCollectMapper, CmsCollect> implements ICmsCollectService {

    @Autowired
    private CmsCollectMapper collectMapper;

    @Autowired
    private CmsArticleMapper articleMapper;

    @Override
    @Transactional
    public boolean toggleCollect(Long userId, Long articleId) {
        CmsCollect existing = collectMapper.selectByUserAndArticle(userId, articleId);

        if (existing != null) {
            if (existing.getStatus() == 1) {
                existing.setStatus(0);
                collectMapper.updateStatus(existing.getId(), 0);
            } else {
                existing.setStatus(1);
                collectMapper.updateStatus(existing.getId(), 1);
            }
            return true;
        } else {
            CmsCollect newCollect = new CmsCollect();
            newCollect.setUserId(userId);
            newCollect.setArticleId(articleId);
            newCollect.setStatus(1);
            return save(newCollect);
        }
    }

    @Override
    public List<CmsArticle> selectCollectedArticles(Long userId) {
        LambdaQueryWrapper<CmsCollect> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CmsCollect::getUserId, userId)
               .eq(CmsCollect::getStatus, 1)
               .orderByDesc(CmsCollect::getCreateTime);
        List<CmsCollect> collects = collectMapper.selectList(wrapper);

        List<Long> articleIds = collects.stream()
                .map(CmsCollect::getArticleId)
                .collect(Collectors.toList());

        if (articleIds.isEmpty()) {
            return List.of();
        }

        return articleMapper.selectList(
                new LambdaQueryWrapper<CmsArticle>()
                        .in(CmsArticle::getArticleId, articleIds)
                        .eq(CmsArticle::getStatus, "published")
        );
    }

    @Override
    public boolean isCollected(Long userId, Long articleId) {
        CmsCollect collect = collectMapper.selectByUserAndArticle(userId, articleId);
        return collect != null && collect.getStatus() == 1;
    }
}