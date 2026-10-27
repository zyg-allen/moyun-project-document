package com.moyun.ledger.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.moyun.ledger.domain.entity.LedgerSavingPlan;
import com.moyun.ledger.domain.vo.SavingPlanDetailVO;
import com.moyun.ledger.service.ILedgerSavingService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 存钱主流程 E2E（真库，事务回滚）：
 * 建计划 → 生成期次 → 标记完成 → 进度与关联字段正确。
 */
@SpringBootTest
@Transactional
class SavingPlanFlowDbTest {

    private static final long UID = 999_991_201L;

    @Autowired
    private ILedgerSavingService savingService;

    @Test
    @DisplayName("固定金额计划：建计划 → 标记一期 → currentAmount 与状态同步")
    void createPlanThenMarkFirstPeriod() {
        LedgerSavingPlan plan = new LedgerSavingPlan();
        plan.setName("E2E 存钱");
        plan.setMethod(LedgerSavingPlan.METHOD_FIXED);
        plan.setTargetAmount(new BigDecimal("100.00"));
        plan.setPeriodAmount(new BigDecimal("50.00"));
        plan.setStartDate(LocalDate.of(2026, 1, 1));

        Long planId = savingService.createPlan(UID, plan);
        assertNotNull(planId);

        SavingPlanDetailVO detail = savingService.planDetail(UID, planId);
        assertNotNull(detail.getPlan());
        assertEquals(2, detail.getRecords().size(), "100/50 应生成 2 期");
        assertEquals(0, detail.getPlan().getCurrentAmount().compareTo(BigDecimal.ZERO));

        Long firstRecordId = detail.getRecords().get(0).getId();
        savingService.markRecord(UID, planId, firstRecordId, 1, new BigDecimal("50.00"), null);

        SavingPlanDetailVO after = savingService.planDetail(UID, planId);
        assertEquals(0, after.getPlan().getCurrentAmount().compareTo(new BigDecimal("50.00")));
        assertTrue(after.getRecords().stream()
                .anyMatch(r -> r.getId().equals(firstRecordId) && r.getStatus() == 1), "第一期应为已存");
    }
}
