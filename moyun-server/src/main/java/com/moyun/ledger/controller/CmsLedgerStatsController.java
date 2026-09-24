package com.moyun.ledger.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ext.aigateway.entity.AiExecuteLog;
import com.moyun.ext.aigateway.mapper.AiExecuteLogMapper;
import com.moyun.ledger.domain.entity.LedgerAiAnalysisReport;
import com.moyun.ledger.domain.entity.LedgerAssetAccount;
import com.moyun.ledger.domain.entity.LedgerBudget;
import com.moyun.ledger.domain.entity.LedgerLiabilityAccount;
import com.moyun.ledger.domain.entity.LedgerMemo;
import com.moyun.ledger.domain.entity.LedgerSavingPlan;
import com.moyun.ledger.domain.entity.LedgerScheduleTask;
import com.moyun.ledger.domain.entity.LedgerTransaction;
import com.moyun.ledger.mapper.LedgerAiAnalysisReportMapper;
import com.moyun.ledger.mapper.LedgerAssetAccountMapper;
import com.moyun.ledger.mapper.LedgerBudgetMapper;
import com.moyun.ledger.mapper.LedgerLiabilityAccountMapper;
import com.moyun.ledger.mapper.LedgerMemoMapper;
import com.moyun.ledger.mapper.LedgerSavingPlanMapper;
import com.moyun.ledger.mapper.LedgerScheduleTaskMapper;
import com.moyun.ledger.mapper.LedgerTransactionMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CMS 记账运营统计 Controller（扩展：模块使用 + AI/Token 消耗 + 收益现状）
 *
 * <p>脱敏红线：仅返回聚合指标（用户数/流水数/模块渗透/AI 成本/打赏营收），
 * 不返回任何用户个体数据、金额明细、账户名称。
 * <p>性能铁律：全部走 SQL 聚合（COUNT DISTINCT / SUM / GROUP BY），
 * 禁止全表 selectList 内存聚合。
 *
 * @author moyun
 */
@Tag(name = "CMS记账运营统计", description = "脱敏聚合运营指标：用户/模块/AI/收益")
@RestController
@RequestMapping("/cms/ledger/stats")
public class CmsLedgerStatsController extends BaseController {

    @Autowired
    private LedgerAssetAccountMapper assetAccountMapper;

    @Autowired
    private LedgerLiabilityAccountMapper liabilityAccountMapper;

    @Autowired
    private LedgerTransactionMapper transactionMapper;

    @Autowired
    private LedgerBudgetMapper budgetMapper;

    @Autowired
    private LedgerSavingPlanMapper savingPlanMapper;

    @Autowired
    private LedgerScheduleTaskMapper scheduleTaskMapper;

    @Autowired
    private LedgerMemoMapper memoMapper;

    @Autowired
    private LedgerAiAnalysisReportMapper reportMapper;

    @Autowired
    private AiExecuteLogMapper aiExecuteLogMapper;

    @Operation(summary = "运营统计总览（脱敏聚合：用户规模 + 模块使用 + AI/Token + 收益现状）")
    @PreAuthorize("@ss.hasPermi('cms:ledgerStats:list')")
    @GetMapping("/overview")
    public AjaxResult overview() {
        Map<String, Object> data = new LinkedHashMap<>();
        LocalDate monthAgo = LocalDate.now().minusDays(30);

        // ============ 1. 用户规模（持有任一启用账户的用户数：资产∪负债，SQL 单列 DISTINCT 后合并） ============
        Set<Object> userRaw = new HashSet<>();
        assetAccountMapper.selectObjs(new QueryWrapper<LedgerAssetAccount>()
                        .select("DISTINCT user_id")
                        .eq("status", LedgerAssetAccount.STATUS_ENABLED))
                .forEach(userRaw::add);
        liabilityAccountMapper.selectObjs(new QueryWrapper<LedgerLiabilityAccount>()
                        .select("DISTINCT user_id")
                        .eq("status", LedgerLiabilityAccount.STATUS_ENABLED))
                .forEach(userRaw::add);
        long userCount = userRaw.size();
        data.put("userCount", userCount);

        // ============ 2. 流水规模（含已删除=历史记账行为量）与活跃 ============
        data.put("transactionCount", transactionMapper.selectCount(null));
        data.put("activeUserCount30d", transactionMapper.selectObjs(new QueryWrapper<LedgerTransaction>()
                .select("DISTINCT user_id")
                .ge("transaction_date", monthAgo)).size());

        // 记账类型分布（有效流水，SQL GROUP BY）
        Map<String, Long> typeDist = new HashMap<>();
        for (Map<String, Object> row : transactionMapper.selectMaps(new QueryWrapper<LedgerTransaction>()
                .select("type", "count(*) as cnt")
                .eq("status", LedgerTransaction.STATUS_NORMAL)
                .groupBy("type"))) {
            typeDist.put(String.valueOf(row.get("type")), toLong(row.get("cnt")));
        }
        data.put("typeDistribution", typeDist);

        // ============ 3. 模块使用情况（各模块去重使用用户数，SQL COUNT DISTINCT） ============
        List<Map<String, Object>> moduleUsage = new ArrayList<>();
        moduleUsage.add(module("transaction", "记账流水", transactionMapper.selectObjs(
                new QueryWrapper<LedgerTransaction>().select("DISTINCT user_id")).size()));
        moduleUsage.add(module("budget", "预算管理", budgetMapper.selectObjs(
                new QueryWrapper<LedgerBudget>().select("DISTINCT user_id")).size()));
        moduleUsage.add(module("saving", "存钱计划", savingPlanMapper.selectObjs(
                new QueryWrapper<LedgerSavingPlan>().select("DISTINCT user_id")).size()));
        moduleUsage.add(module("schedule", "定时记账", scheduleTaskMapper.selectObjs(
                new QueryWrapper<LedgerScheduleTask>().select("DISTINCT user_id")).size()));
        moduleUsage.add(module("memo", "备忘录", memoMapper.selectObjs(
                new QueryWrapper<LedgerMemo>().select("DISTINCT user_id")).size()));
        moduleUsage.add(module("assetAccount", "资产账户", (int) userCount));
        moduleUsage.add(module("aiReport", "AI 分析报告", reportMapper.selectObjs(
                new QueryWrapper<LedgerAiAnalysisReport>().select("DISTINCT user_id")).size()));
        data.put("moduleUsage", moduleUsage);

        // ============ 4. AI 与 Token 消耗（网关执行日志，SQL SUM/GROUP BY） ============
        Map<String, Object> aiStats = new LinkedHashMap<>();
        List<Map<String, Object>> aiTotal = aiExecuteLogMapper.selectMaps(new QueryWrapper<AiExecuteLog>()
                .select("count(*) as calls", "sum(token_used) as tokens", "sum(cost_yuan) as cost"));
        Map<String, Object> aiRow = aiTotal.isEmpty() ? Map.of() : aiTotal.getFirst();
        aiStats.put("callCount", toLong(aiRow.get("calls")));
        aiStats.put("tokenTotal", toLong(aiRow.get("tokens")));
        aiStats.put("costYuan", aiRow.get("cost"));
        // 场景分布（按 scene_code 聚合调用/Token/成本）
        List<Map<String, Object>> sceneDist = new ArrayList<>();
        for (Map<String, Object> row : aiExecuteLogMapper.selectMaps(new QueryWrapper<AiExecuteLog>()
                .select("scene_code", "count(*) as calls", "sum(token_used) as tokens", "sum(cost_yuan) as cost")
                .groupBy("scene_code")
                .orderByDesc("calls"))) {
            Map<String, Object> scene = new LinkedHashMap<>();
            scene.put("sceneCode", row.get("scene_code"));
            scene.put("calls", toLong(row.get("calls")));
            scene.put("tokens", toLong(row.get("tokens")));
            scene.put("costYuan", row.get("cost"));
            sceneDist.add(scene);
        }
        aiStats.put("sceneDistribution", sceneDist);
        data.put("aiStats", aiStats);

        // ============ 5. 收益现状（打赏收益统计统一收口到收入管理模块，此页仅保留跳转入口） ============
        data.put("revenueModulePath", "/pay/revenue");

        return success(data);
    }

    /** 模块使用行组装 */
    private Map<String, Object> module(String key, String label, int userCount) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("key", key);
        m.put("label", label);
        m.put("userCount", userCount);
        return m;
    }

    private Long toLong(Object v) {
        return v == null ? 0L : Long.valueOf(v.toString());
    }
}
