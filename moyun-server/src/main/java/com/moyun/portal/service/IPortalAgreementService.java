package com.moyun.portal.service;

import java.util.List;

import com.moyun.portal.domain.entity.PortalUserAgreement;

/**
 * 门户协议同意记录 服务层
 *
 * @author moyun
 */
public interface IPortalAgreementService {

    /**
     * 记录一次同意（幂等：同一用户+文档+版本只保留一条）。
     *
     * @param userId   门户用户ID
     * @param version  协议版本
     * @param ip       客户端IP（可空）
     * @param userAgent 客户端UA（可空）
     */
    void recordConsent(Long userId, String version, String ip, String userAgent);

    /**
     * 查询某用户的全部同意记录（按时间倒序）
     */
    List<PortalUserAgreement> listByUser(Long userId);

    /**
     * 是否已同意指定版本（用户协议与隐私政策都需同意）
     */
    boolean hasAgreed(Long userId, String version);
}
