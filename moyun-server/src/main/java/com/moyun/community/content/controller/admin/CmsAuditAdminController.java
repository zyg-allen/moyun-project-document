package com.moyun.community.content.controller.admin;

import com.moyun.common.annotation.Log;
import com.moyun.common.core.controller.BaseController;
import com.moyun.common.core.domain.AjaxResult;
import com.moyun.common.core.domain.TableDataInfo;
import com.moyun.common.enums.BusinessType;
import com.moyun.common.utils.SecurityUtils;
import com.moyun.community.content.entity.CmsArticle;
import com.moyun.community.content.entity.CmsAuditRecord;
import com.moyun.community.content.service.ICmsArticleService;
import com.moyun.community.content.service.ICmsAuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/community/audit")
public class CmsAuditAdminController extends BaseController {

    @Autowired
    private ICmsArticleService articleService;

    @Autowired
    private ICmsAuditService auditService;

    @PreAuthorize("@ss.hasPermi('content:audit:list')")
    @GetMapping("/list")
    public TableDataInfo list() {
        startPage();
        List<CmsArticle> list = articleService.selectPendingAuditList();
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('content:audit:query')")
    @GetMapping(value = "/{articleId}")
    public AjaxResult getInfo(@PathVariable("articleId") Long articleId) {
        return success(articleService.selectArticleById(articleId));
    }

    @PreAuthorize("@ss.hasPermi('content:audit:approve')")
    @Log(title = "审核管理", businessType = BusinessType.UPDATE)
    @PostMapping("/approve")
    public AjaxResult approve(@RequestBody CmsAuditRecord auditRecord) {
        Long auditorId = SecurityUtils.getUserId();
        return toAjax(auditService.approveArticle(auditRecord.getArticleId(), auditorId, auditRecord.getAuditReason()));
    }

    @PreAuthorize("@ss.hasPermi('content:audit:reject')")
    @Log(title = "审核管理", businessType = BusinessType.UPDATE)
    @PostMapping("/reject")
    public AjaxResult reject(@RequestBody CmsAuditRecord auditRecord) {
        Long auditorId = SecurityUtils.getUserId();
        return toAjax(auditService.rejectArticle(auditRecord.getArticleId(), auditorId, auditRecord.getAuditReason()));
    }
}