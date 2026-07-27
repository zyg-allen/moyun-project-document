package com.moyun.portal.controller;

import java.util.Map;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.moyun.common.annotation.Anonymous;
import com.moyun.common.constant.HttpStatus;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ext.cms.domain.query.InterviewCommentQuery;
import com.moyun.ext.cms.domain.query.InterviewExperienceQuery;
import com.moyun.ext.cms.domain.query.InterviewQuestionQuery;
import com.moyun.ext.cms.domain.query.InterviewResumeTemplateQuery;
import com.moyun.ext.cms.domain.query.InterviewCompanyQuery;
import com.moyun.ext.cms.domain.vo.InterviewBookmarkVO;
import com.moyun.ext.cms.domain.vo.InterviewCommentVO;
import com.moyun.ext.cms.domain.vo.InterviewExperienceVO;
import com.moyun.ext.cms.domain.vo.InterviewQuestionVO;
import com.moyun.ext.cms.domain.vo.InterviewResumeTemplateVO;
import com.moyun.ext.cms.domain.vo.InterviewSubmissionVO;
import com.moyun.ext.cms.service.IPortalInterviewService;
import com.moyun.portal.domain.entity.PortalInterviewCategory;
import com.moyun.portal.domain.entity.PortalInterviewComment;
import com.moyun.portal.domain.entity.PortalInterviewCompany;
import com.moyun.portal.domain.entity.PortalInterviewExperience;
import com.moyun.portal.domain.entity.PortalInterviewQuestion;
import com.moyun.portal.domain.entity.PortalInterviewResumeTemplate;
import com.moyun.portal.util.PortalSecurityUtils;
import com.moyun.util.bean.PageUtils;

/**
 * 面试空间 Controller（门户端）
 * <p>
 * 已恢复以下接口（Service/Mapper 实现完整保留）：
 * getMyBookmarkList、getMySubmissionList、getMyExperienceList、
 * publishExperience、updateExperience、deleteExperience、deleteComment、
 * getResumeTemplateDetail、getCompanyList、getCompanyDetail
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
        return PortalSecurityUtils.getUserId();
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

    @Operation(summary = "画像推荐题目", description = "基于用户画像（薄弱点 + 岗位必备技能 + 热门兜底）推荐题目，需登录；未登录或无画像时返回空列表")
    @GetMapping("/question/recommend")
    @Anonymous
    public AjaxResult getRecommendedQuestions(@RequestParam(value = "limit", required = false, defaultValue = "6") Integer limit) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.success(java.util.Collections.emptyList());
        }
        int safeLimit = limit == null ? 6 : Math.max(1, Math.min(limit, 12));
        return AjaxResult.success(portalInterviewService.selectRecommendedQuestions(userId, safeLimit));
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
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(portalInterviewService.submitAnswer(questionId, userId, body));
    }

    @Operation(summary = "点赞/取消点赞 题目")
    @PostMapping("/question/{id}/like")
    public AjaxResult toggleQuestionLike(@PathVariable("id") Long questionId) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(portalInterviewService.toggleQuestionLike(questionId, userId));
    }

    @Operation(summary = "收藏/取消收藏 题目")
    @PostMapping("/question/{id}/bookmark")
    public AjaxResult toggleQuestionBookmark(@PathVariable("id") Long questionId, @RequestBody(required = false) Map<String, String> body) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        String note = body != null ? body.get("note") : null;
        return AjaxResult.success(portalInterviewService.toggleQuestionBookmark(questionId, userId, note));
    }

    // ==================== 精选笔记 ====================
    @Operation(summary = "查询某题目的精选笔记列表", description = "公开接口，返回后台采纳的优质笔记")
    @GetMapping("/question/{id}/featured-notes")
    @Anonymous
    public AjaxResult getFeaturedNotes(@PathVariable("id") Long questionId) {
        return AjaxResult.success(portalInterviewService.selectFeaturedSubmissions(questionId));
    }

    // ==================== 我的（个人中心） ====================
    @Operation(summary = "我的收藏题目列表", description = "分页查询当前用户收藏的题目")
    @GetMapping("/bookmark/my")
    public AjaxResult getMyBookmarkList(InterviewQuestionQuery query) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        Page<InterviewBookmarkVO> page = PageUtils.buildPage(query);
        return AjaxResult.success(portalInterviewService.selectBookmarkPage(page, userId));
    }

    @Operation(summary = "我的答题历史", description = "分页查询当前用户的提交记录")
    @GetMapping("/submission/my")
    public AjaxResult getMySubmissionList(InterviewQuestionQuery query) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        Page<InterviewSubmissionVO> page = PageUtils.buildPage(query);
        return AjaxResult.success(portalInterviewService.selectMySubmissionList(page, userId));
    }

    @Operation(summary = "我的面经列表", description = "分页查询当前用户发布的面经（含草稿/待审核）")
    @GetMapping("/experience/my")
    public AjaxResult getMyExperienceList(InterviewExperienceQuery query) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        Page<InterviewExperienceVO> page = PageUtils.buildPage(query);
        return AjaxResult.success(portalInterviewService.selectMyExperienceList(page, query, userId));
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

    @Operation(summary = "点赞/取消点赞 面经")
    @PostMapping("/experience/{id}/like")
    public AjaxResult toggleExperienceLike(@PathVariable("id") Long experienceId) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(portalInterviewService.toggleExperienceLike(experienceId, userId));
    }

    @Operation(summary = "发布面经", description = "用户发布面经，默认进入 pending 待审核状态")
    @PostMapping("/experience")
    public AjaxResult publishExperience(@RequestBody PortalInterviewExperience experience) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(portalInterviewService.insertExperience(experience, userId));
    }

    @Operation(summary = "更新面经", description = "仅作者可更新自己的面经")
    @PutMapping("/experience")
    public AjaxResult updateExperience(@RequestBody PortalInterviewExperience experience) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(portalInterviewService.updateExperience(experience, userId));
    }

    @Operation(summary = "删除面经", description = "仅作者可删除自己的面经")
    @DeleteMapping("/experience/{id}")
    public AjaxResult deleteExperience(@PathVariable("id") Long id) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(portalInterviewService.deleteExperienceById(id, userId));
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
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(portalInterviewService.insertComment(comment, userId));
    }

    @Operation(summary = "点赞/取消点赞 评论")
    @PostMapping("/comment/{id}/like")
    public AjaxResult toggleCommentLike(@PathVariable("id") Long commentId) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(portalInterviewService.toggleCommentLike(commentId, userId));
    }

    @Operation(summary = "删除评论", description = "仅作者可删除自己的评论")
    @DeleteMapping("/comment/{id}")
    public AjaxResult deleteComment(@PathVariable("id") Long id) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(portalInterviewService.deleteCommentById(id, userId));
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

    @Operation(summary = "下载简历模板（返回下载地址，并递增下载次数）")
    @GetMapping("/resume/{id}/download")
    @Anonymous
    public AjaxResult downloadResumeTemplate(@PathVariable("id") Long id) {
        com.moyun.ext.cms.domain.vo.InterviewResumeTemplateVO vo = portalInterviewService.downloadResumeTemplate(id);
        if (vo == null) {
            return AjaxResult.error("模板不存在或已下架");
        }
        return AjaxResult.success(vo);
    }

    @Operation(summary = "点赞/取消点赞 简历模板")
    @PostMapping("/resume/{id}/like")
    public AjaxResult toggleResumeTemplateLike(@PathVariable("id") Long templateId) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(portalInterviewService.toggleResumeTemplateLike(templateId, userId));
    }

    // ==================== 公司标签 ====================
    @Operation(summary = "获取公司标签列表", description = "前台公司聚合页用")
    @GetMapping("/company/list")
    @Anonymous
    public AjaxResult getCompanyList(InterviewCompanyQuery query) {
        return AjaxResult.success(portalInterviewService.selectCompanyList(query));
    }

    @Operation(summary = "获取公司标签详情")
    @GetMapping("/company/{id}")
    @Anonymous
    public AjaxResult getCompanyDetail(@PathVariable("id") Long id) {
        return AjaxResult.success(portalInterviewService.selectCompanyById(id));
    }
}
