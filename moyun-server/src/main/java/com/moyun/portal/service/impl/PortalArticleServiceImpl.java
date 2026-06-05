package com.moyun.portal.service.impl;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.portal.domain.entity.PortalArticle;
import com.moyun.portal.domain.query.ArticleQuery;
import com.moyun.portal.mapper.PortalArticleMapper;
import com.moyun.portal.service.IPortalArticleService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 门户文章 业务层处理
 *
 * @author moyun
 */
@Service
public class PortalArticleServiceImpl extends ServiceImpl<PortalArticleMapper, PortalArticle> implements IPortalArticleService {

    /**
     * 根据条件分页查询文章列表
     *
     * @param page 分页参数
     * @param portalArticle 文章信息
     * @return 分页结果
     */
    @Override
    public Page<PortalArticle> selectPortalArticlePage(Page<PortalArticle> page, ArticleQuery portalArticle) {
        return baseMapper.selectPortalArticlePage(page, portalArticle);
    }

    /**
     * 根据条件查询文章列表（不分页，用于导出等场景）
     *
     * @param portalArticle 文章信息
     * @return 文章信息集合
     */
    @Override
    public List<PortalArticle> selectPortalArticleList(ArticleQuery portalArticle) {
        return baseMapper.selectPortalArticleList(portalArticle);
    }

    /**
     * 通过文章ID查询文章
     *
     * @param id 文章ID
     * @return 文章对象信息
     */
    @Override
    public PortalArticle selectPortalArticleById(Long id) {
        return baseMapper.selectPortalArticleById(id);
    }

    /**
     * 新增文章信息
     *
     * @param portalArticle 文章信息
     * @return 结果
     */
    @Override
    public int insertPortalArticle(PortalArticle portalArticle) {
        return baseMapper.insertPortalArticle(portalArticle);
    }

    /**
     * 修改文章信息
     *
     * @param portalArticle 文章信息
     * @return 结果
     */
    @Override
    public int updatePortalArticle(PortalArticle portalArticle) {
        return baseMapper.updatePortalArticle(portalArticle);
    }

    /**
     * 通过文章ID删除文章
     *
     * @param id 文章ID
     * @return 结果
     */
    @Override
    public int deletePortalArticleById(Long id) {
        return baseMapper.deletePortalArticleById(id);
    }

    /**
     * 批量删除文章信息
     *
     * @param ids 需要删除的文章ID
     * @return 结果
     */
    @Override
    public int deletePortalArticleByIds(Long[] ids) {
        return baseMapper.deletePortalArticleByIds(ids);
    }
}
