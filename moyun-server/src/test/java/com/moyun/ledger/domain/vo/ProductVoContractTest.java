package com.moyun.ledger.domain.vo;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.ledger.domain.entity.LedgerProduct;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 商品列表 VO 契约测试：锁死 App 依赖字段。
 */
class ProductVoContractTest {

    @Test
    void productJsonKeepsContractFields() throws Exception {
        LedgerProduct p = new LedgerProduct();
        p.setId(1L);
        p.setUserId(9L);
        p.setName("可口可乐");
        p.setBarcode("6901234567890");
        p.setUnit("瓶");
        p.setQuantity(10);
        p.setStockQuantity(10);
        p.setCostPrice(new BigDecimal("2.50"));
        p.setSellPrice(new BigDecimal("3.50"));
        p.setCategoryName("饮料");
        p.setStatus(1);

        ProductVO vo = ProductVO.from(p);
        String json = new ObjectMapper().writeValueAsString(vo);

        assertTrue(json.contains("\"name\""), "缺少 name");
        assertTrue(json.contains("\"barcode\""), "缺少 barcode");
        assertTrue(json.contains("\"stockQuantity\""), "缺少 stockQuantity");
        assertTrue(json.contains("\"costPrice\""), "缺少 costPrice");
        assertTrue(json.contains("\"categoryName\""), "缺少 categoryName");
        assertFalse(json.contains("\"userId\""), "不应暴露 userId");
        assertFalse(json.contains("\"providerUserId\""), "不应暴露 providerUserId");
        assertFalse(json.contains("\"updateTime\""), "不应暴露 updateTime");
    }
}
