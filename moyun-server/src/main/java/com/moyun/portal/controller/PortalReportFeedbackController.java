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
import com.moyun.system.domain.dto.AuditTaskSubmitDTO;
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

    @Autowired
    @org.springframework.context.annotation.Lazy
    private com.moyun.system.service.IAuditTaskService auditTaskService;

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
        // 清单 P2：图片张数原先只在前端限制（MAX_IMAGES=3），后端 image 字段无任何校验，
        // 直接绕过前端即可提交任意张数（占用存储、放大审核负担）。这里做服务端校验。
        String imagesError = validateEvidenceImages(report.getImages());
        if (imagesError != null) {
            return AjaxResult.error(imagesError);
        }
        report.setUserId(loginUser.getId());
        report.setUsername(loginUser.getUsername());
        report.setIp(getClientIp(request));
        report.setStatus("pending");
        report.setCreateTime(LocalDateTime.now());
        report.setUpdateTime(LocalDateTime.now());
        reportMapper.insert(report);
        // 提交统一审核任务，使首页/审核中心待办可见
        submitAuditTask("report", report.getId(), null,
                report.getDescription(), report.getReportType(),
                loginUser.getId(), loginUser.getUsername(),
                buildReportExtra(report));
        return success("举报提交成功，我们会尽快处理");
    }

    /** 举报/反馈证据图上限（与前端 MAX_IMAGES 一致） */
    private static final int MAX_EVIDENCE_IMAGES = 3;

    /**
     * 校验证据图（清单 P2）。
     *
     * <p>前端限制可被绕过，服务端必须自行校验：最多 {@value #MAX_EVIDENCE_IMAGES} 张，
     * 且每项必须是非空字符串 URL。</p>
     *
     * @param imagesJson 证据图 JSON 数组字符串（可为空）
     * @return 校验失败时返回错误消息；通过时返回 null
     */
    private String validateEvidenceImages(String imagesJson) {
        if (imagesJson == null || imagesJson.trim().isEmpty()) {
            return null;
        }
        try {
            java.util.List<String> images = com.moyun.ext.ai.util.JsonUtils.fromJson(
                    imagesJson, new com.fasterxml.jackson.core.type.TypeReference<java.util.List<String>>() {
                    });
            if (images == null) {
                return null;
            }
            if (images.size() > MAX_EVIDENCE_IMAGES) {
                return "证据图最多上传 " + MAX_EVIDENCE_IMAGES + " 张";
            }
            for (String url : images) {
                if (url == null || url.trim().isEmpty()) {
                    return "证据图地址不合法";
                }
            }
            return null;
        } catch (Exception e) {
            return "证据图格式不合法";
        }
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
        // 提交统一审核任务，使首页/审核中心待办可见
        submitAuditTask("feedback", feedback.getId(), feedback.getSubject(),
                feedback.getDescription(), feedback.getFeedbackType(),
                loginUser.getId(), loginUser.getUsername(), null);
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

    /**
     * 提交统一审核任务到 sys_audit_task。
     * <p>Controller 非事务方法，调用方需自行保证业务 insert 已成功；submit 内部幂等。
     */
    private void submitAuditTask(String taskType, Long bizId, String title,
                                 String description, String bizType,
                                 Long submitterId, String submitterName, String extraData) {
        AuditTaskSubmitDTO dto = new AuditTaskSubmitDTO();
        dto.setTaskType(taskType);
        dto.setBizId(bizId);
        dto.setTitle(title);
        dto.setDescription(description);
        dto.setBizType(bizType);
        dto.setSubmitterId(submitterId);
        dto.setSubmitterName(submitterName);
        dto.setExtraData(extraData);
        // 举报/反馈优先级默认 medium
        dto.setPriority("medium");
        auditTaskService.submit(dto);
    }

    /**
     * 构造举报扩展数据 JSON（举报目标类型/ID/URL，便于审核中心详情展示）
     */
    private String buildReportExtra(PortalReport report) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.Map<String, Object> extra = new java.util.HashMap<>();
            extra.put("targetType", report.getTargetType());
            extra.put("targetId", report.getTargetId());
            extra.put("targetUrl", report.getTargetUrl());
            extra.put("contact", report.getContact());
            return om.writeValueAsString(extra);
        } catch (Exception e) {
            return null;
        }
    }
}
