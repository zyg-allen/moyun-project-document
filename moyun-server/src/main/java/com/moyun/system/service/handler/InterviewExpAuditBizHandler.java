package com.moyun.system.service.handler;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.moyun.ext.cms.service.IPortalInterviewService;
import com.moyun.portal.domain.entity.PortalInterviewExperience;
import com.moyun.portal.mapper.PortalInterviewExperienceMapper;
import com.moyun.system.service.AuditBizHandler;

/**
 * 面经审核业务处理器（v8.1）
 * <p>
 * 委托 {@link IPortalInterviewService#auditExperience} 处理。
 * 注意：原方法无 auditorId 参数，内部用 SecurityUtils 取当前 sys_user.id（审核中心调用时即处理人）。
 *
 * @author moyun
 */
@Component
public class InterviewExpAuditBizHandler implements AuditBizHandler {

    @Autowired
    private IPortalInterviewService portalInterviewService;
    @Autowired
    private PortalInterviewExperienceMapper experienceMapper;

    @Override
    public String supportedTaskType() {
        return "interview_exp";
    }

    @Override
    public void approve(Long bizId, Long auditorId, String auditorName, String opinion) {
        portalInterviewService.auditExperience(bizId, "published", opinion);
    }

    @Override
    public void reject(Long bizId, Long auditorId, String auditorName, String opinion) {
        portalInterviewService.auditExperience(bizId, "rejected", opinion);
    }

    @Override
    public Map<String, Object> getBizDetail(Long bizId) {
        PortalInterviewExperience e = experienceMapper.selectById(bizId);
        if (e == null) {
            return null;
        }
        Map<String, Object> detail = new HashMap<>();
        detail.put("id", e.getId());
        detail.put("title", e.getTitle());
        detail.put("content", e.getContent());
        detail.put("status", e.getStatus());
        detail.put("userId", e.getUserId());
        detail.put("auditorId", e.getAuditorId());
        detail.put("auditRemark", e.getAuditRemark());
        detail.put("auditTime", e.getAuditTime());
        detail.put("createTime", e.getCreateTime());
        return detail;
    }
}
