package com.moyun.ext.cms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.portal.domain.entity.PortalHelpArticle;

import java.util.List;

/**
 * CMS 帮助文章管理 Service 接口
 *
 * @author moyun
 */
public interface ICmsHelpArticleService {

    Page<PortalHelpArticle> selectHelpArticlePage(Page<PortalHelpArticle> page, PortalHelpArticle article);

    List<PortalHelpArticle> selectHelpArticleList(PortalHelpArticle article);

    PortalHelpArticle selectHelpArticleById(Long id);

    int insertHelpArticle(PortalHelpArticle article);

    int updateHelpArticle(PortalHelpArticle article);

    int deleteHelpArticleByIds(Long[] ids);
}
