package com.moyun.community.content.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.community.content.entity.CmsArticle;
import com.moyun.community.content.entity.CmsAuditRecord;
import com.moyun.community.content.mapper.CmsAuditRecordMapper;
import com.moyun.community.content.service.ICmsArticleService;
import com.moyun.community.content.service.ICmsAuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;

@Service
public class CmsAuditServiceImpl extends ServiceImpl<CmsAuditRecordMapper, CmsAuditRecord> implements ICmsAuditService {

    @Autowired
    private ICmsArticleService articleService;

    @Override
    @Transactional
    public boolean approveArticle(Long articleId, Long auditorId, String reason) {
        CmsArticle article = articleService.getById(articleId);
        if (article == null) {
            return false;
        }

        article.setStatus("published");
        article.setPublishTime(LocalDateTime.now());
        article.setAuditTime(LocalDateTime.now());
        article.setAuditReason(reason);
        articleService.updateById(article);

        CmsAuditRecord record = new CmsAuditRecord();
        record.setArticleId(articleId);
        record.setAuditorId(auditorId);
        record.setAuditStatus("approved");
        record.setAuditReason(reason);
        record.setAuditTime(new Date());
        save(record);

        return true;
    }

    @Override
    @Transactional
    public boolean rejectArticle(Long articleId, Long auditorId, String reason) {
        CmsArticle article = articleService.getById(articleId);
        if (article == null) {
            return false;
        }

        article.setStatus("rejected");
        article.setAuditTime(LocalDateTime.now());
        article.setAuditReason(reason);
        articleService.updateById(article);

        CmsAuditRecord record = new CmsAuditRecord();
        record.setArticleId(articleId);
        record.setAuditorId(auditorId);
        record.setAuditStatus("rejected");
        record.setAuditReason(reason);
        record.setAuditTime(new Date());
        save(record);

        return true;
    }
}
