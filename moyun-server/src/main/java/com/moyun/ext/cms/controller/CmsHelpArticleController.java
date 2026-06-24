package com.moyun.ext.cms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.portal.domain.entity.PortalHelpArticle;
import com.moyun.ext.cms.service.ICmsHelpArticleService;
import com.moyun.util.bean.PageUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * CMS 帮助文章管理 Controller
 *
 * @author moyun
 */
@Tag(name = "CMS帮助文章管理", description = "帮助中心文章的增删改查接口")
@RestController
@RequestMapping("/cms/help-article")
public class CmsHelpArticleController extends BaseController {

    @Autowired
    private ICmsHelpArticleService cmsHelpArticleService;

    @Operation(summary = "查询帮助文章列表", description = "分页查询帮助文章")
    @PreAuthorize("@ss.hasPermi('cms:help-article:list')")
    @GetMapping("/list")
    public AjaxResult list(PortalHelpArticle article) {
        Page<PortalHelpArticle> page = PageUtils.startPage();
        cmsHelpArticleService.selectHelpArticlePage(page, article);
        return success(page);
    }

    @Operation(summary = "获取文章详情", description = "根据ID获取帮助文章详情")
    @PreAuthorize("@ss.hasPermi('cms:help-article:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(cmsHelpArticleService.selectHelpArticleById(id));
    }

    @Operation(summary = "新增文章", description = "新增帮助文章")
    @PreAuthorize("@ss.hasPermi('cms:help-article:add')")
    @Log(title = "帮助文章", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody PortalHelpArticle article) {
        article.setCreateBy(getUsername());
        return toAjax(cmsHelpArticleService.insertHelpArticle(article));
    }

    @Operation(summary = "修改文章", description = "修改帮助文章")
    @PreAuthorize("@ss.hasPermi('cms:help-article:edit')")
    @Log(title = "帮助文章", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody PortalHelpArticle article) {
        article.setUpdateBy(getUsername());
        return toAjax(cmsHelpArticleService.updateHelpArticle(article));
    }

    @Operation(summary = "删除文章", description = "批量删除帮助文章")
    @PreAuthorize("@ss.hasPermi('cms:help-article:remove')")
    @Log(title = "帮助文章", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(cmsHelpArticleService.deleteHelpArticleByIds(ids));
    }
}
