package com.moyun.system.service.handler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.moyun.ext.cms.service.IReportTakedownService;
import com.moyun.portal.domain.entity.PortalReport;
import com.moyun.portal.mapper.PortalReportMapper;
import com.moyun.system.service.AuditBizHandler;

/**
 * 举报处理业务处理器（v8.1）
 * <p>
 * 举报无独立 Service 层，直接通过 Mapper 更新业务表。
 * 通过态（举报成立）：status=resolved，并联动 {@link IReportTakedownService#takedown} 下架被举报内容；
 * 驳回态：status=rejected（举报不成立，不下架）。
 *
 * @author moyun
 */
@Component
public class ReportAuditBizHandler implements AuditBizHandler {

    private static final Logger log = LoggerFactory.getLogger(ReportAuditBizHandler.class);

    @Autowired
    private PortalReportMapper reportMapper;

    @Autowired
    private IReportTakedownService reportTakedownService;

    @Override
    public String supportedTaskType() {
        return "report";
    }

    @Override
    public void approve(Long bizId, Long auditorId, String auditorName, String opinion) {
        LambdaUpdateWrapper<PortalReport> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(PortalReport::getId, bizId)
                .set(PortalReport::getStatus, "resolved")
                .set(PortalReport::getHandleResult, opinion)
                .set(PortalReport::getHandler, auditorName)
                .set(PortalReport::getHandleTime, LocalDateTime.now())
                .set(PortalReport::getUpdateTime, LocalDateTime.now());
        reportMapper.update(null, wrapper);

        // 举报成立：联动下架被举报内容
        PortalReport full = reportMapper.selectById(bizId);
        if (full != null && full.getTargetType() != null && !full.getTargetType().isEmpty()
                && full.getTargetId() != null) {
            boolean takenDown = reportTakedownService.takedown(
                    full.getTargetType(), full.getTargetId(), auditorName);
            log.info("[AuditHandler] 举报联动下架 bizId={} targetType={} targetId={} result={}",
                    bizId, full.getTargetType(), full.getTargetId(), takenDown);
        }
        log.info("[AuditHandler] 举报处理完成(成立) bizId={} auditor={}", bizId, auditorName);
    }

    @Override
    public void reject(Long bizId, Long auditorId, String auditorName, String opinion) {
        LambdaUpdateWrapper<PortalReport> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(PortalReport::getId, bizId)
                .set(PortalReport::getStatus, "rejected")
                .set(PortalReport::getHandleResult, opinion)
                .set(PortalReport::getHandler, auditorName)
                .set(PortalReport::getHandleTime, LocalDateTime.now())
                .set(PortalReport::getUpdateTime, LocalDateTime.now());
        reportMapper.update(null, wrapper);
        log.info("[AuditHandler] 举报已驳回(不成立) bizId={} auditor={} reason={}", bizId, auditorName, opinion);
    }

    @Override
    public Map<String, Object> getBizDetail(Long bizId) {
        PortalReport r = reportMapper.selectById(bizId);
        if (r == null) {
            return null;
        }
        Map<String, Object> detail = new HashMap<>();
        detail.put("id", r.getId());
        detail.put("reportType", r.getReportType());
        detail.put("targetType", r.getTargetType());
        detail.put("targetId", r.getTargetId());
        detail.put("targetUrl", r.getTargetUrl());
        detail.put("description", r.getDescription());
        detail.put("contact", r.getContact());
        detail.put("images", r.getImages());
        detail.put("userId", r.getUserId());
        detail.put("username", r.getUsername());
        detail.put("status", r.getStatus());
        detail.put("handler", r.getHandler());
        detail.put("handleResult", r.getHandleResult());
        detail.put("handleTime", r.getHandleTime());
        detail.put("createTime", r.getCreateTime());
        return detail;
    }
}
