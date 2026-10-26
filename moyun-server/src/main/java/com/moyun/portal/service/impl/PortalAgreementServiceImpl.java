package com.moyun.portal.service.impl;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.portal.domain.entity.PortalUserAgreement;
import com.moyun.portal.mapper.PortalUserAgreementMapper;
import com.moyun.portal.service.IPortalAgreementService;

/**
 * 门户协议同意记录 实现
 *
 * @author moyun
 */
@Service
public class PortalAgreementServiceImpl implements IPortalAgreementService {

    private static final List<String> DOC_TYPES = Arrays.asList("terms", "privacy");

    @Autowired
    private PortalUserAgreementMapper agreementMapper;

    @Override
    public void recordConsent(Long userId, String version, String ip, String userAgent) {
        if (userId == null || version == null || version.trim().isEmpty()) {
            return;
        }
        String v = version.trim();
        for (String docType : DOC_TYPES) {
            Long exists = agreementMapper.selectCount(new LambdaQueryWrapper<PortalUserAgreement>()
                    .eq(PortalUserAgreement::getUserId, userId)
                    .eq(PortalUserAgreement::getDocType, docType)
                    .eq(PortalUserAgreement::getVersion, v));
            if (exists != null && exists > 0) {
                continue;
            }
            PortalUserAgreement row = new PortalUserAgreement();
            row.setUserId(userId);
            row.setDocType(docType);
            row.setVersion(v);
            row.setAgreedAt(LocalDateTime.now());
            row.setIp(ip);
            row.setUserAgent(userAgent != null && userAgent.length() > 255
                    ? userAgent.substring(0, 255) : userAgent);
            agreementMapper.insert(row);
        }
    }

    @Override
    public List<PortalUserAgreement> listByUser(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return agreementMapper.selectList(new LambdaQueryWrapper<PortalUserAgreement>()
                .eq(PortalUserAgreement::getUserId, userId)
                .orderByDesc(PortalUserAgreement::getAgreedAt));
    }

    @Override
    public boolean hasAgreed(Long userId, String version) {
        if (userId == null || version == null || version.trim().isEmpty()) {
            return false;
        }
        Long count = agreementMapper.selectCount(new LambdaQueryWrapper<PortalUserAgreement>()
                .eq(PortalUserAgreement::getUserId, userId)
                .eq(PortalUserAgreement::getVersion, version.trim())
                .in(PortalUserAgreement::getDocType, DOC_TYPES));
        return count != null && count >= DOC_TYPES.size();
    }
}
