package com.moyun.portal.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.annotation.Log;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.AjaxResult;
import com.moyun.common.enums.BusinessType;
import com.moyun.portal.domain.query.ArticleQuery;
import com.moyun.util.bean.PageUtils;
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
    public AjaxResult list(ArticleQuery query) {
        Page<PortalArticle> page = PageUtils.buildPage(query);
        Page<PortalArticle> resultPage = portalArticleService.selectPortalArticlePage(page, query);
        return success(resultPage);
    }

    @Operation(summary = "导出文章", description = "导出文章数据到Excel文件")
    @Log(title = "门户文章", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ArticleQuery query) {
        // 导出时不分页，调用不分页的查询方法
        List<PortalArticle> list = portalArticleService.selectPortalArticleList(query);
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
    public AjaxResult getHotArticles(@RequestParam(defaultValue = "10") Integer limit) {
        Page<PortalArticle> page = PageUtils.buildPage(1, limit);
        List<PortalArticle> list = portalArticleMapper.selectHotArticles(page);
        return success(list);
    }

    @Operation(summary = "获取精选文章", description = "获取精选文章列表")
    @GetMapping("/featured")
    public AjaxResult getFeaturedArticles(@RequestParam(defaultValue = "10") Integer limit) {
        Page<PortalArticle> page = PageUtils.buildPage(1, limit);
        List<PortalArticle> list = portalArticleMapper.selectFeaturedArticles(page);
        return success(list);
    }

    @Operation(summary = "获取轮播文章", description = "获取轮播文章列表")
    @GetMapping("/carousel")
    public AjaxResult getCarouselArticles() {
        List<PortalArticle> list = portalArticleMapper.selectCarouselArticles();
        return success(list);
    }

    @Operation(summary = "获取相关文章", description = "获取相关推荐文章")
    @GetMapping("/{id}/related")
    public AjaxResult getRelatedArticles(@PathVariable Long id, @RequestParam(defaultValue = "5") Integer limit) {
        Page<PortalArticle> page = PageUtils.buildPage(1, limit);
        List<PortalArticle> list = portalArticleMapper.selectRelatedArticles(page, id);
        return success(list);
    }

    @Operation(summary = "按分类获取文章列表", description = "根据分类ID或名称获取文章列表")
    @GetMapping("/byCategory")
    public AjaxResult getArticlesByCategory(ArticleQuery query) {
        Page<PortalArticle> page = PageUtils.buildPage(query);
        Page<PortalArticle> resultPage = portalArticleService.selectPortalArticlePage(page, query);
        return success(resultPage);
    }

    @Operation(summary = "获取首页数据汇总", description = "获取首页所需的全部数据")
    @GetMapping("/home")
    public AjaxResult getHomeData() {
        // 获取轮播文章
        List<PortalArticle> carouselArticles = portalArticleMapper.selectCarouselArticles();

        // 获取精选文章（最多8篇）
        Page<PortalArticle> featuredPage = PageUtils.buildPage(1, 8);
        List<PortalArticle> featuredArticles = portalArticleMapper.selectFeaturedArticles(featuredPage);

        // 获取热门文章（最多10篇）
        Page<PortalArticle> hotPage = PageUtils.buildPage(1, 10);
        List<PortalArticle> hotArticles = portalArticleMapper.selectHotArticles(hotPage);

        // 获取最新文章（最多20篇，用于按主题探索）
        Page<PortalArticle> latestPage = PageUtils.buildPage(1, 20);
        List<PortalArticle> latestArticles = portalArticleMapper.selectLatestArticles(latestPage);

        return success(new HomeDataVO(
            ArticleConvertUtil.toArticleVOList(carouselArticles),
            ArticleConvertUtil.toArticleVOList(featuredArticles),
            ArticleConvertUtil.toArticleVOList(hotArticles),
            ArticleConvertUtil.toArticleVOList(latestArticles)
        ));
    }

    /**
     * 首页数据VO
     */
    public static class HomeDataVO {
        private List<ArticleVO> carouselArticles;
        private List<ArticleVO> featuredArticles;
        private List<ArticleVO> hotArticles;
        private List<ArticleVO> latestArticles;

        public HomeDataVO(List<ArticleVO> carouselArticles, List<ArticleVO> featuredArticles,
                         List<ArticleVO> hotArticles, List<ArticleVO> latestArticles) {
            this.carouselArticles = carouselArticles;
            this.featuredArticles = featuredArticles;
            this.hotArticles = hotArticles;
            this.latestArticles = latestArticles;
        }

        public List<ArticleVO> getCarouselArticles() { return carouselArticles; }
        public List<ArticleVO> getFeaturedArticles() { return featuredArticles; }
        public List<ArticleVO> getHotArticles() { return latestArticles; }
        public List<ArticleVO> getLatestArticles() { return latestArticles; }
    }
}
