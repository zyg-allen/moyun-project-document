package com.moyun.ledger.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.core.base.entity.SysDictData;
import com.moyun.ext.ai.entity.AiSceneConfig;
import com.moyun.ext.ai.enums.AiSceneEnum;
import com.moyun.ext.ai.mapper.AiSceneConfigMapper;
import com.moyun.ext.aigateway.support.AiSceneJsonClient;
import com.moyun.ledger.domain.entity.LedgerAiAnalysisReport;
import com.moyun.ledger.domain.entity.LedgerAssetAccount;
import com.moyun.ledger.domain.entity.LedgerBudget;
import com.moyun.ledger.domain.entity.LedgerCategory;
import com.moyun.ledger.domain.entity.LedgerLiabilityAccount;
import com.moyun.ledger.domain.entity.LedgerTransaction;
import com.moyun.ledger.mapper.LedgerAiAnalysisReportMapper;
import com.moyun.ledger.mapper.LedgerAssetAccountMapper;
import com.moyun.ledger.mapper.LedgerBudgetMapper;
import com.moyun.ledger.mapper.LedgerCategoryMapper;
import com.moyun.ledger.mapper.LedgerLiabilityAccountMapper;
import com.moyun.ledger.mapper.LedgerTransactionMapper;
import com.moyun.ledger.service.ILedgerAiAnalysisService;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.mapper.PortalUserMapper;
import com.moyun.portal.service.IPortalUserService;
import com.moyun.system.service.ISysDictTypeService;
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
 * <p>架构（2B.4 查数下沉后定位）：本 Service 全权负责业务数据侧——查数（流水/负债/资产/
 * 预算/画像/分类）+ 指标计算（数值护栏，LLM 只解读不计算）+ ledgerContext 组装，
 * 再经 {@link AiSceneJsonClient} 走统一网关（finance_analysis 配置行由
 * DefaultSceneExecutor 配置驱动执行：模板渲染 → LLM → 解析）。提示词模板/人设
 * （ai_agent 47）/输出结构由 ai_scene_config 配置驱动，管理页修改即时生效。</p>
 *
 * <p>流程：指纹命中快照→直接返回；否则查数+算指标 → 组装 window/ledgerContext →
 * 网关 LLM 综述（失败本地降级模板综述，指标照常返回）→ 落表月度快照 → 返回。</p>
 *
 * <p><strong>异步任务选型（双轨制定位）</strong>：本服务采用 Redis 状态 + 线程池的
 * 轻量异步模式（任务态 30 分钟 TTL），适用于财务分析这类短时长、结果时效性强的任务
 * ——报告快照本身落 ledger_ai_analysis_report 持久化，任务态无需留痕；
 * 长任务/需审计追溯的 AI 任务走表驱动 AiTaskService（portal_ai_task），
 * 选型规则详见其类注释。</p>
 *
 * @author moyun
 */
@Service
public class LedgerAiAnalysisServiceImpl implements ILedgerAiAnalysisService {
    /** 本服务所属 AI 场景代码（绑定见 ai_scene_config，2B.4 起配置驱动执行） */
    private static final String SCENE_FINANCE_ANALYSIS = AiSceneEnum.FINANCE_ANALYSIS.getCode();

    private static final Logger log = LoggerFactory.getLogger(LedgerAiAnalysisServiceImpl.class);

    private static final String DICT_IDENTITY_TAG = "ledger_identity_tag";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 合法分析维度（非法值回落 month） */
    private static final List<String> VALID_RANGES = List.of("month", "3m", "6m", "year");
    /** 分析维度 → 窗口起始偏移（自然月对齐，month=0 即本月） */
    private static final Map<String, Integer> RANGE_MONTHS = Map.of("month", 0, "3m", 3, "6m", 6, "year", 12);
    private static final Map<String, String> RANGE_LABEL = Map.of("month", "本月", "3m", "近3个月", "6m", "近6个月", "year", "近12个月");

    @Autowired
    private IPortalUserService portalUserService;
    @Autowired
    private PortalUserMapper portalUserMapper;
    @Autowired
    private ISysDictTypeService dictTypeService;
    /** 报告快照 Mapper */
    @Autowired
    private LedgerAiAnalysisReportMapper reportMapper;
    /** 场景配置读取（指纹纳入配置版本，改模板自动失效当月快照） */
    @Autowired
    private AiSceneConfigMapper sceneConfigMapper;
    /** 查数 Mapper（2B.4 自 FinanceAnalysisHandler 下沉：流水/资产/负债/预算/分类） */
    @Autowired
    private LedgerTransactionMapper transactionMapper;
    @Autowired
    private LedgerAssetAccountMapper assetAccountMapper;
    @Autowired
    private LedgerLiabilityAccountMapper liabilityAccountMapper;
    @Autowired
    private LedgerBudgetMapper budgetMapper;
    @Autowired
    private LedgerCategoryMapper categoryMapper;

    /** 业务标准入口（失败统一返回 null，本服务降级模板综述） */
    @Autowired(required = false)
    private AiSceneJsonClient aiSceneJsonClient;

    // ==================== 异步任务 ====================

    private static final String TASK_KEY_PREFIX = "ledger:ai:analysis:task:";
    private static final String RUNNING_KEY_PREFIX = "ledger:ai:analysis:running:";
    /** 任务状态保留时长（完成后仍可轮询取结果） */
    private static final int TASK_TTL_MINUTES = 30;
    /** 进行中任务标记时长（兜底防任务挂死永不释放，正常结束即删） */
    private static final int RUNNING_TTL_MINUTES = 15;

    @Autowired
    private com.moyun.core.config.redis.RedisCache redisCache;

    @org.springframework.beans.factory.annotation.Qualifier("applicationTaskExecutor")
    @Autowired
    private org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor taskExecutor;

    @Override
    public Map<String, Object> submitAnalysisTask(Long userId, String range) {
        if (!VALID_RANGES.contains(range)) {
            range = "month";
        }
        // 防重复提交：同用户进行中任务直接复用（不重复烧 token）
        String runningKey = RUNNING_KEY_PREFIX + userId;
        String existingTaskId = redisCache.getCacheObject(runningKey);
        if (existingTaskId != null) {
            Map<String, Object> existing = redisCache.getCacheObject(TASK_KEY_PREFIX + existingTaskId);
            if (existing != null && !"failed".equals(existing.get("status"))) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("taskId", existingTaskId);
                r.put("status", existing.get("status"));
                r.put("resubmitted", true);
                return r;
            }
        }
        String taskId = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Map<String, Object> task = new LinkedHashMap<>();
        task.put("taskId", taskId);
        task.put("userId", userId);
        task.put("range", range);
        task.put("status", "pending");
        task.put("submitTime", System.currentTimeMillis());
        redisCache.setCacheObject(TASK_KEY_PREFIX + taskId, task, TASK_TTL_MINUTES, java.util.concurrent.TimeUnit.MINUTES);
        redisCache.setCacheObject(runningKey, taskId, RUNNING_TTL_MINUTES, java.util.concurrent.TimeUnit.MINUTES);
        String finalRange = range;
        taskExecutor.submit(() -> executeTask(taskId, userId, finalRange));
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("taskId", taskId);
        r.put("status", "pending");
        r.put("resubmitted", false);
        return r;
    }

    /** 异步执行分析并回写任务状态（refresh 恒为 true：用户主动触发的都是重算） */
    private void executeTask(String taskId, Long userId, String range) {
        String taskKey = TASK_KEY_PREFIX + taskId;
        updateTaskStatus(taskKey, "running");
        try {
            Map<String, Object> report = analyze(userId, true, range);
            Map<String, Object> task = redisCache.getCacheObject(taskKey);
            if (task != null) {
                task.put("status", "success");
                task.put("report", report);
                redisCache.setCacheObject(taskKey, task, TASK_TTL_MINUTES, java.util.concurrent.TimeUnit.MINUTES);
            }
        } catch (Exception e) {
            log.warn("AI 分析任务执行失败 taskId={} userId={}", taskId, userId, e);
            Map<String, Object> task = redisCache.getCacheObject(taskKey);
            if (task != null) {
                task.put("status", "failed");
                task.put("error", e.getMessage() != null ? e.getMessage() : "分析失败，请稍后重试");
                redisCache.setCacheObject(taskKey, task, TASK_TTL_MINUTES, java.util.concurrent.TimeUnit.MINUTES);
            }
        } finally {
            redisCache.deleteObject(RUNNING_KEY_PREFIX + userId);
        }
    }

    private void updateTaskStatus(String taskKey, String status) {
        Map<String, Object> task = redisCache.getCacheObject(taskKey);
        if (task != null) {
            task.put("status", status);
            redisCache.setCacheObject(taskKey, task, TASK_TTL_MINUTES, java.util.concurrent.TimeUnit.MINUTES);
        }
    }

    @Override
    public Map<String, Object> getAnalysisTask(Long userId, String taskId) {
        Map<String, Object> task = redisCache.getCacheObject(TASK_KEY_PREFIX + taskId);
        if (task == null) {
            return Map.of("status", "not_found");
        }
        // 归属校验：非本人任务视为不存在
        Object owner = task.get("userId");
        if (owner == null || !String.valueOf(owner).equals(String.valueOf(userId))) {
            return Map.of("status", "not_found");
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("status", task.get("status"));
        if ("success".equals(task.get("status")) && task.get("report") instanceof Map<?, ?> report) {
            r.put("report", castMap(report));
        }
        if ("failed".equals(task.get("status"))) {
            r.put("error", task.get("error"));
        }
        return r;
    }

    @Override
    public Map<String, Object> analyze(Long userId, boolean refresh, String range) {
        // 维度归一化（非法值回落本月）
        if (!VALID_RANGES.contains(range)) {
            range = "month";
        }
        String rangeLabel = RANGE_LABEL.get(range);
        String period = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
        // 四维度均独立快照（uk user_id+period+analysis_range），切换 tab 各查各的
        // refresh=false（页面进入/切 tab）= 纯查询：命中直接返回，未命中返回 exists:false
        // 由前端引导显式"去分析"（异步任务 refresh=true）——杜绝进入页面隐式触发 LLM
        if (!refresh) {
            LedgerAiAnalysisReport cached = findReport(userId, period, range);
            if (cached != null) {
                Map<String, Object> r = reportToResult(cached, true);
                r.put("range", range);
                r.put("rangeLabel", rangeLabel);
                r.put("reportId", cached.getId());
                return r;
            }
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("exists", false);
            empty.put("range", range);
            empty.put("rangeLabel", rangeLabel);
            empty.put("period", period);
            return empty;
        }
        PortalUser user = portalUserService.selectPortalUserById(userId);
        LocalDate today = LocalDate.now();
        // 数据指纹（流水/资产/负债/预算/画像 + 场景配置变更痕迹）——落库随报告存档
        String fingerprint = buildFingerprint(userId, today, user);

        // ===== 1. 查数 + 指标计算（2B.4 自 FinanceAnalysisHandler 下沉；数值护栏，LLM 只解读不计算）=====
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

        // ===== 2. 收入结构 + 支出 Top5（前端契约 + LLM 上下文）=====
        Map<Long, BigDecimal> incomeByCat = sumByCategory(recent, "income");
        Map<Long, BigDecimal> expenseByCat = sumByCategory(recent, "expense");
        List<Map<String, Object>> incomeSources = buildCategoryShares(incomeByCat, catNames, 0);
        List<Map<String, Object>> topExpenses = buildCategoryShares(expenseByCat, catNames, 5);

        // ===== 3. 指标汇总（前端 KPI + LLM 护栏共用）=====
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

        // ===== 4. LLM 综述（网关配置驱动；失败本地降级模板综述——指标照常返回，前端 KPI 不受影响）=====
        String window = rangeLabel + "（自 " + rangeStart + " 起，含数据 " + (int) months + " 个月；另附近6个月趋势数据）";
        String ledgerContext = buildLedgerContext(userId, user, indicators, incomeSources, topExpenses,
                debts, expenseByCat, catNames, today);
        Map<String, Object> aiReport = callLlmSummary(window, ledgerContext, userId);
        String aiSummary = aiReport != null && aiReport.get("summary") != null
                && !String.valueOf(aiReport.get("summary")).isBlank()
                ? String.valueOf(aiReport.get("summary")) : "";
        boolean aiEnabled = !aiSummary.isBlank();
        List<Map<String, Object>> risks = aiEnabled ? castList(aiReport.get("risks")) : new ArrayList<>();
        List<Map<String, Object>> suggestions = aiEnabled ? castList(aiReport.get("suggestions")) : new ArrayList<>();
        if (!aiEnabled) {
            aiSummary = buildFallbackSummary(indicators);
        }

        // ===== 5. 组装返回 =====
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("period", period);
        result.put("healthScore", healthScore);
        result.put("indicators", indicators);
        result.put("incomeSources", incomeSources);
        result.put("debtRisks", risks);
        result.put("suggestions", suggestions);
        result.put("aiSummary", aiSummary);
        result.put("aiEnabled", aiEnabled);
        result.put("fromCache", false);
        result.put("range", range);
        result.put("rangeLabel", rangeLabel);

        // 落库快照（覆盖式：同 user+period+range 唯一一份，重新分析更新不新增）
        upsertReport(userId, period, range, healthScore, indicators, incomeSources,
                risks, suggestions, aiSummary, aiEnabled, profileStamp(user), fingerprint);
        return result;
    }

    // ==================== LLM 综述（网关配置驱动） ====================

    /**
     * 经统一网关执行 finance_analysis 场景（DefaultSceneExecutor 配置驱动：
     * {{window}}/{{data:财务数据|ledgerContext}} 模板渲染 → LLM → JSON 解析）。
     * 任何失败（AI 未启用/限流/调用失败/解析失败）返回 null——外层降级模板综述。
     */
    private Map<String, Object> callLlmSummary(String window, String ledgerContext, Long userId) {
        if (aiSceneJsonClient == null) {
            log.info("[ledger:ai] AI 服务未启用，降级模板综述 userId={}", userId);
            return null;
        }
        Map<String, Object> input = new HashMap<>();
        input.put("window", window);
        input.put("ledgerContext", ledgerContext);
        JsonNode node = aiSceneJsonClient.executeForJson(SCENE_FINANCE_ANALYSIS, input, userId);
        if (node == null) {
            log.info("[ledger:ai] LLM 财务分析无返回，降级模板综述 userId={}", userId);
            return null;
        }
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("summary", node.path("summary").asText(null));
        report.put("risks", readListField(node, "risks"));
        report.put("suggestions", readListField(node, "suggestions"));
        return report;
    }

    /**
     * ledgerContext = 画像 + 指标 + 收支结构 + 负债时点事实 + 趋势（逐月/环比/预算执行）。
     * 全部为规则引擎算好的事实，LLM 只解读；经网关数据通道占位符隔离注入提示词。
     */
    private String buildLedgerContext(Long userId, PortalUser user, Map<String, Object> indicators,
                                      List<Map<String, Object>> incomeSources,
                                      List<Map<String, Object>> topExpenses,
                                      List<LedgerLiabilityAccount> debts,
                                      Map<Long, BigDecimal> expenseByCat, Map<Long, String> catNames,
                                      LocalDate today) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("profile", profileStamp(user));
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
        try {
            return MAPPER.writeValueAsString(context);
        } catch (Exception e) {
            log.warn("[ledger:ai] ledgerContext 序列化失败（数据通道空值丢弃，LLM 基于指标分析）: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 趋势上下文（近6个月）：monthlyTrend（逐月收支序列）/ categoryMoM（分类环比 Top5）/
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

    /** 按口径查询唯一快照（查询与覆盖同一口径，保证一致性） */
    private LedgerAiAnalysisReport findReport(Long userId, String period, String range) {
        return reportMapper.selectOne(new LambdaQueryWrapper<LedgerAiAnalysisReport>()
                .eq(LedgerAiAnalysisReport::getUserId, userId)
                .eq(LedgerAiAnalysisReport::getPeriod, period)
                .eq(LedgerAiAnalysisReport::getAnalysisRange, range)
                .last("LIMIT 1"));
    }

    // ==================== 查数与指标工具（2B.4 自 FinanceAnalysisHandler 下沉） ====================

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

    private String yuan(BigDecimal amount) {
        return amount == null ? "0.00" : amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /** JsonNode 数组字段 → List<Map>（非数组/元素非对象/异常均返回空列表） */
    private List<Map<String, Object>> readListField(JsonNode node, String field) {
        JsonNode arr = node.get(field);
        if (arr == null || !arr.isArray()) {
            return new ArrayList<>();
        }
        try {
            return MAPPER.convertValue(arr, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (Exception e) {
            log.warn("[ledger:ai] LLM 报告字段解析失败（{}）：{}", field, e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public Map<String, Object> listReports(Long userId, int page, int pageSize) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<LedgerAiAnalysisReport> p =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, pageSize);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<LedgerAiAnalysisReport> r =
                reportMapper.selectPage(p, new LambdaQueryWrapper<LedgerAiAnalysisReport>()
                        .eq(LedgerAiAnalysisReport::getUserId, userId)
                        // 多版本：按生成时间倒序（同 period 新版本在前）
                        .orderByDesc(LedgerAiAnalysisReport::getId));
        List<Map<String, Object>> list = new ArrayList<>();
        for (LedgerAiAnalysisReport rep : r.getRecords()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", rep.getId());
            item.put("period", rep.getPeriod());
            String repRange = rep.getAnalysisRange() != null ? rep.getAnalysisRange() : "month";
            item.put("analysisRange", repRange);
            item.put("rangeLabel", RANGE_LABEL.getOrDefault(repRange, "本月"));
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

    @Override
    public Map<String, Object> getReportDetail(Long userId, Long reportId) {
        LedgerAiAnalysisReport rep = reportMapper.selectById(reportId);
        if (rep == null || !rep.getUserId().equals(userId)) {
            throw new IllegalArgumentException("报告不存在");
        }
        Map<String, Object> result = reportToResult(rep, true);
        result.put("reportId", rep.getId());
        return result;
    }

    @Override
    public void deleteReport(Long userId, Long reportId) {
        LedgerAiAnalysisReport rep = reportMapper.selectById(reportId);
        if (rep == null || !rep.getUserId().equals(userId)) {
            throw new IllegalArgumentException("报告不存在");
        }
        reportMapper.deleteById(reportId);
    }

    /** 快照实体 → 前端报告结构（缓存命中/历史回看时使用） */
    private Map<String, Object> reportToResult(LedgerAiAnalysisReport rep, boolean fromCache) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("period", rep.getPeriod());
        String repRange = rep.getAnalysisRange() != null ? rep.getAnalysisRange() : "month";
        result.put("range", repRange);
        result.put("rangeLabel", RANGE_LABEL.getOrDefault(repRange, "本月"));
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

    /**
     * 覆盖式落库（同 user+period+range 唯一一份，重新分析 UPDATE 覆盖不新增；
     * 查询与覆盖口径一致；失败仅告警不影响返回）
     */
    private void upsertReport(Long userId, String period, String range, int healthScore,
                              Map<String, Object> indicators, List<Map<String, Object>> incomeSources,
                              List<Map<String, Object>> debtRisks, List<Map<String, Object>> suggestions,
                              String aiSummary, boolean aiEnabled, String profileSnapshot, String fingerprint) {
        try {
            LedgerAiAnalysisReport rep = findReport(userId, period, range);
            boolean insert = (rep == null);
            if (insert) {
                rep = new LedgerAiAnalysisReport();
                rep.setUserId(userId);
                rep.setPeriod(period);
                rep.setAnalysisRange(range);
            }
            rep.setHealthScore(healthScore);
            rep.setMetricsJson(MAPPER.writeValueAsString(indicators));
            rep.setIncomeJson(MAPPER.writeValueAsString(incomeSources));
            rep.setRiskJson(MAPPER.writeValueAsString(debtRisks));
            rep.setAdviceJson(MAPPER.writeValueAsString(suggestions));
            rep.setAiSummary(aiSummary);
            rep.setAiEnabled(aiEnabled ? 1 : 0);
            rep.setProfileSnapshot(profileSnapshot);
            rep.setDataFingerprint(fingerprint);
            if (insert) {
                reportMapper.insert(rep);
            } else {
                reportMapper.updateById(rep);
            }
        } catch (Exception e) {
            log.warn("AI 分析报告落库失败 userId={} range={}", userId, range, e);
        }
    }

    private Object parseJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, Object.class);
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

    // ==================== 数据指纹（快照失效判断） ====================

    /**
     * 数据指纹：流水（条数+最后变更时间，近6个月口径与趋势上下文一致）、启用资产/负债账户、
     * 本月预算、画像原始值、finance_analysis 场景配置变更痕迹（cfg）。
     * 任一输入变化自动失效当月快照重算；无变化零 token 命中。
     */
    private String buildFingerprint(Long userId, LocalDate today, PortalUser user) {
        try {
            LocalDate trendStart = today.minusMonths(5).withDayOfMonth(1);
            String tx = aggSignature(transactionMapper.selectMaps(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<LedgerTransaction>()
                            .select("count(*) as cnt", "date_format(max(update_time), '%y%m%d%H%i%s') as last")
                            .eq("user_id", userId).eq("status", 1)
                            .ge("transaction_date", trendStart).le("transaction_date", today)));
            String as = aggSignature(assetAccountMapper.selectMaps(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<LedgerAssetAccount>()
                            .select("count(*) as cnt", "date_format(max(update_time), '%y%m%d%H%i%s') as last")
                            .eq("user_id", userId).eq("status", 1)));
            String li = aggSignature(liabilityAccountMapper.selectMaps(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<LedgerLiabilityAccount>()
                            .select("count(*) as cnt", "date_format(max(update_time), '%y%m%d%H%i%s') as last")
                            .eq("user_id", userId).eq("status", 1)));
            String bd = aggSignature(budgetMapper.selectMaps(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<LedgerBudget>()
                            .select("count(*) as cnt", "date_format(max(update_time), '%y%m%d%H%i%s') as last")
                            .eq("user_id", userId)
                            .eq("year", today.getYear()).eq("month", today.getMonthValue())));
            String fp = "cfg:" + sceneConfigStamp() + "|tx:" + tx + "|as:" + as + "|li:" + li
                    + "|bd:" + bd + "|pf:" + profileStamp(user);
            return fp.length() > 200 ? fp.substring(0, 200) : fp;
        } catch (Exception e) {
            log.warn("数据指纹构建失败（当次不命中缓存，直接重算） userId={}", userId, e);
            return null;
        }
    }

    /** finance_analysis 场景配置变更痕迹（update_time；无配置行返回 na） */
    private String sceneConfigStamp() {
        try {
            List<AiSceneConfig> configs = sceneConfigMapper.selectList(
                    new LambdaQueryWrapper<AiSceneConfig>()
                            .eq(AiSceneConfig::getSceneCode, SCENE_FINANCE_ANALYSIS)
                            .eq(AiSceneConfig::getEnabled, true)
                            .orderByDesc(AiSceneConfig::getPriority)
                            .last("LIMIT 1"));
            if (configs.isEmpty() || configs.get(0).getUpdateTime() == null) {
                return "na";
            }
            return configs.get(0).getUpdateTime()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyMMddHHmmss"));
        } catch (Exception e) {
            return "na";
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

    /** 画像原始值戳（快照存档与指纹共用；展示层翻译在 Handler/getProfile） */
    private String profileStamp(PortalUser user) {
        if (user == null) {
            return "未填写";
        }
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

    // ==================== 类型工具 ====================

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Map<?, ?> data) {
        return (Map<String, Object>) data;
    }

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
}
