package com.moyun.portal.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.moyun.common.annotation.Anonymous;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.portal.domain.entity.PortalCategory;
import com.moyun.portal.domain.query.CategoryQuery;
import com.moyun.portal.service.IPortalCategoryService;

/**
 * 门户分类 Controller
 *
 * 清理说明（2026-06-28）：
 * 原本该 Controller 共 9 个接口，经前端调用核查后确认以下 8 个为死接口（前端未调用）：
 *   - GET  /portal/category/list      → getAllCategories（前端未调用 getCategoryList）
 *   - GET  /portal/category/tree      → getCategoryTree（前端实际调用 /public/tree）
 *   - GET  /portal/category/page      → page
 *   - POST /portal/category/export    → export
 *   - GET  /portal/category/{id}      → getInfo
 *   - POST /portal/category           → add（已加 isAdmin 校验，仍无人调用）
 *   - PUT  /portal/category           → edit（已加 isAdmin 校验，仍无人调用）
 *   - DELETE /portal/category/{ids}   → remove（已加 isAdmin 校验，仍无人调用）
 *
 * 本次清理仅删除 Controller 层方法，对应的 Service / Mapper / XML 实现保持不变，
 * 以便未来恢复或在其他场景复用。仅保留 getPublicCategoryTree（/public/tree）供
 * HomePage.vue / PublishPage.vue 等前端页面调用。
 */
@Tag(name = "门户分类", description = "门户分类的增删改查操作接口")
@RestController
@RequestMapping("/portal/category")
public class PortalCategoryController extends BaseController {

    @Autowired
    private IPortalCategoryService portalCategoryService;

    @Anonymous
    @Operation(summary = "获取公开树形分类列表", description = "获取公开的树形分类")
    @GetMapping("/public/tree")
    public AjaxResult getPublicCategoryTree(CategoryQuery query) {
        query.setStatus("0");
        List<PortalCategory> list = portalCategoryService.selectPortalCategoryTree(query);
        return success(list);
    }
}
