package com.moyun.ledger.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.moyun.ledger.domain.entity.LedgerTransaction;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 流水列表 VO 契约测试。
 */
class LedgerTransactionVoContractTest {

    @Test
    void transactionJsonKeepsContractFields() throws Exception {
        LedgerTransaction t = new LedgerTransaction();
        t.setId(1L);
        t.setType("transfer");
        t.setAmount(new BigDecimal("100.00"));
        t.setAccountId(2L);
        t.setTargetAccountId(3L);
        t.setTransactionDate(LocalDate.of(2026, 10, 27));
        t.setCategoryName("购物");
        t.setAccountName("工资卡");
        t.setTargetAccountName("存钱账户");
        t.setStatus(1);

        LedgerTransactionVO vo = LedgerTransactionVO.from(t);
        assertNotNull(vo);

        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        String json = mapper.writeValueAsString(vo);

        assertTrue(json.contains("\"type\""), "缺少 type");
        assertTrue(json.contains("\"amount\""), "缺少 amount");
        assertTrue(json.contains("\"targetAccountId\""), "缺少 targetAccountId");
        assertTrue(json.contains("\"categoryName\""), "缺少 categoryName");
        assertTrue(json.contains("\"accountName\""), "缺少 accountName");
        // 内部字段不外泄
        assertFalse(json.contains("\"userId\""), "不应暴露 userId");
        assertFalse(json.contains("\"clientUuid\""), "不应暴露 clientUuid");
        assertFalse(json.contains("\"updateTime\""), "不应暴露 updateTime");
    }
}
