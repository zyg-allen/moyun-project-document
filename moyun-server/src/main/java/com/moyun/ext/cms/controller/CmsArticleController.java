package com.moyun.ext.cms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ext.cms.domain.query.CmsArticleQuery;
import com.moyun.ext.cms.domain.vo.CmsArticleVO;
import com.moyun.ext.cms.service.ICmsArticleService;
import com.moyun.portal.domain.entity.PortalArticle;
import com.moyun.util.bean.PageUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * CMS文章管理Controller
 *
 * @author moyun
 */
@Tag(name = "CMS文章管理", description = "CMS文章管理接口")
@RestController
@RequestMapping("/cms/article")
public class CmsArticleController extends BaseController {
    @Autowired
    private ICmsArticleService cmsArticleService;

    /**
     * 获取文章列表
     */
    @Operation(summary = "获取文章列表", description = "根据条件分页查询文章列表")
    @PreAuthorize("@ss.hasPermi('cms:article:list')")
    @GetMapping("/list")
    public AjaxResult list(CmsArticleQuery query) {
        Page<CmsArticleVO> page = PageUtils.buildPage(query);
        page = cmsArticleService.selectArticlePage(page, query);
        return success(page);
    }

    /**
     * 根据文章编号获取详细信息
     */
    @Operation(summary = "获取文章详情", description = "根据文章ID获取文章详细信息")
    @PreAuthorize("@ss.hasPermi('cms:article:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@Parameter(description = "文章ID") @PathVariable Long id) {
        return success(cmsArticleService.selectArticleById(id));
    }

    /**
     * 新增文章
     */
    @Operation(summary = "新增文章", description = "创建新文章")
    @PreAuthorize("@ss.hasPermi('cms:article:add')")
    @Log(title = "文章管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody PortalArticle article) {
        return toAjax(cmsArticleService.insertArticle(article));
    }

    /**
     * 修改文章
     */
    @Operation(summary = "修改文章", description = "更新文章信息")
    @PreAuthorize("@ss.hasPermi('cms:article:edit')")
    @Log(title = "文章管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody PortalArticle article) {
        return toAjax(cmsArticleService.updateArticle(article));
    }

    /**
     * 审核文章
     */
    @Operation(summary = "审核文章", description = "审核文章内容")
    @PreAuthorize("@ss.hasPermi('cms:article:edit')")
    @Log(title = "文章管理", businessType = BusinessType.UPDATE)
    @PutMapping("/audit")
    public AjaxResult audit(@RequestBody PortalArticle article) {
        return toAjax(cmsArticleService.auditArticle(article));
    }

    /**
     * 文章上下架
     */
    @Operation(summary = "文章上下架", description = "发布或下架文章")
    @PreAuthorize("@ss.hasPermi('cms:article:edit')")
    @Log(title = "文章管理", businessType = BusinessType.UPDATE)
    @PutMapping("/publish")
    public AjaxResult publish(@RequestBody PortalArticle article) {
        return toAjax(cmsArticleService.publishArticle(article));
    }

    /**
     * 设置推荐
     */
    @Operation(summary = "设置推荐", description = "设置文章是否推荐")
    @PreAuthorize("@ss.hasPermi('cms:article:edit')")
    @Log(title = "文章管理", businessType = BusinessType.UPDATE)
    @PutMapping("/featured")
    public AjaxResult featured(@RequestBody PortalArticle article) {
        return toAjax(cmsArticleService.setFeatured(article));
    }

    /**
     * 删除文章
     */
    @Operation(summary = "删除文章", description = "批量删除文章")
    @PreAuthorize("@ss.hasPermi('cms:article:remove')")
    @Log(title = "文章管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@Parameter(description = "文章ID数组") @PathVariable Long[] ids) {
        return toAjax(cmsArticleService.deleteArticleByIds(ids));
    }
}
