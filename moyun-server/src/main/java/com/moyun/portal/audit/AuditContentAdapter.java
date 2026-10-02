package com.moyun.portal.audit;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.moyun.core.portal.AuditContentPort;
import com.moyun.ext.cms.service.ICmsArticleService;
import com.moyun.ext.cms.service.ICmsColumnService;
import com.moyun.ext.cms.service.IPortalInterviewService;
import com.moyun.ext.cms.service.IReportTakedownService;
import com.moyun.portal.domain.entity.PortalArticle;
import com.moyun.portal.domain.entity.PortalColumn;
import com.moyun.portal.domain.entity.PortalCreatorCertification;
import com.moyun.portal.domain.entity.PortalFeedback;
import com.moyun.portal.domain.entity.PortalInterviewComment;
import com.moyun.portal.domain.entity.PortalInterviewExperience;
import com.moyun.portal.domain.entity.PortalReport;
import com.moyun.portal.domain.entity.PortalTopic;
import com.moyun.portal.mapper.PortalArticleMapper;
import com.moyun.portal.mapper.PortalColumnMapper;
import com.moyun.portal.mapper.PortalCreatorCertificationMapper;
import com.moyun.portal.mapper.PortalFeedbackMapper;
import com.moyun.portal.mapper.PortalInterviewCommentMapper;
import com.moyun.portal.mapper.PortalInterviewExperienceMapper;
import com.moyun.portal.mapper.PortalReportMapper;
import com.moyun.portal.mapper.PortalTopicMapper;
import com.moyun.portal.service.IPortalCreatorCertificationService;
import com.moyun.portal.service.IPortalTopicService;
import com.moyun.util.crypto.AesGcmUtils;
import com.moyun.util.string.IdCardUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 审核业务内容端口实现（门户侧适配器）
 *
 * <p>{@link AuditContentPort} 的落地实现：门户 Mapper 与门户/CMS 服务的调用集中到这一个类，
 * 管理端因此不再依赖门户数据层。</p>
 *
 * <p><b>行为保持</b>：各 taskType 的详情字段与状态取值、日志文案均与迁移前逐个 handler 一致
 * （逐条对照搬运，未做语义调整）。</p>
 *
 * <p><b>模块方向</b>：本类位于门户侧，因此 {@code portal -> ext.cms} 增加 4 条边
 * （文章/专栏/面试/下架服务），属 ACL 的合理代价，已在 {@code ModuleDependencyGuardTest} 登记理由。</p>
 *
 * @author moyun
 */
@Slf4j
@Component
public class AuditContentAdapter implements AuditContentPort {

    @Autowired
    private PortalArticleMapper articleMapper;
    @Autowired
    private PortalColumnMapper columnMapper;
    @Autowired
    private PortalCreatorCertificationMapper certificationMapper;
    @Autowired
    private PortalFeedbackMapper feedbackMapper;
    @Autowired
    private PortalInterviewCommentMapper interviewCommentMapper;
    @Autowired
    private PortalInterviewExperienceMapper interviewExperienceMapper;
    @Autowired
    private PortalReportMapper reportMapper;
    @Autowired
    private PortalTopicMapper topicMapper;

    @Autowired
    private ICmsArticleService cmsArticleService;
    @Autowired
    private ICmsColumnService cmsColumnService;
    @Autowired
    private IPortalInterviewService portalInterviewService;
    @Autowired
    private IPortalCreatorCertificationService certificationService;
    @Autowired
    private IPortalTopicService portalTopicService;
    @Autowired
    private IReportTakedownService reportTakedownService;

    @Override
    public Map<String, Object> loadDetail(String taskType, Long bizId) {
        return switch (taskType == null ? "" : taskType) {
            case "article" -> articleDetail(bizId);
            case "column" -> columnDetail(bizId);
            case "certification" -> certificationDetail(bizId);
            case "feedback" -> feedbackDetail(bizId);
            case "interview_comment" -> interviewCommentDetail(bizId);
            case "interview_exp" -> interviewExpDetail(bizId);
            case "report" -> reportDetail(bizId);
            case "topic" -> topicDetail(bizId);
            default -> {
                log.warn("[AuditAdapter] 未知审核任务类型，无详情可加载: taskType={} bizId={}", taskType, bizId);
                yield null;
            }
        };
    }

    @Override
    public void applyAudit(String taskType, Long bizId, boolean approved,
                           Long auditorId, String auditorName, String opinion) {
        switch (taskType == null ? "" : taskType) {
            case "article" -> applyArticle(bizId, approved, opinion, auditorName);
            case "column" -> cmsColumnService.auditColumn(bizId, approved ? "published" : "rejected",
                    opinion, auditorId);
            case "certification" -> certificationService.audit(bizId, auditorId,
                    approved ? "approved" : "rejected", opinion);
            case "feedback" -> applyFeedback(bizId, approved, opinion, auditorName);
            case "interview_comment" -> portalInterviewService.auditComment(bizId,
                    approved ? "published" : "rejected", opinion);
            case "interview_exp" -> portalInterviewService.auditExperience(bizId,
                    approved ? "published" : "rejected", opinion);
            case "report" -> applyReport(bizId, approved, opinion, auditorName);
            case "topic" -> portalTopicService.auditTopic(bizId, approved ? "active" : "rejected",
                    opinion, auditorId);
            default -> log.warn("[AuditAdapter] 未知审核任务类型，审核结论未落地: taskType={} bizId={}",
                    taskType, bizId);
        }
    }

    // ==================== 详情（字段与迁移前逐条一致） ====================

    private Map<String, Object> articleDetail(Long bizId) {
        PortalArticle a = articleMapper.selectById(bizId);
        if (a == null) {
            return null;
        }
        Map<String, Object> detail = new HashMap<>();
        detail.put("id", a.getId());
        detail.put("title", a.getTitle());
        detail.put("excerpt", a.getExcerpt());
        detail.put("cover", a.getCover());
        detail.put("content", a.getContent());
        detail.put("status", a.getStatus());
        detail.put("categoryId", a.getCategoryId());
        detail.put("auditorId", a.getAuditorId());
        detail.put("auditRemark", a.getAuditRemark());
        detail.put("auditTime", a.getAuditTime());
        detail.put("createTime", a.getCreateTime());
        return detail;
    }

    private Map<String, Object> columnDetail(Long bizId) {
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

    private Map<String, Object> certificationDetail(Long bizId) {
        PortalCreatorCertification cert = certificationMapper.selectById(bizId);
        if (cert == null) {
            return null;
        }
        Map<String, Object> detail = new HashMap<>();
        detail.put("id", cert.getId());
        detail.put("userId", cert.getUserId());
        detail.put("realName", cert.getRealName());
        // 证件号：优先取脱敏列；否则在"非密文"时现场脱敏；密文一律不回显
        if (cert.getCertNoMask() != null && !cert.getCertNoMask().isEmpty()) {
            detail.put("certNo", cert.getCertNoMask());
        } else if (cert.getCertNo() != null && !cert.getCertNo().isEmpty()
                && !AesGcmUtils.isEncrypted(cert.getCertNo())) {
            detail.put("certNo", IdCardUtil.mask(cert.getCertNo()));
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

    private Map<String, Object> feedbackDetail(Long bizId) {
        PortalFeedback f = feedbackMapper.selectById(bizId);
        if (f == null) {
            return null;
        }
        Map<String, Object> detail = new HashMap<>();
        detail.put("id", f.getId());
        detail.put("feedbackType", f.getFeedbackType());
        detail.put("subject", f.getSubject());
        detail.put("description", f.getDescription());
        detail.put("contact", f.getContact());
        detail.put("userId", f.getUserId());
        detail.put("username", f.getUsername());
        detail.put("status", f.getStatus());
        detail.put("handler", f.getHandler());
        detail.put("handleResult", f.getHandleResult());
        detail.put("handleTime", f.getHandleTime());
        detail.put("createTime", f.getCreateTime());
        return detail;
    }

    private Map<String, Object> interviewCommentDetail(Long bizId) {
        PortalInterviewComment c = interviewCommentMapper.selectById(bizId);
        if (c == null) {
            return null;
        }
        Map<String, Object> detail = new HashMap<>();
        detail.put("id", c.getId());
        detail.put("content", c.getContent());
        detail.put("status", c.getStatus());
        detail.put("userId", c.getUserId());
        detail.put("experienceId", c.getExperienceId());
        detail.put("auditorId", c.getAuditorId());
        detail.put("auditRemark", c.getAuditRemark());
        detail.put("auditTime", c.getAuditTime());
        detail.put("createTime", c.getCreateTime());
        return detail;
    }

    private Map<String, Object> interviewExpDetail(Long bizId) {
        PortalInterviewExperience e = interviewExperienceMapper.selectById(bizId);
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

    private Map<String, Object> reportDetail(Long bizId) {
        PortalReport r = reportMapper.selectById(bizId);
        if (r == null) {
            return null;
        }
        Map<String, Object> detail = new HashMap<>();
        detail.put("id", r.getId());
        detail.put("reportType", r.getReportType());
        detail.put("targetType", r.getTargetType());
        detail.put("targetId", r.getTargetId());
        detail.put("targetUrl", r.getTargetUrl());
        detail.put("description", r.getDescription());
        detail.put("contact", r.getContact());
        detail.put("images", r.getImages());
        detail.put("userId", r.getUserId());
        detail.put("username", r.getUsername());
        detail.put("status", r.getStatus());
        detail.put("handler", r.getHandler());
        detail.put("handleResult", r.getHandleResult());
        detail.put("handleTime", r.getHandleTime());
        detail.put("createTime", r.getCreateTime());
        return detail;
    }

    private Map<String, Object> topicDetail(Long bizId) {
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

    // ==================== 落地（状态取值与迁移前一致） ====================

    private void applyArticle(Long bizId, boolean approved, String opinion, String auditorName) {
        PortalArticle article = new PortalArticle();
        article.setId(bizId);
        article.setStatus(approved ? "published" : "rejected");
        if (approved) {
            if (opinion != null && !opinion.isBlank()) {
                article.setAuditRemark(opinion);
            }
        } else {
            article.setAuditRemark(opinion);
        }
        cmsArticleService.auditArticle(article);
        if (approved) {
            log.info("[AuditHandler] 文章审核通过 bizId={} auditor={}", bizId, auditorName);
        } else {
            log.info("[AuditHandler] 文章审核驳回 bizId={} auditor={} reason={}", bizId, auditorName, opinion);
        }
    }

    private void applyFeedback(Long bizId, boolean approved, String opinion, String auditorName) {
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<PortalFeedback> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(PortalFeedback::getId, bizId)
                .set(PortalFeedback::getStatus, approved ? "resolved" : "rejected")
                .set(PortalFeedback::getHandleResult, opinion)
                .set(PortalFeedback::getHandler, auditorName)
                .set(PortalFeedback::getHandleTime, now)
                .set(PortalFeedback::getUpdateTime, now);
        feedbackMapper.update(null, wrapper);
        if (approved) {
            log.info("[AuditHandler] 反馈处理完成(已解决) bizId={} auditor={}", bizId, auditorName);
        } else {
            log.info("[AuditHandler] 反馈已驳回 bizId={} auditor={} reason={}", bizId, auditorName, opinion);
        }
    }

    private void applyReport(Long bizId, boolean approved, String opinion, String auditorName) {
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<PortalReport> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(PortalReport::getId, bizId)
                .set(PortalReport::getStatus, approved ? "resolved" : "rejected")
                .set(PortalReport::getHandleResult, opinion)
                .set(PortalReport::getHandler, auditorName)
                .set(PortalReport::getHandleTime, now)
                .set(PortalReport::getUpdateTime, now);
        reportMapper.update(null, wrapper);

        if (approved) {
            // 举报成立 → 联动下架被举报对象（迁移前在 handler 内，行为保持一致）
            PortalReport full = reportMapper.selectById(bizId);
            if (full != null && full.getTargetType() != null && !full.getTargetType().isEmpty()
                    && full.getTargetId() != null) {
                boolean takenDown = reportTakedownService.takedown(
                        full.getTargetType(), full.getTargetId(), auditorName);
                log.info("[AuditHandler] 举报联动下架 bizId={} targetType={} targetId={} result={}",
                        bizId, full.getTargetType(), full.getTargetId(), takenDown);
            }
            log.info("[AuditHandler] 举报处理完成(成立) bizId={} auditor={}", bizId, auditorName);
        } else {
            log.info("[AuditHandler] 举报已驳回(不成立) bizId={} auditor={} reason={}",
                    bizId, auditorName, opinion);
        }
    }
}
