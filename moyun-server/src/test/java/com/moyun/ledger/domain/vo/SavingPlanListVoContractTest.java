package com.moyun.ledger.domain.vo;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 存钱列表 VO 契约测试：锁死 plans 数组与汇总字段。
 */
class SavingPlanListVoContractTest {

    @Test
    void listJsonKeepsContractFields() throws Exception {
        SavingPlanListVO.Item item = new SavingPlanListVO.Item();
        item.setId(1L);
        item.setName("买房");
        item.setMethod("52week");
        item.setTargetAmount(new BigDecimal("13780.00"));
        item.setCurrentAmount(new BigDecimal("30.00"));
        item.setStatus(1);

        SavingPlanListVO vo = new SavingPlanListVO();
        vo.setPlans(List.of(item));
        vo.setTotalTarget(new BigDecimal("13780.00"));
        vo.setTotalSaved(new BigDecimal("30.00"));
        vo.setTotalRemaining(new BigDecimal("13750.00"));

        String json = new ObjectMapper().writeValueAsString(vo);
        assertTrue(json.contains("\"plans\""), "缺少 plans");
        assertTrue(json.contains("\"totalTarget\""), "缺少 totalTarget");
        assertTrue(json.contains("\"totalRemaining\""), "缺少 totalRemaining");
        assertTrue(json.contains("\"method\""), "缺少 method");
        assertFalse(json.contains("\"userId\""), "不应暴露 userId");
        assertFalse(json.contains("\"updateTime\""), "不应暴露 updateTime");
        assertEquals(1, vo.getPlans().size());
    }
}
