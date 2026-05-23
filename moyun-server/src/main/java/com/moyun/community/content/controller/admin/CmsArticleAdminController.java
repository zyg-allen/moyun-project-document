package com.moyun.community.content.controller.admin;

import com.moyun.common.annotation.Log;
import com.moyun.common.core.controller.BaseController;
import com.moyun.common.core.domain.AjaxResult;
import com.moyun.common.core.domain.TableDataInfo;
import com.moyun.common.enums.BusinessType;
import com.moyun.common.utils.poi.ExcelUtil;
import com.moyun.community.content.entity.CmsArticle;
import com.moyun.community.content.service.ICmsArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
@RequestMapping("/community/article")
public class CmsArticleAdminController extends BaseController {

    @Autowired
    private ICmsArticleService articleService;

    @PreAuthorize("@ss.hasPermi('content:article:list')")
    @GetMapping("/list")
    public TableDataInfo list(CmsArticle article) {
        startPage();
        List<CmsArticle> list = articleService.selectArticleList(article);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('content:article:export')")
    @Log(title = "文章管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CmsArticle article) {
        List<CmsArticle> list = articleService.selectArticleList(article);
        ExcelUtil<CmsArticle> util = new ExcelUtil<CmsArticle>(CmsArticle.class);
        util.exportExcel(response, list, "文章数据");
    }

    @PreAuthorize("@ss.hasPermi('content:article:query')")
    @GetMapping(value = "/{articleId}")
    public AjaxResult getInfo(@PathVariable("articleId") Long articleId) {
        return success(articleService.selectArticleById(articleId));
    }

    @PreAuthorize("@ss.hasPermi('content:article:add')")
    @Log(title = "文章管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody CmsArticle article) {
        return toAjax(articleService.save(article));
    }

    @PreAuthorize("@ss.hasPermi('content:article:edit')")
    @Log(title = "文章管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody CmsArticle article) {
        return toAjax(articleService.updateById(article));
    }

    @PreAuthorize("@ss.hasPermi('content:article:offline')")
    @Log(title = "文章管理", businessType = BusinessType.UPDATE)
    @PutMapping("/offline/{articleId}")
    public AjaxResult offline(@PathVariable("articleId") Long articleId) {
        return toAjax(articleService.updateArticleStatus(articleId, "offline"));
    }

    @PreAuthorize("@ss.hasPermi('content:article:remove')")
    @Log(title = "文章管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{articleIds}")
    public AjaxResult remove(@PathVariable Long[] articleIds) {
        return toAjax(articleService.removeByIds(List.of(articleIds)));
    }
}