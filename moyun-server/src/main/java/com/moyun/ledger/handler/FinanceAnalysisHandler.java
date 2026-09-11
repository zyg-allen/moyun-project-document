package com.moyun.ledger.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.ext.ai.entity.AiSceneConfig;
import com.moyun.ext.ai2.handler.AbstractAiSceneHandler;
import com.moyun.ext.ai2.model.AiExecuteRequest;
import com.moyun.ext.ai2.model.AiExecuteResponse;
import com.moyun.ext.ai2.model.ChatOutcome;
import com.moyun.ledger.domain.entity.LedgerAssetAccount;
import com.moyun.ledger.domain.entity.LedgerBudget;
import com.moyun.ledger.domain.entity.LedgerCategory;
import com.moyun.ledger.domain.entity.LedgerLiabilityAccount;
import com.moyun.ledger.domain.entity.LedgerTransaction;
import com.moyun.ledger.mapper.LedgerAssetAccountMapper;
import com.moyun.ledger.mapper.LedgerBudgetMapper;
import com.moyun.ledger.mapper.LedgerCategoryMapper;
import com.moyun.ledger.mapper.LedgerLiabilityAccountMapper;
import com.moyun.ledger.mapper.LedgerTransactionMapper;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.service.IPortalUserService;
import com.moyun.core.base.entity.SysDictData;
import com.moyun.system.service.ISysDictTypeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
 * AI财务分析场景Handler（scene = finance_analysis）
 *
 * <p>v11.50 架构定位（配置即场景 / Service 薄化）：数据组装（查库/指标/趋势上下文）与 LLM
 * 变换全部收敛在本 Handler——业务 Service 只传 {userId, range}，本类返回完整报告数据。</p>
 *
 * <p>执行流程：
 * <ol>
 *   <li>查数据：流水（窗口内+近6月趋势）/负债/资产/预算/画像/分类</li>
 *   <li>算指标（数值护栏，LLM 只解读不计算）：收支/资产负债/月均/应急基金月数/健康分</li>
 *   <li>组装 ledgerContext：结构化数据 + 指标 + 趋势（逐月序列/分类环比/预算执行）+ 债务时点事实</li>
 *   <li>模板渲染：人设（ai_scene_config.agent_id 绑定智能体，网关注入）→ system 模板
 *       （{{ledgerContext}}{{window}}）→ user 模板 → 输出约束 output_schema</li>
 *   <li>调 LLM 生成 {summary, healthScore, risks[evidence], suggestions[expectedImpact]}</li>
 *   <li>LLM 失败降级：模板综述 + 空风险/建议（aiEnabled=false），指标照常返回——前端 KPI 不受影响</li>
 * </ol>
 * 管理页改模板/绑 Agent，下次调用立即生效，无需发版。</p>
 *
 * <p>归属 ledger 包原因：Handler 需注入 ledger Mapper（数据组装），放业务包避免
 * ai2 基础设施反向依赖业务模块（依赖方向恒为 业务 → ai2）。</p>
 *
 * @author laomao
 * @since 2026-09-10
 */
@Slf4j
@Component("financeAnalysisHandler")
public class FinanceAnalysisHandler extends AbstractAiSceneHandler {

    private static final String DICT_IDENTITY_TAG = "ledger_identity_tag";

    /** 分析维度 → 窗口起始偏移（自然月对齐）与展示文案 */
    private static final Map<String, Integer> RANGE_MONTHS = Map.of("month", 0, "3m", 3, "6m", 6, "year", 12);
    private static final Map<String, String> RANGE_LABEL = Map.of("month", "本月", "3m", "近3个月", "6m", "近6个月", "year", "近12个月");

    @Autowired
    private LedgerTransactionMapper transactionMapper;
    @Autowired
    private LedgerCategoryMapper categoryMapper;
    @Autowired
    private LedgerAssetAccountMapper assetAccountMapper;
    @Autowired
    private LedgerLiabilityAccountMapper liabilityAccountMapper;
    @Autowired
    private LedgerBudgetMapper budgetMapper;
    @Autowired
    private IPortalUserService portalUserService;
    @Autowired
    private ISysDictTypeService dictTypeService;

    /** 默认系统提示词（配置表 system_prompt_template 缺省时的回落值） */
    private static final String DEFAULT_SYSTEM = """
            你是一位资深个人财务顾问，为用户提供基于真实记账数据的财务分析。

            要求：
            1. 每个结论必须引用具体数据（金额/百分比/月份），禁止"建议合理规划""量入为出"等无数据支撑的空话
            2. 综述要讲"发现的故事"：钱从哪来、花到哪去、趋势如何、最值得注意的一件事
            3. 风险按严重度排序，并给出数据依据（evidence，如"近3月餐饮 ¥6,500，环比+65%"）
            4. 建议必须可执行、具体到动作，并量化预期效果（expectedImpact，如"每月约节省 ¥800"）
            5. 只输出 JSON，禁止输出 JSON 以外的任何内容

            数值以用户提供为准，不要自行计算修改。数据不足时基于已有数据客观分析，不臆造。风险/建议各 2-5 条，按重要性排序。""";

    /** 默认用户提示词模板（配置表 user_prompt_template 缺省时渲染） */
    private static final String DEFAULT_USER_TEMPLATE = """
            统计窗口：{{window}}

            用户财务数据（含规则引擎计算的精确指标与逐月趋势/预算执行/债务测算，数值请直接引用）：
            {{ledgerContext}}""";

    /** 默认输出约束（output_schema 缺省时使用） */
    private static final String DEFAULT_OUTPUT_SCHEMA = """
            {"summary": "string",
             "healthScore": "int 0-100",
             "risks": [{"level": "high|medium|low", "title": "string", "detail": "string", "evidence": "string"}],
             "suggestions": [{"icon": "string", "title": "string", "detail": "string", "expectedImpact": "string"}]}""";

    @Override
    public String getSceneCode() {
        return "finance_analysis";
    }

    /**
     * 统一结构化模式：网关下发场景配置，本 Handler 全权负责"查数→组装→渲染→LLM→降级"。
     * 输入 input：{userId: Long, range: month|3m|6m|year}
     * 返回 data：{summary, healthScore(规则值), risks, suggestions, aiEnabled, indicators, incomeSources}
     */
    @Override
    public AiExecuteResponse<?> execute(AiExecuteRequest request, AiSceneConfig config) {
        Long userId = requireInputLong(request, "userId");
        String range = normalizeRange(getInputString(request, "range"));
        String rangeLabel = RANGE_LABEL.get(range);
        LocalDate today = LocalDate.now();
        PortalUser user = portalUserService.selectPortalUserById(userId);

        // ===== 1. 查数据 =====
        LocalDate rangeStart = "month".equals(range)
                ? today.withDayOfMonth(1)
                : today.minusMonths(RANGE_MONTHS.get(range) - 1L).withDayOfMonth(1);
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

        // ===== 2. 核心指标（数值护栏） =====
        BigDecimal totalIncome = sumByType(recent, "income");
        BigDecimal totalExpense = sumByType(recent, "expense");
        BigDecimal monthlyRepayment = debts.stream()
                .filter(d -> d.getMonthlyPayment() != null)
                .map(LedgerLiabilityAccount::getMonthlyPayment)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalLiability = debts.stream()
                .filter(d -> d.getIncludeInTotal() == null || d.getIncludeInTotal() == 1)
                .map(d -> d.getBalance() == null ? BigDecimal.ZERO : d.getBalance())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<LedgerAssetAccount> assetAccounts = assetAccountMapper.selectList(
                new LambdaQueryWrapper<LedgerAssetAccount>()
                        .eq(LedgerAssetAccount::getUserId, userId)
                        .eq(LedgerAssetAccount::getStatus, 1)
                        .eq(LedgerAssetAccount::getIncludeInTotal, 1));
        BigDecimal totalAsset = assetAccounts.stream()
                .map(a -> a.getBalance() == null ? BigDecimal.ZERO : a.getBalance())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal liquidAsset = assetAccounts.stream()
                .filter(a -> LedgerAssetAccount.TYPE_CASH.equals(a.getType())
                        || LedgerAssetAccount.TYPE_SAVINGS.equals(a.getType())
                        || LedgerAssetAccount.TYPE_EWALLET.equals(a.getType())
                        || LedgerAssetAccount.TYPE_STORED_VALUE.equals(a.getType()))
                .map(a -> a.getBalance() == null ? BigDecimal.ZERO : a.getBalance())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double months = Math.max(1, recent.stream().map(LedgerTransaction::getTransactionDate)
                .map(YearMonth::from).distinct().count());
        BigDecimal avgMonthlyIncome = totalIncome.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        BigDecimal avgMonthlyExpense = totalExpense.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        BigDecimal emergencyFundMonths = avgMonthlyExpense.compareTo(BigDecimal.ZERO) > 0
                ? liquidAsset.divide(avgMonthlyExpense, 1, RoundingMode.HALF_UP) : null;

        // 近3月逐月判断是否入不敷出
        Map<YearMonth, BigDecimal[]> monthly = monthlySeries(recent);
        int deficitMonths = countDeficitMonths(monthly);
        double debtRatio = totalAsset.compareTo(BigDecimal.ZERO) > 0
                ? totalLiability.divide(totalAsset, 4, RoundingMode.HALF_UP).doubleValue()
                : (totalLiability.compareTo(BigDecimal.ZERO) > 0 ? 1.0 : 0.0);
        double repaymentPressure = avgMonthlyIncome.compareTo(BigDecimal.ZERO) > 0
                ? monthlyRepayment.divide(avgMonthlyIncome, 4, RoundingMode.HALF_UP).doubleValue()
                : (monthlyRepayment.compareTo(BigDecimal.ZERO) > 0 ? 1.0 : 0.0);
        double savingRate = avgMonthlyIncome.compareTo(BigDecimal.ZERO) > 0
                ? Math.max(0, avgMonthlyIncome.subtract(avgMonthlyExpense)
                        .divide(avgMonthlyIncome, 4, RoundingMode.HALF_UP).doubleValue()) : 0;

        // ===== 3. 收入结构 + 支出 Top5（前端契约 + LLM 上下文） =====
        Map<Long, BigDecimal> incomeByCat = sumByCategory(recent, "income");
        Map<Long, BigDecimal> expenseByCat = sumByCategory(recent, "expense");
        List<Map<String, Object>> incomeSources = buildCategoryShares(incomeByCat, catNames, 0);
        List<Map<String, Object>> topExpenses = buildCategoryShares(expenseByCat, catNames, 5);

        // ===== 4. 指标汇总（前端 KPI + LLM 护栏共用） =====
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
        indicators.put("range", range);
        indicators.put("rangeLabel", rangeLabel);
        indicators.put("rangeStart", rangeStart.toString());
        indicators.put("liquidAsset", liquidAsset);
        indicators.put("emergencyFundMonths", emergencyFundMonths);
        int healthScore = computeHealthScore(debtRatio, repaymentPressure, savingRate, deficitMonths);

        // ===== 5. LLM 变换（模板渲染 → 调用 → 解析；失败降级不抛错） =====
        Map<String, Object> aiReport = callLlm(request, config, userId, user, indicators,
                incomeSources, topExpenses, debts, expenseByCat, catNames, today, rangeLabel);
        String aiSummary = aiReport.get("summary") != null ? String.valueOf(aiReport.get("summary")) : "";
        boolean aiEnabled = !aiSummary.isBlank();
        if (!aiEnabled) {
            aiSummary = buildFallbackSummary(indicators);
        }
        List<Map<String, Object>> risks = castList(aiReport.get("risks"));
        List<Map<String, Object>> suggestions = castList(aiReport.get("suggestions"));

        // ===== 6. 返回完整报告数据（Service 落表/透传）；metadata 携带实际模型/token（网关补 agentUsed） =====
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("summary", aiSummary);
        data.put("healthScore", healthScore);
        data.put("risks", risks);
        data.put("suggestions", suggestions);
        data.put("aiEnabled", aiEnabled);
        data.put("indicators", indicators);
        data.put("incomeSources", incomeSources);
        AiExecuteResponse<Map<String, Object>> response = AiExecuteResponse.success(data);
        if (aiReport.get("_metadata") instanceof com.moyun.ext.ai2.model.AiMetadata m) {
            response.setMetadata(m);
        }
        return response;
    }

    // ==================== LLM 变换 ====================

    /**
     * 调 LLM 生成报告（综述/风险/建议）。上下文组装 + 模板渲染 + 降级都在本方法内完成，
     * 任何失败都返回空 Map（外层用模板综述降级），不向网关抛错——指标数据照常返回。
     */
    private Map<String, Object> callLlm(AiExecuteRequest request, AiSceneConfig config, Long userId,
                                        PortalUser user, Map<String, Object> indicators,
                                        List<Map<String, Object>> incomeSources,
                                        List<Map<String, Object>> topExpenses,
                                        List<LedgerLiabilityAccount> debts,
                                        Map<Long, BigDecimal> expenseByCat, Map<Long, String> catNames,
                                        LocalDate today, String rangeLabel) {
        try {
            // 1. ledgerContext = 画像 + 指标 + 收支结构 + 负债时点事实 + 趋势（逐月/环比/预算执行）
            Map<String, Object> context = new LinkedHashMap<>();
            context.put("profile", profileText(user));
            context.put("indicators", indicators);
            context.put("incomeSources", incomeSources);
            context.put("topExpenses", topExpenses);
            context.put("debts", debts.stream().map(this::debtFact).collect(Collectors.toList()));
            Map<String, Object> trendContext = buildTrendContext(userId, today, expenseByCat, catNames);
            if (trendContext.get("monthlyTrend") instanceof List<?> l && !l.isEmpty()) {
                context.put("monthlyTrend", l);
            }
            if (trendContext.get("categoryMoM") instanceof List<?> l && !l.isEmpty()) {
                context.put("categoryMoM", l);
            }
            if (trendContext.get("budgetExecution") != null) {
                context.put("budgetExecution", trendContext.get("budgetExecution"));
            }

            // 2. 注入模板变量（{{ledgerContext}}{{window}}），复用基类模板渲染机制
            if (request.getInput() == null) {
                request.setInput(new HashMap<>());
            }
            request.getInput().put("ledgerContext", MAPPER.writeValueAsString(context));
            request.getInput().put("window", rangeLabel + "（自 " + indicators.get("rangeStart")
                    + " 起，含数据 " + indicators.get("sampleMonths") + " 个月；另附近6个月趋势数据）");

            // 3. 提示词组装：人设 → system 模板 → 输出约束；user 模板
            String system = buildSystemPrompt(request, config);
            if (system == null || system.isBlank()) {
                system = DEFAULT_SYSTEM;
            }
            system = mergePersona(request, system);
            system = appendOutputSchema(system, config);
            String userPrompt = buildUserPrompt(request, config);
            if (userPrompt == null || userPrompt.isBlank()) {
                userPrompt = renderTemplate(DEFAULT_USER_TEMPLATE, request);
            }

            // 4. 调用 + 解析（v11.51：结构化结果，实际模型/token 进响应 metadata）
            ChatOutcome outcome =
                    chatDetailed(getSceneCode(), system, userPrompt);
            if (!outcome.isSuccess()) {
                log.info("[ai2:{}] LLM 无返回，降级模板综述 userId={}", getSceneCode(), userId);
                return Map.of();
            }
            Map<String, Object> parsed = parseJsonMap(outcome.getText());
            if (parsed == null) {
                // v11.53 文本兜底：LLM 返回了内容但非合法 JSON（人设带偏成对话/输出被截断），
                // 清洗后作为综述展示（风险/建议留空）——LLM 生成的内容不应整体丢弃。
                // 契约：aiEnabled=true + risks/suggestions 空，前端自然只渲染综述区
                String raw = cleanLlmText(outcome.getText());
                if (!raw.isBlank()) {
                    log.warn("[ai2:{}] LLM 返回非JSON（前100字符: {}），文本兜底为综述 userId={}",
                            getSceneCode(), raw.substring(0, Math.min(100, raw.length())), userId);
                    Map<String, Object> textFallback = new LinkedHashMap<>();
                    textFallback.put("summary", raw);
                    textFallback.put("_metadata", buildMetadata(outcome));
                    return textFallback;
                }
                log.warn("[ai2:{}] LLM 返回解析失败，降级模板综述 userId={}", getSceneCode(), userId);
                return Map.of();
            }
            parsed.put("_metadata", buildMetadata(outcome));
            return parseReport(parsed);
        } catch (Exception e) {
            log.warn("[ai2:{}] LLM 财务分析异常，降级模板综述: {}", getSceneCode(), e.getMessage());
            return Map.of();
        }
    }

    /** system 提示词追加输出约束（配置的 output_schema 优先） */
    private String appendOutputSchema(String system, AiSceneConfig config) {
        String schema = (config != null && config.getOutputSchema() != null && !config.getOutputSchema().isBlank())
                ? config.getOutputSchema() : DEFAULT_OUTPUT_SCHEMA;
        return system + "\n\n输出结构（严格按此 JSON 输出）：\n" + schema;
    }

    /** LLM JSON → 报告结构（risks[].evidence / suggestions[].expectedImpact 透传） */
    private Map<String, Object> parseReport(Map<String, Object> parsed) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("summary", str(parsed, "summary"));
        Object hs = parsed.get("healthScore");
        report.put("healthScore", hs instanceof Number n ? n.intValue() : null);
        if (parsed.get("risks") instanceof List<?> riskList) {
            List<Map<String, Object>> risks = new ArrayList<>();
            for (Object r : riskList) {
                if (r instanceof Map<?, ?> rm) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("level", strOrEmpty(rm, "level"));
                    item.put("title", strOrEmpty(rm, "title"));
                    item.put("detail", strOrEmpty(rm, "detail"));
                    item.put("evidence", strOrEmpty(rm, "evidence"));
                    risks.add(item);
                }
            }
            report.put("risks", risks);
        }
        if (parsed.get("suggestions") instanceof List<?> sugList) {
            List<Map<String, Object>> suggestions = new ArrayList<>();
            for (Object s : sugList) {
                if (s instanceof Map<?, ?> sm) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("icon", strOrEmpty(sm, "icon"));
                    item.put("title", strOrEmpty(sm, "title"));
                    item.put("detail", strOrEmpty(sm, "detail"));
                    item.put("expectedImpact", strOrEmpty(sm, "expectedImpact"));
                    suggestions.add(item);
                }
            }
            report.put("suggestions", suggestions);
        }
        return report;
    }

    // ==================== 趋势上下文（近6个月） ====================

    /**
     * 趋势上下文：monthlyTrend（逐月收支序列）/ categoryMoM（分类环比 Top5）/
     * budgetExecution（预算执行 usedPct+月末剩余天数）。全部为规则引擎算好的事实，LLM 只解读。
     */
    private Map<String, Object> buildTrendContext(Long userId, LocalDate today,
                                                  Map<Long, BigDecimal> curMonthByCat, Map<Long, String> catNames) {
        Map<String, Object> ctx = new LinkedHashMap<>();
        LocalDate trendStart = today.minusMonths(5).withDayOfMonth(1);
        List<LedgerTransaction> trailing = transactionMapper.selectList(new LambdaQueryWrapper<LedgerTransaction>()
                .eq(LedgerTransaction::getUserId, userId)
                .eq(LedgerTransaction::getStatus, 1)
                .ge(LedgerTransaction::getTransactionDate, trendStart)
                .le(LedgerTransaction::getTransactionDate, today));

        // 1. 逐月收支序列
        Map<YearMonth, BigDecimal[]> series = monthlySeries(trailing);
        List<Map<String, Object>> monthlyTrend = new ArrayList<>();
        for (Map.Entry<YearMonth, BigDecimal[]> e : series.entrySet()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("month", e.getKey().toString());
            m.put("income", e.getValue()[0]);
            m.put("expense", e.getValue()[1]);
            m.put("surplus", e.getValue()[0].subtract(e.getValue()[1]));
            monthlyTrend.add(m);
        }
        ctx.put("monthlyTrend", monthlyTrend);

        // 2. 分类环比（本月 vs 上月，Top5 显著变化）
        YearMonth cur = YearMonth.from(today);
        YearMonth prev = cur.minusMonths(1);
        Map<Long, BigDecimal> prevByCat = new LinkedHashMap<>();
        for (LedgerTransaction t : trailing) {
            if (!"expense".equals(t.getType()) || t.getCategoryId() == null) continue;
            if (prev.equals(YearMonth.from(t.getTransactionDate()))) {
                prevByCat.merge(t.getCategoryId(),
                        t.getAmount() == null ? BigDecimal.ZERO : t.getAmount(), BigDecimal::add);
            }
        }
        List<Map<String, Object>> categoryMoM = new ArrayList<>();
        curMonthByCat.entrySet().stream()
                .filter(e -> e.getValue().subtract(prevByCat.getOrDefault(e.getKey(), BigDecimal.ZERO))
                        .compareTo(BigDecimal.ZERO) != 0)
                .sorted((a, b) -> {
                    BigDecimal da = a.getValue().subtract(prevByCat.getOrDefault(a.getKey(), BigDecimal.ZERO)).abs();
                    BigDecimal db = b.getValue().subtract(prevByCat.getOrDefault(b.getKey(), BigDecimal.ZERO)).abs();
                    return db.compareTo(da);
                })
                .limit(5)
                .forEach(e -> {
                    BigDecimal prevAmount = prevByCat.getOrDefault(e.getKey(), BigDecimal.ZERO);
                    BigDecimal delta = e.getValue().subtract(prevAmount);
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", catNames.getOrDefault(e.getKey(), "其他"));
                    m.put("currentMonth", e.getValue());
                    m.put("previousMonth", prevAmount);
                    m.put("delta", delta);
                    m.put("changePct", prevAmount.compareTo(BigDecimal.ZERO) > 0
                            ? delta.multiply(BigDecimal.valueOf(100))
                                    .divide(prevAmount, 1, RoundingMode.HALF_UP).doubleValue()
                            : null);
                    categoryMoM.add(m);
                });
        ctx.put("categoryMoM", categoryMoM);

        // 3. 预算执行（本月总预算 + 分类预算；无预算时省略）
        List<LedgerBudget> budgets = budgetMapper.selectList(new LambdaQueryWrapper<LedgerBudget>()
                .eq(LedgerBudget::getUserId, userId)
                .eq(LedgerBudget::getYear, today.getYear())
                .eq(LedgerBudget::getMonth, today.getMonthValue()));
        if (!budgets.isEmpty()) {
            BigDecimal totalBudget = budgets.stream()
                    .filter(b -> b.getCategoryId() == null)
                    .map(b -> b.getAmount() == null ? BigDecimal.ZERO : b.getAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal curMonthExpense = curMonthByCat.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            Map<String, Object> budgetExecution = new LinkedHashMap<>();
            budgetExecution.put("totalBudget", totalBudget);
            budgetExecution.put("totalSpent", curMonthExpense);
            budgetExecution.put("remaining", totalBudget.subtract(curMonthExpense));
            budgetExecution.put("usedPct", totalBudget.compareTo(BigDecimal.ZERO) > 0
                    ? curMonthExpense.multiply(BigDecimal.valueOf(100))
                            .divide(totalBudget, 1, RoundingMode.HALF_UP).doubleValue()
                    : null);
            budgetExecution.put("daysLeftInMonth", today.lengthOfMonth() - today.getDayOfMonth() + 1);
            List<Map<String, Object>> catBudgets = new ArrayList<>();
            for (LedgerBudget b : budgets) {
                if (b.getCategoryId() == null) continue;
                BigDecimal budget = b.getAmount() == null ? BigDecimal.ZERO : b.getAmount();
                BigDecimal spent = curMonthByCat.getOrDefault(b.getCategoryId(), BigDecimal.ZERO);
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", catNames.getOrDefault(b.getCategoryId(), "分类#" + b.getCategoryId()));
                m.put("budget", budget);
                m.put("spent", spent);
                m.put("usedPct", budget.compareTo(BigDecimal.ZERO) > 0
                        ? spent.multiply(BigDecimal.valueOf(100))
                                .divide(budget, 1, RoundingMode.HALF_UP).doubleValue()
                        : null);
                catBudgets.add(m);
            }
            if (!catBudgets.isEmpty()) {
                budgetExecution.put("categories", catBudgets);
            }
            ctx.put("budgetExecution", budgetExecution);
        }
        return ctx;
    }

    // ==================== 工具方法 ====================

    /** 负债明细 → 时点事实（到期日/利率/月供/清偿测算；不做结论性判断） */
    private Map<String, Object> debtFact(LedgerLiabilityAccount d) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("name", d.getName());
        f.put("type", d.getType());
        f.put("balance", d.getBalance());
        if (d.getMonthlyPayment() != null) f.put("monthlyPayment", d.getMonthlyPayment());
        if (d.getAnnualRate() != null) f.put("annualRate", d.getAnnualRate());
        if (d.getDueDate() != null) f.put("dueDate", d.getDueDate().toString());
        if (d.getRepaymentDay() != null) f.put("repaymentDay", d.getRepaymentDay());
        if (d.getTotalTerms() != null && d.getPaidTerms() != null) {
            f.put("progress", d.getPaidTerms() + "/" + d.getTotalTerms() + "期");
        }
        if (d.getMonthlyPayment() != null && d.getMonthlyPayment().compareTo(BigDecimal.ZERO) > 0
                && d.getBalance() != null && d.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            f.put("payoffMonths", d.getBalance().divide(d.getMonthlyPayment(), 0, RoundingMode.CEILING).intValue());
        }
        return f;
    }

    /** 财务健康分：储蓄率 + 收支平衡 + 负债率 + 还款压力（各 25 分，简单可解释） */
    private int computeHealthScore(double debtRatio, double repaymentPressure, double savingRate, int deficitMonths) {
        double score = Math.min(25, savingRate * 100 * 0.25)
                + (deficitMonths >= 2 ? 0 : 25)
                + (debtRatio < 0.5 ? 25 : debtRatio < 0.8 ? 12 : 0)
                + (repaymentPressure < 0.3 ? 25 : repaymentPressure < 0.5 ? 12 : 0);
        return Math.max(0, Math.min(100, (int) Math.round(score)));
    }

    /** LLM 失败时的模板综述（无风险/建议列表——先看 LLM 效果，不做规则兜底） */
    private String buildFallbackSummary(Map<String, Object> indicators) {
        StringBuilder sb = new StringBuilder("（模板分析）根据你的记账数据：");
        sb.append(indicators.get("rangeLabel")).append("月均收入 ¥")
                .append(yuan((BigDecimal) indicators.get("avgMonthlyIncome")))
                .append("、月均支出 ¥").append(yuan((BigDecimal) indicators.get("avgMonthlyExpense")))
                .append("，资产负债率 ").append(indicators.get("debtRatio")).append("%。");
        if ((Integer) indicators.get("deficitMonths") >= 2) {
            sb.append("已连续入不敷出，请重点复盘支出。");
        } else {
            sb.append("整体收支可控。");
        }
        return sb.toString();
    }

    /** 流水 → 逐月收支序列（YearMonth → [income, expense]） */
    private Map<YearMonth, BigDecimal[]> monthlySeries(List<LedgerTransaction> transactions) {
        Map<YearMonth, BigDecimal[]> series = new LinkedHashMap<>();
        for (LedgerTransaction t : transactions) {
            if (!"income".equals(t.getType()) && !"expense".equals(t.getType())) continue;
            BigDecimal[] arr = series.computeIfAbsent(YearMonth.from(t.getTransactionDate()), k -> {
                BigDecimal[] a = new BigDecimal[2];
                a[0] = BigDecimal.ZERO;
                a[1] = BigDecimal.ZERO;
                return a;
            });
            BigDecimal amount = t.getAmount() == null ? BigDecimal.ZERO : t.getAmount();
            if ("income".equals(t.getType())) {
                arr[0] = arr[0].add(amount);
            } else {
                arr[1] = arr[1].add(amount);
            }
        }
        return series;
    }

    /** 近3月入不敷出的月数 */
    private int countDeficitMonths(Map<YearMonth, BigDecimal[]> monthly) {
        int deficit = 0;
        List<YearMonth> keys = new ArrayList<>(monthly.keySet());
        for (int i = Math.max(0, keys.size() - 3); i < keys.size(); i++) {
            BigDecimal[] arr = monthly.get(keys.get(i));
            if (arr[1].compareTo(arr[0]) > 0) deficit++;
        }
        return deficit;
    }

    /** 按收支类型汇总金额 */
    private BigDecimal sumByType(List<LedgerTransaction> transactions, String type) {
        return transactions.stream()
                .filter(t -> type.equals(t.getType()))
                .map(t -> t.getAmount() == null ? BigDecimal.ZERO : t.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** 按分类汇总金额（仅 income/expense） */
    private Map<Long, BigDecimal> sumByCategory(List<LedgerTransaction> transactions, String type) {
        Map<Long, BigDecimal> byCat = new LinkedHashMap<>();
        for (LedgerTransaction t : transactions) {
            if (!type.equals(t.getType()) || t.getCategoryId() == null) continue;
            byCat.merge(t.getCategoryId(),
                    t.getAmount() == null ? BigDecimal.ZERO : t.getAmount(), BigDecimal::add);
        }
        return byCat;
    }

    /** 分类金额聚合 → 占比列表（amount 降序；limit<=0 不限制） */
    private List<Map<String, Object>> buildCategoryShares(Map<Long, BigDecimal> byCat,
                                                          Map<Long, String> catNames, int limit) {
        List<Map<String, Object>> shares = new ArrayList<>();
        BigDecimal total = byCat.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        byCat.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .forEach(e -> {
                    if (limit > 0 && shares.size() >= limit) {
                        return;
                    }
                    Map<String, Object> src = new HashMap<>();
                    src.put("name", catNames.getOrDefault(e.getKey(), "其他"));
                    src.put("amount", e.getValue());
                    src.put("ratio", total.compareTo(BigDecimal.ZERO) > 0
                            ? e.getValue().multiply(BigDecimal.valueOf(100))
                                    .divide(total, 1, RoundingMode.HALF_UP)
                                    .doubleValue()
                            : 0);
                    shares.add(src);
                });
        return shares;
    }

    /** 分类名映射（0=系统预设 + 当前用户自定义） */
    private Map<Long, String> loadCategoryNames(Long userId) {
        return categoryMapper.selectList(new LambdaQueryWrapper<LedgerCategory>()
                        .and(w -> w.eq(LedgerCategory::getUserId, 0L)
                                .or().eq(LedgerCategory::getUserId, userId)))
                .stream().collect(Collectors.toMap(LedgerCategory::getId, LedgerCategory::getName, (a, b) -> a));
    }

    /** 画像文本（身份标签/职位/公司 → LLM 上下文） */
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

    private String normalizeRange(String range) {
        return RANGE_MONTHS.containsKey(range) ? range : "month";
    }

    /** 从 input 取必填 Long 参数 */
    private Long requireInputLong(AiExecuteRequest request, String key) {
        Object value = request.getInput() != null ? request.getInput().get(key) : null;
        if (value instanceof Number n) {
            return n.longValue();
        }
        if (value instanceof String s && !s.isBlank()) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        throw new IllegalArgumentException("缺少必填参数: " + key);
    }

    /** Object → List<Map<String,Object>>（非 List 或元素非 Map 时返回空列表） */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object value) {
        if (!(value instanceof List<?> list)) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> m) {
                result.add((Map<String, Object>) m);
            }
        }
        return result;
    }

    private String str(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value != null ? String.valueOf(value) : null;
    }

    private String strOrEmpty(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value != null ? String.valueOf(value) : "";
    }

    private String yuan(BigDecimal amount) {
        return amount == null ? "0.00" : amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
