package com.moyun.agent.controller;

import com.moyun.agent.common.ApiResponse;
import com.moyun.agent.service.TokenUsageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Token使用统计控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/token-usage")
@RequiredArgsConstructor
public class TokenUsageController {

    private final TokenUsageService tokenUsageService;

    /**
     * 获取今日统计
     */
    @GetMapping("/today")
    public ApiResponse<Map<String, Object>> getTodayStats() {
        try {
            Map<String, Object> stats = tokenUsageService.getTodayStats();
            return ApiResponse.success(stats);
        } catch (Exception e) {
            log.error("获取今日统计失败", e);
            return ApiResponse.error("获取统计数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取统计概览
     */
    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> getOverview(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        try {
            // 默认查询最近7天
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
            return ApiResponse.success(overview);
        } catch (Exception e) {
            log.error("获取统计概览失败", e);
            return ApiResponse.error("获取统计数据失败: " + e.getMessage());
        }
    }

    /**
     * 按日期统计
     */
    @GetMapping("/by-date")
    public ApiResponse<List<Map<String, Object>>> statByDate(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        if (startDate == null) {
            startDate = LocalDate.now().minusDays(29);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        List<Map<String, Object>> stats = tokenUsageService.statByDate(startDate, endDate);
        return ApiResponse.success(stats);
    }

    /**
     * 按模型统计
     */
    @GetMapping("/by-model")
    public ApiResponse<List<Map<String, Object>>> statByModel(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        if (startDate == null) {
            startDate = LocalDate.now().minusDays(29);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        List<Map<String, Object>> stats = tokenUsageService.statByModel(startDate, endDate);
        return ApiResponse.success(stats);
    }

    /**
     * 按智能体统计
     */
    @GetMapping("/by-agent/{agentId}")
    public ApiResponse<List<Map<String, Object>>> statByAgent(
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
        return ApiResponse.success(stats);
    }

    /**
     * 获取服务监控指标
     */
    @GetMapping("/metrics")
    public ApiResponse<Map<String, Object>> getMetrics() {
        try {
            Map<String, Object> metrics = tokenUsageService.getMetrics();
            return ApiResponse.success(metrics);
        } catch (Exception e) {
            log.error("获取监控指标失败", e);
            return ApiResponse.error("获取监控指标失败: " + e.getMessage());
        }
    }

    /**
     * 手动刷新日志到数据库
     */
    @PostMapping("/flush")
    public ApiResponse<Map<String, Object>> manualFlush() {
        try {
            int flushed = tokenUsageService.manualFlush();
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("flushed", flushed);
            result.put("pending", tokenUsageService.getPendingLogsCount());
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("手动刷新失败", e);
            return ApiResponse.error("刷新失败: " + e.getMessage());
        }
    }

    /**
     * 清除价格缓存
     */
    @PostMapping("/clear-price-cache")
    public ApiResponse<String> clearPriceCache() {
        try {
            tokenUsageService.clearPriceCache();
            return ApiResponse.success("价格缓存已清除");
        } catch (Exception e) {
            log.error("清除价格缓存失败", e);
            return ApiResponse.error("清除失败: " + e.getMessage());
        }
    }
}
