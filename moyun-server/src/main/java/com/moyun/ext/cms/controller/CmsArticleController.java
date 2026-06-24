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
import com.moyun.portal.service.IPortalTagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

/**
 * CMS文章管理Controller
 *
 * @author moyun
 */
@Tag(name = "CMS文章管理", description = "CMS文章管理接口")
@RestController
@RequestMapping("/cms/article")
@Validated
public class CmsArticleController extends BaseController {

    @Autowired
    private ICmsArticleService cmsArticleService;

    @Autowired
    private IPortalTagService portalTagService;

    /**
     * 获取文章列表（分页）
     */
    @Operation(summary = "获取文章列表", description = "根据条件分页查询文章列表")
    @PreAuthorize("@ss.hasPermi('cms:article:list')")
    @GetMapping("/list")
    public AjaxResult list(CmsArticleQuery query) {
        // 构造分页对象（泛型为 VO）
        Page<CmsArticleVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        // 执行查询
        Page<CmsArticleVO> result = cmsArticleService.selectArticlePage(page, query);
        return success(result);
    }

    /**
     * 根据文章编号获取详细信息
     */
    @Operation(summary = "获取文章详情", description = "根据文章ID获取文章详细信息")
    @PreAuthorize("@ss.hasPermi('cms:article:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@Parameter(description = "文章ID", required = true)
                              @PathVariable @NotNull(message = "文章ID不能为空") Long id) {
        CmsArticleVO vo = cmsArticleService.selectArticleById(id);
        return success(vo);
    }

    /**
     * 新增文章
     */
    @Operation(summary = "新增文章", description = "创建新文章")
    @PreAuthorize("@ss.hasPermi('cms:article:add')")
    @Log(title = "文章管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Valid @RequestBody PortalArticle article) {
        int result = cmsArticleService.insertArticle(article);
        // 新增成功后绑定标签
        if (result > 0 && article.getId() != null) {
            portalTagService.bindTags("article", article.getId(),
                    article.getTagIds(), article.getTagNames(), "article");
        }
        return toAjax(result);
    }

    /**
     * 修改文章
     */
    @Operation(summary = "修改文章", description = "更新文章信息")
    @PreAuthorize("@ss.hasPermi('cms:article:edit')")
    @Log(title = "文章管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Valid @RequestBody PortalArticle article) {
        int result = cmsArticleService.updateArticle(article);
        // 修改成功后同步更新标签绑定
        if (result > 0 && article.getId() != null) {
            portalTagService.bindTags("article", article.getId(),
                    article.getTagIds(), article.getTagNames(), "article");
        }
        return toAjax(result);
    }

    /**
     * 审核文章
     */
    @Operation(summary = "审核文章", description = "审核文章内容")
    @PreAuthorize("@ss.hasPermi('cms:article:edit')")
    @Log(title = "文章管理", businessType = BusinessType.UPDATE)
    @PutMapping("/audit")
    public AjaxResult audit(@RequestBody PortalArticle article) {
        int result = cmsArticleService.auditArticle(article);
        return toAjax(result);
    }

    /**
     * 文章上下架
     */
    @Operation(summary = "文章上下架", description = "发布或下架文章")
    @PreAuthorize("@ss.hasPermi('cms:article:edit')")
    @Log(title = "文章管理", businessType = BusinessType.UPDATE)
    @PutMapping("/publish")
    public AjaxResult publish(@RequestBody PortalArticle article) {
        int result = cmsArticleService.publishArticle(article);
        return toAjax(result);
    }

    /**
     * 设置推荐
     */
    @Operation(summary = "设置推荐", description = "设置文章是否推荐")
    @PreAuthorize("@ss.hasPermi('cms:article:edit')")
    @Log(title = "文章管理", businessType = BusinessType.UPDATE)
    @PutMapping("/featured")
    public AjaxResult featured(@RequestBody PortalArticle article) {
        int result = cmsArticleService.setFeatured(article);
        return toAjax(result);
    }

    /**
     * 设置置顶
     */
    @Operation(summary = "设置置顶", description = "设置文章是否置顶")
    @PreAuthorize("@ss.hasPermi('cms:article:edit')")
    @Log(title = "文章管理", businessType = BusinessType.UPDATE)
    @PutMapping("/top")
    public AjaxResult top(@RequestBody PortalArticle article) {
        int result = cmsArticleService.setTop(article);
        return toAjax(result);
    }

    /**
     * 设置轮播
     */
    @Operation(summary = "设置轮播", description = "设置文章是否轮播")
    @PreAuthorize("@ss.hasPermi('cms:article:edit')")
    @Log(title = "文章管理", businessType = BusinessType.UPDATE)
    @PutMapping("/carousel")
    public AjaxResult carousel(@RequestBody PortalArticle article) {
        int result = cmsArticleService.setCarousel(article);
        return toAjax(result);
    }

    /**
     * 批量删除文章（通过请求体传递 ID 数组）
     */
    @Operation(summary = "批量删除文章", description = "通过 ID 数组批量删除文章")
    @PreAuthorize("@ss.hasPermi('cms:article:remove')")
    @Log(title = "文章管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/batch")
    public AjaxResult removeBatch(@RequestBody Long[] ids) {
        int result = cmsArticleService.deleteArticleByIds(ids);
        // 删除成功后解绑标签
        if (result > 0) {
            for (Long id : ids) {
                portalTagService.unbindTags("article", id);
            }
        }
        return toAjax(result);
    }

    /**
     * 删除文章（单个或批量，路径用逗号分隔）
     * 例如：/1 或 /1,2,3
     */
    @Operation(summary = "删除文章", description = "根据 ID 删除文章，支持逗号分隔多个")
    @PreAuthorize("@ss.hasPermi('cms:article:remove')")
    @Log(title = "文章管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@Parameter(description = "文章ID，多个用逗号分隔", required = true)
                             @PathVariable @NotNull(message = "文章ID不能为空") String ids) {
        // 手动解析 ID 字符串
        Long[] idArray = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .toArray(Long[]::new);
        int result = cmsArticleService.deleteArticleByIds(idArray);
        // 删除成功后解绑标签
        if (result > 0) {
            for (Long id : idArray) {
                portalTagService.unbindTags("article", id);
            }
        }
        return toAjax(result);
    }
}