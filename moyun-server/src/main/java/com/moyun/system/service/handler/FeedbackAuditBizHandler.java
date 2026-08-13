package com.moyun.system.service.handler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.moyun.portal.domain.entity.PortalFeedback;
import com.moyun.portal.mapper.PortalFeedbackMapper;
import com.moyun.system.service.AuditBizHandler;

/**
 * 意见反馈处理业务处理器（v8.1）
 * <p>
 * 反馈无独立 Service 层，直接通过 Mapper 更新业务表。
 * 通过态：status=resolved（已解决）；驳回态：status=rejected。
 *
 * @author moyun
 */
@Component
public class FeedbackAuditBizHandler implements AuditBizHandler {

    private static final Logger log = LoggerFactory.getLogger(FeedbackAuditBizHandler.class);

    @Autowired
    private PortalFeedbackMapper feedbackMapper;

    @Override
    public String supportedTaskType() {
        return "feedback";
    }

    @Override
    public void approve(Long bizId, Long auditorId, String auditorName, String opinion) {
        LambdaUpdateWrapper<PortalFeedback> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(PortalFeedback::getId, bizId)
                .set(PortalFeedback::getStatus, "resolved")
                .set(PortalFeedback::getHandleResult, opinion)
                .set(PortalFeedback::getHandler, auditorName)
                .set(PortalFeedback::getHandleTime, LocalDateTime.now())
                .set(PortalFeedback::getUpdateTime, LocalDateTime.now());
        feedbackMapper.update(null, wrapper);
        log.info("[AuditHandler] 反馈处理完成(已解决) bizId={} auditor={}", bizId, auditorName);
    }

    @Override
    public void reject(Long bizId, Long auditorId, String auditorName, String opinion) {
        LambdaUpdateWrapper<PortalFeedback> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(PortalFeedback::getId, bizId)
                .set(PortalFeedback::getStatus, "rejected")
                .set(PortalFeedback::getHandleResult, opinion)
                .set(PortalFeedback::getHandler, auditorName)
                .set(PortalFeedback::getHandleTime, LocalDateTime.now())
                .set(PortalFeedback::getUpdateTime, LocalDateTime.now());
        feedbackMapper.update(null, wrapper);
        log.info("[AuditHandler] 反馈已驳回 bizId={} auditor={} reason={}", bizId, auditorName, opinion);
    }

    @Override
    public Map<String, Object> getBizDetail(Long bizId) {
        PortalFeedback f = feedbackMapper.selectById(bizId);
        if (f == null) {
            return null;
        }
        Map<String, Object> detail = new HashMap<>();
        detail.put("id", f.getId());
        detail.put("feedbackType", f.getFeedbackType());
        detail.put("subject", f.getSubject());
        detail.put("description", f.getDescription());
        detail.put("contact", f.getContact());
        detail.put("userId", f.getUserId());
        detail.put("username", f.getUsername());
        detail.put("status", f.getStatus());
        detail.put("handler", f.getHandler());
        detail.put("handleResult", f.getHandleResult());
        detail.put("handleTime", f.getHandleTime());
        detail.put("createTime", f.getCreateTime());
        return detail;
    }
}
