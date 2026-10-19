package com.moyun.ledger.service.impl;

import com.moyun.ledger.domain.entity.LedgerProduct;
import com.moyun.ledger.domain.entity.LedgerShelf;
import com.moyun.ledger.domain.entity.LedgerStockFlow;
import com.moyun.ledger.domain.entity.LedgerWarehouse;
import com.moyun.ledger.domain.entity.LedgerWarehouseMember;
import com.moyun.ledger.mapper.LedgerWarehouseMemberMapper;
import com.moyun.ledger.service.ILedgerInventoryService;
import com.moyun.ledger.service.ILedgerShelfService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 协作库存：访问权边界 —— 真库测试。
 *
 * <h3>锁住的行为</h3>
 * <ol>
 *   <li><b>成员能访问</b>：非 owner 的活跃成员可以看货架/商品、能出入库</li>
 *   <li><b>陌生人被拒</b>：既不是 owner 也不在成员表里 → 拒绝</li>
 *   <li><b>成员不能删仓库</b>：破坏性操作仅 owner</li>
 *   <li><b>成员能编辑 owner 建的商品</b>：按商品所属仓库判断，不按 product.user_id</li>
 *   <li><b>被移除后立刻失去访问权</b>：is_active=0 即失效</li>
 * </ol>
 *
 * <p>这几条是协作功能的安全边界，漏一个就是越权漏洞，所以用测试固定住。</p>
 */
@SpringBootTest
@Transactional
class LedgerWarehouseCollabDbTest {

    private static final long OWNER = 999_980_301L;
    private static final long MEMBER = 999_980_302L;
    private static final long STRANGER = 999_980_303L;

    @Autowired
    private ILedgerInventoryService inventoryService;

    @Autowired
    private ILedgerShelfService shelfService;

    @Autowired
    private LedgerWarehouseMemberMapper memberMapper;

    @Test
    @DisplayName("成员能看货架/商品、能出入库；陌生人被拒；成员不能删仓库")
    void memberAccessAndBoundaries() {
        Long whId = createWarehouse(OWNER, "协作测试仓");
        Long shelfId = createShelf(whId, "A-01", "A架");
        Long pid = createProduct(whId, "协作商品", new BigDecimal("10.00"));

        // owner 入库 10
        inventoryService.stockIn(OWNER, whId, pid, shelfId, 10,
                LedgerStockFlow.REASON_PURCHASE, new BigDecimal("100.00"), null);

        // ---------- 陌生人在加成员前：被拒 ----------
        assertThrows(RuntimeException.class,
                () -> shelfService.listShelves(STRANGER, whId),
                "陌生人不应能看货架");

        // ---------- 加成员 ----------
        addMember(whId, MEMBER);

        // 成员能看到
        List<LedgerShelf> shelves = shelfService.listShelves(MEMBER, whId);
        assertTrue(shelves.stream().anyMatch(s -> shelfId.equals(s.getId())), "成员应能看到货架");

        Map<String, Object> products = inventoryService.listProducts(MEMBER, whId, null, null, null, 1, 10);
        @SuppressWarnings("unchecked")
        List<LedgerProduct> records = (List<LedgerProduct>) products.get("records");
        assertEquals(1, records.size(), "成员应能看到商品");

        // 成员能出库
        inventoryService.stockOut(MEMBER, whId, pid, shelfId, 3,
                LedgerStockFlow.REASON_SALE, new BigDecimal("20.00"), null, "成员卖出");
        assertEquals(7, inventoryService.warehouseOverview(MEMBER, whId).get("totalQuantity"),
                "成员出库后应为 7 件");

        // 成员不能删仓库
        assertThrows(RuntimeException.class,
                () -> inventoryService.deleteWarehouse(MEMBER, whId),
                "成员不应能删仓库");

        // 成员能编辑 owner 建的商品
        LedgerProduct upd = new LedgerProduct();
        upd.setName("成员改过的名字");
        inventoryService.updateProduct(MEMBER, pid, upd);

        // 陌生人依然被拒
        assertThrows(RuntimeException.class,
                () -> inventoryService.listProducts(STRANGER, whId, null, null, null, 1, 10),
                "陌生人不应能看商品");
    }

    @Test
    @DisplayName("移除成员后立刻失去访问权")
    void removedMemberLosesAccess() {
        Long whId = createWarehouse(OWNER, "移除测试仓");
        addMember(whId, MEMBER);

        // 加进来能访问
        assertNotNull(shelfService.listShelves(MEMBER, whId), "刚加入应能访问");

        // 移除（物理删除，与控制器行为一致）
        memberMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LedgerWarehouseMember>()
                .eq(LedgerWarehouseMember::getWarehouseId, whId)
                .eq(LedgerWarehouseMember::getUserId, MEMBER));

        assertThrows(RuntimeException.class,
                () -> shelfService.listShelves(MEMBER, whId),
                "移除后应立即无权访问");
    }

    @Test
    @DisplayName("协作仓库的概览按成员统计销售业绩")
    void memberSalesAggregation() {
        Long whId = createWarehouse(OWNER, "业绩统计仓");
        Long shelfId = createShelf(whId, "A-01", "A架");
        Long pid = createProduct(whId, "统计商品", new BigDecimal("10.00"));
        inventoryService.stockIn(OWNER, whId, pid, shelfId, 20,
                LedgerStockFlow.REASON_PURCHASE, new BigDecimal("200.00"), null);

        addMember(whId, MEMBER);

        // owner 卖 2 件 @20，成员卖 3 件 @20
        inventoryService.stockOut(OWNER, whId, pid, shelfId, 2,
                LedgerStockFlow.REASON_SALE, new BigDecimal("20.00"), null, null);
        inventoryService.stockOut(MEMBER, whId, pid, shelfId, 3,
                LedgerStockFlow.REASON_SALE, new BigDecimal("20.00"), null, null);

        Map<String, Object> ov = inventoryService.warehouseOverview(OWNER, whId);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> stats = (List<Map<String, Object>>) ov.get("memberSales");
        assertNotNull(stats, "应返回成员业绩");
        assertEquals(2, stats.size(), "两个操作人各一条");

        // 按金额降序：成员 3×20=60 在前，owner 2×20=40 在后
        assertEquals(60, ((BigDecimal) stats.get(0).get("amount")).intValue(), "成员业绩 60");
        assertEquals(3, ((Number) stats.get(0).get("quantity")).intValue(), "成员 3 件");
        assertEquals(40, ((BigDecimal) stats.get(1).get("amount")).intValue(), "owner 业绩 40");
    }

    // ---------------- 内部 ----------------

    private Long createWarehouse(Long uid, String name) {
        LedgerWarehouse w = new LedgerWarehouse();
        w.setName(name);
        w.setWarehouseType(LedgerWarehouse.TYPE_PARTNER);
        return inventoryService.createWarehouse(uid, w);
    }

    private Long createShelf(Long warehouseId, String code, String name) {
        LedgerShelf s = new LedgerShelf();
        s.setWarehouseId(warehouseId);
        s.setCode(code);
        s.setName(name);
        return shelfService.createShelf(OWNER, s);
    }

    private Long createProduct(Long warehouseId, String name, BigDecimal cost) {
        LedgerProduct p = new LedgerProduct();
        p.setWarehouseId(warehouseId);
        p.setName(name);
        p.setCostPrice(cost);
        return inventoryService.createProduct(OWNER, p);
    }

    private void addMember(Long warehouseId, Long uid) {
        LedgerWarehouseMember m = new LedgerWarehouseMember();
        m.setWarehouseId(warehouseId);
        m.setUserId(uid);
        m.setRole("member");
        m.setInvitedBy(OWNER);
        m.setIsActive(1);
        m.setStatus(LedgerWarehouseMember.STATUS_NORMAL);
        memberMapper.insert(m);
    }
}
