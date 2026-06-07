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
import com.moyun.util.file.Base64ImageUtils;
import com.moyun.util.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

    @Autowired
    private Base64ImageUtils base64ImageUtils;

    @Override
    public Page<CmsArticleVO> selectArticlePage(Page<CmsArticleVO> page, CmsArticleQuery query)
    {
        Page<PortalArticle> entityPage = new Page<>(page.getCurrent(), page.getSize());
        entityPage = portalArticleMapper.selectPage(entityPage, buildQueryWrapper(query));
        
        Page<CmsArticleVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        List<CmsArticleVO> voList = BeanUtil.copyToList(entityPage.getRecords(), CmsArticleVO.class);
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
        // 自动处理Base64图片
        processArticleImages(article);
        // 自动设置后台作者信息
        fillAdminAuthorInfo(article);
        // 设置默认状态为已发布
        if (article.getStatus() == null) {
            article.setStatus("published");
        }
        // 设置发布时间
        if (article.getPublishedAt() == null) {
            article.setPublishedAt(LocalDateTime.now());
        }
        return portalArticleMapper.insert(article);
    }

    /**
     * 自动设置后台作者信息
     *
     * @param article 文章信息
     */
    private void fillAdminAuthorInfo(PortalArticle article) {
        // 自动设置作者ID为当前登录的后台用户ID（如果还没设置）
        if (article.getAuthorId() == null) {
            Long adminUserId = SecurityUtils.getUserId();
            if (adminUserId != null) {
                article.setAuthorId(adminUserId);
            }
        }
    }

    /**
     * 处理文章的Base64图片（封面、富文本、Markdown内容）
     *
     * @param article 文章信息
     */
    private void processArticleImages(PortalArticle article) {
        // 处理封面
        if (article.getCover() != null) {
            article.setCover(base64ImageUtils.uploadBase64Image(article.getCover()));
        }
        // 处理富文本内容
        if (article.getContent() != null) {
            article.setContent(base64ImageUtils.processContentImages(article.getContent()));
        }
        // 处理Markdown内容
        if (article.getContentMarkdown() != null) {
            article.setContentMarkdown(base64ImageUtils.processContentImages(article.getContentMarkdown()));
        }
    }

    @Override
    public int updateArticle(PortalArticle article)
    {
        // 自动处理Base64图片
        processArticleImages(article);
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
