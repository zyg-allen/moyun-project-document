package com.moyun.system.service.handler;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.moyun.portal.domain.entity.PortalCreatorCertification;
import com.moyun.portal.mapper.PortalCreatorCertificationMapper;
import com.moyun.portal.service.IPortalCreatorCertificationService;
import com.moyun.system.service.AuditBizHandler;

/**
 * 创作者认证审核业务处理器（v8.1）
 * <p>
 * 委托 {@link IPortalCreatorCertificationService#audit} 处理。
 * 注意：通过态是 approved（不是 published），参数顺序 (id, auditorId, status, remark)。
 *
 * @author moyun
 */
@Component
public class CertificationAuditBizHandler implements AuditBizHandler {

    @Autowired
    private IPortalCreatorCertificationService certificationService;
    @Autowired
    private PortalCreatorCertificationMapper certificationMapper;

    @Override
    public String supportedTaskType() {
        return "certification";
    }

    @Override
    public void approve(Long bizId, Long auditorId, String auditorName, String opinion) {
        // 通过态是 approved
        certificationService.audit(bizId, auditorId, "approved", opinion);
    }

    @Override
    public void reject(Long bizId, Long auditorId, String auditorName, String opinion) {
        certificationService.audit(bizId, auditorId, "rejected", opinion);
    }

    @Override
    public Map<String, Object> getBizDetail(Long bizId) {
        PortalCreatorCertification cert = certificationMapper.selectById(bizId);
        if (cert == null) {
            return null;
        }
        Map<String, Object> detail = new HashMap<>();
        detail.put("id", cert.getId());
        detail.put("userId", cert.getUserId());
        detail.put("realName", cert.getRealName());
        // v10.8 实名合规：证件号仅返回脱敏值，密文与明文均不外泄
        if (cert.getCertNoMask() != null && !cert.getCertNoMask().isEmpty()) {
            detail.put("certNo", cert.getCertNoMask());
        } else if (cert.getCertNo() != null && !cert.getCertNo().isEmpty()
                && !com.moyun.util.crypto.AesGcmUtils.isEncrypted(cert.getCertNo())) {
            // 存量明文兼容：运行时脱敏
            detail.put("certNo", com.moyun.util.string.IdCardUtil.mask(cert.getCertNo()));
        } else {
            detail.put("certNo", null);
        }
        detail.put("certType", cert.getCertType());
        detail.put("derivedGender", cert.getDerivedGender());
        detail.put("derivedBirth", cert.getDerivedBirth());
        detail.put("verifyChannel", cert.getVerifyChannel());
        detail.put("certImageFront", cert.getCertImageFront());
        detail.put("certImageBack", cert.getCertImageBack());
        detail.put("status", cert.getStatus());
        detail.put("auditorId", cert.getAuditorId());
        detail.put("auditRemark", cert.getAuditRemark());
        detail.put("auditedTime", cert.getAuditedTime());
        detail.put("createTime", cert.getCreateTime());
        return detail;
    }
}
