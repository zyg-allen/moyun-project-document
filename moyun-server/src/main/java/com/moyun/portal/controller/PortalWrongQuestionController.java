package com.moyun.portal.controller;

import com.moyun.common.constant.HttpStatus;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ext.cms.domain.query.WrongQuestionQuery;
import com.moyun.ext.cms.service.IWrongQuestionService;
import com.moyun.portal.util.PortalSecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 错题本 Controller（任务 3.3，门户端）
 * <p>
 * 所有接口均需登录。
 *
 * @author moyun
 */
@Tag(name = "错题本", description = "错题列表、状态筛选、标记掌握、今日待复习")
@RestController
@RequestMapping("/portal/learn/wrong")
public class PortalWrongQuestionController extends BaseController {

    @Autowired
    private IWrongQuestionService wrongQuestionService;

    private Long currentUserId() {
        return PortalSecurityUtils.getUserId();
    }

    @Operation(summary = "错题列表", description = "分页查询错题，支持按状态/标签/关键词筛选")
    @GetMapping("/list")
    public AjaxResult list(WrongQuestionQuery query) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(wrongQuestionService.listWrongQuestions(userId, query));
    }

    @Operation(summary = "标记已掌握", description = "将指定题目标记为已掌握（status -> mastered）")
    @PostMapping("/{questionId}/master")
    public AjaxResult markMastered(@PathVariable("questionId") Long questionId) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(wrongQuestionService.markMastered(userId, questionId));
    }

    @Operation(summary = "今日待复习", description = "返回今日待复习错题列表（基于艾宾浩斯）")
    @GetMapping("/review")
    public AjaxResult review() {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(wrongQuestionService.listTodayReview(userId));
    }

    @Operation(summary = "错题统计", description = "返回错题数、今日待复习数")
    @GetMapping("/count")
    public AjaxResult count() {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("wrongCount", wrongQuestionService.countWrong(userId, null));
        stats.put("unMasteredCount", wrongQuestionService.countWrong(userId, "wrong"));
        stats.put("reviewingCount", wrongQuestionService.countWrong(userId, "reviewing"));
        stats.put("masteredCount", wrongQuestionService.countWrong(userId, "mastered"));
        stats.put("todayReviewCount", wrongQuestionService.countTodayReview(userId));
        return AjaxResult.success(stats);
    }
}
