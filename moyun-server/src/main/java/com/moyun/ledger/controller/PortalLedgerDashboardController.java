package com.moyun.ledger.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.ledger.service.ILedgerDashboardService;
import com.moyun.portal.util.PortalSecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 门户记账-总览控制器（首页 dashboard）
 *
 * @author moyun
 */
@RestController
@RequestMapping("/portal/ledger/dashboard")
public class PortalLedgerDashboardController {

    @Autowired
    private ILedgerDashboardService dashboardService;

    /** 首页总览 */
    @GetMapping
    public AjaxResult dashboard() {
        Long userId = PortalSecurityUtils.getUserId();
        return AjaxResult.success(dashboardService.dashboard(userId));
    }
}
