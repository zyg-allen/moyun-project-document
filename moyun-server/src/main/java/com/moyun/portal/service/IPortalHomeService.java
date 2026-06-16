package com.moyun.portal.service;

import java.util.List;

import com.moyun.portal.domain.vo.HomeModuleVO;
import com.moyun.portal.domain.vo.HomePageVO;

/**
 * 首页数据服务接口
 * 方案A：核心文章聚合 + 非核心模块独立接口
 *
 * @author moyun
 */
public interface IPortalHomeService {

    // ==================== 核心模块（聚合接口） ====================

    /**
     * 获取首页核心文章数据（轮播、精选、热门、最新）
     */
    HomePageVO getHomePageArticles();

    // ==================== 非核心模块（独立接口） ====================

    /**
     * 获取分类列表
     */
    List<HomeModuleVO.CategorySimpleVO> getHomeCategories();

    /**
     * 获取热门标签列表
     */
    List<HomeModuleVO.TagSimpleVO> getHomeTags();

    /**
     * 获取友情链接列表
     */
    List<HomeModuleVO.FriendLinkSimpleVO> getHomeFriendLinks();

    /**
     * 获取名家列表
     */
    List<HomeModuleVO.AuthorSimpleVO> getHomeAuthors();

    /**
     * 获取分类推荐文章
     */
    List<HomeModuleVO.CategoryArticlesVO> getHomeCategoryArticles();

    // ==================== 缓存管理 ====================

    /**
     * 刷新首页所有缓存
     */
    void refreshAllHomeCache();

    /**
     * 清除首页所有缓存
     */
    void clearAllHomeCache();
}
