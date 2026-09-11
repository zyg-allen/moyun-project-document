package com.moyun.ext.cms.controller;

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
import com.moyun.ext.cms.domain.query.InterviewResumeTemplateQuery;
import com.moyun.ext.cms.domain.query.UserResumeQuery;
import com.moyun.ext.cms.domain.vo.InterviewResumeTemplateVO;
import com.moyun.ext.cms.domain.vo.UserResumeVO;
import com.moyun.ext.cms.service.IPortalInterviewService;
import com.moyun.ext.cms.service.IUserResumeService;
import com.moyun.portal.domain.entity.PortalInterviewResumeTemplate;
import com.moyun.portal.service.IPortalTagService;
import com.moyun.util.bean.PageUtils;

/**
 * CMS简历模板管理Controller
 * <p>
 * 物理拆分自原 CmsInterviewController，共享类级 @RequestMapping("/cms/interview")，
 * 仅承载简历模板与用户简历只读查看接口，方法级路径与原实现完全一致。
 *
 * @author moyun
 */
@Tag(name = "CMS简历模板管理", description = "CMS简历模板管理接口")
@RestController
@RequestMapping("/cms/interview")
public class CmsInterviewResumeController extends BaseController {

    @Autowired
    private IPortalInterviewService portalInterviewService;

    @Autowired
    private IPortalTagService portalTagService;

    @Autowired
    private IUserResumeService userResumeService;

    // ========================================================================
    // 简历模板管理
    // ========================================================================

    @Operation(summary = "获取简历模板分页列表", description = "根据条件分页查询简历模板列表")
    @PreAuthorize("@ss.hasPermi('cms:interview:resume:list')")
    @GetMapping("/resume/list")
    public AjaxResult listResume(InterviewResumeTemplateQuery query) {
        Page<InterviewResumeTemplateVO> page = PageUtils.buildPage(query);
        page = portalInterviewService.selectResumeTemplatePage(page, query, null);
        return success(page);
    }

    @Operation(summary = "获取简历模板详情", description = "根据ID获取简历模板详细信息")
    @PreAuthorize("@ss.hasPermi('cms:interview:resume:query')")
    @GetMapping("/resume/{id}")
    public AjaxResult getResume(@Parameter(description = "模板ID") @PathVariable Long id) {
        return success(portalInterviewService.selectResumeTemplateById(id));
    }

    @Operation(summary = "新增简历模板", description = "创建新简历模板")
    @PreAuthorize("@ss.hasPermi('cms:interview:resume:add')")
    @Log(title = "简历模板", businessType = BusinessType.INSERT)
    @PostMapping("/resume")
    public AjaxResult addResume(@Validated @RequestBody PortalInterviewResumeTemplate template) {
        return toAjax(portalInterviewService.insertResumeTemplate(template));
    }

    @Operation(summary = "修改简历模板", description = "更新简历模板信息")
    @PreAuthorize("@ss.hasPermi('cms:interview:resume:edit')")
    @Log(title = "简历模板", businessType = BusinessType.UPDATE)
    @PutMapping("/resume")
    public AjaxResult editResume(@Validated @RequestBody PortalInterviewResumeTemplate template) {
        return toAjax(portalInterviewService.updateResumeTemplate(template));
    }

    @Operation(summary = "删除简历模板", description = "删除简历模板")
    @PreAuthorize("@ss.hasPermi('cms:interview:resume:remove')")
    @Log(title = "简历模板", businessType = BusinessType.DELETE)
    @DeleteMapping("/resume/{ids}")
    public AjaxResult removeResume(@Parameter(description = "模板ID数组") @PathVariable Long[] ids) {
        int result = portalInterviewService.deleteResumeTemplateByIds(ids);
        // 删除成功后解绑标签
        if (result > 0) {
            for (Long id : ids) {
                portalTagService.unbindTags("interview_resume_template", id);
            }
        }
        return toAjax(result);
    }

    // ========================================================================
    // 用户简历管理（Admin 只读查看 + 审计）
    // 独立权限 system:user:resume，与账号管理解耦；复用 IUserResumeService（按 userId 隔离）
    // 设计说明：不在 PortalUserResumeController（用户自服务）加 admin 入口，避免权限混淆；
    //          admin 通过本端点查看指定用户简历，@Log 记录审计日志满足最小必要原则。
    // ========================================================================

    @Operation(summary = "Admin 查看用户简历列表", description = "分页查询指定用户的简历列表（只读，带审计日志）")
    @PreAuthorize("@ss.hasPermi('system:user:resume')")
    @Log(title = "用户简历", businessType = BusinessType.OTHER)
    @GetMapping("/user-resume/{userId}/list")
    public AjaxResult listUserResume(@Parameter(description = "目标用户ID") @PathVariable Long userId,
                                     UserResumeQuery query) {
        Page<UserResumeVO> page = PageUtils.buildPage(query);
        return success(userResumeService.selectMyResumePage(page, userId, query));
    }

    @Operation(summary = "Admin 查看用户简历详情", description = "查看指定用户的简历详情（只读，带审计日志）")
    @PreAuthorize("@ss.hasPermi('system:user:resume')")
    @Log(title = "用户简历", businessType = BusinessType.OTHER)
    @GetMapping("/user-resume/{userId}/{id}")
    public AjaxResult getUserResumeDetail(@Parameter(description = "目标用户ID") @PathVariable Long userId,
                                          @Parameter(description = "简历ID") @PathVariable Long id) {
        UserResumeVO vo = userResumeService.selectResumeDetail(id, userId);
        if (vo == null) {
            return AjaxResult.error("简历不存在或不属于该用户");
        }
        return success(vo);
    }

}
