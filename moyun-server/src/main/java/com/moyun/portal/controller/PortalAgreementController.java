package com.moyun.portal.controller;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moyun.core.base.AjaxResult;
import com.moyun.portal.service.IPortalAgreementService;
import com.moyun.portal.util.PortalSecurityUtils;

/**
 * 门户用户协议同意记录（与门户账号体系共用）
 *
 * - GET  /portal/user/agreement/list    当前用户的同意记录
 * - POST /portal/user/agreement/consent 记录同意（登录后补签/换版本重签）
 *
 * @author moyun
 */
@RestController
@RequestMapping("/portal/user/agreement")
public class PortalAgreementController {

    @Autowired
    private IPortalAgreementService agreementService;

    @GetMapping("/list")
    public AjaxResult list() {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error("未登录");
        }
        return AjaxResult.success(agreementService.listByUser(userId));
    }

    @PostMapping("/consent")
    public AjaxResult consent(@RequestBody Map<String, String> body, HttpServletRequest request) {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error("未登录");
        }
        String version = body == null ? null : body.get("version");
        if (version == null || version.trim().isEmpty()) {
            return AjaxResult.error("协议版本不能为空");
        }
        agreementService.recordConsent(userId, version.trim(), clientIp(request), request.getHeader("User-Agent"));
        return AjaxResult.success();
    }

    private String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            int idx = ip.indexOf(',');
            return idx > 0 ? ip.substring(0, idx).trim() : ip.trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }
}
