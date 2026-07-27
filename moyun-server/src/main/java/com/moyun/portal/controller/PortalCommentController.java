package com.moyun.portal.controller;

import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.moyun.common.annotation.Log;
import com.moyun.common.annotation.RateLimiter;
import com.moyun.common.constant.HttpStatus;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.portal.domain.entity.PortalComment;
import com.moyun.portal.service.IPortalCommentService;
import com.moyun.portal.util.PortalSecurityUtils;

/**
 * 门户评论 Controller
 *
 * 清理说明：本类已删除前端未使用的死接口，仅保留在用的三个方法：
 *   - getArticleComments（获取文章评论列表，含回复）
 *   - add（新增评论）
 *   - toggleLike（评论点赞/取消点赞，阶段五新增）
 * 已删除的死接口：list / export / getInfo / edit / remove。
 * 对应的 Service / Mapper / XML 实现保留不动，便于后续管理后台复用。
 */
@Tag(name = "门户评论", description = "门户评论的增删改查操作接口")
@RestController
@RequestMapping("/portal/comment")
public class PortalCommentController extends BaseController {

    @Autowired
    private IPortalCommentService portalCommentService;

    @Operation(summary = "获取文章的评论列表（含回复）", description = "获取文章的评论列表，包含回复内容，支持分页")
    @GetMapping("/article/{articleId}")
    public AjaxResult getArticleComments(
            @Parameter(description = "文章ID") @PathVariable Long articleId,
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer pageSize) {
        // 评论列表为公开接口，但 isLiked 字段需要登录态；未登录时 currentUserId=null，前端显示"未点赞"状态
        Long currentUserId = PortalSecurityUtils.getUserId();
        Map<String, Object> result = portalCommentService.getCommentsByArticle(articleId, pageNum, pageSize, currentUserId);
        return success(result);
    }

    @Operation(summary = "新增评论", description = "创建新评论")
    @Log(title = "门户评论", businessType = BusinessType.INSERT)
    @RateLimiter(time = 60, count = 10)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody PortalComment portalComment) {
        // 发布评论需登录态校验（与 SecurityConfig 链 authenticated 双重防护）
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "请先登录后再评论");
        }
        return toAjax(portalCommentService.insertPortalComment(portalComment));
    }

    @Operation(summary = "评论点赞/取消点赞", description = "切换评论点赞状态，幂等 toggle，返回最新点赞数和状态")
    @PostMapping("/{id}/like")
    public AjaxResult toggleLike(@Parameter(description = "评论ID") @PathVariable Long id) {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        return success(portalCommentService.toggleLike(id, userId));
    }
}
