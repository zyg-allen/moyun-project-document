package com.moyun.portal.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moyun.common.annotation.Anonymous;
import com.moyun.common.annotation.RepeatSubmit;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.portal.domain.dto.JudgeSubmitDTO;
import com.moyun.portal.domain.dto.TestCaseUpsertDTO;
import com.moyun.portal.domain.vo.JudgeResultVO;
import com.moyun.portal.domain.vo.TestCaseVO;
import com.moyun.portal.service.IPortalJudgeService;
import com.moyun.portal.util.PortalSecurityUtils;

/**
 * OJ 在线判题 Controller（v6.3 OJ 判题系统）
 * <p>
 * 门户端：提交判题、查询判题结果、获取题目样例用例
 *
 * @author moyun
 */
@Tag(name = "OJ 在线判题", description = "在线代码判题与测试用例接口")
@RestController
@RequestMapping("/portal/judge")
public class PortalJudgeController extends BaseController {

    @Autowired
    private IPortalJudgeService portalJudgeService;

    private Long currentUserId() {
        return PortalSecurityUtils.getUserId();
    }

    // ==================== 判题 ====================

    @Operation(summary = "提交代码判题",
            description = "提交代码执行判题。同步场景立即返回完整结果；" +
                    "异步场景（moyun.judge.async-enabled=true）立即返回 submissionId + status=PENDING，" +
                    "前端调用 GET /portal/judge/result/{submissionId} 轮询最终结果（status 变为非 PENDING 即终态）")
    @PostMapping("/submit")
    @RepeatSubmit(interval = 2000, message = "请勿重复提交判题请求")
    public AjaxResult submit(@Valid @RequestBody JudgeSubmitDTO dto) {
        JudgeResultVO vo = portalJudgeService.submitJudge(dto, currentUserId());
        return AjaxResult.success(vo);
    }

    @Operation(summary = "查询判题结果", description = "根据提交记录ID查询判题结果（异步场景轮询入口）")
    @GetMapping("/result/{submissionId}")
    public AjaxResult getResult(@PathVariable Long submissionId) {
        JudgeResultVO vo = portalJudgeService.getJudgeResult(submissionId, currentUserId());
        return AjaxResult.success(vo);
    }

    // ==================== 测试用例（公开接口） ====================

    @Operation(summary = "获取题目样例用例", description = "返回 is_sample=1 的用例，用于详情页展示题意")
    @GetMapping("/cases/sample/{questionId}")
    @Anonymous
    public AjaxResult listSampleCases(@PathVariable Long questionId) {
        List<TestCaseVO> list = portalJudgeService.listSampleCases(questionId);
        return AjaxResult.success(list);
    }

    // ==================== 测试用例（CMS 后台管理） ====================
    // 注：以下接口的鉴权由 Spring Security 全局规则保证（仅管理员可访问），
    // 此处未单独加 @PreAuthorize，与现有 CMS Controller 风格保持一致。

    @Operation(summary = "CMS-获取题目全部用例", description = "后台获取题目全部用例（含隐藏用例）")
    @GetMapping("/admin/cases/{questionId}")
    public AjaxResult listAllCases(@PathVariable Long questionId) {
        List<TestCaseVO> list = portalJudgeService.listAllCases(questionId);
        return AjaxResult.success(list);
    }

    @Operation(summary = "CMS-新增测试用例", description = "为题目新增测试用例")
    @PostMapping("/admin/cases")
    public AjaxResult createCase(@Valid @RequestBody TestCaseUpsertDTO dto) {
        TestCaseVO vo = portalJudgeService.createTestCase(dto);
        return AjaxResult.success(vo);
    }

    @Operation(summary = "CMS-修改测试用例", description = "修改指定测试用例")
    @PutMapping("/admin/cases/{id}")
    public AjaxResult updateCase(@PathVariable Long id, @Valid @RequestBody TestCaseUpsertDTO dto) {
        TestCaseVO vo = portalJudgeService.updateTestCase(id, dto);
        return AjaxResult.success(vo);
    }

    @Operation(summary = "CMS-删除测试用例", description = "删除指定测试用例")
    @DeleteMapping("/admin/cases/{id}")
    public AjaxResult deleteCase(@PathVariable Long id) {
        portalJudgeService.deleteTestCase(id);
        return AjaxResult.success();
    }
}
