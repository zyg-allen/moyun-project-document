package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.portal.domain.entity.PortalHelpArticle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 帮助中心文章 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalHelpArticleMapper extends BaseMapper<PortalHelpArticle> {

    /**
     * 分页查询帮助文章
     */
    Page<PortalHelpArticle> selectHelpArticlePage(Page<PortalHelpArticle> page, @Param("article") PortalHelpArticle article);

    /**
     * 查询帮助文章列表（不分页）
     */
    List<PortalHelpArticle> selectHelpArticleList(@Param("article") PortalHelpArticle article);

    /**
     * 根据分类ID查询文章列表（前台用）
     */
    List<PortalHelpArticle> selectByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * 查询精选文章（前台用）
     */
    List<PortalHelpArticle> selectFeaturedList(@Param("limit") Integer limit);

    /**
     * 搜索帮助文章（前台用）
     */
    List<PortalHelpArticle> searchArticles(@Param("keyword") String keyword);

    /**
     * 根据ID查询文章详情（前台用，仅返回已发布）
     */
    PortalHelpArticle selectPublishedById(@Param("id") Long id);

    /**
     * 增加查看次数
     */
    int incrementViewCount(@Param("id") Long id);
}
