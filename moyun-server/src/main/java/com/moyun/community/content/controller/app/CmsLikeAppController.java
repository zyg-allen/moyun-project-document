package com.moyun.community.content.controller.app;

import com.moyun.common.core.controller.BaseController;
import com.moyun.common.core.domain.AjaxResult;
import com.moyun.common.utils.SecurityUtils;
import com.moyun.community.content.entity.CmsLike;
import com.moyun.community.content.service.ICmsLikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 前台点赞管理Controller
 *
 * @author moyun
 * @date 2026-04-21
 */
@Tag(name = "前台点赞管理", description = "文章、评论点赞等接口")
@RestController
@RequestMapping("/app/like")
public class CmsLikeAppController extends BaseController {

    @Autowired
    private ICmsLikeService likeService;

    /**
     * 切换点赞状态
     */
    @Operation(summary = "切换点赞", description = "点赞或取消点赞（支持文章和评论）")
    @PostMapping("/toggle")
    public AjaxResult toggle(
            @Parameter(description = "目标类型(article/comment)", required = true, example = "article")
            @RequestParam String targetType,
            @Parameter(description = "目标ID", required = true, example = "1")
            @RequestParam Long targetId) {
        Long userId = SecurityUtils.getUserId();
        return toAjax(likeService.toggleLike(userId, targetType, targetId));
    }

    /**
     * 获取我的点赞列表
     */
    @Operation(summary = "我的点赞", description = "查询当前用户的点赞记录")
    @GetMapping("/my")
    public AjaxResult myLikes() {
        Long userId = SecurityUtils.getUserId();
        List<CmsLike> list = likeService.selectLikeListByUser(userId);
        return success(list);
    }

    /**
     * 检查是否已点赞
     */
    @Operation(summary = "检查点赞状态", description = "检查当前用户是否已点赞指定目标")
    @GetMapping("/check")
    public AjaxResult check(
            @Parameter(description = "目标类型(article/comment)", required = true, example = "article")
            @RequestParam String targetType,
            @Parameter(description = "目标ID", required = true, example = "1")
            @RequestParam Long targetId) {
        Long userId = SecurityUtils.getUserId();
        boolean liked = likeService.isLiked(userId, targetType, targetId);
        return success(liked);
    }
}