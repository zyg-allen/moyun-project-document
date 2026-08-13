package com.moyun.system.service.handler;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.moyun.ext.cms.service.ICmsArticleService;
import com.moyun.portal.domain.entity.PortalArticle;
import com.moyun.portal.mapper.PortalArticleMapper;
import com.moyun.system.service.AuditBizHandler;

/**
 * 文章审核业务处理器（v8.1）
 * <p>
 * 委托 {@link ICmsArticleService#auditArticle(PortalArticle)} 处理，业务表通过态为 published，驳回态 rejected。
 *
 * @author moyun
 */
@Component
public class ArticleAuditBizHandler implements AuditBizHandler {

    private static final Logger log = LoggerFactory.getLogger(ArticleAuditBizHandler.class);

    @Autowired
    private ICmsArticleService cmsArticleService;
    @Autowired
    private PortalArticleMapper articleMapper;

    @Override
    public String supportedTaskType() {
        return "article";
    }

    @Override
    public void approve(Long bizId, Long auditorId, String auditorName, String opinion) {
        PortalArticle article = new PortalArticle();
        article.setId(bizId);
        article.setStatus("published");
        if (opinion != null && !opinion.isBlank()) {
            article.setAuditRemark(opinion);
        }
        cmsArticleService.auditArticle(article);
        log.info("[AuditHandler] 文章审核通过 bizId={} auditor={}", bizId, auditorName);
    }

    @Override
    public void reject(Long bizId, Long auditorId, String auditorName, String opinion) {
        PortalArticle article = new PortalArticle();
        article.setId(bizId);
        article.setStatus("rejected");
        article.setAuditRemark(opinion);
        cmsArticleService.auditArticle(article);
        log.info("[AuditHandler] 文章审核驳回 bizId={} auditor={} reason={}", bizId, auditorName, opinion);
    }

    @Override
    public Map<String, Object> getBizDetail(Long bizId) {
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
}
