package com.moyun.portal.service.impl;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moyun.portal.config.CertSecurityProperties;
import com.moyun.portal.domain.entity.PortalCreatorCertification;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.mapper.PortalCreatorCertificationMapper;
import com.moyun.portal.mapper.PortalUserMapper;
import com.moyun.portal.service.IPortalCreatorCertificationService;
import com.moyun.portal.service.realname.RealNameVerifier;
import com.moyun.system.domain.dto.AuditTaskSubmitDTO;
import com.moyun.system.domain.entity.SysNotification;
import com.moyun.system.service.ISysNotificationService;
import com.moyun.util.crypto.AesGcmUtils;
import com.moyun.util.string.IdCardUtil;

/**
 * 创作者认证 业务层实现
 *
 * <p>状态机：pending -> approved / rejected。
 * 同一用户存在 pending 申请时拒绝重复提交；已通过认证用户重复申请会被拒绝。</p>
 *
 * @author moyun
 */
@Slf4j
@Service
public class PortalCreatorCertificationServiceImpl
        extends ServiceImpl<PortalCreatorCertificationMapper, PortalCreatorCertification>
        implements IPortalCreatorCertificationService {

    @Autowired
    private PortalUserMapper portalUserMapper;

    @Autowired
    private ISysNotificationService notificationService;

    @Autowired
    private CertSecurityProperties certSecurityProperties;

    @Autowired
    private RealNameVerifier realNameVerifier;

    @Autowired
    @org.springframework.context.annotation.Lazy
    private com.moyun.system.service.IAuditTaskService auditTaskService;

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
        // v10.8 实名合规：证件号只存密文 + 脱敏展示值，明文一律不落库
        String certNoPlain = dto.getCertNo() == null ? null : dto.getCertNo().trim();
        if (certNoPlain != null && !certNoPlain.isEmpty()) {
            entity.setCertNoEnc(AesGcmUtils.encrypt(certNoPlain, certSecurityProperties.getCertNoEncryptKey()));
            entity.setCertNoMask(IdCardUtil.mask(certNoPlain));
            // 由身份证号推导性别与出生日期，存于认证表供审核/风控使用，不回填公开资料
            if ("identity".equals(dto.getCertType())) {
                LocalDate birth = IdCardUtil.getBirthDate(certNoPlain);
                entity.setDerivedBirth(birth);
                String gender = IdCardUtil.getGender(certNoPlain);
                entity.setDerivedGender("M".equals(gender) ? "男" : "F".equals(gender) ? "女" : null);
            }
        }
        // 实名核验渠道（当前 manual，后期接入第三方 API 时自动切换）
        entity.setVerifyChannel(realNameVerifier.channel());
        entity.setCertImage(dto.getCertImage());
        // 身份证双面照片（人像面 / 国徽面），新增字段，老数据可空
        entity.setCertImageFront(dto.getCertImageFront());
        entity.setCertImageBack(dto.getCertImageBack());
        entity.setIntro(dto.getIntro());
        entity.setWorks(dto.getWorks());
        entity.setStatus("pending");
        entity.setCreatedTime(LocalDateTime.now());
        baseMapper.insert(entity);

        // v8.1：提交统一审核任务（写 sys_audit_task），使首页/审核中心待办可见
        AuditTaskSubmitDTO auditDto = new AuditTaskSubmitDTO();
        auditDto.setTaskType("certification");
        auditDto.setBizId(entity.getId());
        auditDto.setTitle("创作者认证申请-" + entity.getRealName());
        auditDto.setDescription(entity.getIntro());
        auditDto.setSubmitterId(userId);
        if (userId != null) {
            try {
                PortalUser u = portalUserMapper.selectPortalUserById(userId);
                if (u != null) {
                    auditDto.setSubmitterName(u.getUsername());
                }
            } catch (Exception ignored) {
                // submitterName 仅用于展示，查询失败不影响审核任务提交
            }
        }
        auditTaskService.submit(auditDto);

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
        PortalCreatorCertification cert = baseMapper.selectLatestByUserId(userId);
        applyMasking(cert);
        return cert;
    }

    /**
     * 重写 getById：所有按 ID 查询统一脱敏后返回（后台详情、审核等入口共用）
     */
    @Override
    public PortalCreatorCertification getById(Serializable id) {
        PortalCreatorCertification cert = super.getById(id);
        applyMasking(cert);
        return cert;
    }

    /**
     * 统一脱敏：
     * <ul>
     *   <li>新数据：certNo 替换为 certNoMask 脱敏值</li>
     *   <li>存量明文数据：运行时脱敏后返回</li>
     *   <li>certNoEnc 密文永不外泄（实体已 @JsonIgnore，此处双保险置空）</li>
     * </ul>
     */
    private void applyMasking(PortalCreatorCertification cert) {
        if (cert == null) {
            return;
        }
        if (cert.getCertNoMask() != null && !cert.getCertNoMask().isEmpty()) {
            cert.setCertNo(cert.getCertNoMask());
        } else if (cert.getCertNo() != null && !cert.getCertNo().isEmpty()
                && !AesGcmUtils.isEncrypted(cert.getCertNo())) {
            // 存量明文兼容：运行时脱敏
            cert.setCertNo(IdCardUtil.mask(cert.getCertNo()));
        } else {
            cert.setCertNo(null);
        }
        cert.setCertNoEnc(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
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

        // 业务闭环 1：关闭申请时下发的待办通知（type=todo）
        // apply 阶段通过 sendTodoNotification 向所有审核员下发了待办，data 含 bizType+id；
        // 此处按 bizType+id 精确匹配关闭，避免审核完成后待办仍残留在审核员的待办列表中。
        try {
            notificationService.completeTodoByBizData("creator_certification", id);
        } catch (Exception e) {
            log.warn("关闭创作者认证待办失败（不影响审核主流程）：id={}, err={}", id, e.getMessage());
        }

        // 业务闭环 1.5：同步 sys_audit_task 为终态
        // 当审核从旧入口（/cms/creator/certification/{id}/audit）直接发起时，
        // sys_audit_task 不会被 AuditTaskServiceImpl.handle() 更新，导致审核中心仍显示为待办。
        // 此处按 taskType=certification + bizId 精确匹配同步，幂等：仅 pending 可更新。
        try {
            String auditorName = auditorId != null ? String.valueOf(auditorId) : "系统";
            try {
                auditorName = com.moyun.util.security.SecurityUtils.getUsername();
            } catch (Exception ignored) {
                // SecurityContext 不可用时回退为 ID
            }
            auditTaskService.syncTaskStatusByBiz("certification", id, status, auditorId, auditorName, remark);
        } catch (Exception e) {
            log.warn("同步认证审核任务状态失败（不影响审核主流程）：id={}, err={}", id, e.getMessage());
        }

        // 业务闭环 2：把审核结果通知申请人，让用户在前台消息中心看到反馈
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
        applyMasking(entity);
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
        Page<PortalCreatorCertification> result = baseMapper.selectPage(page, wrapper);
        result.getRecords().forEach(this::applyMasking);
        return result;
    }
}
