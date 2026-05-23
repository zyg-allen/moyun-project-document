package com.moyun.community.content.controller.app;

import com.moyun.common.core.controller.BaseController;
import com.moyun.common.core.domain.AjaxResult;
import com.moyun.common.core.domain.TableDataInfo;
import com.moyun.common.utils.SecurityUtils;
import com.moyun.community.content.entity.CmsArticle;
import com.moyun.community.content.service.ICmsArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

/**
 * 前台文章管理Controller
 *
 * @author moyun
 * @date 2026-04-21
 */
@Tag(name = "前台文章管理", description = "面向用户的文章浏览、发布、收藏等接口")
@RestController
@RequestMapping("/app/article")
public class ArticleAppController extends BaseController {

    @Autowired
    private ICmsArticleService articleService;

    /**
     * 获取文章详情
     */
    @Operation(summary = "获取文章详情", description = "根据文章ID获取详细信息，并自动增加浏览次数")
    @GetMapping("/detail/{articleId}")
    public AjaxResult detail(
            @Parameter(description = "文章ID", required = true, example = "1")
            @PathVariable Long articleId) {
        CmsArticle article = articleService.selectArticleById(articleId);
        if (article == null) {
            return error("文章不存在");
        }
        articleService.incrementViewCount(articleId);
        return success(article);
    }

    /**
     * 获取文章列表
     */
    @Operation(summary = "获取文章列表", description = "分页查询文章列表，支持按分类、标签等条件筛选")
    @GetMapping("/list")
    public TableDataInfo list(CmsArticle article) {
        startPage();
        return getDataTable(articleService.selectArticleList(article));
    }

    /**
     * 获取我的文章列表
     */
    @Operation(summary = "获取我的文章", description = "查询当前登录用户发布的文章列表")
    @GetMapping("/my")
    public TableDataInfo myArticles() {
        Long userId = SecurityUtils.getUserId();
        startPage();
        return getDataTable(articleService.selectMyArticleList(userId));
    }

    /**
     * 保存文章草稿
     */
    @Operation(summary = "保存文章", description = "创建新文章，默认状态为草稿")
    @PostMapping("/save")
    public AjaxResult save(
            @Parameter(description = "文章信息", required = true)
            @RequestBody CmsArticle article) {
        article.setAuthorId(SecurityUtils.getUserId());
        article.setStatus("draft");
        return toAjax(articleService.save(article));
    }

    /**
     * 更新文章
     */
    @Operation(summary = "更新文章", description = "修改已存在的文章内容")
    @PutMapping("/update")
    public AjaxResult update(
            @Parameter(description = "文章信息（需包含ID）", required = true)
            @RequestBody CmsArticle article) {
        return toAjax(articleService.updateById(article));
    }

    /**
     * 提交文章审核
     */
    @Operation(summary = "提交审核", description = "将草稿状态的文章提交为待审核状态")
    @PostMapping("/submit/{articleId}")
    public AjaxResult submit(
            @Parameter(description = "文章ID", required = true, example = "1")
            @PathVariable Long articleId) {
        CmsArticle article = articleService.selectArticleById(articleId);
        if (article == null) {
            return error("文章不存在");
        }
        article.setStatus("pending");
        article.setPublishTime(new Date());
        return toAjax(articleService.updateById(article));
    }

    /**
     * 删除文章
     */
    @Operation(summary = "删除文章", description = "根据文章ID删除文章（逻辑删除）")
    @DeleteMapping("/{articleId}")
    public AjaxResult delete(
            @Parameter(description = "文章ID", required = true, example = "1")
            @PathVariable Long articleId) {
        return toAjax(articleService.removeById(articleId));
    }

    /**
     * 增加浏览次数
     */
    @Operation(summary = "增加浏览次数", description = "记录文章被查看的次数")
    @PostMapping("/view/{articleId}")
    public AjaxResult view(
            @Parameter(description = "文章ID", required = true, example = "1")
            @PathVariable Long articleId) {
        return toAjax(articleService.incrementViewCount(articleId));
    }

    /**
     * 点赞文章
     */
    @Operation(summary = "点赞文章", description = "对文章进行点赞操作")
    @PostMapping("/like/{articleId}")
    public AjaxResult like(
            @Parameter(description = "文章ID", required = true, example = "1")
            @PathVariable Long articleId) {
        return toAjax(articleService.incrementLikeCount(articleId));
    }

    /**
     * 收藏文章
     */
    @Operation(summary = "收藏文章", description = "将文章添加到个人收藏夹")
    @PostMapping("/collect/{articleId}")
    public AjaxResult collect(
            @Parameter(description = "文章ID", required = true, example = "1")
            @PathVariable Long articleId) {
        return toAjax(articleService.incrementCollectCount(articleId));
    }
}