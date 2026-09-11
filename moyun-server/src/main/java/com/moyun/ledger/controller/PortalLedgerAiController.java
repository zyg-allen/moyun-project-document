package com.moyun.ledger.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.ledger.service.ILedgerAiAnalysisService;
import com.moyun.portal.util.PortalSecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 门户记账-AI 财务分析控制器
 *
 * @author moyun
 */
@RestController
@RequestMapping("/portal/ledger/ai")
public class PortalLedgerAiController {

    @Autowired
    private ILedgerAiAnalysisService aiAnalysisService;

    /**
     * 生成财务分析报告（规则引擎指标 + LLM 综述）
     * v11.36：当月已有报告直接返回快照（零 token）；?refresh=true 强制重新分析覆盖当月
     * v11.55：页面进入走本接口（快照命中毫秒级返回）；主动"重新分析"改走异步任务接口
     */
    @GetMapping("/analysis")
    public AjaxResult analysis(@RequestParam(value = "refresh", required = false, defaultValue = "false") boolean refresh,
                               @RequestParam(value = "range", required = false, defaultValue = "month") String range) {
        Long userId = PortalSecurityUtils.getUserId();
        return AjaxResult.success(aiAnalysisService.analyze(userId, refresh, range));
    }

    /**
     * 提交异步分析任务（v11.55）：LLM 生成长，立即返回 taskId，前端轮询任务状态。
     * 同用户已有进行中任务时复用（防重复烧 token）。
     */
    @PostMapping("/analysis/task")
    public AjaxResult submitTask(@RequestBody(required = false) Map<String, Object> body) {
        Long userId = PortalSecurityUtils.getUserId();
        String range = body != null && body.get("range") != null ? String.valueOf(body.get("range")) : "month";
        return AjaxResult.success(aiAnalysisService.submitAnalysisTask(userId, range));
    }

    /** 轮询异步任务状态（v11.55）：pending/running/success(带 report)/failed(带 error)/not_found */
    @GetMapping("/analysis/task/{taskId}")
    public AjaxResult getTask(@PathVariable("taskId") String taskId) {
        Long userId = PortalSecurityUtils.getUserId();
        return AjaxResult.success(aiAnalysisService.getAnalysisTask(userId, taskId));
    }

    /** 历史报告分页（v11.36；v11.55 起多版本按生成时间倒序） */
    @GetMapping("/reports")
    public AjaxResult reports(@RequestParam(value = "page", required = false, defaultValue = "1") int page,
                              @RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize) {
        Long userId = PortalSecurityUtils.getUserId();
        return AjaxResult.success(aiAnalysisService.listReports(userId, page, Math.min(pageSize, 50)));
    }

    /** 报告版本详情（v11.55 历史完整回看） */
    @GetMapping("/reports/{id}")
    public AjaxResult reportDetail(@PathVariable("id") Long id) {
        Long userId = PortalSecurityUtils.getUserId();
        return AjaxResult.success(aiAnalysisService.getReportDetail(userId, id));
    }

    /** 删除报告版本（v11.55） */
    @DeleteMapping("/reports/{id}")
    public AjaxResult deleteReport(@PathVariable("id") Long id) {
        Long userId = PortalSecurityUtils.getUserId();
        aiAnalysisService.deleteReport(userId, id);
        return AjaxResult.success();
    }

    /** 获取用户画像（含身份标签字典选项） */
    @GetMapping("/profile")
    public AjaxResult getProfile() {
        Long userId = PortalSecurityUtils.getUserId();
        return AjaxResult.success(aiAnalysisService.getProfile(userId));
    }

    /** 更新用户画像（职位/公司/身份标签，与门户共用 portal_user 表） */
    @PostMapping("/profile")
    public AjaxResult updateProfile(@RequestBody Map<String, String> body) {
        Long userId = PortalSecurityUtils.getUserId();
        return AjaxResult.success(aiAnalysisService.updateProfile(userId, body));
    }
}
