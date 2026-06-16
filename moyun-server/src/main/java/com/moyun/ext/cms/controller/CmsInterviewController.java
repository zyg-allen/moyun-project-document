package com.moyun.ext.cms.controller;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ext.cms.domain.query.InterviewCommentQuery;
import com.moyun.ext.cms.domain.query.InterviewCompanyQuery;
import com.moyun.ext.cms.domain.query.InterviewExperienceQuery;
import com.moyun.ext.cms.domain.query.InterviewQuestionQuery;
import com.moyun.ext.cms.domain.query.InterviewResumeTemplateQuery;
import com.moyun.ext.cms.domain.vo.InterviewCategoryVO;
import com.moyun.ext.cms.domain.vo.InterviewCommentVO;
import com.moyun.ext.cms.domain.vo.InterviewCompanyVO;
import com.moyun.ext.cms.domain.vo.InterviewExperienceVO;
import com.moyun.ext.cms.domain.vo.InterviewQuestionDetailVO;
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
 * CMS面试模块管理Controller
 *
 * @author moyun
 */
@Tag(name = "CMS面试模块管理", description = "CMS面试模块管理接口")
@RestController
@RequestMapping("/cms/interview")
public class CmsInterviewController extends BaseController {

    @Autowired
    private IPortalInterviewService portalInterviewService;

    // ========================================================================
    // 题目管理
    // ========================================================================

    @Operation(summary = "获取题目分页列表", description = "根据条件分页查询题目列表")
    @PreAuthorize("@ss.hasPermi('cms:interview:list')")
    @GetMapping("/question/list")
    public AjaxResult listQuestion(InterviewQuestionQuery query) {
        Page<InterviewQuestionVO> page = PageUtils.buildPage(query);
        page = portalInterviewService.selectQuestionPage(page, query, null);
        return success(page);
    }

    @Operation(summary = "获取题目详情", description = "根据题目ID获取题目详细信息")
    @PreAuthorize("@ss.hasPermi('cms:interview:query')")
    @GetMapping("/question/{id}")
    public AjaxResult getQuestion(@Parameter(description = "题目ID") @PathVariable Long id) {
        InterviewQuestionDetailVO detail = portalInterviewService.selectQuestionDetailById(id, null);
        return success(detail);
    }

    @Operation(summary = "新增题目", description = "创建新题目")
    @PreAuthorize("@ss.hasPermi('cms:interview:add')")
    @Log(title = "面试题目", businessType = BusinessType.INSERT)
    @PostMapping("/question")
    public AjaxResult addQuestion(@Validated @RequestBody PortalInterviewQuestion question) {
        return toAjax(portalInterviewService.insertQuestion(question));
    }

    @Operation(summary = "修改题目", description = "更新题目信息")
    @PreAuthorize("@ss.hasPermi('cms:interview:edit')")
    @Log(title = "面试题目", businessType = BusinessType.UPDATE)
    @PutMapping("/question")
    public AjaxResult editQuestion(@Validated @RequestBody PortalInterviewQuestion question) {
        return toAjax(portalInterviewService.updateQuestion(question));
    }

    @Operation(summary = "批量删除题目", description = "批量删除题目")
    @PreAuthorize("@ss.hasPermi('cms:interview:remove')")
    @Log(title = "面试题目", businessType = BusinessType.DELETE)
    @DeleteMapping("/question")
    public AjaxResult removeQuestion(@RequestBody Long[] ids) {
        return toAjax(portalInterviewService.deleteQuestionByIds(ids));
    }

    // ========================================================================
    // 分类管理
    // ========================================================================

    @Operation(summary = "获取分类列表", description = "获取题目分类列表")
    @PreAuthorize("@ss.hasPermi('cms:interview:list')")
    @GetMapping("/category/list")
    public AjaxResult listCategory() {
        List<InterviewCategoryVO> list = portalInterviewService.selectCategoryList();
        return success(list);
    }

    @Operation(summary = "获取分类详情", description = "根据分类ID获取详细信息")
    @PreAuthorize("@ss.hasPermi('cms:interview:query')")
    @GetMapping("/category/{id}")
    public AjaxResult getCategory(@Parameter(description = "分类ID") @PathVariable Long id) {
        return success(portalInterviewService.selectCategoryById(id));
    }

    @Operation(summary = "新增分类", description = "创建新分类")
    @PreAuthorize("@ss.hasPermi('cms:interview:add')")
    @Log(title = "面试分类", businessType = BusinessType.INSERT)
    @PostMapping("/category")
    public AjaxResult addCategory(@Validated @RequestBody PortalInterviewCategory category) {
        return toAjax(portalInterviewService.insertCategory(category));
    }

    @Operation(summary = "修改分类", description = "更新分类信息")
    @PreAuthorize("@ss.hasPermi('cms:interview:edit')")
    @Log(title = "面试分类", businessType = BusinessType.UPDATE)
    @PutMapping("/category")
    public AjaxResult editCategory(@Validated @RequestBody PortalInterviewCategory category) {
        return toAjax(portalInterviewService.updateCategory(category));
    }

    @Operation(summary = "删除分类", description = "删除分类")
    @PreAuthorize("@ss.hasPermi('cms:interview:remove')")
    @Log(title = "面试分类", businessType = BusinessType.DELETE)
    @DeleteMapping("/category/{ids}")
    public AjaxResult removeCategory(@Parameter(description = "分类ID数组") @PathVariable Long[] ids) {
        return toAjax(portalInterviewService.deleteCategoryByIds(ids));
    }

    // ========================================================================
    // 面经管理
    // ========================================================================

    @Operation(summary = "获取面经分页列表", description = "根据条件分页查询面经列表")
    @PreAuthorize("@ss.hasPermi('cms:interview:list')")
    @GetMapping("/experience/list")
    public AjaxResult listExperience(InterviewExperienceQuery query) {
        Page<InterviewExperienceVO> page = PageUtils.buildPage(query);
        page = portalInterviewService.selectExperiencePage(page, query, null);
        return success(page);
    }

    @Operation(summary = "获取面经详情", description = "根据面经ID获取详细信息")
    @PreAuthorize("@ss.hasPermi('cms:interview:query')")
    @GetMapping("/experience/{id}")
    public AjaxResult getExperience(@Parameter(description = "面经ID") @PathVariable Long id) {
        return success(portalInterviewService.selectExperienceDetailById(id, null));
    }

    @Operation(summary = "审核面经", description = "审核面经内容")
    @PreAuthorize("@ss.hasPermi('cms:interview:edit')")
    @Log(title = "面试面经", businessType = BusinessType.UPDATE)
    @PutMapping("/experience/audit")
    public AjaxResult auditExperience(@RequestBody Map<String, Object> body) {
        Long id = Long.valueOf(String.valueOf(body.get("id")));
        String status = String.valueOf(body.get("status"));
        String remark = body.get("remark") != null ? String.valueOf(body.get("remark")) : null;
        return toAjax(portalInterviewService.auditExperience(id, status, remark));
    }

    @Operation(summary = "面经置顶", description = "设置面经是否置顶")
    @PreAuthorize("@ss.hasPermi('cms:interview:edit')")
    @Log(title = "面试面经", businessType = BusinessType.UPDATE)
    @PutMapping("/experience/top")
    public AjaxResult topExperience(@RequestBody Map<String, Object> body) {
        Long id = Long.valueOf(String.valueOf(body.get("id")));
        Boolean isTop = Boolean.valueOf(String.valueOf(body.get("isTop")));
        return toAjax(portalInterviewService.topExperience(id, isTop));
    }

    @Operation(summary = "删除面经", description = "删除面经")
    @PreAuthorize("@ss.hasPermi('cms:interview:remove')")
    @Log(title = "面试面经", businessType = BusinessType.DELETE)
    @DeleteMapping("/experience/{id}")
    public AjaxResult removeExperience(@Parameter(description = "面经ID") @PathVariable Long id) {
        return toAjax(portalInterviewService.deleteExperienceById(id, null));
    }

    // ========================================================================
    // 评论管理
    // ========================================================================

    @Operation(summary = "获取评论分页列表", description = "根据条件分页查询评论列表")
    @PreAuthorize("@ss.hasPermi('cms:interview:list')")
    @GetMapping("/comment/list")
    public AjaxResult listComment(InterviewCommentQuery query) {
        Page<InterviewCommentVO> page = PageUtils.buildPage(query);
        page = portalInterviewService.selectCommentPage(page, query, null);
        return success(page);
    }

    @Operation(summary = "审核评论", description = "审核评论内容")
    @PreAuthorize("@ss.hasPermi('cms:interview:edit')")
    @Log(title = "面试评论", businessType = BusinessType.UPDATE)
    @PutMapping("/comment/audit")
    public AjaxResult auditComment(@RequestBody Map<String, Object> body) {
        Long id = Long.valueOf(String.valueOf(body.get("id")));
        String status = String.valueOf(body.get("status"));
        String remark = body.get("remark") != null ? String.valueOf(body.get("remark")) : null;
        return toAjax(portalInterviewService.auditComment(id, status, remark));
    }

    @Operation(summary = "删除评论", description = "删除评论")
    @PreAuthorize("@ss.hasPermi('cms:interview:remove')")
    @Log(title = "面试评论", businessType = BusinessType.DELETE)
    @DeleteMapping("/comment/{id}")
    public AjaxResult removeComment(@Parameter(description = "评论ID") @PathVariable Long id) {
        return toAjax(portalInterviewService.deleteCommentById(id, null));
    }

    // ========================================================================
    // 简历模板管理
    // ========================================================================

    @Operation(summary = "获取简历模板分页列表", description = "根据条件分页查询简历模板列表")
    @PreAuthorize("@ss.hasPermi('cms:interview:list')")
    @GetMapping("/resume/list")
    public AjaxResult listResume(InterviewResumeTemplateQuery query) {
        Page<InterviewResumeTemplateVO> page = PageUtils.buildPage(query);
        page = portalInterviewService.selectResumeTemplatePage(page, query, null);
        return success(page);
    }

    @Operation(summary = "获取简历模板详情", description = "根据ID获取简历模板详细信息")
    @PreAuthorize("@ss.hasPermi('cms:interview:query')")
    @GetMapping("/resume/{id}")
    public AjaxResult getResume(@Parameter(description = "模板ID") @PathVariable Long id) {
        return success(portalInterviewService.selectResumeTemplateById(id));
    }

    @Operation(summary = "新增简历模板", description = "创建新简历模板")
    @PreAuthorize("@ss.hasPermi('cms:interview:add')")
    @Log(title = "简历模板", businessType = BusinessType.INSERT)
    @PostMapping("/resume")
    public AjaxResult addResume(@Validated @RequestBody PortalInterviewResumeTemplate template) {
        return toAjax(portalInterviewService.insertResumeTemplate(template));
    }

    @Operation(summary = "修改简历模板", description = "更新简历模板信息")
    @PreAuthorize("@ss.hasPermi('cms:interview:edit')")
    @Log(title = "简历模板", businessType = BusinessType.UPDATE)
    @PutMapping("/resume")
    public AjaxResult editResume(@Validated @RequestBody PortalInterviewResumeTemplate template) {
        return toAjax(portalInterviewService.updateResumeTemplate(template));
    }

    @Operation(summary = "删除简历模板", description = "删除简历模板")
    @PreAuthorize("@ss.hasPermi('cms:interview:remove')")
    @Log(title = "简历模板", businessType = BusinessType.DELETE)
    @DeleteMapping("/resume/{ids}")
    public AjaxResult removeResume(@Parameter(description = "模板ID数组") @PathVariable Long[] ids) {
        return toAjax(portalInterviewService.deleteResumeTemplateByIds(ids));
    }

    // ========================================================================
    // 公司标签管理
    // ========================================================================

    @Operation(summary = "获取公司列表", description = "获取公司标签列表")
    @PreAuthorize("@ss.hasPermi('cms:interview:list')")
    @GetMapping("/company/list")
    public AjaxResult listCompany(InterviewCompanyQuery query) {
        List<InterviewCompanyVO> list = portalInterviewService.selectCompanyList(query);
        return success(list);
    }

    @Operation(summary = "获取公司详情", description = "根据公司ID获取详细信息")
    @PreAuthorize("@ss.hasPermi('cms:interview:query')")
    @GetMapping("/company/{id}")
    public AjaxResult getCompany(@Parameter(description = "公司ID") @PathVariable Long id) {
        return success(portalInterviewService.selectCompanyById(id));
    }

    @Operation(summary = "新增公司", description = "创建新公司标签")
    @PreAuthorize("@ss.hasPermi('cms:interview:add')")
    @Log(title = "公司标签", businessType = BusinessType.INSERT)
    @PostMapping("/company")
    public AjaxResult addCompany(@Validated @RequestBody PortalInterviewCompany company) {
        return toAjax(portalInterviewService.insertCompany(company));
    }

    @Operation(summary = "修改公司", description = "更新公司标签信息")
    @PreAuthorize("@ss.hasPermi('cms:interview:edit')")
    @Log(title = "公司标签", businessType = BusinessType.UPDATE)
    @PutMapping("/company")
    public AjaxResult editCompany(@Validated @RequestBody PortalInterviewCompany company) {
        return toAjax(portalInterviewService.updateCompany(company));
    }

    @Operation(summary = "删除公司", description = "删除公司标签")
    @PreAuthorize("@ss.hasPermi('cms:interview:remove')")
    @Log(title = "公司标签", businessType = BusinessType.DELETE)
    @DeleteMapping("/company/{ids}")
    public AjaxResult removeCompany(@Parameter(description = "公司ID数组") @PathVariable Long[] ids) {
        return toAjax(portalInterviewService.deleteCompanyByIds(ids));
    }

}
