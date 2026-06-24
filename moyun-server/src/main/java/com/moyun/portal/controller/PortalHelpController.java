package com.moyun.portal.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.portal.service.IPortalHelpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 门户帮助中心 Controller（前台公开访问）
 *
 * @author moyun
 */
@Tag(name = "门户帮助中心", description = "帮助中心分类、文章查询接口（公开）")
@RestController
@RequestMapping("/portal/help")
public class PortalHelpController extends BaseController {

    @Autowired
    private IPortalHelpService portalHelpService;

    /**
     * 帮助中心首页数据（分类列表 + 精选问题）
     */
    @Operation(summary = "帮助中心首页数据", description = "返回所有分类和精选问题列表")
    @GetMapping("/home")
    public AjaxResult home() {
        Map<String, Object> data = new HashMap<>();
        data.put("categories", portalHelpService.selectActiveCategoryList());
        data.put("featuredArticles", portalHelpService.selectFeaturedArticles(5));
        return success(data);
    }

    /**
     * 查询所有分类
     */
    @Operation(summary = "查询所有分类", description = "返回所有启用的帮助分类")
    @GetMapping("/categories")
    public AjaxResult categories() {
        return success(portalHelpService.selectActiveCategoryList());
    }

    /**
     * 根据分类查询文章列表
     */
    @Operation(summary = "按分类查询文章", description = "根据分类ID查询该分类下的所有已发布文章")
    @GetMapping("/category/{categoryId}")
    public AjaxResult listByCategory(@Parameter(description = "分类ID") @PathVariable Long categoryId) {
        return success(portalHelpService.selectArticlesByCategory(categoryId));
    }

    /**
     * 查询精选文章
     */
    @Operation(summary = "查询精选文章", description = "返回精选帮助文章列表")
    @GetMapping("/featured")
    public AjaxResult featured(@Parameter(description = "数量") @RequestParam(required = false, defaultValue = "5") Integer limit) {
        return success(portalHelpService.selectFeaturedArticles(limit));
    }

    /**
     * 搜索帮助文章
     */
    @Operation(summary = "搜索帮助文章", description = "根据关键词搜索帮助文章")
    @GetMapping("/search")
    public AjaxResult search(@Parameter(description = "关键词") @RequestParam String keyword) {
        return success(portalHelpService.searchArticles(keyword));
    }

    /**
     * 查询文章详情
     */
    @Operation(summary = "查询文章详情", description = "根据ID查询帮助文章详情，同时增加查看次数")
    @GetMapping("/article/{id}")
    public AjaxResult detail(@Parameter(description = "文章ID") @PathVariable Long id) {
        Object article = portalHelpService.selectArticleDetail(id);
        if (article == null) {
            return error("文章不存在或未发布");
        }
        return success(article);
    }
}
