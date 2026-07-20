package com.moyun.portal.service.impl;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.moyun.portal.domain.entity.PortalCreatorCertification;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.mapper.PortalCreatorCertificationMapper;
import com.moyun.portal.mapper.PortalUserMapper;
import com.moyun.portal.service.IPortalCreatorCertificationService;

/**
 * 创作者认证 业务层实现
 *
 * <p>状态机：pending -> approved / rejected。
 * 同一用户存在 pending 申请时拒绝重复提交；已通过认证用户重复申请会被拒绝。</p>
 *
 * @author moyun
 */
@Service
public class PortalCreatorCertificationServiceImpl
        extends ServiceImpl<PortalCreatorCertificationMapper, PortalCreatorCertification>
        implements IPortalCreatorCertificationService {

    @Autowired
    private PortalUserMapper portalUserMapper;

    @Override
    public PortalCreatorCertification apply(Long userId, PortalCreatorCertification dto) {
        // 已通过认证的用户不允许再次申请
        if (baseMapper.countApprovedByUserId(userId) > 0) {
            throw new RuntimeException("您已通过认证，无需重复申请");
        }
        // 拒绝重复提交：同用户已有 pending 申请
        if (baseMapper.countPendingByUserId(userId) > 0) {
            throw new RuntimeException("您已提交认证申请，正在审核中，请勿重复提交");
        }

        PortalCreatorCertification entity = new PortalCreatorCertification();
        entity.setUserId(userId);
        entity.setRealName(dto.getRealName());
        entity.setCertType(dto.getCertType());
        entity.setCertNo(dto.getCertNo());
        entity.setCertImage(dto.getCertImage());
        entity.setIntro(dto.getIntro());
        entity.setWorks(dto.getWorks());
        entity.setStatus("pending");
        entity.setCreatedTime(LocalDateTime.now());
        baseMapper.insert(entity);
        return entity;
    }

    @Override
    public PortalCreatorCertification getMy(Long userId) {
        return baseMapper.selectLatestByUserId(userId);
    }

    @Override
    public PortalCreatorCertification audit(Long id, Long auditorId, String status, String remark) {
        PortalCreatorCertification entity = baseMapper.selectById(id);
        if (entity == null) {
            throw new RuntimeException("认证申请不存在");
        }
        if (!"approved".equals(status) && !"rejected".equals(status)) {
            throw new RuntimeException("审核状态非法，仅支持 approved / rejected");
        }
        if (!"pending".equals(entity.getStatus())) {
            throw new RuntimeException("该申请已审核，不可重复审核");
        }
        entity.setStatus(status);
        entity.setAuditorId(auditorId);
        entity.setAuditRemark(remark);
        entity.setAuditedTime(LocalDateTime.now());
        baseMapper.updateById(entity);
        // 同步 portal_user.is_certified_creator：通过=1，驳回=0
        Integer certified = "approved".equals(status) ? 1 : 0;
        LambdaUpdateWrapper<PortalUser> userUpdate = new LambdaUpdateWrapper<>();
        userUpdate.eq(PortalUser::getId, entity.getUserId())
                .set(PortalUser::getIsCertifiedCreator, certified);
        portalUserMapper.update(null, userUpdate);
        return entity;
    }

    @Override
    public Page<PortalCreatorCertification> list(PortalCreatorCertification query, Page<PortalCreatorCertification> page) {
        LambdaQueryWrapper<PortalCreatorCertification> wrapper = new LambdaQueryWrapper<>();
        if (query.getStatus() != null && !query.getStatus().isEmpty()) {
            wrapper.eq(PortalCreatorCertification::getStatus, query.getStatus());
        }
        if (query.getCertType() != null && !query.getCertType().isEmpty()) {
            wrapper.eq(PortalCreatorCertification::getCertType, query.getCertType());
        }
        if (query.getUserId() != null) {
            wrapper.eq(PortalCreatorCertification::getUserId, query.getUserId());
        }
        if (query.getRealName() != null && !query.getRealName().isEmpty()) {
            wrapper.like(PortalCreatorCertification::getRealName, query.getRealName());
        }
        wrapper.orderByDesc(PortalCreatorCertification::getId);
        return baseMapper.selectPage(page, wrapper);
    }
}
