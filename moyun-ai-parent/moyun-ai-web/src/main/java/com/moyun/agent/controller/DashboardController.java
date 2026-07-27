package com.moyun.agent.controller;

import com.moyun.agent.common.ApiResponse;
import com.moyun.agent.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 数据大屏控制器
 *
 * <p>提供系统各项统计数据的聚合接口</p>
 *
 * @author laomao
 */
@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final AgentService agentService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final ModelConfigService modelConfigService;
    private final TokenUsageService tokenUsageService;
    private final ConversationService conversationService;
    private final ToolService toolService;
    private final WorkflowService workflowService;

    /**
     * 获取大屏概览数据
     */
    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> getOverview() {
        try {
            Map<String, Object> overview = new HashMap<>();

            // 1. 核心指标
            Map<String, Object> coreMetrics = new HashMap<>();
            coreMetrics.put("agentCount", agentService.count());
            coreMetrics.put("knowledgeCount", knowledgeBaseService.count());
            coreMetrics.put("modelCount", modelConfigService.countEnabled());
            coreMetrics.put("toolCount", toolService.count());
            coreMetrics.put("conversationCount", conversationService.count());
            coreMetrics.put("workflowCount", workflowService.count());
            overview.put("coreMetrics", coreMetrics);
            
            // 工作流统计
            Map<String, Object> workflowStats = workflowService.getStats();
            overview.put("workflowStats", workflowStats);
            
            // 工作流执行排行
            List<Map<String, Object>> workflowRanking = workflowService.getExecutionRanking(5);
            overview.put("workflowRanking", workflowRanking);

            // 2. Token统计（今日）
            Map<String, Object> todayTokenStats = tokenUsageService.getTodayStats();
            overview.put("todayTokenStats", todayTokenStats);

            // 3. Token统计（本月）
            LocalDate today = LocalDate.now();
            LocalDate monthStart = today.withDayOfMonth(1);
            Map<String, Object> monthTokenStats = tokenUsageService.getStats(monthStart, today);
            overview.put("monthTokenStats", monthTokenStats);

            // 4. 最近7天Token趋势
            LocalDate weekAgo = today.minusDays(6);
            List<Map<String, Object>> tokenTrend = tokenUsageService.statByDate(weekAgo, today);
            overview.put("tokenTrend", tokenTrend);

            // 5. 模型使用分布
            List<Map<String, Object>> modelUsage = tokenUsageService.statByModel(monthStart, today);
            overview.put("modelUsage", modelUsage);

            // 6. 请求类型分布
            List<Map<String, Object>> typeUsage = tokenUsageService.statByRequestType(monthStart, today);
            overview.put("typeUsage", typeUsage);

            // 7. 智能体列表（带统计）
            List<Map<String, Object>> agentStats = getAgentStats();
            overview.put("agentStats", agentStats);

            // 8. 系统状态
            Map<String, Object> systemStatus = tokenUsageService.getMetrics();
            systemStatus.put("serverTime", LocalDateTime.now().toString());
            overview.put("systemStatus", systemStatus);

            return ApiResponse.success(overview);
        } catch (Exception e) {
            log.error("获取大屏数据失败", e);
            return ApiResponse.error("获取数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取实时数据（用于定时刷新）
     */
    @GetMapping("/realtime")
    public ApiResponse<Map<String, Object>> getRealtime() {
        try {
            Map<String, Object> realtime = new HashMap<>();

            // 今日Token统计
            realtime.put("todayStats", tokenUsageService.getTodayStats());

            // 系统状态
            realtime.put("systemStatus", tokenUsageService.getMetrics());

            // 服务器时间
            realtime.put("serverTime", LocalDateTime.now().toString());

            return ApiResponse.success(realtime);
        } catch (Exception e) {
            log.error("获取实时数据失败", e);
            return ApiResponse.error("获取数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取智能体统计
     */
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

                // 获取该智能体的Token使用
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

            // 按请求数排序
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
