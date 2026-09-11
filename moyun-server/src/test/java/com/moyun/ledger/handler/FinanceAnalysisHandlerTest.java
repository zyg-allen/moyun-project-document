package com.moyun.ledger.handler;

import com.moyun.ext.ai2.model.AiExecuteRequest;
import com.moyun.ext.ai2.model.AiExecuteResponse;
import com.moyun.ledger.domain.entity.LedgerAssetAccount;
import com.moyun.ledger.domain.entity.LedgerCategory;
import com.moyun.ledger.domain.entity.LedgerLiabilityAccount;
import com.moyun.ledger.domain.entity.LedgerTransaction;
import com.moyun.ledger.mapper.LedgerAssetAccountMapper;
import com.moyun.ledger.mapper.LedgerBudgetMapper;
import com.moyun.ledger.mapper.LedgerCategoryMapper;
import com.moyun.ledger.mapper.LedgerLiabilityAccountMapper;
import com.moyun.ledger.mapper.LedgerTransactionMapper;
import com.moyun.portal.service.IPortalUserService;
import com.moyun.system.service.ISysDictTypeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 财务分析 Handler 指标护栏单测（v11.60 P0-4：数值护栏——LLM 只解读不计算）
 *
 * <p>Mock 全部 Mapper（离线可测）；基类 sceneResolver/llmService 保持 null →
 * chatDetailed 返回失败 outcome → LLM 自然降级模板综述——同时验证
 * 「LLM 失败时指标照常返回、前端 KPI 不受影响」的降级契约。</p>
 *
 * <p>护栏验证点：应急基金月数（流动资产/月均支出）、健康分四段公式、
 * 资产负债率/还款压力/储蓄率、debtFact 清偿测算（payoffMonths）、input 契约。</p>
 *
 * @author laomao
 * @since 2026-09-11
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FinanceAnalysisHandlerTest {

    @Mock private LedgerTransactionMapper transactionMapper;
    @Mock private LedgerCategoryMapper categoryMapper;
    @Mock private LedgerAssetAccountMapper assetAccountMapper;
    @Mock private LedgerLiabilityAccountMapper liabilityAccountMapper;
    @Mock private LedgerBudgetMapper budgetMapper;
    @Mock private IPortalUserService portalUserService;
    @Mock private ISysDictTypeService dictTypeService;

    @InjectMocks
    private FinanceAnalysisHandler handler;

    private static final Long USER_ID = 100L;
    private static final LocalDate TODAY = LocalDate.now();

    @BeforeEach
    void setUp() {
        // 流水（本月）：收入 10000 / 支出 6000（餐饮）
        LedgerTransaction income = tx("income", "10000", 1L, TODAY.withDayOfMonth(5));
        LedgerTransaction expense = tx("expense", "6000", 2L, TODAY.withDayOfMonth(6));
        when(transactionMapper.selectList(any())).thenReturn(List.of(income, expense));

        // 资产：现金3万 + 储蓄2万（流动）+ 基金5万（计入总资产但非流动）
        // 注：includeInTotal/status 过滤由 SQL 完成，mock 模拟"SQL 过滤后"的返回（只含 includeInTotal=1）
        when(assetAccountMapper.selectList(any())).thenReturn(List.of(
                asset(LedgerAssetAccount.TYPE_CASH, "30000", 1),
                asset(LedgerAssetAccount.TYPE_SAVINGS, "20000", 1),
                asset("fund", "50000", 1)));

        // 负债：房贷余额5万 / 月供2000（计入负债总额）
        LedgerLiabilityAccount debt = new LedgerLiabilityAccount();
        debt.setName("房贷");
        debt.setType("mortgage");
        debt.setBalance(new BigDecimal("50000"));
        debt.setMonthlyPayment(new BigDecimal("2000"));
        debt.setIncludeInTotal(1);
        debt.setStatus(1);
        when(liabilityAccountMapper.selectList(any())).thenReturn(List.of(debt));

        // 分类：1=工资 2=餐饮
        LedgerCategory cat1 = new LedgerCategory();
        cat1.setId(1L);
        cat1.setName("工资");
        LedgerCategory cat2 = new LedgerCategory();
        cat2.setId(2L);
        cat2.setName("餐饮");
        when(categoryMapper.selectList(any())).thenReturn(List.of(cat1, cat2));

        when(budgetMapper.selectList(any())).thenReturn(List.of());
        when(portalUserService.selectPortalUserById(USER_ID)).thenReturn(null);
    }

    // ==================== 指标护栏 ====================

    @Test
    @SuppressWarnings("unchecked")
    void execute_indicatorGuardrails() {
        AiExecuteResponse<Map<String, Object>> resp = execute("month");
        Map<String, Object> indicators = (Map<String, Object>) resp.getData().get("indicators");

        // 总资产 10万（现金+储蓄+基金；股票 includeInTotal=0 剔除）
        assertEquals(0, new BigDecimal("100000").compareTo((BigDecimal) indicators.get("totalAsset")));
        // 流动资产 5万（cash+savings；fund 非流动）
        assertEquals(0, new BigDecimal("50000").compareTo((BigDecimal) indicators.get("liquidAsset")));
        // 负债 5万 / 净资产 5万
        assertEquals(0, new BigDecimal("50000").compareTo((BigDecimal) indicators.get("totalLiability")));
        assertEquals(0, new BigDecimal("50000").compareTo((BigDecimal) indicators.get("netWorth")));
        // 资产负债率 50.0%（50000/100000）
        assertEquals(50.0, indicators.get("debtRatio"));
        // 还款压力 20.0%（2000/10000）
        assertEquals(20.0, indicators.get("repaymentPressure"));
        // 储蓄率 40.0%（(10000-6000)/10000）
        assertEquals(40.0, indicators.get("savingRate"));
        // 应急基金月数 = 50000/6000 = 8.3（流动资产/月均支出，1位小数 HALF_UP）
        assertEquals(0, new BigDecimal("8.3").compareTo((BigDecimal) indicators.get("emergencyFundMonths")));
        // 采样月数 1 / 入不敷出月数 0
        assertEquals(1, indicators.get("sampleMonths"));
        assertEquals(0, indicators.get("deficitMonths"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void execute_healthScore_fourFactorFormula() {
        // healthScore = 储蓄率40%→10 + 无赤字→25 + 负债率0.5(不<0.5)→12 + 还款压力0.2(<0.3)→25 = 72
        AiExecuteResponse<Map<String, Object>> resp = execute("month");
        assertEquals(72, resp.getData().get("healthScore"),
                "健康分四段公式：10+25+12+25=72");
    }

    @Test
    void computeHealthScore_segmentBoundaries() throws Exception {
        Method m = FinanceAnalysisHandler.class.getDeclaredMethod("computeHealthScore",
                double.class, double.class, double.class, int.class);
        m.setAccessible(true);
        // 全优：储蓄率100%→25 + 无赤字→25 + 负债率<0.5→25 + 压力<0.3→25 = 100
        assertEquals(100, m.invoke(handler, 0.2, 0.1, 1.0, 0));
        // 全差：0 + 赤字≥2→0 + 负债率≥0.8→0 + 压力≥0.5→0 = 0
        assertEquals(0, m.invoke(handler, 0.9, 0.6, 0.0, 3));
        // 边界：负债率恰 0.5 → 12 分段（非 25，严格小于）；还款压力恰 0.5 → 0 分段（0.5<0.5 为 false）
        assertEquals(37, m.invoke(handler, 0.5, 0.5, 0.0, 0));
        // 储蓄率 20% → 5 分；负债率 0.3/压力 0.2 均满分段：5+25+25+25 = 80
        assertEquals(80, m.invoke(handler, 0.3, 0.2, 0.2, 0));
    }

    @Test
    @SuppressWarnings("unchecked")
    void execute_deficitMonths_counted() {
        // 构造近3月：前两月支出>收入（赤字），本月盈余
        LedgerTransaction m1i = tx("income", "5000", 1L, TODAY.minusMonths(2).withDayOfMonth(10));
        LedgerTransaction m1e = tx("expense", "8000", 2L, TODAY.minusMonths(2).withDayOfMonth(11));
        LedgerTransaction m2i = tx("income", "5000", 1L, TODAY.minusMonths(1).withDayOfMonth(10));
        LedgerTransaction m2e = tx("expense", "9000", 2L, TODAY.minusMonths(1).withDayOfMonth(11));
        LedgerTransaction m0i = tx("income", "10000", 1L, TODAY.withDayOfMonth(5));
        LedgerTransaction m0e = tx("expense", "6000", 2L, TODAY.withDayOfMonth(6));
        when(transactionMapper.selectList(any()))
                .thenReturn(List.of(m1i, m1e, m2i, m2e, m0i, m0e));

        AiExecuteResponse<Map<String, Object>> resp = execute("3m");
        Map<String, Object> indicators = (Map<String, Object>) resp.getData().get("indicators");
        assertEquals(2, indicators.get("deficitMonths"), "近3月应有 2 个月入不敷出");
        assertEquals(3, indicators.get("sampleMonths"));
        // healthScore：储蓄率(20000-23000)/20000<0→0分 + 赤字2→0分 + 负债率0.5→12分
        // + 还款压力 2000/6666.67=0.30（恰不满足<0.3，落0.3-0.5分段）→12分 = 24
        assertEquals(24, resp.getData().get("healthScore"));
    }

    // ==================== 清偿测算（debtFact） ====================

    @Test
    @SuppressWarnings("unchecked")
    void debtFact_payoffMonths_ceilingDivision() throws Exception {
        Method m = FinanceAnalysisHandler.class.getDeclaredMethod("debtFact",
                LedgerLiabilityAccount.class);
        m.setAccessible(true);

        // 余额5万/月供2000 → 25 期
        LedgerLiabilityAccount d1 = new LedgerLiabilityAccount();
        d1.setName("房贷");
        d1.setBalance(new BigDecimal("50000"));
        d1.setMonthlyPayment(new BigDecimal("2000"));
        Map<String, Object> f1 = (Map<String, Object>) m.invoke(handler, d1);
        assertEquals(25, f1.get("payoffMonths"));

        // 除不尽向上取整：50001/2000 → 26
        d1.setBalance(new BigDecimal("50001"));
        assertEquals(26, ((Map<String, Object>) m.invoke(handler, d1)).get("payoffMonths"));

        // 无月供 → 无 payoffMonths 键（不做清偿测算）
        d1.setMonthlyPayment(null);
        assertFalse(((Map<String, Object>) m.invoke(handler, d1)).containsKey("payoffMonths"));

        // 余额清零 → 无 payoffMonths 键
        d1.setMonthlyPayment(new BigDecimal("2000"));
        d1.setBalance(BigDecimal.ZERO);
        assertFalse(((Map<String, Object>) m.invoke(handler, d1)).containsKey("payoffMonths"));

        // 期数进度透传
        d1.setBalance(new BigDecimal("50000"));
        d1.setTotalTerms(360);
        d1.setPaidTerms(120);
        assertEquals("120/360期", ((Map<String, Object>) m.invoke(handler, d1)).get("progress"));
    }

    // ==================== LLM 降级契约 ====================

    @Test
    @SuppressWarnings("unchecked")
    void execute_llmFailure_degradesToTemplateSummary() {
        // sceneResolver/llmService 为 null → chatDetailed 失败 → aiEnabled=false + 模板综述
        AiExecuteResponse<Map<String, Object>> resp = execute("month");
        Map<String, Object> data = resp.getData();

        assertEquals(false, data.get("aiEnabled"), "LLM 不可用应降级");
        String summary = (String) data.get("summary");
        assertTrue(summary.startsWith("（模板分析）"), "应回落模板综述: " + summary);
        assertTrue(summary.contains("月均收入 ¥10000.00"), "综述应含精确指标（数值护栏）");
        // 指标照常返回——前端 KPI 不受 LLM 失败影响
        assertNotNull(data.get("indicators"));
        assertNotNull(data.get("healthScore"));
        assertTrue(((List<?>) data.get("risks")).isEmpty());
        assertTrue(((List<?>) data.get("suggestions")).isEmpty());
        // 收入结构（前端契约）
        List<Map<String, Object>> incomeSources = (List<Map<String, Object>>) data.get("incomeSources");
        assertEquals(1, incomeSources.size());
        assertEquals("工资", incomeSources.get(0).get("name"));
        assertEquals(100.0, incomeSources.get(0).get("ratio"), "单一收入源占比 100%");
    }

    // ==================== input 契约 ====================

    @Test
    void execute_missingUserId_throws() {
        AiExecuteRequest request = new AiExecuteRequest();
        request.setInput(new HashMap<>());
        assertThrows(IllegalArgumentException.class,
                () -> handler.execute(request, null));
    }

    @Test
    @SuppressWarnings("unchecked")
    void execute_invalidRange_fallsBackToMonth() {
        AiExecuteResponse<Map<String, Object>> resp = execute("bogus_range");
        Map<String, Object> indicators = (Map<String, Object>) resp.getData().get("indicators");
        assertEquals("month", indicators.get("range"), "非法 range 应回落 month");
        assertEquals("本月", indicators.get("rangeLabel"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void execute_range3m_windowStartsThreeMonthsAgo() {
        AiExecuteResponse<Map<String, Object>> resp = execute("3m");
        Map<String, Object> indicators = (Map<String, Object>) resp.getData().get("indicators");
        LocalDate expectedStart = TODAY.minusMonths(2).withDayOfMonth(1);
        assertEquals(expectedStart.toString(), indicators.get("rangeStart"), "3m 窗口应含当月起共3个自然月");
        assertEquals("近3个月", indicators.get("rangeLabel"));
    }

    // ==================== 辅助 ====================

    @SuppressWarnings("unchecked")
    private AiExecuteResponse<Map<String, Object>> execute(String range) {
        AiExecuteRequest request = new AiExecuteRequest();
        Map<String, Object> input = new HashMap<>();
        input.put("userId", USER_ID);
        if (range != null) {
            input.put("range", range);
        }
        request.setInput(input);
        AiExecuteResponse<?> resp = handler.execute(request, null);
        assertEquals(Integer.valueOf(0), resp.getCode(), "执行应成功: " + resp.getMsg());
        return (AiExecuteResponse<Map<String, Object>>) resp;
    }

    private LedgerTransaction tx(String type, String amount, Long categoryId, LocalDate date) {
        LedgerTransaction t = new LedgerTransaction();
        t.setUserId(USER_ID);
        t.setType(type);
        t.setAmount(new BigDecimal(amount));
        t.setCategoryId(categoryId);
        t.setTransactionDate(date);
        t.setStatus(1);
        return t;
    }

    private LedgerAssetAccount asset(String type, String balance, Integer includeInTotal) {
        LedgerAssetAccount a = new LedgerAssetAccount();
        a.setUserId(USER_ID);
        a.setType(type);
        a.setBalance(new BigDecimal(balance));
        a.setIncludeInTotal(includeInTotal);
        a.setStatus(1);
        return a;
    }
}
