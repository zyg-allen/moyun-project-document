package com.moyun.community.content.controller.app;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.utils.SecurityUtils;
import com.moyun.community.common.domain.ApiResponse;
import com.moyun.community.common.domain.PageResult;
import com.moyun.community.content.entity.CmsArticle;
import com.moyun.community.content.entity.CmsCategory;
import com.moyun.community.content.entity.CmsComment;
import com.moyun.community.content.entity.CmsTag;
import com.moyun.community.content.entity.UserProfile;
import com.moyun.community.content.mapper.CmsArticleMapper;
import com.moyun.community.content.mapper.CmsCategoryMapper;
import com.moyun.community.content.mapper.CmsCommentMapper;
import com.moyun.community.content.mapper.CmsTagMapper;
import com.moyun.community.content.mapper.UserProfileMapper;
import com.moyun.community.content.model.vo.PortalArticleVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 前台文章Controller
 *
 * @author moyun
 */
@Tag(name = "文章模块", description = "文章列表、详情、发布等接口")
@RestController
@RequestMapping("/api/article")
@RequiredArgsConstructor
public class PortalArticleController {

    private final CmsArticleMapper articleMapper;
    private final CmsCategoryMapper categoryMapper;
    private final CmsTagMapper tagMapper;
    private final CmsCommentMapper commentMapper;
    private final UserProfileMapper userProfileMapper;

    /**
     * 获取文章列表
     */
    @Operation(summary = "获取文章列表", description = "分页查询文章列表")
    @GetMapping("/list")
    public ApiResponse<PageResult<PortalArticleVo>> getArticleList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "分类ID") @RequestParam(required = false) String categoryId,
            @Parameter(description = "标签") @RequestParam(required = false) String tag,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "排序方式") @RequestParam(defaultValue = "newest") String sort) {
        
        Page<CmsArticle> pageObj = new Page<>(page, pageSize);
        LambdaQueryWrapper<CmsArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CmsArticle::getStatus, "published");
        
        if (categoryId != null && !categoryId.isEmpty()) {
            try {
                wrapper.eq(CmsArticle::getCategoryId, Long.parseLong(categoryId));
            } catch (NumberFormatException ignored) {
            }
        }
        
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(CmsArticle::getTitle, keyword);
        }
        
        if ("hot".equals(sort)) {
            wrapper.orderByDesc(CmsArticle::getViewCount);
        } else {
            wrapper.orderByDesc(CmsArticle::getPublishTime);
        }
        
        Page<CmsArticle> resultPage = articleMapper.selectPage(pageObj, wrapper);
        List<PortalArticleVo> voList = resultPage.getRecords().stream()
                .map(this::convertToPortalArticleVo)
                .collect(Collectors.toList());
        
        PageResult<PortalArticleVo> pageResult = PageResult.of(
                voList,
                resultPage.getTotal(),
                page,
                pageSize
        );
        
        return ApiResponse.success(pageResult);
    }

    /**
     * 获取文章详情
     */
    @Operation(summary = "获取文章详情", description = "根据文章ID获取文章详细信息")
    @GetMapping("/{articleId}")
    public ApiResponse<PortalArticleVo> getArticleDetail(
            @Parameter(description = "文章ID", required = true)
            @PathVariable String articleId) {
        try {
            Long id = Long.parseLong(articleId);
            CmsArticle article = articleMapper.selectById(id);
            if (article == null) {
                return ApiResponse.error(404, "文章不存在");
            }
            
            article.setViewCount(article.getViewCount() != null ? article.getViewCount() + 1 : 1);
            articleMapper.updateById(article);
            
            PortalArticleVo vo = convertToPortalArticleVo(article);
            return ApiResponse.success(vo);
        } catch (NumberFormatException e) {
            return ApiResponse.error("无效的文章ID");
        }
    }

    /**
     * 创建文章
     */
    @Operation(summary = "创建文章", description = "发布新文章")
    @PostMapping
    public ApiResponse<PortalArticleVo> createArticle(
            @Parameter(description = "文章信息", required = true)
            @RequestBody PortalArticleVo articleVo) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }
        
        CmsArticle article = new CmsArticle();
        article.setTitle(articleVo.getTitle());
        article.setContent(articleVo.getContent());
        article.setContentMarkdown(articleVo.getContentMarkdown());
        article.setSummary(articleVo.getExcerpt());
        article.setCoverUrl(articleVo.getCover());
        article.setAuthorId(userId);
        article.setStatus(articleVo.getStatus() != null ? articleVo.getStatus() : "draft");
        
        if (articleVo.getCategoryId() != null) {
            try {
                article.setCategoryId(Long.parseLong(articleVo.getCategoryId()));
            } catch (NumberFormatException ignored) {
            }
        }
        
        article.setViewCount(0L);
        article.setLikeCount(0L);
        article.setCommentCount(0);
        article.setWordCount(articleVo.getWordCount() != null ? articleVo.getWordCount() : 0);
        article.setCreateTime(java.time.LocalDateTime.now());
        
        articleMapper.insert(article);
        
        PortalArticleVo resultVo = convertToPortalArticleVo(article);
        return ApiResponse.success(resultVo);
    }

    /**
     * 更新文章
     */
    @Operation(summary = "更新文章", description = "更新文章信息")
    @PutMapping("/{articleId}")
    public ApiResponse<PortalArticleVo> updateArticle(
            @Parameter(description = "文章ID", required = true)
            @PathVariable String articleId,
            @Parameter(description = "文章信息", required = true)
            @RequestBody PortalArticleVo articleVo) {
        try {
            Long id = Long.parseLong(articleId);
            CmsArticle article = articleMapper.selectById(id);
            if (article == null) {
                return ApiResponse.error(404, "文章不存在");
            }
            
            Long userId = SecurityUtils.getUserId();
            if (!article.getAuthorId().equals(userId)) {
                return ApiResponse.error(403, "无权修改此文章");
            }
            
            if (articleVo.getTitle() != null) {
                article.setTitle(articleVo.getTitle());
            }
            if (articleVo.getContent() != null) {
                article.setContent(articleVo.getContent());
            }
            if (articleVo.getContentMarkdown() != null) {
                article.setContentMarkdown(articleVo.getContentMarkdown());
            }
            if (articleVo.getExcerpt() != null) {
                article.setSummary(articleVo.getExcerpt());
            }
            if (articleVo.getCover() != null) {
                article.setCoverUrl(articleVo.getCover());
            }
            if (articleVo.getStatus() != null) {
                article.setStatus(articleVo.getStatus());
            }
            if (articleVo.getCategoryId() != null) {
                try {
                    article.setCategoryId(Long.parseLong(articleVo.getCategoryId()));
                } catch (NumberFormatException ignored) {
                }
            }
            
            article.setUpdateTime(java.time.LocalDateTime.now());
            articleMapper.updateById(article);
            
            PortalArticleVo resultVo = convertToPortalArticleVo(article);
            return ApiResponse.success(resultVo);
        } catch (NumberFormatException e) {
            return ApiResponse.error("无效的文章ID");
        }
    }

    /**
     * 删除文章
     */
    @Operation(summary = "删除文章", description = "删除文章")
    @DeleteMapping("/{articleId}")
    public ApiResponse<Void> deleteArticle(
            @Parameter(description = "文章ID", required = true)
            @PathVariable String articleId) {
        try {
            Long id = Long.parseLong(articleId);
            CmsArticle article = articleMapper.selectById(id);
            if (article == null) {
                return ApiResponse.error(404, "文章不存在");
            }
            
            Long userId = SecurityUtils.getUserId();
            if (!article.getAuthorId().equals(userId)) {
                return ApiResponse.error(403, "无权删除此文章");
            }
            
            articleMapper.deleteById(id);
            return ApiResponse.success();
        } catch (NumberFormatException e) {
            return ApiResponse.error("无效的文章ID");
        }
    }

    /**
     * 文章点赞
     */
    @Operation(summary = "文章点赞", description = "对文章进行点赞")
    @PostMapping("/{articleId}/like")
    public ApiResponse<Void> likeArticle(
            @Parameter(description = "文章ID", required = true)
            @PathVariable String articleId) {
        try {
            Long id = Long.parseLong(articleId);
            CmsArticle article = articleMapper.selectById(id);
            if (article == null) {
                return ApiResponse.error(404, "文章不存在");
            }
            
            article.setLikeCount(article.getLikeCount() != null ? article.getLikeCount() + 1 : 1);
            articleMapper.updateById(article);
            return ApiResponse.success();
        } catch (NumberFormatException e) {
            return ApiResponse.error("无效的文章ID");
        }
    }

    /**
     * 取消点赞
     */
    @Operation(summary = "取消点赞", description = "取消文章点赞")
    @DeleteMapping("/{articleId}/like")
    public ApiResponse<Void> unlikeArticle(
            @Parameter(description = "文章ID", required = true)
            @PathVariable String articleId) {
        try {
            Long id = Long.parseLong(articleId);
            CmsArticle article = articleMapper.selectById(id);
            if (article == null) {
                return ApiResponse.error(404, "文章不存在");
            }
            
            if (article.getLikeCount() != null && article.getLikeCount() > 0) {
                article.setLikeCount(article.getLikeCount() - 1);
                articleMapper.updateById(article);
            }
            return ApiResponse.success();
        } catch (NumberFormatException e) {
            return ApiResponse.error("无效的文章ID");
        }
    }

    /**
     * 增加浏览次数
     */
    @Operation(summary = "增加浏览次数", description = "记录文章浏览")
    @PostMapping("/{articleId}/view")
    public ApiResponse<Void> incrementViewCount(
            @Parameter(description = "文章ID", required = true)
            @PathVariable String articleId) {
        try {
            Long id = Long.parseLong(articleId);
            CmsArticle article = articleMapper.selectById(id);
            if (article == null) {
                return ApiResponse.error(404, "文章不存在");
            }
            
            article.setViewCount(article.getViewCount() != null ? article.getViewCount() + 1 : 1);
            articleMapper.updateById(article);
            return ApiResponse.success();
        } catch (NumberFormatException e) {
            return ApiResponse.error("无效的文章ID");
        }
    }

    /**
     * 获取热门文章
     */
    @Operation(summary = "获取热门文章", description = "获取热门文章列表")
    @GetMapping("/hot")
    public ApiResponse<List<PortalArticleVo>> getHotArticles(
            @Parameter(description = "数量") @RequestParam(defaultValue = "10") Integer limit) {
        Page<CmsArticle> pageObj = new Page<>(1, limit);
        LambdaQueryWrapper<CmsArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CmsArticle::getStatus, "published")
                .orderByDesc(CmsArticle::getViewCount);
        
        Page<CmsArticle> resultPage = articleMapper.selectPage(pageObj, wrapper);
        List<PortalArticleVo> voList = resultPage.getRecords().stream()
                .map(this::convertToPortalArticleVo)
                .collect(Collectors.toList());
        
        return ApiResponse.success(voList);
    }

    /**
     * 获取精选文章
     */
    @Operation(summary = "获取精选文章", description = "获取精选文章列表")
    @GetMapping("/featured")
    public ApiResponse<List<PortalArticleVo>> getFeaturedArticles() {
        LambdaQueryWrapper<CmsArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CmsArticle::getStatus, "published")
                .eq(CmsArticle::getIsRecommend, 1)
                .orderByDesc(CmsArticle::getPublishTime);
        
        List<CmsArticle> articles = articleMapper.selectList(wrapper);
        List<PortalArticleVo> voList = articles.stream()
                .map(this::convertToPortalArticleVo)
                .collect(Collectors.toList());
        
        return ApiResponse.success(voList);
    }

    /**
     * 转换为前台文章VO
     */
    private PortalArticleVo convertToPortalArticleVo(CmsArticle article) {
        PortalArticleVo vo = new PortalArticleVo();
        vo.setId(String.valueOf(article.getArticleId()));
        vo.setTitle(article.getTitle());
        vo.setContent(article.getContent());
        vo.setContentMarkdown(article.getContentMarkdown());
        vo.setExcerpt(article.getSummary());
        vo.setCover(article.getCoverUrl());
        vo.setViews(article.getViewCount() != null ? article.getViewCount().intValue() : 0);
        vo.setLikes(article.getLikeCount() != null ? article.getLikeCount().intValue() : 0);
        vo.setCommentsCount(article.getCommentCount());
        vo.setIsTop(article.getIsTop() != null && article.getIsTop() == 1);
        vo.setIsFeatured(article.getIsRecommend() != null && article.getIsRecommend() == 1);
        vo.setStatus(article.getStatus());
        vo.setWordCount(article.getWordCount());
        
        if (article.getCreateTime() != null) {
            vo.setCreatedAt(article.getCreateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        if (article.getUpdateTime() != null) {
            vo.setUpdatedAt(article.getUpdateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        if (article.getPublishTime() != null) {
            vo.setPublishedAt(article.getPublishTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        
        if (article.getCategoryId() != null) {
            vo.setCategoryId(String.valueOf(article.getCategoryId()));
            CmsCategory category = categoryMapper.selectById(article.getCategoryId());
            if (category != null) {
                vo.setCategory(category.getCategoryName());
            }
        }
        
        if (article.getAuthorId() != null) {
            vo.setAuthorId(String.valueOf(article.getAuthorId()));
            UserProfile profile = userProfileMapper.selectByUserId(article.getAuthorId());
            if (profile != null) {
                vo.setAuthorNickname(profile.getNickname());
                vo.setAuthorAvatar(profile.getAvatar());
            }
        }
        
        vo.setTags(new ArrayList<>());
        vo.setIsLiked(false);
        vo.setIsBookmarked(false);
        
        return vo;
    }
}
