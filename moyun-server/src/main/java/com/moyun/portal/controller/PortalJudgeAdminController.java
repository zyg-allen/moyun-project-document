package com.moyun.portal.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.portal.domain.dto.TestCaseUpsertDTO;
import com.moyun.portal.domain.vo.TestCaseVO;
import com.moyun.portal.service.IPortalJudgeService;

/**
 * OJ 测试用例管理 Controller（v6.3 OJ 判题系统）
 *
 * <p>路径前缀 /portal/admin/ 由核心 SecurityConfig 处理 admin token，
 * 方法级权限通过 @PreAuthorize("@ss.hasPermi(...)") 校验，
 * 权限标识与题库管理（CmsInterviewController）保持一致。</p>
 *
 * <p>迁移说明（2026-08-25）：原路径 /portal/judge/admin/** 被门户安全链
 * （PortalSecurityConfig）处理，仅识别门户用户 token，导致 CMS 后台访问 401
 * "登录状态已过期"。迁移至 /portal/admin/judge/** 后由核心安全链识别 admin token。</p>
 *
 * @author moyun
 */
@Tag(name = "OJ 测试用例管理", description = "CMS 后台测试用例管理接口")
@RestController
@RequestMapping("/portal/admin/judge")
public class PortalJudgeAdminController extends BaseController {

    @Autowired
    private IPortalJudgeService portalJudgeService;

    @Operation(summary = "CMS-获取题目全部用例", description = "后台获取题目全部用例（含隐藏用例）")
    @PreAuthorize("@ss.hasPermi('cms:interview:query')")
    @GetMapping("/cases/{questionId}")
    public AjaxResult listAllCases(@PathVariable Long questionId) {
        List<TestCaseVO> list = portalJudgeService.listAllCases(questionId);
        return AjaxResult.success(list);
    }

    @Operation(summary = "CMS-新增测试用例", description = "为题目新增测试用例")
    @PreAuthorize("@ss.hasPermi('cms:interview:add')")
    @PostMapping("/cases")
    public AjaxResult createCase(@Valid @RequestBody TestCaseUpsertDTO dto) {
        TestCaseVO vo = portalJudgeService.createTestCase(dto);
        return AjaxResult.success(vo);
    }

    @Operation(summary = "CMS-修改测试用例", description = "修改指定测试用例")
    @PreAuthorize("@ss.hasPermi('cms:interview:edit')")
    @PutMapping("/cases/{id}")
    public AjaxResult updateCase(@PathVariable Long id, @Valid @RequestBody TestCaseUpsertDTO dto) {
        TestCaseVO vo = portalJudgeService.updateTestCase(id, dto);
        return AjaxResult.success(vo);
    }

    @Operation(summary = "CMS-删除测试用例", description = "删除指定测试用例")
    @PreAuthorize("@ss.hasPermi('cms:interview:remove')")
    @DeleteMapping("/cases/{id}")
    public AjaxResult deleteCase(@PathVariable Long id) {
        portalJudgeService.deleteTestCase(id);
        return AjaxResult.success();
    }
}
