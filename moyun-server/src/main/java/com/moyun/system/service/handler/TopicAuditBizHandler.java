package com.moyun.system.service.handler;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.moyun.portal.domain.entity.PortalTopic;
import com.moyun.portal.mapper.PortalTopicMapper;
import com.moyun.portal.service.IPortalTopicService;
import com.moyun.system.service.AuditBizHandler;

/**
 * 话题审核业务处理器（v8.1）
 * <p>
 * 委托 {@link IPortalTopicService#auditTopic} 处理。
 * 注意：话题通过态是 {@code active}（不是 published）。
 *
 * @author moyun
 */
@Component
public class TopicAuditBizHandler implements AuditBizHandler {

    @Autowired
    private IPortalTopicService portalTopicService;
    @Autowired
    private PortalTopicMapper topicMapper;

    @Override
    public String supportedTaskType() {
        return "topic";
    }

    @Override
    public void approve(Long bizId, Long auditorId, String auditorName, String opinion) {
        // 话题审核通过态是 active
        portalTopicService.auditTopic(bizId, "active", opinion, auditorId);
    }

    @Override
    public void reject(Long bizId, Long auditorId, String auditorName, String opinion) {
        portalTopicService.auditTopic(bizId, "rejected", opinion, auditorId);
    }

    @Override
    public Map<String, Object> getBizDetail(Long bizId) {
        PortalTopic t = topicMapper.selectById(bizId);
        if (t == null) {
            return null;
        }
        Map<String, Object> detail = new HashMap<>();
        detail.put("id", t.getId());
        detail.put("title", t.getTitle());
        detail.put("description", t.getDescription());
        detail.put("status", t.getStatus());
        detail.put("creatorId", t.getCreatorId());
        detail.put("auditorId", t.getAuditorId());
        detail.put("auditRemark", t.getAuditRemark());
        detail.put("auditTime", t.getAuditTime());
        return detail;
    }
}
