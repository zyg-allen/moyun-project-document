package com.moyun.system.service.handler;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.moyun.ext.cms.service.IPortalInterviewService;
import com.moyun.portal.domain.entity.PortalInterviewComment;
import com.moyun.portal.mapper.PortalInterviewCommentMapper;
import com.moyun.system.service.AuditBizHandler;

/**
 * 面经评论审核业务处理器（v8.1）
 * <p>
 * 委托 {@link IPortalInterviewService#auditComment} 处理。
 *
 * @author moyun
 */
@Component
public class InterviewCommentAuditBizHandler implements AuditBizHandler {

    @Autowired
    private IPortalInterviewService portalInterviewService;
    @Autowired
    private PortalInterviewCommentMapper commentMapper;

    @Override
    public String supportedTaskType() {
        return "interview_comment";
    }

    @Override
    public void approve(Long bizId, Long auditorId, String auditorName, String opinion) {
        portalInterviewService.auditComment(bizId, "published", opinion);
    }

    @Override
    public void reject(Long bizId, Long auditorId, String auditorName, String opinion) {
        portalInterviewService.auditComment(bizId, "rejected", opinion);
    }

    @Override
    public Map<String, Object> getBizDetail(Long bizId) {
        PortalInterviewComment c = commentMapper.selectById(bizId);
        if (c == null) {
            return null;
        }
        Map<String, Object> detail = new HashMap<>();
        detail.put("id", c.getId());
        detail.put("content", c.getContent());
        detail.put("status", c.getStatus());
        detail.put("userId", c.getUserId());
        detail.put("experienceId", c.getExperienceId());
        detail.put("auditorId", c.getAuditorId());
        detail.put("auditRemark", c.getAuditRemark());
        detail.put("auditTime", c.getAuditTime());
        detail.put("createTime", c.getCreateTime());
        return detail;
    }
}
