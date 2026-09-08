package com.moyun.ledger.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.core.base.entity.SysDictData;
import com.moyun.ext.ai.entity.ModelConfig;
import com.moyun.ext.ai.service.ModelConfigService;
import com.moyun.ext.ai.service.AiSceneResolver;
import com.moyun.ledger.domain.entity.LedgerAssetAccount;
import com.moyun.ledger.domain.entity.LedgerAiAnalysisReport;
import com.moyun.ledger.domain.entity.LedgerCategory;
import com.moyun.ledger.domain.entity.LedgerLiabilityAccount;
import com.moyun.ledger.domain.entity.LedgerTransaction;
import com.moyun.ledger.mapper.LedgerAssetAccountMapper;
import com.moyun.ledger.mapper.LedgerAiAnalysisReportMapper;
import com.moyun.ledger.mapper.LedgerCategoryMapper;
import com.moyun.ledger.mapper.LedgerLiabilityAccountMapper;
import com.moyun.ledger.mapper.LedgerTransactionMapper;
import com.moyun.ledger.service.ILedgerAiAnalysisService;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.mapper.PortalUserMapper;
import com.moyun.portal.service.IPortalUserService;
import com.moyun.system.service.ISysDictTypeService;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 记账 AI 财务分析服务实现
 *
 * 规则引擎计算硬指标（资产负债率/还款压力/入不敷出等），
 * LLM 生成个性化综述与建议（AI 未启用或调用失败时降级为模板文案）。
 *
 * @author moyun
 */
@Service
public class LedgerAiAnalysisServiceImpl implements ILedgerAiAnalysisService {
    /** v11.39：本服务所属 AI 场景代码（AiSceneEnum 注册；绑定见 ai_scene_config） */
    private static final String SCENE_FINANCE_ANALYSIS = "finance_analysis";

    private static final Logger log = LoggerFactory.getLogger(LedgerAiAnalysisServiceImpl.class);

    private static final String DICT_IDENTITY_TAG = "ledger_identity_tag";

    @Autowired
    private LedgerTransactionMapper transactionMapper;
    @Autowired
    private LedgerCategoryMapper categoryMapper;
    @Autowired
    private LedgerAssetAccountMapper assetAccountMapper;
    @Autowired
    private LedgerLiabilityAccountMapper liabilityAccountMapper;
    @Autowired
    private IPortalUserService portalUserService;
    @Autowired
    private PortalUserMapper portalUserMapper;
    @Autowired
    private ISysDictTypeService dictTypeService;
    /** v11.36：报告快照 Mapper */
    @Autowired
    private LedgerAiAnalysisReportMapper reportMapper;
    /** LLM 能力由 ext-ai 模块提供，未启用时为空（降级模板） */
    @Autowired(required = false)
    private ModelConfigService modelConfigService;

    /** v11.39：场景解析器（Factory：scene_code → 绑定模型） */
    @Autowired(required = false)
    private AiSceneResolver sceneResolver;

    /** 分析维度 → 窗口起始（自然月对齐）与展示文案 */
    private static final Map<String, Integer> RANGE_MONTHS = Map.of("month", 0, "3m", 3, "6m", 6, "year", 12);
    private static final Map<String, String> RANGE_LABEL = Map.of("month", "本月", "3m", "近3个月", "6m", "近6个月", "year", "近12个月");

    @Override
    public Map<String, Object> analyze(Long userId, boolean refresh, String range) {
        // 维度归一化（非法值回落本月）
        if (!RANGE_MONTHS.containsKey(range)) {
            range = "month";
        }
        String rangeLabel = RANGE_LABEL.get(range);
        String period = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
        // v11.37：仅"本月"维度读写月度快照；其他维度实时计算（快照表 period 与自然月对齐）
        boolean snapshotable = "month".equals(range);
        PortalUser user = portalUserService.selectPortalUserById(userId);
        LocalDate today = LocalDate.now();
        // v11.40：数据指纹（本月流水/资产/负债/画像变更痕迹）——命中快照前比对，数据变了自动重算，无需手动 refresh
        String fingerprint = snapshotable ? buildFingerprint(userId, today, user) : null;
        if (snapshotable && !refresh) {
            LedgerAiAnalysisReport cached = reportMapper.selectOne(new LambdaQueryWrapper<LedgerAiAnalysisReport>()
                    .eq(LedgerAiAnalysisReport::getUserId, userId)
                    .eq(LedgerAiAnalysisReport::getPeriod, period));
            // 指纹一致才命中缓存；不一致（含存量快照无指纹）→ 落穿重算并覆盖
            if (cached != null && fingerprint != null && fingerprint.equals(cached.getDataFingerprint())) {
                Map<String, Object> r = reportToResult(cached, true);
                r.put("range", range);
                r.put("rangeLabel", rangeLabel);
                return r;
            }
        }
        // v11.37：维度窗口（自然月对齐）：month=本月1号；3m/6m/year=含本月往前 N 个自然月
        LocalDate rangeStart = "month".equals(range)
                ? today.withDayOfMonth(1)
                : today.minusMonths(RANGE_MONTHS.get(range) - 1L).withDayOfMonth(1);
        // 窗口起始 rangeStart 由维度参数决定（v11.37）

        // ===== 数据聚合 =====
        List<LedgerTransaction> recent = transactionMapper.selectList(new LambdaQueryWrapper<LedgerTransaction>()
                .eq(LedgerTransaction::getUserId, userId)
                .eq(LedgerTransaction::getStatus, 1)
                .ge(LedgerTransaction::getTransactionDate, rangeStart)
                .le(LedgerTransaction::getTransactionDate, today));
        List<LedgerLiabilityAccount> debts = liabilityAccountMapper.selectList(
                new LambdaQueryWrapper<LedgerLiabilityAccount>()
                        .eq(LedgerLiabilityAccount::getUserId, userId)
                        .eq(LedgerLiabilityAccount::getStatus, 1)
                        .and(w -> w.isNull(LedgerLiabilityAccount::getSettleFlag)
                                .or().ne(LedgerLiabilityAccount::getSettleFlag, 1)));
        Map<Long, String> catNames = loadCategoryNames(userId);

        // ===== 核心指标 =====
        BigDecimal totalIncome = recent.stream()
                .filter(t -> "income".equals(t.getType()))
                .map(t -> t.getAmount() == null ? BigDecimal.ZERO : t.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpense = recent.stream()
                .filter(t -> "expense".equals(t.getType()))
                .map(t -> t.getAmount() == null ? BigDecimal.ZERO : t.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal monthlyRepayment = debts.stream()
                .filter(d -> d.getMonthlyPayment() != null)
                .map(d -> d.getMonthlyPayment() == null ? BigDecimal.ZERO : d.getMonthlyPayment())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalLiability = debts.stream()
                .filter(d -> d.getIncludeInTotal() == null || d.getIncludeInTotal() == 1)
                .map(d -> d.getBalance() == null ? BigDecimal.ZERO : d.getBalance())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAsset = assetAccountMapper.selectList(new LambdaQueryWrapper<LedgerAssetAccount>()
                        .eq(LedgerAssetAccount::getUserId, userId)
                        .eq(LedgerAssetAccount::getStatus, 1)
                        .eq(LedgerAssetAccount::getIncludeInTotal, 1))
                .stream()
                .map(a -> a.getBalance() == null ? BigDecimal.ZERO : a.getBalance())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double months = Math.max(1, recent.stream().map(LedgerTransaction::getTransactionDate)
                .map(YearMonth::from).distinct().count());
        BigDecimal avgMonthlyIncome = totalIncome.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        BigDecimal avgMonthlyExpense = totalExpense.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);

        // 近3月逐月判断是否入不敷出
        Map<YearMonth, BigDecimal[]> monthly = new LinkedHashMap<>();
        for (LedgerTransaction t : recent) {
            if (!"income".equals(t.getType()) && !"expense".equals(t.getType())) continue;
            BigDecimal[] arr = monthly.computeIfAbsent(YearMonth.from(t.getTransactionDate()), k -> new BigDecimal[2]);
            if (arr[0] == null) arr[0] = BigDecimal.ZERO;
            if (arr[1] == null) arr[1] = BigDecimal.ZERO;
            if ("income".equals(t.getType())) {
                arr[0] = arr[0].add(t.getAmount() == null ? BigDecimal.ZERO : t.getAmount());
            } else {
                arr[1] = arr[1].add(t.getAmount() == null ? BigDecimal.ZERO : t.getAmount());
            }
        }
        int deficitMonths = 0;
        List<YearMonth> keys = new ArrayList<>(monthly.keySet());
        for (int i = Math.max(0, keys.size() - 3); i < keys.size(); i++) {
            BigDecimal[] arr = monthly.get(keys.get(i));
            if (arr[1].compareTo(arr[0]) > 0) deficitMonths++;
        }

        double debtRatio = totalAsset.compareTo(BigDecimal.ZERO) > 0 
                ? totalLiability.divide(totalAsset, 4, RoundingMode.HALF_UP).doubleValue() 
                : (totalLiability.compareTo(BigDecimal.ZERO) > 0 ? 1.0 : 0.0);
        double repaymentPressure = avgMonthlyIncome.compareTo(BigDecimal.ZERO) > 0 
                ? monthlyRepayment.divide(avgMonthlyIncome, 4, RoundingMode.HALF_UP).doubleValue()
                : (monthlyRepayment.compareTo(BigDecimal.ZERO) > 0 ? 1.0 : 0.0);
        double savingRate = avgMonthlyIncome.compareTo(BigDecimal.ZERO) > 0
                ? Math.max(0, avgMonthlyIncome.subtract(avgMonthlyExpense)
                        .divide(avgMonthlyIncome, 4, RoundingMode.HALF_UP).doubleValue()) : 0;

        // ===== 收入来源（近6月 income 分类占比） =====
        Map<Long, BigDecimal> incomeByCat = new LinkedHashMap<>();
        for (LedgerTransaction t : recent) {
            if (!"income".equals(t.getType()) || t.getCategoryId() == null) continue;
            BigDecimal amount = t.getAmount() == null ? BigDecimal.ZERO : t.getAmount();
            incomeByCat.merge(t.getCategoryId(), amount, BigDecimal::add);
        }
        List<Map<String, Object>> incomeSources = new ArrayList<>();
        BigDecimal incomeTotal = incomeByCat.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        incomeByCat.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .forEach(e -> {
                    Map<String, Object> src = new HashMap<>();
                    src.put("name", catNames.getOrDefault(e.getKey(), "其他收入"));
                    src.put("amount", e.getValue());
                                        // 修复（v11.37）：原 multiply(TEN) 把占比缩小了 10 倍（100% 显示成 10%）
                    src.put("ratio", incomeTotal.compareTo(BigDecimal.ZERO) > 0
                            ? e.getValue().multiply(BigDecimal.valueOf(100))
                                    .divide(incomeTotal, 1, RoundingMode.HALF_UP)
                                    .doubleValue()
                            : 0);
                    incomeSources.add(src);
                });

        // ===== 债务风险（规则引擎） =====
        List<Map<String, Object>> debtRisks = new ArrayList<>();
        int todayDay = today.getDayOfMonth();
        for (LedgerLiabilityAccount d : debts) {
            boolean isCard = "credit_card".equals(d.getType());
            boolean isCarLoan = "bank_loan".equals(d.getType())
                    || (d.getName() != null && d.getName().contains("车贷"));
            if ((isCard || isCarLoan) && d.getRepaymentDay() != null) {
                int diff = d.getRepaymentDay() - todayDay;
                if (diff >= 0 && diff <= 5) {
                    debtRisks.add(risk("high", (isCard ? "信用卡" : "车贷") + "还款日临近",
                            "「" + d.getName() + "」" + d.getRepaymentDay() + "日还款"
                                    + (d.getMonthlyPayment() != null ? "，月供 ¥" + yuan(d.getMonthlyPayment()) : "")
                                    + "，请确保账户余额充足。"));
                }
            }
            if (isCard && d.getBalance() != null && avgMonthlyIncome.compareTo(BigDecimal.ZERO) > 0 
                    && d.getBalance().compareTo(avgMonthlyIncome.multiply(BigDecimal.valueOf(3))) > 0) {
                debtRisks.add(risk("mid", "信用卡负债偏高",
                        "「" + d.getName() + "」欠款已达月收入的 3 倍以上，建议优先偿还、避免循环利息。"));
            }
        }
        if (debtRatio >= 0.8 && totalLiability.compareTo(BigDecimal.ZERO) > 0) {
            debtRisks.add(risk("high", "资产负债率过高",
                    "当前资产负债率 " + pct(debtRatio) + "（负债 ¥" + yuan(totalLiability)
                            + " / 资产 ¥" + yuan(totalAsset) + "），建议控制新增负债、优先偿还高息债务。"));
        } else if (debtRatio >= 0.5 && totalLiability.compareTo(BigDecimal.ZERO) > 0) {
            debtRisks.add(risk("mid", "负债水平中等偏高",
                    "当前资产负债率 " + pct(debtRatio) + "，处于 50%-80% 区间，注意留足应急资金。"));
        }
        if (repaymentPressure >= 0.5 && monthlyRepayment.compareTo(BigDecimal.ZERO) > 0) {
            debtRisks.add(risk(avgMonthlyIncome.compareTo(BigDecimal.ZERO) > 0 ? "high" : "mid", "还款压力过大",
                    "月供合计 ¥" + yuan(monthlyRepayment) + "，占月均收入的 " + pct(repaymentPressure)
                            + "，超过 50% 警戒线，需警惕断供风险。"));
        } else if (repaymentPressure >= 0.3 && monthlyRepayment.compareTo(BigDecimal.ZERO) > 0) {
            debtRisks.add(risk("low", "还款压力中等",
                    "月供占月均收入的 " + pct(repaymentPressure) + "，建议保持在 30% 以内更稳健。"));
        }
        if (deficitMonths >= 2) {
            debtRisks.add(risk("high", "连续入不敷出",
                    "近 " + deficitMonths + " 个月支出超过收入，长期将侵蚀资产，建议立即复盘支出结构。"));
        }

        // ===== 建议（规则引擎） =====
        List<Map<String, Object>> suggestions = new ArrayList<>();
        if (avgMonthlyIncome.compareTo(BigDecimal.ZERO) == 0
                && totalExpense.compareTo(BigDecimal.ZERO) > 0) {
            suggestions.add(suggest("📝", "补记收入来源",
                    "近期只有支出记录、没有收入记录。若实际有收入（工资/兼职等），请及时补记，分析才准确；若确实无收入，请重点控制支出。"));
        }
        if (!incomeSources.isEmpty() && incomeTotal.compareTo(BigDecimal.ZERO) > 0
                && ((BigDecimal) incomeSources.get(0).get("amount"))
                        .compareTo(incomeTotal.multiply(BigDecimal.valueOf(0.8))) >= 0) {
            BigDecimal topAmount = (BigDecimal) incomeSources.get(0).get("amount");
            suggestions.add(suggest("💼", "拓展收入来源",
                    "收入 " + pct(topAmount.divide(incomeTotal, 4, RoundingMode.HALF_UP).doubleValue()) + " 来自「"
                            + incomeSources.get(0).get("name") + "」，过于单一。可结合职业（"
                            + profileText(user) + "）评估兼职、技能变现等第二收入曲线。"));
        }
        if (deficitMonths >= 2) {
            suggestions.add(suggest("🧾", "控制非必要支出",
                    "月均支出 ¥" + yuan(avgMonthlyExpense) + " 已超月均收入 ¥" + yuan(avgMonthlyIncome)
                            + "，建议按「必要 - 想要 - 储蓄」三账户法拆分，先砍「想要」。"));
        }
        if (savingRate < 0.1 && avgMonthlyIncome.compareTo(BigDecimal.ZERO) > 0 && deficitMonths == 0) {
            suggestions.add(suggest("🐷", "提升储蓄率",
                    "当前月储蓄率 " + pct(savingRate) + "，低于 10%。建议发薪日先转存 10%-20% 再消费。"));
        }
        if (totalLiability.compareTo(BigDecimal.ZERO) == 0 && avgMonthlyIncome.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal surplus = avgMonthlyIncome.subtract(avgMonthlyExpense);
            suggestions.add(suggest("📈", "无债一身轻，可开始理财",
                    "当前无在还负债，月结余 ¥" + yuan(surplus.compareTo(BigDecimal.ZERO) > 0 ? surplus : BigDecimal.ZERO)
                            + " 可考虑「货币基金打底 + 指数定投增值」的组合。"));
        }
        if (monthlyRepayment.compareTo(BigDecimal.ZERO) > 0 && totalLiability.compareTo(BigDecimal.ZERO) > 0 
                && avgMonthlyIncome.compareTo(BigDecimal.ZERO) > 0) {
            suggestions.add(suggest("🏦", "优化债务结构",
                    "若存在多笔债务，优先偿还利率最高的（通常是信用卡）；有余力时可考虑低息置换。"));
        }
        if (suggestions.isEmpty()) {
            boolean hasHighRisk = debtRisks.stream().anyMatch(r -> "high".equals(r.get("level")));
            suggestions.add(hasHighRisk
                    ? suggest("⚠️", "优先处理高风险项",
                            "存在需要立即关注的高风险债务问题，请先处理上方风险提示，再逐步优化收支结构。")
                    : suggest("✅", "财务状况稳健",
                            "收支平衡、负债可控。继续保持记账习惯，让每一分钱都有迹可循。"));
        }

        // ===== 指标汇总 =====
        Map<String, Object> indicators = new LinkedHashMap<>();
        indicators.put("totalAsset", totalAsset);
        indicators.put("totalLiability", totalLiability);
        indicators.put("netWorth", totalAsset.subtract(totalLiability));
        indicators.put("debtRatio", Math.round(debtRatio * 1000) / 10.0);
        indicators.put("avgMonthlyIncome", avgMonthlyIncome);
        indicators.put("avgMonthlyExpense", avgMonthlyExpense);
        indicators.put("monthlyRepayment", monthlyRepayment);
        indicators.put("repaymentPressure", Math.round(repaymentPressure * 1000) / 10.0);
        indicators.put("savingRate", Math.round(savingRate * 1000) / 10.0);
        indicators.put("deficitMonths", deficitMonths);
        indicators.put("sampleMonths", (int) months);
        // v11.37：维度标注（前端标题与 LLM 提示词共用）
        indicators.put("range", range);
        indicators.put("rangeLabel", rangeLabel);
        indicators.put("rangeStart", rangeStart.toString());

        // ===== LLM 综述（失败降级模板） =====
        String aiSummary = generateSummary(indicators, incomeSources, debtRisks, user);
        boolean aiEnabled = !aiSummary.startsWith("（模板");

        // ===== v11.36：落库月度快照（当月 UNIQUE，存在则覆盖） =====
        int healthScore = computeHealthScore(debtRatio, repaymentPressure, savingRate, deficitMonths);
        String profileSnapshot = profileText(user);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("period", period);
        result.put("healthScore", healthScore);
        result.put("indicators", indicators);
        result.put("incomeSources", incomeSources);
        result.put("debtRisks", debtRisks);
        result.put("suggestions", suggestions);
        result.put("aiSummary", aiSummary);
        result.put("aiEnabled", aiEnabled);
        result.put("fromCache", false);
        result.put("range", range);
        result.put("rangeLabel", rangeLabel);
        if (snapshotable) {
            // 仅"本月"维度写月度快照：3m/6m/year 是实时视图，避免快照表膨胀与口径混杂
            saveReport(userId, period, healthScore, indicators, incomeSources,
                    debtRisks, suggestions, aiSummary, aiEnabled, profileSnapshot, fingerprint);
        }
        return result;
    }

    @Override
    public Map<String, Object> listReports(Long userId, int page, int pageSize) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<LedgerAiAnalysisReport> p =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, pageSize);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<LedgerAiAnalysisReport> r =
                reportMapper.selectPage(p, new LambdaQueryWrapper<LedgerAiAnalysisReport>()
                        .eq(LedgerAiAnalysisReport::getUserId, userId)
                        .orderByDesc(LedgerAiAnalysisReport::getPeriod));
        List<Map<String, Object>> list = new ArrayList<>();
        for (LedgerAiAnalysisReport rep : r.getRecords()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", rep.getId());
            item.put("period", rep.getPeriod());
            item.put("healthScore", rep.getHealthScore());
            item.put("aiSummary", rep.getAiSummary());
            item.put("aiEnabled", rep.getAiEnabled());
            item.put("profileSnapshot", rep.getProfileSnapshot());
            item.put("updateTime", rep.getUpdateTime());
            list.add(item);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", list);
        data.put("total", r.getTotal());
        return data;
    }

    /** 快照实体 → 前端报告结构（缓存命中时使用） */
    private Map<String, Object> reportToResult(LedgerAiAnalysisReport rep, boolean fromCache) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("period", rep.getPeriod());
        result.put("healthScore", rep.getHealthScore());
        result.put("indicators", parseJson(rep.getMetricsJson()));
        result.put("incomeSources", parseJson(rep.getIncomeJson()));
        result.put("debtRisks", parseJson(rep.getRiskJson()));
        result.put("suggestions", parseJson(rep.getAdviceJson()));
        result.put("aiSummary", rep.getAiSummary());
        result.put("aiEnabled", rep.getAiEnabled() != null && rep.getAiEnabled() == 1);
        result.put("fromCache", fromCache);
        return result;
    }

    /** 落库（当月存在则更新；失败仅告警不影响返回） */
    private void saveReport(Long userId, String period, int healthScore,
                            Map<String, Object> indicators, List<Map<String, Object>> incomeSources,
                            List<Map<String, Object>> debtRisks, List<Map<String, Object>> suggestions,
                            String aiSummary, boolean aiEnabled, String profileSnapshot, String fingerprint) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            LedgerAiAnalysisReport rep = reportMapper.selectOne(new LambdaQueryWrapper<LedgerAiAnalysisReport>()
                    .eq(LedgerAiAnalysisReport::getUserId, userId)
                    .eq(LedgerAiAnalysisReport::getPeriod, period));
            boolean exists = rep != null;
            if (rep == null) {
                rep = new LedgerAiAnalysisReport();
                rep.setUserId(userId);
                rep.setPeriod(period);
            }
            rep.setHealthScore(healthScore);
            rep.setMetricsJson(om.writeValueAsString(indicators));
            rep.setIncomeJson(om.writeValueAsString(incomeSources));
            rep.setRiskJson(om.writeValueAsString(debtRisks));
            rep.setAdviceJson(om.writeValueAsString(suggestions));
            rep.setAiSummary(aiSummary);
            rep.setAiEnabled(aiEnabled ? 1 : 0);
            rep.setProfileSnapshot(profileSnapshot);
            rep.setDataFingerprint(fingerprint);
            if (exists) {
                reportMapper.updateById(rep);
            } else {
                reportMapper.insert(rep);
            }
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("AI 分析报告落库失败 userId={}", userId, e);
        }
    }

    /** 财务健康分：储蓄率 + 收支平衡 + 负债率 + 还款压力（各 25 分，简单可解释） */
    private int computeHealthScore(double debtRatio, double repaymentPressure, double savingRate, int deficitMonths) {
        double score = Math.min(25, savingRate * 100 * 0.25)
                + (deficitMonths >= 2 ? 0 : 25)
                + (debtRatio < 0.5 ? 25 : debtRatio < 0.8 ? 12 : 0)
                + (repaymentPressure < 0.3 ? 25 : repaymentPressure < 0.5 ? 12 : 0);
        return Math.max(0, Math.min(100, (int) Math.round(score)));
    }

    private Object parseJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Object.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Map<String, Object> getProfile(Long userId) {
        PortalUser user = portalUserService.selectPortalUserById(userId);
        Map<String, Object> profile = buildProfile(user);
        List<Map<String, String>> options = new ArrayList<>();
        try {
            for (SysDictData d : dictTypeService.selectDictDataByType(DICT_IDENTITY_TAG)) {
                Map<String, String> opt = new HashMap<>();
                opt.put("value", d.getDictValue());
                opt.put("label", d.getDictLabel());
                options.add(opt);
            }
        } catch (Exception e) {
            log.warn("查询身份标签字典失败", e);
        }
        profile.put("identityOptions", options);
        return profile;
    }

    @Override
    public Map<String, Object> updateProfile(Long userId, Map<String, String> body) {
        PortalUser user = portalUserService.selectPortalUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (body.containsKey("position")) {
            String position = body.get("position");
            if (position != null && position.length() > 100) {
                throw new IllegalArgumentException("职位长度不能超过100个字符");
            }
            user.setPosition(position);
        }
        if (body.containsKey("company")) {
            String company = body.get("company");
            if (company != null && company.length() > 200) {
                throw new IllegalArgumentException("公司长度不能超过200个字符");
            }
            user.setCompany(company);
        }
        if (body.containsKey("identityTag")) {
            String tag = body.get("identityTag");
            if (tag != null && tag.length() > 32) {
                throw new IllegalArgumentException("身份标签长度不能超过32个字符");
            }
            // 白名单校验：必须是字典内的合法值（空值视为清除）
            boolean valid = tag == null || tag.isEmpty();
            if (!valid) {
                valid = dictTypeService.selectDictDataByType(DICT_IDENTITY_TAG).stream()
                        .anyMatch(d -> tag.equals(d.getDictValue()));
            }
            if (!valid) {
                throw new IllegalArgumentException("非法身份标签");
            }
            user.setIdentityTag(tag);
        }
        portalUserMapper.updateById(user);
        return getProfile(userId);
    }

    // ==================== 私有方法 ====================

    private Map<Long, String> loadCategoryNames(Long userId) {
        // 修复（v11.36.1）：系统预设分类 user_id=0（非 NULL），原 isNull 条件永远查不到"工资"等系统分类，
        // 导致收入来源全部显示为"其他收入"。与 LedgerCategoryServiceImpl 口径对齐：0=系统 + 当前用户自定义。
        return categoryMapper.selectList(new LambdaQueryWrapper<LedgerCategory>()
                        .and(w -> w.eq(LedgerCategory::getUserId, 0L)
                                .or().eq(LedgerCategory::getUserId, userId)))
                .stream().collect(Collectors.toMap(LedgerCategory::getId, LedgerCategory::getName, (a, b) -> a));
    }

    private Map<String, Object> buildProfile(PortalUser user) {
        Map<String, Object> p = new LinkedHashMap<>();
        if (user != null) {
            p.put("position", user.getPosition());
            p.put("company", user.getCompany());
            p.put("identityTag", user.getIdentityTag());
            p.put("identityTagLabel", identityTagLabel(user.getIdentityTag()));
        }
        return p;
    }

    private String identityTagLabel(String tag) {
        if (tag == null || tag.isEmpty()) return null;
        try {
            return dictTypeService.selectDictDataByType(DICT_IDENTITY_TAG).stream()
                    .filter(d -> tag.equals(d.getDictValue()))
                    .map(SysDictData::getDictLabel).findFirst().orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private String profileText(PortalUser user) {
        if (user == null) return "未填写";
        StringBuilder sb = new StringBuilder();
        String tagLabel = identityTagLabel(user.getIdentityTag());
        if (tagLabel != null) sb.append(tagLabel);
        if (user.getPosition() != null && !user.getPosition().isEmpty()) {
            if (sb.length() > 0) sb.append(" / ");
            sb.append(user.getPosition());
        }
        if (user.getCompany() != null && !user.getCompany().isEmpty()) {
            if (sb.length() > 0) sb.append(" @ ");
            sb.append(user.getCompany());
        }
        return sb.length() > 0 ? sb.toString() : "未填写";
    }

    /**
     * 数据指纹（v11.40）：本月流水（条数+最后变更时间）、启用资产/负债账户（数量+最后变更时间）、画像文本
     * <p>命中当月快照前比对——任一输入变化自动失效快照重算（无需手动 refresh）；无变化仍零 token 命中。
     * 统计口径与 analyze 一致（status=1，本月窗口 [月初, 今天]）。
     */
    private String buildFingerprint(Long userId, LocalDate today, PortalUser user) {
        try {
            LocalDate monthStart = today.withDayOfMonth(1);
            String tx = aggSignature(transactionMapper.selectMaps(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<LedgerTransaction>()
                            .select("count(*) as cnt", "date_format(max(update_time), '%y%m%d%H%i%s') as last")
                            .eq("user_id", userId).eq("status", 1)
                            .ge("transaction_date", monthStart).le("transaction_date", today)));
            String as = aggSignature(assetAccountMapper.selectMaps(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<LedgerAssetAccount>()
                            .select("count(*) as cnt", "date_format(max(update_time), '%y%m%d%H%i%s') as last")
                            .eq("user_id", userId).eq("status", 1)));
            String li = aggSignature(liabilityAccountMapper.selectMaps(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<LedgerLiabilityAccount>()
                            .select("count(*) as cnt", "date_format(max(update_time), '%y%m%d%H%i%s') as last")
                            .eq("user_id", userId).eq("status", 1)));
            String pf = profileText(user);
            String fp = "tx:" + tx + "|as:" + as + "|li:" + li + "|pf:" + pf;
            return fp.length() > 200 ? fp.substring(0, 200) : fp;
        } catch (Exception e) {
            log.warn("数据指纹构建失败（当次不命中缓存，直接重算） userId={}", userId, e);
            return null;
        }
    }

    /** 聚合行 → "count@last" 签名（行为空返回 "0@null"） */
    private String aggSignature(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty() || rows.get(0) == null) {
            return "0@null";
        }
        Object cnt = rows.get(0).get("cnt");
        Object last = rows.get(0).get("last");
        return (cnt == null ? 0 : cnt) + "@" + (last == null ? "null" : last);
    }

    /** LLM 生成综述（失败降级模板） */
    private String generateSummary(Map<String, Object> indicators,
                                   List<Map<String, Object>> incomeSources,
                                   List<Map<String, Object>> debtRisks, PortalUser user) {
        String prompt = buildPrompt(indicators, incomeSources, debtRisks, user);
        String llm = callLlm(prompt);
        if (llm != null && !llm.isEmpty()) {
            return llm.trim();
        }
        StringBuilder sb = new StringBuilder("（模板分析）根据你的记账数据：");
        sb.append(indicators.get("rangeLabel")).append("月均收入 ¥")
                .append(yuan((BigDecimal) indicators.get("avgMonthlyIncome")))
                .append("、月均支出 ¥").append(yuan((BigDecimal) indicators.get("avgMonthlyExpense")))
                .append("，资产负债率 ").append(indicators.get("debtRatio")).append("%。");
        if (!debtRisks.isEmpty()) {
            sb.append("存在 ").append(debtRisks.size()).append(" 项债务风险需关注，");
        }
        if ((Integer) indicators.get("deficitMonths") >= 2) {
            sb.append("已连续入不敷出，请重点复盘支出。");
        } else {
            sb.append("整体收支可控。");
        }
        return sb.toString();
    }

    private String buildPrompt(Map<String, Object> indicators,
                               List<Map<String, Object>> incomeSources,
                               List<Map<String, Object>> debtRisks, PortalUser user) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位专业、友善的个人财务顾问。请根据以下用户记账数据，输出一份中文财务综述，")
                .append("包含：1）财务现状概述（2-3 句）；2）核心问题或亮点（如有债务风险请明确点出）；")
                .append("3）3 条以内的具体行动建议。\n")
                .append("要求：口语化、具体、引用数据；总字数 300 字以内；不要使用 Markdown 标记。\n\n");
        sb.append("【用户画像】").append(profileText(user)).append('\n');
        sb.append("【核心指标】统计范围：").append(indicators.get("rangeLabel"))
                .append("（含数据 ").append(indicators.get("sampleMonths")).append(" 个月）：月均收入 ¥")
                .append(yuan((BigDecimal) indicators.get("avgMonthlyIncome")))
                .append("，月均支出 ¥").append(yuan((BigDecimal) indicators.get("avgMonthlyExpense")))
                .append("，月储蓄率 ").append(indicators.get("savingRate")).append("%，")
                .append("月供合计 ¥").append(yuan((BigDecimal) indicators.get("monthlyRepayment")))
                .append("（占收入 ").append(indicators.get("repaymentPressure")).append("%），")
                .append("总资产 ¥").append(yuan((BigDecimal) indicators.get("totalAsset")))
                .append("，总负债 ¥").append(yuan(toBigDecimal(indicators.get("totalLiability"))))
                .append("，资产负债率 ").append(indicators.get("debtRatio")).append("%，")
                .append("连续入不敷出月数 ").append(indicators.get("deficitMonths")).append("。\n");
        sb.append("【收入来源】");
        if (incomeSources.isEmpty()) {
            sb.append("暂无收入记录。");
        } else {
            incomeSources.forEach(s -> sb.append(s.get("name")).append(" ¥")
                    .append(yuan(toBigDecimal(s.get("amount")))).append("（").append(s.get("ratio")).append("%）"));
        }
        sb.append('\n');
        sb.append("【已识别风险】");
        if (debtRisks.isEmpty()) {
            sb.append("无。");
        } else {
            debtRisks.forEach(r -> sb.append(r.get("title")).append("；"));
        }
        sb.append('\n');
        return sb.toString();
    }

    /**
     * 金额归一化：Map 中金额字段类型不可控（BigDecimal/Long/Integer/Number），
     * 统一转 BigDecimal，避免 ClassCastException。
     */
    private BigDecimal toBigDecimal(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal) return (BigDecimal) v;
        if (v instanceof Long || v instanceof Integer) return BigDecimal.valueOf(((Number) v).longValue());
        if (v instanceof Number) return BigDecimal.valueOf(((Number) v).doubleValue());
        try {
            return new BigDecimal(v.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
    /**
     * 调用 LLM（v11.39 场景感知：finance_analysis 有绑定则用绑定模型，否则默认模型；未配置/失败返回 null）
     * 模型选择走 AiSceneResolver 工厂（责任链：Agent → 直绑模型 → 默认），业务只带场景码。
     */
    private String callLlm(String prompt) {
        if (modelConfigService == null) {
            return null;
        }
        try {
            // 1. 场景绑定优先（ai_scene_config: finance_analysis）
            ChatLanguageModel model = null;
            if (sceneResolver != null) {
                try {
                    model = sceneResolver.resolveChatModel(SCENE_FINANCE_ANALYSIS);
                } catch (Exception e) {
                    log.warn("场景 finance_analysis 模型解析失败，回落默认: {}", e.getMessage());
                }
            }
            // 2. 无绑定 → 默认聊天模型
            if (model == null) {
                ModelConfig config = modelConfigService.getDefaultChatConfig();
                if (config == null) {
                    return null;
                }
                model = modelConfigService.createChatModel(config.getId());
            }
            ChatResponse response = model.chat(UserMessage.from(prompt));
            return response.aiMessage().text();
        } catch (Exception e) {
            log.warn("AI 财务综述生成失败，降级模板: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, Object> risk(String level, String title, String detail) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("level", level);
        r.put("title", title);
        r.put("detail", detail);
        return r;
    }

    private Map<String, Object> suggest(String icon, String title, String detail) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("icon", icon);
        s.put("title", title);
        s.put("detail", detail);
        return s;
    }

    private String yuan(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String pct(double v) {
        return String.format("%.1f%%", v * 100);
    }
}
