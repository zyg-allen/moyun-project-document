package com.moyun.ext.cms.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.ext.cms.domain.query.CmsArticleQuery;
import com.moyun.ext.cms.domain.vo.CmsArticleVO;
import com.moyun.ext.cms.service.ICmsArticleService;
import com.moyun.portal.domain.entity.PortalArticle;
import com.moyun.portal.mapper.PortalArticleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * CMS文章服务实现类
 *
 * @author moyun
 */
@Service
public class CmsArticleServiceImpl implements ICmsArticleService
{
    @Autowired
    private PortalArticleMapper portalArticleMapper;

    @Override
    public Page<CmsArticleVO> selectArticlePage(CmsArticleQuery query)
    {
        Page<PortalArticle> page = new Page<>(query.getPageNum(), query.getPageSize());
        page = portalArticleMapper.selectPage(page, buildQueryWrapper(query));
        
        Page<CmsArticleVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<CmsArticleVO> voList = BeanUtil.copyToList(page.getRecords(), CmsArticleVO.class);
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public List<PortalArticle> selectArticleList(CmsArticleQuery query)
    {
        return portalArticleMapper.selectList(buildQueryWrapper(query));
    }

    @Override
    public PortalArticle selectArticleById(Long id)
    {
        return portalArticleMapper.selectById(id);
    }

    @Override
    public int insertArticle(PortalArticle article)
    {
        return portalArticleMapper.insert(article);
    }

    @Override
    public int updateArticle(PortalArticle article)
    {
        return portalArticleMapper.updateById(article);
    }

    @Override
    public int auditArticle(PortalArticle article)
    {
        PortalArticle updateArticle = new PortalArticle();
        updateArticle.setId(article.getId());
        updateArticle.setStatus(article.getStatus());
        return portalArticleMapper.updateById(updateArticle);
    }

    @Override
    public int publishArticle(PortalArticle article)
    {
        PortalArticle updateArticle = new PortalArticle();
        updateArticle.setId(article.getId());
        updateArticle.setStatus(article.getStatus());
        return portalArticleMapper.updateById(updateArticle);
    }

    @Override
    public int setFeatured(PortalArticle article)
    {
        PortalArticle updateArticle = new PortalArticle();
        updateArticle.setId(article.getId());
        updateArticle.setIsFeatured(article.getIsFeatured());
        return portalArticleMapper.updateById(updateArticle);
    }

    @Override
    public int deleteArticleByIds(Long[] ids)
    {
        return portalArticleMapper.deleteBatchIds(Arrays.asList(ids));
    }

    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<PortalArticle> buildQueryWrapper(CmsArticleQuery query)
    {
        LambdaQueryWrapper<PortalArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(ObjectUtil.isNotEmpty(query.getTitle()), PortalArticle::getTitle, query.getTitle());
        wrapper.eq(ObjectUtil.isNotNull(query.getAuthorId()), PortalArticle::getAuthorId, query.getAuthorId());
        wrapper.eq(ObjectUtil.isNotNull(query.getCategoryId()), PortalArticle::getCategoryId, query.getCategoryId());
        wrapper.eq(ObjectUtil.isNotEmpty(query.getStatus()), PortalArticle::getStatus, query.getStatus());
        wrapper.eq(ObjectUtil.isNotNull(query.getIsFeatured()), PortalArticle::getIsFeatured, query.getIsFeatured());
        wrapper.orderByDesc(PortalArticle::getCreateTime);
        return wrapper;
    }
}
