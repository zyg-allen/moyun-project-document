package com.moyun.portal.controller;

import java.util.Map;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.moyun.common.annotation.Anonymous;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ext.cms.domain.query.InterviewCommentQuery;
import com.moyun.ext.cms.domain.query.InterviewExperienceQuery;
import com.moyun.ext.cms.domain.query.InterviewQuestionQuery;
import com.moyun.ext.cms.domain.query.InterviewResumeTemplateQuery;
import com.moyun.ext.cms.domain.vo.InterviewBookmarkVO;
import com.moyun.ext.cms.domain.vo.InterviewCommentVO;
import com.moyun.ext.cms.domain.vo.InterviewExperienceVO;
import com.moyun.ext.cms.domain.vo.InterviewQuestionVO;
import com.moyun.ext.cms.domain.vo.InterviewResumeTemplateVO;
import com.moyun.ext.cms.service.IPortalInterviewService;
import com.moyun.portal.domain.entity.PortalInterviewCategory;
import com.moyun.portal.domain.entity.PortalInterviewComment;
import com.moyun.portal.domain.entity.PortalInterviewCompany;
import com.moyun.portal.domain.entity.PortalInterviewExperience;
import com.moyun.portal.domain.entity.PortalInterviewQuestion;
import com.moyun.portal.domain.entity.PortalInterviewResumeTemplate;
import com.moyun.util.bean.PageUtils;

/**
 * 面试空间 Controller（门户端）
 *
 * @author moyun
 */
@Tag(name = "面试空间", description = "面试空间相关接口")
@RestController
@RequestMapping("/portal/interview")
public class PortalInterviewController extends BaseController {

    @Autowired
    private IPortalInterviewService portalInterviewService;

    private Long currentUserId() {
        // 后续可结合当前登录用户体系获取 userId，目前为未登录用户
        try {
            Object userId = org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes().getAttribute("portal_user_id", org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST);
            if (userId != null) return ((Number) userId).longValue();
        } catch (Exception ignored) {}
        return null;
    }

    // ==================== 首页聚合 ====================
    @Operation(summary = "获取面试空间首页数据", description = "获取分类、热门题目、热门面经、简历模板、热门公司")
    @GetMapping("/home")
    @Anonymous
    public AjaxResult getInterviewHome() {
        return AjaxResult.success(portalInterviewService.getHomeData(currentUserId()));
    }

    // ==================== 分类管理 ====================
    @Operation(summary = "获取分类列表", description = "获取所有题目分类")
    @GetMapping("/category/list")
    @Anonymous
    public AjaxResult getCategoryList() {
        return AjaxResult.success(portalInterviewService.selectCategoryList());
    }

    // ==================== 题目管理 ====================
    @Operation(summary = "获取题目分页列表")
    @GetMapping("/question/list")
    @Anonymous
    public AjaxResult getQuestionList(InterviewQuestionQuery query) {
        Page<InterviewQuestionVO> page = PageUtils.buildPage(query);
        return AjaxResult.success(portalInterviewService.selectQuestionPage(page, query, currentUserId()));
    }

    @Operation(summary = "获取题目详情")
    @GetMapping("/question/{id}")
    @Anonymous
    public AjaxResult getQuestionDetail(@PathVariable("id") Long id) {
        return AjaxResult.success(portalInterviewService.selectQuestionDetailById(id, currentUserId()));
    }

    @Operation(summary = "提交答案")
    @PostMapping("/question/{id}/submit")
    public AjaxResult submitAnswer(@PathVariable("id") Long questionId, @RequestBody Map<String, Object> body) {
        return AjaxResult.success(portalInterviewService.submitAnswer(questionId, currentUserId(), body));
    }

    @Operation(summary = "点赞/取消点赞 题目")
    @PostMapping("/question/{id}/like")
    public AjaxResult toggleQuestionLike(@PathVariable("id") Long questionId) {
        return AjaxResult.success(portalInterviewService.toggleQuestionLike(questionId, currentUserId()));
    }

    @Operation(summary = "收藏/取消收藏 题目")
    @PostMapping("/question/{id}/bookmark")
    public AjaxResult toggleQuestionBookmark(@PathVariable("id") Long questionId, @RequestBody(required = false) Map<String, String> body) {
        String note = body != null ? body.get("note") : null;
        return AjaxResult.success(portalInterviewService.toggleQuestionBookmark(questionId, currentUserId(), note));
    }

    // ==================== 我的收藏 ====================
    @Operation(summary = "我的收藏列表")
    @GetMapping("/bookmark/list")
    public AjaxResult getMyBookmarkList() {
        Page<InterviewBookmarkVO> page = this.startPage();
        return AjaxResult.success(portalInterviewService.selectBookmarkPage(page, currentUserId()));
    }

    // ==================== 面经管理 ====================
    @Operation(summary = "获取面经分页列表")
    @GetMapping("/experience/list")
    @Anonymous
    public AjaxResult getExperienceList(InterviewExperienceQuery query) {
        Page<InterviewExperienceVO> page = PageUtils.buildPage(query);
        return AjaxResult.success(portalInterviewService.selectExperiencePage(page, query, currentUserId()));
    }

    @Operation(summary = "获取面经详情")
    @GetMapping("/experience/{id}")
    @Anonymous
    public AjaxResult getExperienceDetail(@PathVariable("id") Long id) {
        return AjaxResult.success(portalInterviewService.selectExperienceDetailById(id, currentUserId()));
    }

    @Operation(summary = "发布面经")
    @PostMapping("/experience")
    public AjaxResult publishExperience(@RequestBody PortalInterviewExperience experience) {
        return AjaxResult.success(portalInterviewService.insertExperience(experience, currentUserId()));
    }

    @Operation(summary = "更新面经")
    @PutMapping("/experience")
    public AjaxResult updateExperience(@RequestBody PortalInterviewExperience experience) {
        return AjaxResult.success(portalInterviewService.updateExperience(experience, currentUserId()));
    }

    @Operation(summary = "删除面经")
    @DeleteMapping("/experience/{id}")
    public AjaxResult deleteExperience(@PathVariable("id") Long id) {
        return AjaxResult.success(portalInterviewService.deleteExperienceById(id, currentUserId()));
    }

    @Operation(summary = "点赞/取消点赞 面经")
    @PostMapping("/experience/{id}/like")
    public AjaxResult toggleExperienceLike(@PathVariable("id") Long experienceId) {
        return AjaxResult.success(portalInterviewService.toggleExperienceLike(experienceId, currentUserId()));
    }

    // ==================== 评论管理 ====================
    @Operation(summary = "获取评论列表")
    @GetMapping("/comment/list")
    @Anonymous
    public AjaxResult getCommentList(InterviewCommentQuery query) {
        Page<InterviewCommentVO> page = PageUtils.buildPage(query);
        return AjaxResult.success(portalInterviewService.selectCommentPage(page, query, currentUserId()));
    }

    @Operation(summary = "发表评论/回复")
    @PostMapping("/comment")
    public AjaxResult publishComment(@RequestBody PortalInterviewComment comment) {
        return AjaxResult.success(portalInterviewService.insertComment(comment, currentUserId()));
    }

    @Operation(summary = "删除评论")
    @DeleteMapping("/comment/{id}")
    public AjaxResult deleteComment(@PathVariable("id") Long id) {
        return AjaxResult.success(portalInterviewService.deleteCommentById(id, currentUserId()));
    }

    @Operation(summary = "点赞/取消点赞 评论")
    @PostMapping("/comment/{id}/like")
    public AjaxResult toggleCommentLike(@PathVariable("id") Long commentId) {
        return AjaxResult.success(portalInterviewService.toggleCommentLike(commentId, currentUserId()));
    }

    // ==================== 简历模板 ====================
    @Operation(summary = "获取简历模板分页列表")
    @GetMapping("/resume/list")
    @Anonymous
    public AjaxResult getResumeTemplateList(InterviewResumeTemplateQuery query) {
        Page<InterviewResumeTemplateVO> page = PageUtils.buildPage(query);
        return AjaxResult.success(portalInterviewService.selectResumeTemplatePage(page, query, currentUserId()));
    }

    @Operation(summary = "获取简历模板详情")
    @GetMapping("/resume/{id}")
    @Anonymous
    public AjaxResult getResumeTemplateDetail(@PathVariable("id") Long id) {
        return AjaxResult.success(portalInterviewService.selectResumeTemplateById(id));
    }

    @Operation(summary = "下载简历模板（返回下载地址）")
    @GetMapping("/resume/{id}/download")
    @Anonymous
    public AjaxResult downloadResumeTemplate(@PathVariable("id") Long id) {
        return AjaxResult.success(portalInterviewService.selectResumeTemplateById(id));
    }

    @Operation(summary = "点赞/取消点赞 简历模板")
    @PostMapping("/resume/{id}/like")
    public AjaxResult toggleResumeTemplateLike(@PathVariable("id") Long templateId) {
        return AjaxResult.success(portalInterviewService.toggleResumeTemplateLike(templateId, currentUserId()));
    }

    // ==================== 公司标签 ====================
    @Operation(summary = "获取公司标签列表")
    @GetMapping("/company/list")
    @Anonymous
    public AjaxResult getCompanyList() {
        return AjaxResult.success(portalInterviewService.selectCompanyList(new com.moyun.ext.cms.domain.query.InterviewCompanyQuery()));
    }

    @Operation(summary = "获取公司标签详情")
    @GetMapping("/company/{id}")
    @Anonymous
    public AjaxResult getCompanyDetail(@PathVariable("id") Long id) {
        return AjaxResult.success(portalInterviewService.selectCompanyById(id));
    }
}
