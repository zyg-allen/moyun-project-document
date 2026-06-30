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
 * 清理说明：本 Controller 曾包含以下接口，经核查前端无调用，已作为死接口删除：
 *   - GET /portal/help/categories          查询所有分类（home 已聚合返回，无需单独调用）
 *   - GET /portal/help/category/{id}       按分类查询文章（前端未使用）
 *   - GET /portal/help/featured            查询精选文章（home 已聚合返回，无需单独调用）
 *   - GET /portal/help/article/{id}        查询文章详情（前端未使用）
 * 保留接口：home（首页聚合）、search（搜索）。
 * 注意：对应的 Service / Mapper / XML 实现保持不变，仅删除 Controller 层入口。
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
     * 搜索帮助文章
     */
    @Operation(summary = "搜索帮助文章", description = "根据关键词搜索帮助文章")
    @GetMapping("/search")
    public AjaxResult search(@Parameter(description = "关键词") @RequestParam String keyword) {
        return success(portalHelpService.searchArticles(keyword));
    }
}
