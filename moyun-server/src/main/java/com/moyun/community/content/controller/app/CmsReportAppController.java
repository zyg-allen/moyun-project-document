package com.moyun.community.content.controller.app;

import com.moyun.common.core.controller.BaseController;
import com.moyun.common.core.domain.AjaxResult;
import com.moyun.common.utils.SecurityUtils;
import com.moyun.community.content.entity.CmsReport;
import com.moyun.community.content.service.ICmsReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/app/report")
public class CmsReportAppController extends BaseController {

    @Autowired
    private ICmsReportService reportService;

    @PostMapping
    public AjaxResult add(@RequestBody CmsReport report) {
        report.setReportUserId(SecurityUtils.getUserId());
        return toAjax(reportService.addReport(report));
    }
}