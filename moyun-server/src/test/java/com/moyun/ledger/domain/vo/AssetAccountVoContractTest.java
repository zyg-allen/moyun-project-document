package com.moyun.ledger.domain.vo;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.ledger.domain.entity.LedgerAssetAccount;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 资产账户 VO 契约测试：列表/选择器依赖的字段必须稳定。
 */
class AssetAccountVoContractTest {

    @Test
    void assetJsonKeepsContractFields() throws Exception {
        LedgerAssetAccount a = new LedgerAssetAccount();
        a.setId(1L);
        a.setUserId(9L);
        a.setName("工资卡");
        a.setAccountType("bank");
        a.setBalance(new BigDecimal("1000.00"));
        a.setIcon("🏦");
        a.setIncludeInTotal(1);
        a.setStatus(1);

        AssetAccountVO vo = AssetAccountVO.from(a);
        String json = new ObjectMapper().writeValueAsString(vo);

        assertTrue(json.contains("\"id\""), "缺少 id");
        assertTrue(json.contains("\"name\""), "缺少 name");
        assertTrue(json.contains("\"accountType\""), "缺少 accountType");
        assertTrue(json.contains("\"balance\""), "缺少 balance");
        assertTrue(json.contains("\"icon\""), "缺少 icon");
        assertTrue(json.contains("\"includeInTotal\""), "缺少 includeInTotal");
        assertFalse(json.contains("\"userId\""), "不应暴露 userId");
        assertFalse(json.contains("\"version\""), "不应暴露 version");
    }
}
