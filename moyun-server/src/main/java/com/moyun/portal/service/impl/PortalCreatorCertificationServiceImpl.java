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
import com.moyun.system.domain.entity.SysNotification;
import com.moyun.system.service.ISysNotificationService;

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

    @Autowired
    private ISysNotificationService notificationService;

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

        // 业务闭环：发送"待审核"待办通知给所有系统用户 + 被系统用户绑定的前台用户
        // 使用 type=todo 个人通知（scope=user）定向发送，未绑定前台用户不可见
        // data 携带申请 ID，便于前端跳转
        try {
            SysNotification notice = new SysNotification();
            notice.setTitle("新创作者认证申请待审核");
            notice.setContent("用户 " + entity.getRealName() + "（userId=" + userId + "）提交了创作者认证申请，请尽快审核");
            notice.setNoticeType("1");
            notice.setStatus("0");
            notice.setData("{\"bizType\":\"creator_certification\",\"id\":" + entity.getId() + "}");
            notificationService.sendTodoNotification(notice);
        } catch (Exception ignored) {
            // 通知发送失败不应阻断申请提交流程
        }
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

        // 业务闭环：把审核结果通知申请人，让用户在前台消息中心看到反馈
        try {
            SysNotification notice = new SysNotification();
            notice.setType("system");
            notice.setScope("user");
            notice.setUserId(entity.getUserId());
            notice.setUserType("portal");
            notice.setNoticeType("1");
            notice.setStatus("0");
            if ("approved".equals(status)) {
                notice.setTitle("创作者认证已通过");
                notice.setContent("恭喜您，您的创作者认证申请已通过审核，现已获得创作者标识。");
            } else {
                notice.setTitle("创作者认证未通过");
                notice.setContent("您的创作者认证申请未通过审核。" + (remark != null && !remark.isEmpty() ? "原因：" + remark : ""));
            }
            notice.setData("{\"bizType\":\"creator_certification\",\"id\":" + id + ",\"status\":\"" + status + "\"}");
            notificationService.insertNotification(notice);
        } catch (Exception ignored) {
            // 通知发送失败不应阻断审核流程
        }
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
