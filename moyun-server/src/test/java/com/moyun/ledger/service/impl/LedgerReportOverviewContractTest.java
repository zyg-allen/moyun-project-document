package com.moyun.ledger.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.moyun.ledger.domain.entity.LedgerTransaction;
import com.moyun.ledger.mapper.LedgerTransactionMapper;
import com.moyun.ledger.service.ILedgerReportService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class LedgerReportOverviewContractTest {

    private static final long UID = 999_990_901L;

    @Autowired
    private ILedgerReportService reportService;

    @Autowired
    private LedgerTransactionMapper transactionMapper;

    @Test
    @DisplayName("overview 返回约定的 key、月度趋势 12 项、分类排行结构正确")
    void overviewKeepsContractKeys() {
        insert("income", "100.00", LocalDate.of(2026, 2, 3));
        insert("expense", "50.00", LocalDate.of(2026, 3, 4));

        Map<String, Object> data = reportService.overview(UID, 2026);
        assertNotNull(data);

        for (String key : List.of("monthlyTrend", "yearIncome", "yearExpense",
                "categoryIncome", "categoryExpense", "categoryIncomeSub", "categoryExpenseSub",
                "netWorthTrend", "accountDistribution", "liabilityOverview", "budgetExec")) {
            assertTrue(data.containsKey(key), "缺少报表字段：" + key);
        }

        Object trendObj = data.get("monthlyTrend");
        assertTrue(trendObj instanceof List, "monthlyTrend 必须是数组");
        List<?> trend = (List<?>) trendObj;
        assertEquals(12, trend.size(), "monthlyTrend 必须是 12 个月");
        Map<?, ?> first = (Map<?, ?>) trend.get(0);
        assertTrue(first.containsKey("month"), "monthlyTrend 项缺少 month");
        assertTrue(first.containsKey("income"), "monthlyTrend 项缺少 income");
        assertTrue(first.containsKey("expense"), "monthlyTrend 项缺少 expense");

        assertTrue(data.get("yearIncome") instanceof BigDecimal, "yearIncome 必须是数字");
        assertTrue(data.get("yearExpense") instanceof BigDecimal, "yearExpense 必须是数字");

        Object categoryExpense = data.get("categoryExpense");
        assertTrue(categoryExpense instanceof List, "categoryExpense 必须是数组");
        List<?> ranks = (List<?>) categoryExpense;
        if (!ranks.isEmpty()) {
            Map<?, ?> rank = (Map<?, ?>) ranks.get(0);
            assertTrue(rank.containsKey("name"), "分类排行缺少 name");
            assertTrue(rank.containsKey("amount"), "分类排行缺少 amount");
        }
    }

    private void insert(String type, String amount, LocalDate date) {
        LedgerTransaction t = new LedgerTransaction();
        t.setUserId(UID);
        t.setType(type);
        t.setAmount(new BigDecimal(amount));
        t.setTransactionDate(date);
        t.setStatus(LedgerTransaction.STATUS_NORMAL);
        t.setCategoryId(1L);
        t.setIsBudget(1);
        t.setStockIn(0);
        transactionMapper.insert(t);
    }
}