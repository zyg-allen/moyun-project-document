package com.moyun.ledger.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.ledger.domain.entity.PortalDataAuth;
import com.moyun.ledger.domain.entity.SysDataAccessLog;
import com.moyun.ledger.mapper.PortalDataAuthMapper;
import com.moyun.ledger.mapper.SysDataAccessLogMapper;
import com.moyun.ledger.service.ILedgerDataAuthService;
import com.moyun.util.ip.IpUtils;
import com.moyun.util.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户数据授权服务实现
 *
 * @author moyun
 */
@Slf4j
@Service
public class LedgerDataAuthServiceImpl implements ILedgerDataAuthService {

    @Autowired
    private PortalDataAuthMapper authMapper;

    @Autowired
    private SysDataAccessLogMapper accessLogMapper;

    @Override
    public PortalDataAuth findActive(Long userId) {
        if (userId == null) {
            return null;
        }
        List<PortalDataAuth> list = authMapper.selectList(new LambdaQueryWrapper<PortalDataAuth>()
                .eq(PortalDataAuth::getUserId, userId)
                .eq(PortalDataAuth::getBizDomain, PortalDataAuth.DOMAIN_LEDGER)
                .isNull(PortalDataAuth::getRevokedAt)
                .orderByDesc(PortalDataAuth::getGrantedAt));
        LocalDateTime now = LocalDateTime.now();
        for (PortalDataAuth auth : list) {
            if (auth.isActive(now)) {
                return auth;
            }
        }
        return null;
    }

    @Override
    public Map<Long, PortalDataAuth> findActiveBatch(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<PortalDataAuth> list = authMapper.selectList(new LambdaQueryWrapper<PortalDataAuth>()
                .in(PortalDataAuth::getUserId, userIds)
                .eq(PortalDataAuth::getBizDomain, PortalDataAuth.DOMAIN_LEDGER)
                .isNull(PortalDataAuth::getRevokedAt)
                .orderByDesc(PortalDataAuth::getGrantedAt));
        LocalDateTime now = LocalDateTime.now();
        Map<Long, PortalDataAuth> result = new HashMap<>();
        for (PortalDataAuth auth : list) {
            if (auth.isActive(now) && !result.containsKey(auth.getUserId())) {
                result.put(auth.getUserId(), auth);
            }
        }
        return result;
    }

    @Override
    public boolean isAuthorized(Long userId, String scope) {
        PortalDataAuth auth = findActive(userId);
        return auth != null && auth.covers(scope);
    }

    @Override
    public void recordAccess(Long targetUserId, String scope, String action,
                             String queryDesc, String result, Long authId) {
        SysDataAccessLog row = new SysDataAccessLog();
        row.setOperatorId(SecurityUtils.getUserId());
        row.setOperatorName(SecurityUtils.getUsername());
        row.setTargetUserId(targetUserId);
        row.setBizDomain(PortalDataAuth.DOMAIN_LEDGER);
        row.setScope(scope);
        row.setAuthId(authId);
        row.setAction(action == null ? SysDataAccessLog.ACTION_QUERY : action);
        row.setQueryDesc(trim(queryDesc, 500));
        row.setResult(result);
        row.setClientIp(trim(IpUtils.getIpAddr(), 64));
        row.setAccessTime(LocalDateTime.now());
        accessLogMapper.insert(row);
        log.info("[敏感数据访问] operator={} target={} scope={} action={} result={}",
                row.getOperatorName(), targetUserId, scope, row.getAction(), result);
    }

    @Override
    public String statusOf(PortalDataAuth auth) {
        if (auth == null) {
            return "none";
        }
        if (auth.getRevokedAt() != null) {
            return "revoked";
        }
        if (auth.getExpireAt() != null && !auth.getExpireAt().isAfter(LocalDateTime.now())) {
            return "expired";
        }
        return "valid";
    }

    /** 合法授权范围（与 App 授权页、后台展示保持一致） */
    public static final java.util.List<String> ALLOWED_SCOPES = java.util.List.of(
            PortalDataAuth.SCOPE_TRANSACTION,
            PortalDataAuth.SCOPE_ACCOUNT,
            PortalDataAuth.SCOPE_BUDGET,
            PortalDataAuth.SCOPE_INVENTORY,
            PortalDataAuth.SCOPE_AI,
            PortalDataAuth.SCOPE_PROFILE);

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public PortalDataAuth grant(Long userId, java.util.List<String> scopes, String purpose,
                                Integer hours, String docVersion) {
        if (userId == null) {
            throw new com.moyun.common.exception.system.ServiceException("登录已过期，请重新登录");
        }
        if (scopes == null || scopes.isEmpty()) {
            throw new com.moyun.common.exception.system.ServiceException("请至少选择一个授权范围");
        }
        java.util.List<String> normalized = scopes.stream()
                .filter(java.util.Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
        for (String scope : normalized) {
            if (!ALLOWED_SCOPES.contains(scope)) {
                throw new com.moyun.common.exception.system.ServiceException("授权范围不合法：" + scope);
            }
        }
        String p = purpose == null ? "" : purpose.trim();
        if (p.isEmpty()) {
            throw new com.moyun.common.exception.system.ServiceException("请填写授权用途");
        }
        if (p.length() > 200) {
            throw new com.moyun.common.exception.system.ServiceException("授权用途不能超过 200 字");
        }
        int h = hours == null ? 24 : hours;
        if (h < 1 || h > 168) {
            throw new com.moyun.common.exception.system.ServiceException("有效期需在 1~168 小时之间");
        }

        LocalDateTime now = LocalDateTime.now();
        // 同域只保留一条有效授权：先撤回旧的，再新增（历史可追溯）
        authMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<PortalDataAuth>()
                .eq(PortalDataAuth::getUserId, userId)
                .eq(PortalDataAuth::getBizDomain, PortalDataAuth.DOMAIN_LEDGER)
                .isNull(PortalDataAuth::getRevokedAt)
                .set(PortalDataAuth::getRevokedAt, now)
                .set(PortalDataAuth::getRevokeReason, "重新授权覆盖"));

        PortalDataAuth auth = new PortalDataAuth();
        auth.setUserId(userId);
        auth.setBizDomain(PortalDataAuth.DOMAIN_LEDGER);
        auth.setScopes(String.join(",", normalized));
        auth.setPurpose(p);
        auth.setChannel("app");
        auth.setGrantType("user_initiated");
        auth.setDocVersion(docVersion == null || docVersion.isBlank() ? "v1.0" : docVersion.trim());
        auth.setGrantedAt(now);
        auth.setExpireAt(now.plusHours(h));
        auth.setCreateTime(now);
        authMapper.insert(auth);
        log.info("[数据授权] user={} scopes={} hours={}", userId, auth.getScopes(), h);
        return auth;
    }

    @Override
    public void revoke(Long userId, String reason) {
        PortalDataAuth active = findActive(userId);
        if (active == null) {
            throw new com.moyun.common.exception.system.ServiceException("当前没有可撤回的授权");
        }
        authMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<PortalDataAuth>()
                .eq(PortalDataAuth::getId, active.getId())
                .set(PortalDataAuth::getRevokedAt, LocalDateTime.now())
                .set(PortalDataAuth::getRevokeReason,
                        reason == null || reason.isBlank() ? "用户主动撤回" : reason.trim()));
        log.info("[数据授权] user={} 撤回授权 id={}", userId, active.getId());
    }

    private String trim(String text, int max) {
        if (text == null) {
            return null;
        }
        return text.length() <= max ? text : text.substring(0, max);
    }
}
