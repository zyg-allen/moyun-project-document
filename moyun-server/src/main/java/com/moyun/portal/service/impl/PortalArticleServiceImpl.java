package com.moyun.portal.service.impl;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.portal.domain.entity.PortalArticle;
import com.moyun.portal.domain.query.ArticleQuery;
import com.moyun.portal.mapper.PortalArticleMapper;
import com.moyun.portal.service.IPortalArticleService;
import com.moyun.portal.service.IPortalCategoryService;
import com.moyun.portal.util.PortalSecurityUtils;
import com.moyun.util.file.Base64ImageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 门户文章 业务层处理
 *
 * @author moyun
 */
@Service
public class PortalArticleServiceImpl extends ServiceImpl<PortalArticleMapper, PortalArticle> implements IPortalArticleService {

    @Autowired
    private IPortalCategoryService portalCategoryService;

    @Autowired
    private Base64ImageUtils base64ImageUtils;

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
     * 新增文章信息（前台用户发布）
     * 自动设置作者ID为当前登录的门户用户
     *
     * @param portalArticle 文章信息
     * @return 结果
     */
    @Override
    public int insertPortalArticle(PortalArticle portalArticle) {
        // 自动处理Base64图片
        processArticleImages(portalArticle);
        // 自动设置前台作者信息
        fillPortalAuthorAndCategory(portalArticle);
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
        // 自动处理Base64图片
        processArticleImages(portalArticle);
        // TODO: 运行数据库升级脚本后取消注释
        // fillCategoryPath(portalArticle);
        return baseMapper.updatePortalArticle(portalArticle);
    }
    
    /**
     * 前台发布文章（带自动发布逻辑）
     *
     * @param portalArticle 文章信息
     * @return 结果
     */
    @Override
    public int publishArticle(PortalArticle portalArticle) {
        // 自动处理Base64图片
        processArticleImages(portalArticle);
        // 自动设置前台作者信息
        fillPortalAuthorAndCategory(portalArticle);
        // 设置默认状态为已发布
        if (portalArticle.getStatus() == null) {
            portalArticle.setStatus("published");
        }
        // 设置发布时间
        if (portalArticle.getPublishedAt() == null) {
            portalArticle.setPublishedAt(LocalDateTime.now());
        }
        return baseMapper.insertPortalArticle(portalArticle);
    }
    
    /**
     * 自动设置前台作者信息和分类路径
     *
     * @param portalArticle 文章信息
     */
    private void fillPortalAuthorAndCategory(PortalArticle portalArticle) {
        // 自动设置作者ID（如果还没设置）
        if (portalArticle.getAuthorId() == null) {
            portalArticle.setAuthorId(PortalSecurityUtils.getUserId());
        }
        // TODO: 运行数据库升级脚本后取消注释
        // fillCategoryPath(portalArticle);
    }

    /**
     * 处理文章的Base64图片（封面、富文本、Markdown内容）
     *
     * @param portalArticle 文章信息
     */
    private void processArticleImages(PortalArticle portalArticle) {
        // 处理封面
        if (portalArticle.getCover() != null) {
            portalArticle.setCover(base64ImageUtils.uploadBase64Image(portalArticle.getCover()));
        }
        // 处理富文本内容
        if (portalArticle.getContent() != null) {
            portalArticle.setContent(base64ImageUtils.processContentImages(portalArticle.getContent()));
        }
        // 处理Markdown内容
        if (portalArticle.getContentMarkdown() != null) {
            portalArticle.setContentMarkdown(base64ImageUtils.processContentImages(portalArticle.getContentMarkdown()));
        }
    }
    
    /**
     * 自动填充分类路径和顶级分类ID
     *
     * @param portalArticle 文章信息
     */
    // TODO: 运行数据库升级脚本后取消注释
    /*
    private void fillCategoryPath(PortalArticle portalArticle) {
        if (portalArticle.getCategoryId() != null) {
            portalArticle.setCategoryPath(portalCategoryService.getCategoryPath(portalArticle.getCategoryId()));
            portalArticle.setRootCategoryId(portalCategoryService.getRootCategoryId(portalArticle.getCategoryId()));
        }
    }
    */

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
