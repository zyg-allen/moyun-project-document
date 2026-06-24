package com.moyun.ext.cms.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.ext.cms.service.ICmsHelpArticleService;
import com.moyun.portal.domain.entity.PortalHelpArticle;
import com.moyun.portal.mapper.PortalHelpArticleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * CMS 帮助文章管理 Service 实现
 *
 * @author moyun
 */
@Service
public class CmsHelpArticleServiceImpl implements ICmsHelpArticleService {

    @Autowired
    private PortalHelpArticleMapper helpArticleMapper;

    @Override
    public Page<PortalHelpArticle> selectHelpArticlePage(Page<PortalHelpArticle> page, PortalHelpArticle article) {
        return helpArticleMapper.selectHelpArticlePage(page, article);
    }

    @Override
    public List<PortalHelpArticle> selectHelpArticleList(PortalHelpArticle article) {
        return helpArticleMapper.selectHelpArticleList(article);
    }

    @Override
    public PortalHelpArticle selectHelpArticleById(Long id) {
        return helpArticleMapper.selectById(id);
    }

    @Override
    public int insertHelpArticle(PortalHelpArticle article) {
        return helpArticleMapper.insert(article);
    }

    @Override
    public int updateHelpArticle(PortalHelpArticle article) {
        return helpArticleMapper.updateById(article);
    }

    @Override
    public int deleteHelpArticleByIds(Long[] ids) {
        return helpArticleMapper.deleteBatchIds(java.util.Arrays.asList(ids));
    }
}
