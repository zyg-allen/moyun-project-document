package com.moyun.portal.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.portal.domain.dto.ArticlePublishDTO;
import com.moyun.portal.domain.entity.PortalArticle;
import com.moyun.portal.domain.entity.PortalArticleView;
import com.moyun.portal.domain.entity.PortalLike;
import com.moyun.portal.domain.query.ArticleQuery;
import com.moyun.portal.domain.vo.ArticleVO;
import com.moyun.portal.mapper.PortalArticleMapper;
import com.moyun.portal.mapper.PortalArticleViewMapper;
import com.moyun.portal.mapper.PortalLikeMapper;
import com.moyun.portal.service.IPortalArticleService;
import com.moyun.portal.service.IPortalArticleViewService;
import com.moyun.portal.util.ArticleConvertUtil;
import com.moyun.portal.util.PortalSecurityUtils;
import com.moyun.util.bean.PageUtils;
import com.moyun.util.file.ExcelUtil;

@Tag(name = "门户文章", description = "门户文章的增删改查操作接口")
@RestController
@RequestMapping("/portal/article")
public class PortalArticleController extends BaseController {

    @Autowired
    private IPortalArticleService portalArticleService;

    @Autowired
    private PortalArticleMapper portalArticleMapper;

    @Autowired
    private PortalLikeMapper portalLikeMapper;

    @Autowired
    private PortalArticleViewMapper portalArticleViewMapper;

    @Autowired
    private IPortalArticleViewService portalArticleViewService;

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

    @Operation(summary = "根据别名获取文章详情", description = "根据文章别名(slug)获取文章详细信息，用于SEO语义化URL")
    @GetMapping(value = "/slug/{slug}")
    public AjaxResult getInfoBySlug(@Parameter(description = "文章别名") @PathVariable String slug) {
        PortalArticle article = portalArticleMapper.selectPortalArticleBySlug(slug);
        if (article == null) {
            return error("文章不存在");
        }
        return success(ArticleConvertUtil.toArticleVO(article));
    }

    @Operation(summary = "新增文章", description = "创建新文章")
    @Log(title = "门户文章", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody PortalArticle portalArticle) {
        return toAjax(portalArticleService.insertPortalArticle(portalArticle));
    }

    /**
     * 前台发布文章
     */
    @Operation(summary = "前台发布文章", description = "用户在前台发布新文章，自动获取作者信息")
    @Log(title = "门户文章", businessType = BusinessType.INSERT)
    @PostMapping("/publish")
    public AjaxResult publish(@Validated @RequestBody ArticlePublishDTO publishDTO) {
        // 将 DTO 转换为实体
        PortalArticle article = new PortalArticle();
        article.setTitle(publishDTO.getTitle());
        article.setContent(publishDTO.getContent());
        article.setExcerpt(publishDTO.getExcerpt());
        article.setCover(publishDTO.getCover());
        article.setCategoryId(publishDTO.getCategoryId());
        article.setIsFeatured(publishDTO.getIsFeatured());
        article.setIsTop(publishDTO.getIsTop());
        article.setIsCarousel(publishDTO.getIsCarousel());

        // 调用 Service 发布
        int result = portalArticleService.publishArticle(article);
        return toAjax(result);
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

    /**
     * 文章点赞 - 行业标准实现
     * 1. 检查是否已点赞
     * 2. 如果未点赞，则点赞并增加计数
     * 3. 如果已点赞，则取消点赞并减少计数
     * 4. 返回最新点赞数和点赞状态
     */
    @Operation(summary = "文章点赞/取消点赞", description = "点赞或取消点赞文章，返回最新点赞数")
    @PostMapping("/{id}/like")
    @Transactional
    public AjaxResult toggleLikeArticle(@PathVariable Long id) {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return error("请先登录");
        }

        // 检查文章是否存在
        PortalArticle article = portalArticleService.selectPortalArticleById(id);
        if (article == null) {
            return error("文章不存在");
        }

        // 查询是否已点赞
        LambdaQueryWrapper<PortalLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PortalLike::getUserId, userId)
                .eq(PortalLike::getArticleId, id);
        PortalLike existingLike = portalLikeMapper.selectOne(wrapper);

        boolean isLiked;
        long newLikeCount;

        if (existingLike == null) {
            // 未点赞，执行点赞
            PortalLike like = new PortalLike();
            like.setUserId(userId);
            like.setArticleId(id);
            like.setCreateTime(LocalDateTime.now());
            portalLikeMapper.insert(like);

            // 增加点赞数
            article.setLikes(article.getLikes() == null ? 1 : article.getLikes() + 1);
            portalArticleService.updatePortalArticle(article);

            isLiked = true;
            newLikeCount = article.getLikes();
        } else {
            // 已点赞，取消点赞
            portalLikeMapper.deleteById(existingLike.getId());

            // 减少点赞数
            if (article.getLikes() != null && article.getLikes() > 0) {
                article.setLikes(article.getLikes() - 1);
                portalArticleService.updatePortalArticle(article);
            }

            isLiked = false;
            newLikeCount = article.getLikes() == null ? 0 : article.getLikes();
        }

        // 返回最新状态
        Map<String, Object> result = new HashMap<>();
        result.put("isLiked", isLiked);
        result.put("likeCount", newLikeCount);
        return success(result);
    }

    /**
     * 检查是否已点赞
     */
    @Operation(summary = "检查点赞状态", description = "检查当前用户是否已点赞该文章")
    @GetMapping("/{id}/like/status")
    public AjaxResult checkLikeStatus(@PathVariable Long id) {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("isLiked", false);
            return success(result);
        }

        LambdaQueryWrapper<PortalLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PortalLike::getUserId, userId)
                .eq(PortalLike::getArticleId, id);
        long count = portalLikeMapper.selectCount(wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("isLiked", count > 0);
        return success(result);
    }

    /**
     * 增加浏览量 - 行业标准防刷逻辑
     * <p>
     * 防刷策略：
     * 1. 登录用户：同一用户对同一文章，5分钟内重复浏览不增加阅读量
     * 2. 游客用户：同一IP对同一文章，5分钟内重复浏览不增加阅读量
     * 3. 记录每次浏览历史，用于数据分析
     */
    @Operation(summary = "增加浏览量", description = "增加文章浏览量，支持防刷逻辑")
    @PostMapping("/{id}/view")
    @Transactional
    public AjaxResult incrementView(@PathVariable Long id, HttpServletRequest request) {
        PortalArticle article = portalArticleService.selectPortalArticleById(id);
        if (article == null) {
            return error("文章不存在");
        }

        // 获取用户ID和IP地址
        Long userId = PortalSecurityUtils.getUserId();
        String ip = getClientIp(request);

        // 防刷时间窗口：5分钟（300秒）
        LocalDateTime startTime = LocalDateTime.now().minusMinutes(5);

        // 检查是否在有效时间内已经浏览过
        int recentViews = 0;
        if (userId != null) {
            // 登录用户：按用户ID检查
            recentViews = portalArticleViewMapper.countRecentViews(id, userId, ip, startTime);
        } else {
            // 游客：按IP检查
            recentViews = portalArticleViewMapper.countRecentViewsByIp(id, ip, startTime);
        }

        boolean increased = false;
        if (recentViews == 0) {
            // 有效时间内未浏览过，增加阅读量
            article.setViews(article.getViews() == null ? 1 : article.getViews() + 1);
            portalArticleService.updatePortalArticle(article);
            increased = true;

            // 记录浏览历史
            PortalArticleView view = new PortalArticleView();
            view.setArticleId(id);
            view.setUserId(userId);
            view.setIp(ip);
            view.setViewTime(LocalDateTime.now());
            view.setUserAgent(request.getHeader("User-Agent"));
            portalArticleViewMapper.insert(view);
        }

        // 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("increased", increased);
        result.put("views", article.getViews());
        result.put("message", increased ? "阅读量+1" : "阅读量未变化");

        return success(result);
    }

    // ==================== 数据管理接口 - 以下是管理后台接口 ====================

    /**
     * 校验文章阅读量一致性
     */
    @Operation(summary = "校验阅读量一致性", description = "检查 portal_article.views 与 portal_article_view 是否一致")
    @GetMapping("/{id}/view/check")
    public AjaxResult checkViewsConsistency(@PathVariable Long id) {
        boolean consistent = portalArticleViewService.checkArticleViewsConsistency(id);
        long dbViews = 0;
        long realViews = 0;

        PortalArticle article = portalArticleService.selectPortalArticleById(id);
        if (article != null) {
            dbViews = article.getViews() == null ? 0 : article.getViews();
        }
        realViews = portalArticleViewService.getRealArticleViews(id);

        Map<String, Object> result = new HashMap<>();
        result.put("consistent", consistent);
        result.put("dbViews", dbViews);
        result.put("realViews", realViews);
        result.put("difference", Math.abs(dbViews - realViews));

        return success(result);
    }

    /**
     * 修复单篇文章阅读量
     */
    @Operation(summary = "修复文章阅读量", description = "从 portal_article_view 重新统计并更新文章阅读量")
    @Log(title = "修复文章阅读量", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/view/repair")
    @Transactional
    public AjaxResult repairArticleViews(@PathVariable Long id) {
        long newViews = portalArticleViewService.repairArticleViews(id);

        Map<String, Object> result = new HashMap<>();
        result.put("articleId", id);
        result.put("newViews", newViews);
        result.put("message", "修复成功");

        return success(result);
    }

    /**
     * 修复所有文章阅读量（批量）
     */
    @Operation(summary = "批量修复所有文章阅读量", description = "从 portal_article_view 重新统计所有文章的阅读量")
    @Log(title = "批量修复文章阅读量", businessType = BusinessType.UPDATE)
    @PostMapping("/view/repair/all")
    @Transactional
    public AjaxResult repairAllArticleViews() {
        long repairedCount = portalArticleViewService.repairAllArticleViews();

        Map<String, Object> result = new HashMap<>();
        result.put("repairedCount", repairedCount);
        result.put("message", "批量修复成功");

        return success(result);
    }

    /**
     * 获取文章真实阅读量（从 portal_article_view 表统计）
     */
    @Operation(summary = "获取文章真实阅读量", description = "从 portal_article_view 表统计真实阅读量（去重）")
    @GetMapping("/{id}/view/real")
    public AjaxResult getRealViews(@PathVariable Long id) {
        int realViews = portalArticleViewService.getRealArticleViews(id);

        Map<String, Object> result = new HashMap<>();
        result.put("articleId", id);
        result.put("realViews", realViews);

        return success(result);
    }

    // ==================== 私有方法 ====================

    /**
     * 获取客户端真实IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 如果是多个IP（经过代理），取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
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

    @Operation(summary = "获取分类推荐文章", description = "获取指定分类的推荐文章列表")
    @GetMapping("/categoryRecommended")
    public AjaxResult getCategoryRecommendedArticles(
            @RequestParam(required = false) String categoryName,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "8") Integer limit) {
        ArticleQuery query = new ArticleQuery();
        query.setCategoryName(categoryName);
        query.setCategoryId(categoryId);
        query.setIsCategoryRecommended(true);
        query.setPageNum(1);
        query.setPageSize(limit);

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
            this.hotArticles = latestArticles;
            this.latestArticles = latestArticles;
        }

        public List<ArticleVO> getCarouselArticles() {
            return carouselArticles;
        }

        public List<ArticleVO> getFeaturedArticles() {
            return featuredArticles;
        }

        public List<ArticleVO> getHotArticles() {
            return hotArticles;
        }

        public List<ArticleVO> getLatestArticles() {
            return latestArticles;
        }
    }
}
