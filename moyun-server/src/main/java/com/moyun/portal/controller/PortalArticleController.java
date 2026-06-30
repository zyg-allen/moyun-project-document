package com.moyun.portal.controller;

import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.moyun.common.annotation.Log;
import com.moyun.common.constant.HttpStatus;
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
import com.moyun.portal.service.IPortalGrowthService;
import com.moyun.portal.service.IPortalTagService;
import com.moyun.portal.util.ArticleConvertUtil;
import com.moyun.portal.util.PortalSecurityUtils;
import com.moyun.util.bean.PageUtils;

/**
 * 门户文章 Controller
 *
 * 清理说明（仅删 Controller 方法，保留 Service/Mapper/XML）：
 * 以下 13 个接口经前端调用链核对确认已无任何调用方，属于死接口，已删除：
 *   - export                 前端未调用文章导出
 *   - getInfoBySlug          SEO slug 接口，前端未调用 getArticleDetailBySlug
 *   - add                    前端使用 publish / saveDraft 而非 add
 *   - checkLikeStatus        前端 checkLikeStatus 函数仅定义未调用
 *   - checkViewsConsistency  阅读量一致性校验，无前端调用
 *   - repairArticleViews     单篇阅读量修复，无前端调用
 *   - repairAllArticleViews  批量阅读量修复，无前端调用
 *   - getRealViews           真实阅读量查询，无前端调用
 *   - getHotArticles         前端 getHotArticles 仅定义未调用，home 已聚合
 *   - getFeaturedArticles    同上，home 已聚合
 *   - getCarouselArticles    同上，home 已聚合
 *   - getRelatedArticles     前端 getRelatedArticles 仅定义未调用
 *   - getArticlesByCategory  前端 mockData 有同名 mock 函数，与 API 无关
 *
 * 保留的 11 个在用接口：list / myArticles / getInfo / publish / saveDraft /
 * edit / remove / toggleLikeArticle / incrementView / getCategoryRecommendedArticles / getHomeData
 *
 * 依赖保留说明：
 *   - portalArticleMapper      : toggleLikeArticle(incrementLikes) / incrementView(incrementViews) 仍使用
 *   - portalLikeMapper         : toggleLikeArticle(selectOne/insert/deleteById) 仍使用
 *   - portalArticleViewMapper  : incrementView(countRecentViews/countRecentViewsByIp) 仍使用
 *   - portalGrowthService      : toggleLikeArticle(recordEventWithTarget) 仍使用
 *   - portalTagService         : publish/saveDraft/edit/remove(bindTags/unbindTags) 仍使用
 *   - IPortalArticleViewService 已无调用方，注入与 import 一并移除
 */
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
    private IPortalGrowthService portalGrowthService;

    @Autowired
    private IPortalTagService portalTagService;

    @Operation(summary = "获取文章列表", description = "根据条件分页查询文章列表")
    @GetMapping("/list")
    public AjaxResult list(ArticleQuery query) {
        Page<PortalArticle> page = PageUtils.buildPage(query);
        Page<PortalArticle> resultPage = portalArticleService.selectPortalArticlePage(page, query);
        return success(resultPage);
    }

    /**
     * 我的文章列表（前台，按 authorId + status 查询）
     * 用于作者查看自己所有状态的文章（草稿/待审核/已发布/已拒绝）
     * authorId 自动从当前登录用户获取，前端无需传
     * status 可选值：draft / pending / published / rejected / archived
     */
    @Operation(summary = "我的文章列表", description = "查询当前登录用户的所有文章，支持按状态筛选")
    @GetMapping("/my")
    public AjaxResult myArticles(ArticleQuery query) {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        // 强制 authorId 为当前登录用户，防止越权查询他人文章
        query.setAuthorId(userId);
        Page<PortalArticle> page = PageUtils.buildPage(query);
        Page<PortalArticle> resultPage = portalArticleService.selectMyArticlesPage(page, query);
        return success(resultPage);
    }

    @Operation(summary = "获取文章详情", description = "根据文章ID获取文章详细信息")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@Parameter(description = "文章ID") @PathVariable Long id) {
        PortalArticle article = portalArticleService.selectPortalArticleById(id);
        return success(ArticleConvertUtil.toArticleVO(article));
    }

    /**
     * 前台发布文章
     * 发布后状态为 pending（待审核），需后台人工审核通过后才变更为 published
     */
    @Operation(summary = "前台发布文章", description = "用户在前台发布新文章，发布后进入待审核状态")
    @Log(title = "门户文章", businessType = BusinessType.INSERT)
    @PostMapping("/publish")
    public AjaxResult publish(@Validated @RequestBody ArticlePublishDTO publishDTO) {
        // 将 DTO 转换为实体
        PortalArticle article = new PortalArticle();
        article.setTitle(publishDTO.getTitle());
        article.setContent(publishDTO.getContent());
        // 同步编辑器模式（richtext / markdown），切换编辑器时持久化该字段
        article.setEditorMode(publishDTO.getEditorMode());
        // 同步 Markdown 原始内容（便于后续编辑回显）
        article.setContentMarkdown(publishDTO.getContentMarkdown());
        // 用户自定义 slug（为空时由 Service 自动生成，确保 SEO 语义化路径非空）
        article.setSlug(publishDTO.getSlug());
        article.setExcerpt(publishDTO.getExcerpt());
        article.setCover(publishDTO.getCover());
        article.setCategoryId(publishDTO.getCategoryId());
        article.setLink(publishDTO.getLink());
        article.setIsFeatured(publishDTO.getIsFeatured());
        article.setIsTop(publishDTO.getIsTop());
        article.setIsCarousel(publishDTO.getIsCarousel());

        // 调用 Service 发布（默认状态 pending 待审核）
        int result = portalArticleService.publishArticle(article);
        // 发布成功后绑定标签（同步维护 portal_entity_tag + portal_tag.reference_count）
        if (result > 0 && article.getId() != null) {
            portalTagService.bindTags("article", article.getId(),
                    publishDTO.getTagIds(), publishDTO.getTagNames(), "article");
        }
        // 返回文章详情（含 id、status、slug、createTime 等）
        Map<String, Object> data = new HashMap<>();
        data.put("id", article.getId());
        data.put("status", article.getStatus());
        data.put("slug", article.getSlug());
        data.put("publishedAt", article.getPublishedAt());
        data.put("message", "发布成功，等待审核");
        return success(data);
    }

    /**
     * 前台保存草稿（真实入库，返回草稿详情含 id）
     * 新建草稿返回 id，更新草稿保持原 id
     */
    @Operation(summary = "前台保存草稿", description = "保存文章草稿，真实入库并返回草稿详情（含id）")
    @Log(title = "门户文章-草稿", businessType = BusinessType.INSERT)
    @PostMapping("/draft")
    public AjaxResult saveDraft(@RequestBody PortalArticle portalArticle) {
        PortalArticle saved = portalArticleService.saveDraft(portalArticle);
        // 草稿保存成功后同步绑定标签（草稿阶段也允许绑定，便于后续发布）
        if (saved == null || saved.getId() == null) {
            return AjaxResult.error("草稿保存失败");
        }
        portalTagService.bindTags("article", saved.getId(),
                portalArticle.getTagIds(), portalArticle.getTagNames(), "article");
        return success(saved);
    }

    @Operation(summary = "修改文章", description = "更新文章信息")
    @Log(title = "门户文章", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody PortalArticle portalArticle) {
        int result = portalArticleService.updatePortalArticle(portalArticle);
        // 修改成功后同步更新标签绑定（bindTags 内部会计算差集，只增减变化的 tag）
        if (result > 0 && portalArticle.getId() != null) {
            portalTagService.bindTags("article", portalArticle.getId(),
                    portalArticle.getTagIds(), portalArticle.getTagNames(), "article");
        }
        return toAjax(result);
    }

    @Operation(summary = "删除文章", description = "批量删除文章")
    @Log(title = "门户文章", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@Parameter(description = "文章ID数组") @PathVariable Long[] ids) {
        int result = portalArticleService.deletePortalArticleByIds(ids);
        // 删除成功后解绑标签（同步减少 reference_count）
        if (result > 0) {
            for (Long id : ids) {
                portalTagService.unbindTags("article", id);
            }
        }
        return toAjax(result);
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
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "请先登录");
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
            try {
                portalLikeMapper.insert(like);
            } catch (DuplicateKeyException e) {
                // 并发场景：另一事务已先插入（uk_user_article 唯一索引触发）
                // 按"已赞→取消"语义处理，保证幂等
                PortalLike conflict = portalLikeMapper.selectOne(wrapper);
                if (conflict != null) {
                    portalLikeMapper.deleteById(conflict.getId());
                }
                if (article.getLikes() != null && article.getLikes() > 0) {
                    portalArticleMapper.incrementLikes(id, -1);
                }
                Map<String, Object> r = new HashMap<>();
                r.put("isLiked", false);
                r.put("likeCount", article.getLikes() == null || article.getLikes() <= 0 ? 0 : article.getLikes() - 1);
                return success(r);
            }

            // 原子增加点赞数
            portalArticleMapper.incrementLikes(id, 1);

            // 为文章作者记录获赞成长事件
            if (article.getAuthorId() != null && !article.getAuthorId().equals(userId)) {
                portalGrowthService.recordEventWithTarget("article", "receive_like",
                        article.getAuthorId(), userId, "article", id);
            }

            isLiked = true;
            newLikeCount = (article.getLikes() == null ? 0 : article.getLikes()) + 1;
        } else {
            // 已点赞，取消点赞
            portalLikeMapper.deleteById(existingLike.getId());

            // 原子减少点赞数
            if (article.getLikes() != null && article.getLikes() > 0) {
                portalArticleMapper.incrementLikes(id, -1);
            }

            isLiked = false;
            newLikeCount = article.getLikes() == null || article.getLikes() <= 0 ? 0 : article.getLikes() - 1;
        }

        // 返回最新状态
        Map<String, Object> result = new HashMap<>();
        result.put("isLiked", isLiked);
        result.put("likeCount", newLikeCount);
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
        long currentViews = article.getViews() == null ? 0 : article.getViews();
        if (recentViews == 0) {
            // 有效时间内未浏览过，原子增加阅读量
            portalArticleMapper.incrementViews(id, 1);
            increased = true;
            currentViews = currentViews + 1;

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
        result.put("views", currentViews);
        result.put("message", increased ? "阅读量+1" : "阅读量未变化");

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
