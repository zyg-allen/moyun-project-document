package com.moyun.ext.ai.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.ext.ai.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/cms/ai/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final AgentService agentService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final ModelConfigService modelConfigService;
    private final TokenUsageService tokenUsageService;
    private final ConversationService conversationService;
    private final ToolService toolService;
    private final WorkflowService workflowService;

    @GetMapping("/overview")
    @PreAuthorize("@ss.hasPermi('cms:ai:dashboard:list')")
    public AjaxResult getOverview() {
        try {
            Map<String, Object> overview = new HashMap<>();

            Map<String, Object> coreMetrics = new HashMap<>();
            coreMetrics.put("agentCount", agentService.count());
            coreMetrics.put("knowledgeCount", knowledgeBaseService.count());
            coreMetrics.put("modelCount", modelConfigService.countEnabled());
            coreMetrics.put("toolCount", toolService.count());
            coreMetrics.put("conversationCount", conversationService.count());
            coreMetrics.put("workflowCount", workflowService.count());
            overview.put("coreMetrics", coreMetrics);
            
            Map<String, Object> workflowStats = workflowService.getStats();
            overview.put("workflowStats", workflowStats);
            
            List<Map<String, Object>> workflowRanking = workflowService.getExecutionRanking(5);
            overview.put("workflowRanking", workflowRanking);

            Map<String, Object> todayTokenStats = tokenUsageService.getTodayStats();
            overview.put("todayTokenStats", todayTokenStats);

            LocalDate today = LocalDate.now();
            LocalDate monthStart = today.withDayOfMonth(1);
            Map<String, Object> monthTokenStats = tokenUsageService.getStats(monthStart, today);
            overview.put("monthTokenStats", monthTokenStats);

            LocalDate weekAgo = today.minusDays(6);
            List<Map<String, Object>> tokenTrend = tokenUsageService.statByDate(weekAgo, today);
            overview.put("tokenTrend", tokenTrend);

            List<Map<String, Object>> modelUsage = tokenUsageService.statByModel(monthStart, today);
            overview.put("modelUsage", modelUsage);

            List<Map<String, Object>> typeUsage = tokenUsageService.statByRequestType(monthStart, today);
            overview.put("typeUsage", typeUsage);

            List<Map<String, Object>> agentStats = getAgentStats();
            overview.put("agentStats", agentStats);

            Map<String, Object> systemStatus = tokenUsageService.getMetrics();
            systemStatus.put("serverTime", LocalDateTime.now().toString());
            overview.put("systemStatus", systemStatus);

            return AjaxResult.success(overview);
        } catch (Exception e) {
            log.error("获取大屏数据失败", e);
            return AjaxResult.error("获取数据失败: " + e.getMessage());
        }
    }

    @GetMapping("/realtime")
    @PreAuthorize("@ss.hasPermi('cms:ai:dashboard:list')")
    public AjaxResult getRealtime() {
        try {
            Map<String, Object> realtime = new HashMap<>();

            realtime.put("todayStats", tokenUsageService.getTodayStats());

            realtime.put("systemStatus", tokenUsageService.getMetrics());

            realtime.put("serverTime", LocalDateTime.now().toString());

            return AjaxResult.success(realtime);
        } catch (Exception e) {
            log.error("获取实时数据失败", e);
            return AjaxResult.error("获取数据失败: " + e.getMessage());
        }
    }

    private List<Map<String, Object>> getAgentStats() {
        List<Map<String, Object>> stats = new ArrayList<>();
        try {
            var agents = agentService.list();
            LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
            LocalDate today = LocalDate.now();

            for (var agent : agents) {
                Map<String, Object> stat = new HashMap<>();
                stat.put("id", agent.getId());
                stat.put("name", agent.getName());
                stat.put("description", agent.getDescription());
                stat.put("enabled", agent.getEnabled());

                List<Map<String, Object>> agentTokenStats = tokenUsageService.statByAgent(
                        agent.getId(), monthStart, today);
                if (!agentTokenStats.isEmpty()) {
                    Map<String, Object> tokenStat = agentTokenStats.get(0);
                    stat.put("totalTokens", tokenStat.getOrDefault("total_tokens", 0));
                    stat.put("totalCost", tokenStat.getOrDefault("total_cost", 0));
                    stat.put("requestCount", tokenStat.getOrDefault("request_count", 0));
                } else {
                    stat.put("totalTokens", 0);
                    stat.put("totalCost", 0);
                    stat.put("requestCount", 0);
                }

                stats.add(stat);
            }

            stats.sort((a, b) -> {
                long countA = ((Number) a.getOrDefault("requestCount", 0)).longValue();
                long countB = ((Number) b.getOrDefault("requestCount", 0)).longValue();
                return Long.compare(countB, countA);
            });

        } catch (Exception e) {
            log.warn("获取智能体统计失败: {}", e.getMessage());
        }
        return stats;
    }
}
