package com.moyun.portal.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.annotation.Log;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.TableDataInfo;
import com.moyun.common.enums.BusinessType;
import com.moyun.util.file.ExcelUtil;
import com.moyun.portal.domain.entity.PortalArticle;
import com.moyun.portal.domain.vo.ArticleVO;
import com.moyun.portal.mapper.PortalArticleMapper;
import com.moyun.portal.service.IPortalArticleService;
import com.moyun.portal.util.ArticleConvertUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "门户文章", description = "门户文章的增删改查操作接口")
@RestController
@RequestMapping("/portal/article")
public class PortalArticleController extends BaseController {

    @Autowired
    private IPortalArticleService portalArticleService;

    @Autowired
    private PortalArticleMapper portalArticleMapper;

    @Operation(summary = "获取文章列表", description = "根据条件分页查询文章列表")
    @GetMapping("/list")
    public TableDataInfo list(PortalArticle portalArticle) {
        startPage();
        List<PortalArticle> list = portalArticleService.selectPortalArticleList(portalArticle);
        return getDataTable(ArticleConvertUtil.toArticleVOList(list));
    }

    @Operation(summary = "导出文章", description = "导出文章数据到Excel文件")
    @Log(title = "门户文章", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, PortalArticle portalArticle) {
        List<PortalArticle> list = portalArticleService.selectPortalArticleList(portalArticle);
        ExcelUtil<PortalArticle> util = new ExcelUtil<PortalArticle>(PortalArticle.class);
        util.exportExcel(response, list, "门户文章数据");
    }

    @Operation(summary = "获取文章详情", description = "根据文章ID获取文章详细信息")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@Parameter(description = "文章ID") @PathVariable Long id) {
        PortalArticle article = portalArticleService.selectPortalArticleById(id);
        return success(ArticleConvertUtil.toArticleVO(article));
    }

    @Operation(summary = "新增文章", description = "创建新文章")
    @Log(title = "门户文章", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody PortalArticle portalArticle) {
        return toAjax(portalArticleService.insertPortalArticle(portalArticle));
    }

    @Operation(summary = "修改文章", description = "更新文章信息")
    @Log(title = "门户文章", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody PortalArticle portalArticle) {
        return toAjax(portalArticleService.updatePortalArticle(portalArticle));
    }

    @Operation(summary = "删除文章", description = "批量删除文章")
    @Log(title = "门户文章", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@Parameter(description = "文章ID数组") @PathVariable Long[] ids) {
        return toAjax(portalArticleService.deletePortalArticleByIds(ids));
    }

    @Operation(summary = "文章点赞", description = "给文章点赞")
    @PostMapping("/{id}/like")
    public AjaxResult likeArticle(@PathVariable Long id) {
        PortalArticle article = portalArticleService.selectPortalArticleById(id);
        if (article != null) {
            article.setLikes(article.getLikes() == null ? 1 : article.getLikes() + 1);
            portalArticleService.updatePortalArticle(article);
        }
        return success();
    }

    @Operation(summary = "取消点赞", description = "取消文章点赞")
    @DeleteMapping("/{id}/like")
    public AjaxResult unlikeArticle(@PathVariable Long id) {
        PortalArticle article = portalArticleService.selectPortalArticleById(id);
        if (article != null && article.getLikes() != null && article.getLikes() > 0) {
            article.setLikes(article.getLikes() - 1);
            portalArticleService.updatePortalArticle(article);
        }
        return success();
    }

    @Operation(summary = "增加浏览量", description = "增加文章浏览量")
    @PostMapping("/{id}/view")
    public AjaxResult incrementView(@PathVariable Long id) {
        PortalArticle article = portalArticleService.selectPortalArticleById(id);
        if (article != null) {
            article.setViews(article.getViews() == null ? 1 : article.getViews() + 1);
            portalArticleService.updatePortalArticle(article);
        }
        return success();
    }

    @Operation(summary = "获取热门文章", description = "获取热门文章列表，按浏览量排序")
    @GetMapping("/hot")
    public TableDataInfo getHotArticles(@RequestParam(defaultValue = "10") Integer limit) {
        Page<PortalArticle> page = new Page<>(1, limit);
        LambdaQueryWrapper<PortalArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PortalArticle::getStatus, "published")
               .orderByDesc(PortalArticle::getViews)
               .orderByDesc(PortalArticle::getCreateTime);
        Page<PortalArticle> resultPage = portalArticleMapper.selectPage(page, wrapper);
        return getDataTable(ArticleConvertUtil.toArticleVOList(resultPage.getRecords()), resultPage.getTotal());
    }

    @Operation(summary = "获取精选文章", description = "获取精选文章列表")
    @GetMapping("/featured")
    public TableDataInfo getFeaturedArticles(@RequestParam(defaultValue = "10") Integer limit) {
        Page<PortalArticle> page = new Page<>(1, limit);
        LambdaQueryWrapper<PortalArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PortalArticle::getStatus, "published")
               .eq(PortalArticle::getIsFeatured, true)
               .orderByDesc(PortalArticle::getIsTop)
               .orderByDesc(PortalArticle::getCreateTime);
        Page<PortalArticle> resultPage = portalArticleMapper.selectPage(page, wrapper);
        return getDataTable(ArticleConvertUtil.toArticleVOList(resultPage.getRecords()), resultPage.getTotal());
    }

    @Operation(summary = "获取轮播文章", description = "获取轮播文章列表")
    @GetMapping("/carousel")
    public AjaxResult getCarouselArticles() {
        LambdaQueryWrapper<PortalArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PortalArticle::getStatus, "published")
               .eq(PortalArticle::getIsCarousel, true)
               .orderByDesc(PortalArticle::getCreateTime);
        List<PortalArticle> list = portalArticleMapper.selectList(wrapper);
        return success(ArticleConvertUtil.toArticleVOList(list));
    }

    @Operation(summary = "获取相关文章", description = "获取相关推荐文章")
    @GetMapping("/{id}/related")
    public TableDataInfo getRelatedArticles(@PathVariable Long id, @RequestParam(defaultValue = "5") Integer limit) {
        Page<PortalArticle> page = new Page<>(1, limit);
        PortalArticle currentArticle = portalArticleService.selectPortalArticleById(id);
        LambdaQueryWrapper<PortalArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PortalArticle::getStatus, "published")
               .ne(PortalArticle::getId, id);
        if (currentArticle != null && currentArticle.getCategoryId() != null) {
            wrapper.eq(PortalArticle::getCategoryId, currentArticle.getCategoryId());
        }
        wrapper.orderByDesc(PortalArticle::getCreateTime);
        Page<PortalArticle> resultPage = portalArticleMapper.selectPage(page, wrapper);
        return getDataTable(ArticleConvertUtil.toArticleVOList(resultPage.getRecords()), resultPage.getTotal());
    }
}