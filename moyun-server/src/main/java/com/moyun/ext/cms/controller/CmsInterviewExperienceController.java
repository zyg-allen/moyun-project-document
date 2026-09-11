package com.moyun.ext.cms.controller;

import java.util.Map;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.util.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ext.cms.domain.query.InterviewCommentQuery;
import com.moyun.ext.cms.domain.query.InterviewExperienceQuery;
import com.moyun.ext.cms.domain.vo.InterviewCommentVO;
import com.moyun.ext.cms.domain.vo.InterviewExperienceVO;
import com.moyun.ext.cms.service.IPortalInterviewService;
import com.moyun.portal.domain.entity.PortalInterviewComment;
import com.moyun.portal.domain.entity.PortalInterviewExperience;
import com.moyun.portal.mapper.PortalInterviewCommentMapper;
import com.moyun.portal.service.IPortalTagService;
import com.moyun.util.bean.PageUtils;

/**
 * CMS面经管理Controller
 * <p>
 * 物理拆分自原 CmsInterviewController，共享类级 @RequestMapping("/cms/interview")，
 * 仅承载面经与评论管理接口，方法级路径与原实现完全一致。
 *
 * @author moyun
 */
@Tag(name = "CMS面经管理", description = "CMS面经管理接口")
@RestController
@RequestMapping("/cms/interview")
public class CmsInterviewExperienceController extends BaseController {

    @Autowired
    private IPortalInterviewService portalInterviewService;

    @Autowired
    private IPortalTagService portalTagService;

    @Autowired
    private PortalInterviewCommentMapper commentMapper;

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

    @Operation(summary = "新增面经", description = "CMS 后台直接创建面经（后台发布默认已发布状态）")
    @PreAuthorize("@ss.hasPermi('cms:interview:add')")
    @Log(title = "面试面经", businessType = BusinessType.INSERT)
    @PostMapping("/experience")
    public AjaxResult addExperience(@RequestBody PortalInterviewExperience experience) {
        Long userId = SecurityUtils.getUserId();
        return toAjax(portalInterviewService.insertExperience(experience, userId));
    }

    @Operation(summary = "修改面经", description = "更新面经内容")
    @PreAuthorize("@ss.hasPermi('cms:interview:edit')")
    @Log(title = "面试面经", businessType = BusinessType.UPDATE)
    @PutMapping("/experience")
    public AjaxResult editExperience(@RequestBody PortalInterviewExperience experience) {
        Long userId = SecurityUtils.getUserId();
        return toAjax(portalInterviewService.updateExperience(experience, userId));
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
        int result = portalInterviewService.deleteExperienceById(id, null);
        // 删除成功后解绑标签
        if (result > 0) {
            portalTagService.unbindTags("interview_experience", id);
        }
        return toAjax(result);
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

    @Operation(summary = "获取评论详情", description = "根据评论ID获取详情（供审核工作台详情区渲染）")
    @PreAuthorize("@ss.hasPermi('cms:interview:query')")
    @GetMapping("/comment/{id}")
    public AjaxResult getComment(@Parameter(description = "评论ID") @PathVariable Long id) {
        PortalInterviewComment comment = commentMapper.selectById(id);
        if (comment == null) {
            return error("评论不存在");
        }
        return success(comment);
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

}
