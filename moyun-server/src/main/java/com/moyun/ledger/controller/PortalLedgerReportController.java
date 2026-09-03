package com.moyun.ledger.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.ledger.service.ILedgerReportService;
import com.moyun.portal.util.PortalSecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * 门户记账-报表中心 Controller（Phase 3）
 *
 * @author moyun
 */
@RestController
@RequestMapping("/portal/ledger/reports")
public class PortalLedgerReportController {

    @Autowired
    private ILedgerReportService reportService;

    /** 年度报表总览：月度趋势/分类占比/净资产趋势/账户分布/在还负债/当月预算 */
    @GetMapping("/overview")
    public AjaxResult overview(@RequestParam(defaultValue = "2026") int year) {
        Long userId = PortalSecurityUtils.getUserId();
        if (year < 2000 || year > 2100) {
            return AjaxResult.error("年份不合法");
        }
        return AjaxResult.success(reportService.overview(userId, year));
    }

    /** 流水 CSV 导出（UTF-8 BOM，Excel 兼容） */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String startDate,
                                         @RequestParam(required = false) String endDate) {
        Long userId = PortalSecurityUtils.getUserId();
        LocalDate start = startDate != null && !startDate.isEmpty() ? LocalDate.parse(startDate) : null;
        LocalDate end = endDate != null && !endDate.isEmpty() ? LocalDate.parse(endDate) : null;
        String csv = reportService.buildCsv(userId, start, end);
        String filename = "ledger-" + (start != null ? start : "all") + "~" + (end != null ? end : "now") + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + java.net.URLEncoder.encode(filename, StandardCharsets.UTF_8))
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }
}
