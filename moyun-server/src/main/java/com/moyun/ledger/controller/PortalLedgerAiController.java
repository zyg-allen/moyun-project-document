package com.moyun.ledger.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.ledger.service.ILedgerAiAnalysisService;
import com.moyun.portal.util.PortalSecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    /** 生成财务分析报告（规则引擎指标 + LLM 综述） */
    @GetMapping("/analysis")
    public AjaxResult analysis() {
        Long userId = PortalSecurityUtils.getUserId();
        return AjaxResult.success(aiAnalysisService.analyze(userId));
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
