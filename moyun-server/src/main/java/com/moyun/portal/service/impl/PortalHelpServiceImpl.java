package com.moyun.portal.service.impl;

import com.moyun.portal.domain.entity.PortalHelpArticle;
import com.moyun.portal.domain.entity.PortalHelpCategory;
import com.moyun.portal.mapper.PortalHelpArticleMapper;
import com.moyun.portal.mapper.PortalHelpCategoryMapper;
import com.moyun.portal.service.IPortalHelpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 帮助中心（前台）Service 实现
 *
 * @author moyun
 */
@Service
public class PortalHelpServiceImpl implements IPortalHelpService {

    @Autowired
    private PortalHelpCategoryMapper helpCategoryMapper;

    @Autowired
    private PortalHelpArticleMapper helpArticleMapper;

    @Override
    public List<PortalHelpCategory> selectActiveCategoryList() {
        return helpCategoryMapper.selectActiveCategoryList();
    }

    @Override
    public List<PortalHelpArticle> selectArticlesByCategory(Long categoryId) {
        return helpArticleMapper.selectByCategoryId(categoryId);
    }

    @Override
    public List<PortalHelpArticle> selectFeaturedArticles(Integer limit) {
        return helpArticleMapper.selectFeaturedList(limit == null || limit <= 0 ? 5 : limit);
    }

    @Override
    public List<PortalHelpArticle> searchArticles(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        return helpArticleMapper.searchArticles(keyword.trim());
    }

    @Override
    public PortalHelpArticle selectArticleDetail(Long id) {
        PortalHelpArticle article = helpArticleMapper.selectPublishedById(id);
        if (article != null) {
            // 增加查看次数
            helpArticleMapper.incrementViewCount(id);
        }
        return article;
    }
}
