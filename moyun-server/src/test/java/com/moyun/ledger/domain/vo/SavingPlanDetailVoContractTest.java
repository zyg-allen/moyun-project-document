package com.moyun.ledger.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.moyun.ledger.domain.entity.LedgerSavingPlan;
import com.moyun.ledger.domain.entity.LedgerSavingRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 存钱详情 VO 契约测试：锁定前端依赖的字段与结构，防止改表/改字段把页面打崩。
 */
class SavingPlanDetailVoContractTest {

    @Test
    void detailJsonKeepsContractFields() throws Exception {
        LedgerSavingPlan plan = new LedgerSavingPlan();
        plan.setId(1L);
        plan.setName("买房");
        plan.setMethod("52week");
        plan.setTargetAmount(new BigDecimal("13780.00"));
        plan.setCurrentAmount(new BigDecimal("30.00"));
        plan.setStartDate(LocalDate.of(2026, 10, 26));
        plan.setSourceAccountId(10L);
        plan.setSavingsAccountId(11L);

        LedgerSavingRecord record = new LedgerSavingRecord();
        record.setId(100L);
        record.setPeriodIndex(1);
        record.setTargetAmount(new BigDecimal("10.00"));
        record.setAmount(new BigDecimal("10.00"));
        record.setStatus(1);
        record.setRecordDate(LocalDate.of(2026, 10, 26));
        record.setTransactionId(900L);

        SavingPlanDetailVO.Plan p = new SavingPlanDetailVO.Plan();
        p.setId(plan.getId());
        p.setName(plan.getName());
        p.setMethod(plan.getMethod());
        p.setTargetAmount(plan.getTargetAmount());
        p.setCurrentAmount(plan.getCurrentAmount());
        p.setStartDate(plan.getStartDate());
        p.setSourceAccountId(plan.getSourceAccountId());
        p.setSavingsAccountId(plan.getSavingsAccountId());

        SavingPlanDetailVO.Record r = new SavingPlanDetailVO.Record();
        r.setId(record.getId());
        r.setPeriodIndex(record.getPeriodIndex());
        r.setTargetAmount(record.getTargetAmount());
        r.setAmount(record.getAmount());
        r.setStatus(record.getStatus());
        r.setRecordDate(record.getRecordDate());
        r.setTransactionId(record.getTransactionId());

        SavingPlanDetailVO vo = new SavingPlanDetailVO();
        vo.setPlan(p);
        List<SavingPlanDetailVO.Record> records = new ArrayList<>();
        records.add(r);
        vo.setRecords(records);

        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        String json = mapper.writeValueAsString(vo);

        assertTrue(json.contains("\"plan\""), "缺少 plan");
        assertTrue(json.contains("\"records\""), "缺少 records");
        assertTrue(json.contains("\"targetAmount\""), "缺少 targetAmount");
        assertTrue(json.contains("\"periodIndex\""), "缺少 periodIndex");
        assertTrue(json.contains("\"recordDate\""), "缺少 recordDate");
        assertTrue(json.contains("\"transactionId\""), "缺少 transactionId");
        // 内部字段不外泄
        assertFalse(json.contains("\"userId\""), "不应暴露 userId");
        assertFalse(json.contains("\"createTime\""), "不应暴露 createTime");
        assertEquals(1, vo.getRecords().size());
    }
}
