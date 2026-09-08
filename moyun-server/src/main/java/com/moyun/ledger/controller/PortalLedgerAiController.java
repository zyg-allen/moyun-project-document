package com.moyun.ledger.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.ledger.service.ILedgerAiAnalysisService;
import com.moyun.portal.util.PortalSecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
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
     */
    @GetMapping("/analysis")
    public AjaxResult analysis(@RequestParam(value = "refresh", required = false, defaultValue = "false") boolean refresh,
                               @RequestParam(value = "range", required = false, defaultValue = "month") String range) {
        Long userId = PortalSecurityUtils.getUserId();
        return AjaxResult.success(aiAnalysisService.analyze(userId, refresh, range));
    }

    /** 历史报告分页（v11.36，月度快照列表） */
    @GetMapping("/reports")
    public AjaxResult reports(@RequestParam(value = "page", required = false, defaultValue = "1") int page,
                              @RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize) {
        Long userId = PortalSecurityUtils.getUserId();
        return AjaxResult.success(aiAnalysisService.listReports(userId, page, Math.min(pageSize, 50)));
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
