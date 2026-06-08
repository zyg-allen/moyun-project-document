package com.moyun.portal.controller;

import com.moyun.common.annotation.Anonymous;
import com.moyun.core.base.AjaxResult;
import com.moyun.portal.domain.vo.HomeModuleVO;
import com.moyun.portal.domain.vo.HomePageVO;
import com.moyun.portal.service.IPortalHomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 首页聚合数据Controller
 * 方案A：核心文章聚合接口 + 非核心模块独立接口
 *
 * @author moyun
 */
@Slf4j
@Tag(name = "门户首页", description = "首页聚合数据接口")
@RestController
@RequestMapping("/portal/home")
public class PortalHomeController {

    @Autowired
    private IPortalHomeService portalHomeService;

    // ==================== 核心模块（聚合接口） ====================

    @Anonymous
    @Operation(summary = "获取首页核心文章数据", description = "轮播、精选、热门、最新文章，10分钟缓存")
    @GetMapping("/articles")
    public AjaxResult getHomeArticles() {
        try {
            HomePageVO data = portalHomeService.getHomePageArticles();
            return AjaxResult.success(data);
        } catch (Exception e) {
            log.error("获取首页文章数据失败", e);
            return AjaxResult.error("获取首页文章数据失败");
        }
    }

    // ==================== 非核心模块（独立接口） ====================

    @Anonymous
    @Operation(summary = "获取首页分类列表", description = "30分钟缓存")
    @GetMapping("/categories")
    public AjaxResult getHomeCategories() {
        try {
            List<HomeModuleVO.CategorySimpleVO> data = portalHomeService.getHomeCategories();
            return AjaxResult.success(data);
        } catch (Exception e) {
            log.error("获取首页分类失败", e);
            return AjaxResult.error("获取首页分类失败");
        }
    }

    @Anonymous
    @Operation(summary = "获取首页热门标签", description = "30分钟缓存")
    @GetMapping("/tags")
    public AjaxResult getHomeTags() {
        try {
            List<HomeModuleVO.TagSimpleVO> data = portalHomeService.getHomeTags();
            return AjaxResult.success(data);
        } catch (Exception e) {
            log.error("获取首页标签失败", e);
            return AjaxResult.error("获取首页标签失败");
        }
    }

    @Anonymous
    @Operation(summary = "获取首页友情链接", description = "60分钟缓存")
    @GetMapping("/friendLinks")
    public AjaxResult getHomeFriendLinks() {
        try {
            List<HomeModuleVO.FriendLinkSimpleVO> data = portalHomeService.getHomeFriendLinks();
            return AjaxResult.success(data);
        } catch (Exception e) {
            log.error("获取友情链接失败", e);
            return AjaxResult.error("获取友情链接失败");
        }
    }

    @Anonymous
    @Operation(summary = "获取首页名家列表", description = "20分钟缓存")
    @GetMapping("/authors")
    public AjaxResult getHomeAuthors() {
        try {
            List<HomeModuleVO.AuthorSimpleVO> data = portalHomeService.getHomeAuthors();
            return AjaxResult.success(data);
        } catch (Exception e) {
            log.error("获取名家列表失败", e);
            return AjaxResult.error("获取名家列表失败");
        }
    }

    @Anonymous
    @Operation(summary = "获取首页分类推荐文章", description = "15分钟缓存")
    @GetMapping("/categoryArticles")
    public AjaxResult getHomeCategoryArticles() {
        try {
            List<HomeModuleVO.CategoryArticlesVO> data = portalHomeService.getHomeCategoryArticles();
            return AjaxResult.success(data);
        } catch (Exception e) {
            log.error("获取分类推荐文章失败", e);
            return AjaxResult.error("获取分类推荐文章失败");
        }
    }

    // ==================== 缓存管理（管理端使用） ====================

    @Operation(summary = "刷新首页所有缓存", description = "管理端手动刷新所有首页缓存")
    @PostMapping("/refresh")
    public AjaxResult refreshAllHomeCache() {
        log.info("请求刷新首页所有缓存");
        try {
            portalHomeService.refreshAllHomeCache();
            return AjaxResult.success("缓存刷新成功");
        } catch (Exception e) {
            log.error("刷新首页缓存失败", e);
            return AjaxResult.error("刷新首页缓存失败");
        }
    }

    @Operation(summary = "清除首页所有缓存", description = "管理端手动清除所有首页缓存")
    @PostMapping("/clear")
    public AjaxResult clearAllHomeCache() {
        log.info("请求清除首页所有缓存");
        try {
            portalHomeService.clearAllHomeCache();
            return AjaxResult.success("缓存清除成功");
        } catch (Exception e) {
            log.error("清除首页缓存失败", e);
            return AjaxResult.error("清除首页缓存失败");
        }
    }
}
