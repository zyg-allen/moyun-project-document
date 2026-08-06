package com.moyun.ext.ai.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.ext.ai.service.TokenUsageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/cms/ai/token-usage")
@RequiredArgsConstructor
public class TokenUsageController {

    private final TokenUsageService tokenUsageService;

    @GetMapping("/today")
    @PreAuthorize("@ss.hasPermi('cms:ai:token-usage:list')")
    public AjaxResult getTodayStats() {
        try {
            Map<String, Object> stats = tokenUsageService.getTodayStats();
            return AjaxResult.success(stats);
        } catch (Exception e) {
            log.error("获取今日统计失败", e);
            return AjaxResult.error("获取统计数据失败: " + e.getMessage());
        }
    }

    @GetMapping("/overview")
    @PreAuthorize("@ss.hasPermi('cms:ai:token-usage:list')")
    public AjaxResult getOverview(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        try {
            if (startDate == null) {
                startDate = LocalDate.now().minusDays(6);
            }
            if (endDate == null) {
                endDate = LocalDate.now();
            }

            log.info("查询Token统计: {} 至 {}", startDate, endDate);
            Map<String, Object> overview = tokenUsageService.getOverview(startDate, endDate);
            overview.put("startDate", startDate.toString());
            overview.put("endDate", endDate.toString());
            return AjaxResult.success(overview);
        } catch (Exception e) {
            log.error("获取统计概览失败", e);
            return AjaxResult.error("获取统计数据失败: " + e.getMessage());
        }
    }

    @GetMapping("/by-date")
    @PreAuthorize("@ss.hasPermi('cms:ai:token-usage:list')")
    public AjaxResult statByDate(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        if (startDate == null) {
            startDate = LocalDate.now().minusDays(29);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        List<Map<String, Object>> stats = tokenUsageService.statByDate(startDate, endDate);
        return AjaxResult.success(stats);
    }

    @GetMapping("/by-model")
    @PreAuthorize("@ss.hasPermi('cms:ai:token-usage:list')")
    public AjaxResult statByModel(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        if (startDate == null) {
            startDate = LocalDate.now().minusDays(29);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        List<Map<String, Object>> stats = tokenUsageService.statByModel(startDate, endDate);
        return AjaxResult.success(stats);
    }

    @GetMapping("/by-agent/{agentId}")
    @PreAuthorize("@ss.hasPermi('cms:ai:token-usage:list')")
    public AjaxResult statByAgent(
            @PathVariable Long agentId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        if (startDate == null) {
            startDate = LocalDate.now().minusDays(29);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        List<Map<String, Object>> stats = tokenUsageService.statByAgent(agentId, startDate, endDate);
        return AjaxResult.success(stats);
    }

    @GetMapping("/metrics")
    @PreAuthorize("@ss.hasPermi('cms:ai:token-usage:list')")
    public AjaxResult getMetrics() {
        try {
            Map<String, Object> metrics = tokenUsageService.getMetrics();
            return AjaxResult.success(metrics);
        } catch (Exception e) {
            log.error("获取监控指标失败", e);
            return AjaxResult.error("获取监控指标失败: " + e.getMessage());
        }
    }

    @PostMapping("/flush")
    @PreAuthorize("@ss.hasPermi('cms:ai:token-usage:edit')")
    public AjaxResult manualFlush() {
        try {
            int flushed = tokenUsageService.manualFlush();
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("flushed", flushed);
            result.put("pending", tokenUsageService.getPendingLogsCount());
            return AjaxResult.success(result);
        } catch (Exception e) {
            log.error("手动刷新失败", e);
            return AjaxResult.error("刷新失败: " + e.getMessage());
        }
    }

    @PostMapping("/clear-price-cache")
    @PreAuthorize("@ss.hasPermi('cms:ai:token-usage:edit')")
    public AjaxResult clearPriceCache() {
        try {
            tokenUsageService.clearPriceCache();
            return AjaxResult.success("价格缓存已清除");
        } catch (Exception e) {
            log.error("清除价格缓存失败", e);
            return AjaxResult.error("清除失败: " + e.getMessage());
        }
    }
}
