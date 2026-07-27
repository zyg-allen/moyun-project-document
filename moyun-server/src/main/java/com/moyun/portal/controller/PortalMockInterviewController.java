package com.moyun.portal.controller;

import com.moyun.common.constant.HttpStatus;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.page.PageDomain;
import com.moyun.ext.cms.service.IMockInterviewService;
import com.moyun.portal.util.PortalSecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * AI 模拟面试官 Controller（任务 3.10 学习者成长闭环）
 * <p>
 * 全部接口需登录。简化实现：规则化评分，不依赖外部 LLM。
 * v5.9 阶段0：支持"基于我的画像出题"（薄弱点 + 岗位必备技能驱动三路召回）。
 *
 * @author moyun
 */
@Tag(name = "AI 模拟面试官", description = "模拟面试开始、作答、评分、结束、历史、画像")
@RestController
@RequestMapping("/portal/interview/mock")
public class PortalMockInterviewController extends BaseController {

    @Autowired
    private IMockInterviewService mockInterviewService;

    private Long currentUserId() {
        return PortalSecurityUtils.getUserId();
    }

    @Operation(summary = "开始模拟面试", description = "按岗位/场景从题库抽取 5 道题，生成面试会话。personalized=true 时启用画像驱动抽题")
    @PostMapping("/start")
    public AjaxResult start(@RequestBody StartRequest body) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        String position = body == null ? null : body.getPosition();
        String scene = body == null ? null : body.getScene();
        boolean personalized = body != null && Boolean.TRUE.equals(body.getPersonalized());
        return AjaxResult.success(mockInterviewService.start(userId, position, scene, personalized));
    }

    @Operation(summary = "面试详情", description = "查询面试会话详情（含问答列表与已答数、画像快照）")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable("id") Long id) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        Object detail = mockInterviewService.getDetail(id, userId);
        if (detail == null) {
            return AjaxResult.error("面试记录不存在");
        }
        return AjaxResult.success(detail);
    }

    @Operation(summary = "提交答案", description = "提交某题答案，返回 AI 规则评分（score + aiFeedback）")
    @PostMapping("/{id}/answer")
    public AjaxResult answer(@PathVariable("id") Long id, @RequestBody AnswerRequest body) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        if (body == null || body.getQuestionIdx() == null || body.getAnswer() == null) {
            return AjaxResult.error("questionIdx 与 answer 不能为空");
        }
        return AjaxResult.success(mockInterviewService.answer(id, userId, body.getQuestionIdx(), body.getAnswer()));
    }

    @Operation(summary = "结束面试", description = "结束面试并生成总结（自动计算平均分），结束后异步刷新用户画像")
    @PostMapping("/{id}/finish")
    public AjaxResult finish(@PathVariable("id") Long id) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(mockInterviewService.finish(id, userId));
    }

    @Operation(summary = "我的模拟面试列表", description = "分页查询当前用户的模拟面试历史")
    @GetMapping("/my/list")
    public AjaxResult myList(PageDomain query) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(mockInterviewService.listMy(userId, query));
    }

    @Operation(summary = "我的画像快照", description = "返回当前用户的薄弱点、岗位必备技能与面试统计，用于前端展示\"基于我的画像出题\"前置信息")
    @GetMapping("/my/profile")
    public AjaxResult myProfile(@RequestParam(value = "position", required = false) String position,
                                @RequestParam(value = "scene", required = false) String scene) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(mockInterviewService.getMyProfile(userId, position, scene));
    }

    /** 开始面试请求体 */
    @lombok.Data
    public static class StartRequest {
        /** 面试岗位（如 后端开发） */
        private String position;
        /** 面试场景（如 算法/系统设计，对应题目 tags） */
        private String scene;
        /** 是否基于用户画像（薄弱点 + 岗位必备技能）驱动抽题 */
        private Boolean personalized;
    }

    /** 提交答案请求体 */
    @lombok.Data
    public static class AnswerRequest {
        /** 题目序号（从 0 开始） */
        private Integer questionIdx;
        /** 用户回答 */
        private String answer;
    }
}
