package com.moyun.portal.controller;

import java.util.Map;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.moyun.common.annotation.Anonymous;
import com.moyun.common.constant.HttpStatus;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.page.PageDomain;
import com.moyun.portal.domain.entity.PortalContestSubmission;
import com.moyun.portal.domain.entity.PortalWritingContest;
import com.moyun.portal.service.IPortalContestService;
import com.moyun.portal.util.PortalSecurityUtils;

/**
 * 创作挑战/征文活动 Controller（门户端）
 * <p>
 * 公开接口（@Anonymous）：活动列表、活动详情；
 * 需登录接口：投稿、投票、我的投稿。
 *
 * @author moyun
 */
@Tag(name = "创作挑战/征文活动", description = "征文活动发布、投稿、投票、展示")
@RestController
@RequestMapping("/portal/contest")
public class PortalContestController extends BaseController {

    @Autowired
    private IPortalContestService contestService;

    private Long currentUserId() {
        return PortalSecurityUtils.getUserId();
    }

    // ==================== 公开接口 ====================

    @Operation(summary = "活动列表", description = "公开分页查询活动列表（默认不展示草稿）")
    @GetMapping("/list")
    @Anonymous
    public AjaxResult list(PageDomain pageDomain,
                           @Parameter(description = "状态筛选 draft/collecting/voting/ended") @RequestParam(required = false) String status) {
        Page<PortalWritingContest> page = contestService.listContests(pageDomain, status);
        return AjaxResult.success(page);
    }

    @Operation(summary = "活动详情", description = "公开查询活动详情（含投稿列表，已登录则附带当前用户投票标记）")
    @GetMapping("/{id}")
    @Anonymous
    public AjaxResult detail(@Parameter(description = "活动ID") @PathVariable Long id) {
        Map<String, Object> data = contestService.getContestDetail(id, currentUserId());
        if (data == null || data.isEmpty()) {
            return AjaxResult.error("活动不存在");
        }
        return AjaxResult.success(data);
    }

    // ==================== 投稿 / 投票 / 我的投稿（需登录） ====================

    @Operation(summary = "投稿", description = "需登录，同一活动同一用户仅可投稿一次")
    @PostMapping("/{id}/submit")
    public AjaxResult submit(@Parameter(description = "活动ID") @PathVariable Long id,
                             @RequestBody Map<String, Object> body) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        Object articleIdObj = body.get("articleId");
        if (articleIdObj == null) {
            return AjaxResult.error("articleId 不能为空");
        }
        Long articleId;
        try {
            articleId = Long.valueOf(String.valueOf(articleIdObj));
        } catch (NumberFormatException e) {
            return AjaxResult.error("articleId 格式不正确");
        }
        try {
            Long submissionId = contestService.submit(id, userId, articleId);
            return AjaxResult.success(submissionId);
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @Operation(summary = "投票", description = "需登录，toggle：再次投票取消")
    @PostMapping("/submission/{id}/vote")
    public AjaxResult vote(@Parameter(description = "投稿ID") @PathVariable Long id) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        try {
            Map<String, Object> result = contestService.toggleVote(id, userId);
            return AjaxResult.success(result);
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @Operation(summary = "我的投稿", description = "需登录，分页查询当前用户参与过的所有投稿")
    @GetMapping("/my/submissions")
    public AjaxResult mySubmissions(PageDomain pageDomain) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        Page<PortalContestSubmission> page = contestService.listMySubmissions(userId, pageDomain);
        return AjaxResult.success(page);
    }
}
