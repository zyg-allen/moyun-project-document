package com.moyun.system.service.handler;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.moyun.ext.cms.service.ICmsColumnService;
import com.moyun.portal.domain.entity.PortalColumn;
import com.moyun.portal.mapper.PortalColumnMapper;
import com.moyun.system.service.AuditBizHandler;

/**
 * 专栏审核业务处理器（v8.1）
 * <p>
 * 委托 {@link ICmsColumnService#auditColumn} 处理，业务表通过态 published，驳回态 rejected。
 *
 * @author moyun
 */
@Component
public class ColumnAuditBizHandler implements AuditBizHandler {

    @Autowired
    private ICmsColumnService cmsColumnService;
    @Autowired
    private PortalColumnMapper columnMapper;

    @Override
    public String supportedTaskType() {
        return "column";
    }

    @Override
    public void approve(Long bizId, Long auditorId, String auditorName, String opinion) {
        cmsColumnService.auditColumn(bizId, "published", opinion, auditorId);
    }

    @Override
    public void reject(Long bizId, Long auditorId, String auditorName, String opinion) {
        cmsColumnService.auditColumn(bizId, "rejected", opinion, auditorId);
    }

    @Override
    public Map<String, Object> getBizDetail(Long bizId) {
        PortalColumn c = columnMapper.selectById(bizId);
        if (c == null) {
            return null;
        }
        Map<String, Object> detail = new HashMap<>();
        detail.put("id", c.getId());
        detail.put("title", c.getTitle());
        detail.put("subtitle", c.getSubtitle());
        detail.put("description", c.getDescription());
        detail.put("cover", c.getCover());
        detail.put("status", c.getStatus());
        detail.put("userId", c.getUserId());
        detail.put("categoryId", c.getCategoryId());
        detail.put("auditorId", c.getAuditorId());
        detail.put("auditRemark", c.getAuditRemark());
        detail.put("auditTime", c.getAuditTime());
        return detail;
    }
}
