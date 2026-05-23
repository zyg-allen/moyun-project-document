package com.moyun.community.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.community.content.entity.CmsAuditRecord;

public interface ICmsAuditService extends IService<CmsAuditRecord> {

    boolean approveArticle(Long articleId, Long auditorId, String reason);

    boolean rejectArticle(Long articleId, Long auditorId, String reason);
}
