package com.moyun.community.content.controller.admin;

import com.moyun.common.annotation.Log;
import com.moyun.common.core.controller.BaseController;
import com.moyun.common.core.domain.AjaxResult;
import com.moyun.common.core.domain.TableDataInfo;
import com.moyun.common.enums.BusinessType;
import com.moyun.common.utils.SecurityUtils;
import com.moyun.community.content.entity.CmsReport;
import com.moyun.community.content.service.ICmsReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/report")
public class CmsReportAdminController extends BaseController {

    @Autowired
    private ICmsReportService reportService;

    @PreAuthorize("@ss.hasPermi('content:report:list')")
    @GetMapping("/list")
    public TableDataInfo list() {
        startPage();
        List<CmsReport> list = reportService.list();
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('content:report:query')")
    @GetMapping(value = "/{reportId}")
    public AjaxResult getInfo(@PathVariable("reportId") Long reportId) {
        return success(reportService.getById(reportId));
    }

    @PreAuthorize("@ss.hasPermi('content:report:handle')")
    @Log(title = "举报管理", businessType = BusinessType.UPDATE)
    @PutMapping("/handle/{reportId}")
    public AjaxResult handle(@PathVariable Long reportId,
                            @RequestParam String status,
                            @RequestParam(required = false) String handleResult) {
        Long handlerId = SecurityUtils.getUserId();
        return toAjax(reportService.handleReport(reportId, status, handleResult, handlerId));
    }

    @PreAuthorize("@ss.hasPermi('content:report:remove')")
    @Log(title = "举报管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{reportIds}")
    public AjaxResult remove(@PathVariable Long[] reportIds) {
        return toAjax(reportService.removeByIds(List.of(reportIds)));
    }
}