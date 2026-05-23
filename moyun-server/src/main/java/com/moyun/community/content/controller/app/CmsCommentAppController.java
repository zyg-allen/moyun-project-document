package com.moyun.community.content.controller.app;

import com.moyun.common.core.controller.BaseController;
import com.moyun.common.core.domain.AjaxResult;
import com.moyun.common.core.domain.TableDataInfo;
import com.moyun.common.utils.SecurityUtils;
import com.moyun.community.content.entity.CmsComment;
import com.moyun.community.content.service.ICmsCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 前台评论管理Controller
 *
 * @author moyun
 * @date 2026-04-21
 */
@Tag(name = "前台评论管理", description = "文章评论、回复、点赞等接口")
@RestController
@RequestMapping("/app/comment")
public class CmsCommentAppController extends BaseController {

    @Autowired
    private ICmsCommentService commentService;

    /**
     * 获取文章评论列表
     */
    @Operation(summary = "获取评论列表", description = "分页查询指定文章的评论列表")
    @GetMapping("/list/{articleId}")
    public TableDataInfo list(
            @Parameter(description = "文章ID", required = true, example = "1")
            @PathVariable Long articleId) {
        startPage();
        List<CmsComment> list = commentService.selectCommentListByArticle(articleId);
        return getDataTable(list);
    }

    /**
     * 发表评论
     */
    @Operation(summary = "发表评论", description = "对文章发表新评论")
    @PostMapping
    public AjaxResult add(
            @Parameter(description = "评论信息", required = true)
            @RequestBody CmsComment comment) {
        comment.setUserId(SecurityUtils.getUserId());
        comment.setParentId(0L);
        return toAjax(commentService.addComment(comment));
    }

    /**
     * 回复评论
     */
    @Operation(summary = "回复评论", description = "对已有评论进行回复")
    @PostMapping("/reply")
    public AjaxResult reply(
            @Parameter(description = "回复信息", required = true)
            @RequestBody CmsComment comment) {
        comment.setUserId(SecurityUtils.getUserId());
        return toAjax(commentService.replyComment(comment));
    }

    /**
     * 删除评论
     */
    @Operation(summary = "删除评论", description = "删除指定的评论")
    @DeleteMapping("/{commentId}")
    public AjaxResult remove(
            @Parameter(description = "评论ID", required = true, example = "1")
            @PathVariable Long commentId) {
        return toAjax(commentService.deleteComment(commentId));
    }

    /**
     * 点赞评论
     */
    @Operation(summary = "点赞评论", description = "对评论进行点赞或取消点赞")
    @PostMapping("/like/{commentId}")
    public AjaxResult like(
            @Parameter(description = "评论ID", required = true, example = "1")
            @PathVariable Long commentId) {
        return toAjax(commentService.toggleLike(commentId));
    }
}