package com.moyun.portal.service.impl;

import com.moyun.portal.domain.entity.PortalArticle;
import com.moyun.portal.mapper.PortalArticleMapper;
import com.moyun.portal.mapper.PortalArticleViewMapper;
import com.moyun.portal.service.IPortalArticleService;
import com.moyun.portal.service.IPortalArticleViewService;
import com.moyun.portal.domain.query.ArticleQuery;
import com.moyun.util.bean.PageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 文章浏览数据同步服务实现
 */
@Service
public class PortalArticleViewServiceImpl implements IPortalArticleViewService {

    private static final Logger log = LoggerFactory.getLogger(PortalArticleViewServiceImpl.class);

    @Autowired
    private PortalArticleViewMapper portalArticleViewMapper;

    @Autowired
    private PortalArticleMapper portalArticleMapper;

    @Autowired
    private IPortalArticleService portalArticleService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long repairArticleViews(Long articleId) {
        // 1. 获取真实阅读量
        long realViews = getRealArticleViews(articleId);

        // 2. 更新 portal_article 表
        PortalArticle article = portalArticleService.selectPortalArticleById(articleId);
        if (article != null) {
            long oldViews = article.getViews() == null ? 0 : article.getViews();
            if (oldViews != realViews) {
                article.setViews(realViews);
                portalArticleService.updatePortalArticle(article);
                log.info("修复文章[{}]阅读量: {} -> {}", articleId, oldViews, realViews);
            }
        }

        return realViews;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long repairAllArticleViews() {
        log.info("开始批量修复所有文章的阅读量...");

        // 1. 查询所有有浏览记录的文章ID
        List<Long> articleIds = portalArticleViewMapper.selectAllViewedArticleIds();

        int repairedCount = 0;

        // 2. 逐个修复
        for (Long articleId : articleIds) {
            repairArticleViews(articleId);
            repairedCount++;

            // 每100篇打个日志
            if (repairedCount % 100 == 0) {
                log.info("已修复 {} 篇文章的阅读量", repairedCount);
            }
        }

        log.info("批量修复完成，共修复 {} 篇文章", repairedCount);
        return repairedCount;
    }

    @Override
    public boolean checkArticleViewsConsistency(Long articleId) {
        PortalArticle article = portalArticleService.selectPortalArticleById(articleId);
        if (article == null) {
            return true; // 文章不存在，认为一致
        }

        long dbViews = article.getViews() == null ? 0 : article.getViews();
        int realViews = getRealArticleViews(articleId);

        boolean consistent = dbViews == realViews;

        if (!consistent) {
            log.warn("文章[{}]阅读量不一致，数据库: {}, 真实: {}", articleId, dbViews, realViews);
        }

        return consistent;
    }

    @Override
    public int getRealArticleViews(Long articleId) {
        return portalArticleViewMapper.countUniqueViews(articleId);
    }
}
