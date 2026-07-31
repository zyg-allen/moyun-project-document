package com.moyun.portal.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.constant.HttpStatus;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.portal.domain.entity.PortalFeedback;
import com.moyun.portal.domain.entity.PortalReport;
import com.moyun.portal.domain.model.PortalLoginUser;
import com.moyun.portal.mapper.PortalFeedbackMapper;
import com.moyun.portal.mapper.PortalReportMapper;
import com.moyun.portal.util.PortalSecurityUtils;
import com.moyun.util.bean.PageUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 门户举报与反馈 Controller（前台用户提交）
 *
 * 路径规划：
 *   - /portal/report/submit    用户提交举报
 *   - /portal/feedback/submit  用户提交反馈
 *
 * 安全说明：
 *   - 两个接口都需要登录（通过 PortalJwtAuthenticationTokenFilter 鉴权）
 *   - 未登录用户无法提交，会返回 401
 *
 * @author moyun
 */
@Tag(name = "门户举报与反馈", description = "前台用户提交举报和反馈")
@RestController
public class PortalReportFeedbackController extends BaseController {

    @Autowired
    private PortalReportMapper reportMapper;

    @Autowired
    private PortalFeedbackMapper feedbackMapper;

    /**
     * 提交举报
     */
    @Operation(summary = "提交举报", description = "前台用户提交内容举报")
    @PostMapping("/portal/report/submit")
    public AjaxResult submitReport(@Validated @RequestBody PortalReport report, HttpServletRequest request) {
        PortalLoginUser loginUser = PortalSecurityUtils.getLoginUser();
        if (loginUser == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "请先登录后再提交举报");
        }
        report.setUserId(loginUser.getId());
        report.setUsername(loginUser.getUsername());
        report.setIp(getClientIp(request));
        report.setStatus("pending");
        report.setCreateTime(LocalDateTime.now());
        report.setUpdateTime(LocalDateTime.now());
        reportMapper.insert(report);
        return success("举报提交成功，我们会尽快处理");
    }

    /**
     * 提交反馈
     */
    @Operation(summary = "提交反馈", description = "前台用户提交意见反馈")
    @PostMapping("/portal/feedback/submit")
    public AjaxResult submitFeedback(@Validated @RequestBody PortalFeedback feedback, HttpServletRequest request) {
        PortalLoginUser loginUser = PortalSecurityUtils.getLoginUser();
        if (loginUser == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "请先登录后再提交反馈");
        }
        feedback.setUserId(loginUser.getId());
        feedback.setUsername(loginUser.getUsername());
        feedback.setIp(getClientIp(request));
        feedback.setStatus("pending");
        feedback.setCreateTime(LocalDateTime.now());
        feedback.setUpdateTime(LocalDateTime.now());
        feedbackMapper.insert(feedback);
        return success("反馈提交成功，感谢您的支持");
    }

    /**
     * 查询当前用户的举报列表（分页，含处理进度）
     */
    @Operation(summary = "我的举报列表", description = "分页查询当前登录用户提交的举报记录，含处理状态进度")
    @GetMapping("/portal/report/my-list")
    public AjaxResult myReportList(PortalReport query) {
        PortalLoginUser loginUser = PortalSecurityUtils.getLoginUser();
        if (loginUser == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        Page<PortalReport> page = PageUtils.startPage();
        LambdaQueryWrapper<PortalReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PortalReport::getUserId, loginUser.getId())
                .eq(query.getStatus() != null && !query.getStatus().isEmpty(),
                        PortalReport::getStatus, query.getStatus())
                .eq(query.getReportType() != null && !query.getReportType().isEmpty(),
                        PortalReport::getReportType, query.getReportType())
                .orderByDesc(PortalReport::getCreateTime);
        Page<PortalReport> result = reportMapper.selectPage(page, wrapper);
        return success(result);
    }

    /**
     * 查询当前用户的反馈列表（分页，含处理进度）
     */
    @Operation(summary = "我的反馈列表", description = "分页查询当前登录用户提交的反馈记录，含处理状态进度")
    @GetMapping("/portal/feedback/my-list")
    public AjaxResult myFeedbackList(PortalFeedback query) {
        PortalLoginUser loginUser = PortalSecurityUtils.getLoginUser();
        if (loginUser == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        Page<PortalFeedback> page = PageUtils.startPage();
        LambdaQueryWrapper<PortalFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PortalFeedback::getUserId, loginUser.getId())
                .eq(query.getStatus() != null && !query.getStatus().isEmpty(),
                        PortalFeedback::getStatus, query.getStatus())
                .eq(query.getFeedbackType() != null && !query.getFeedbackType().isEmpty(),
                        PortalFeedback::getFeedbackType, query.getFeedbackType())
                .orderByDesc(PortalFeedback::getCreateTime);
        Page<PortalFeedback> result = feedbackMapper.selectPage(page, wrapper);
        return success(result);
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
