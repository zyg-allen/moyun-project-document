package com.moyun.ledger.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.ledger.domain.entity.LedgerProduct;
import com.moyun.ledger.domain.entity.LedgerShelf;
import com.moyun.ledger.domain.entity.LedgerStock;
import com.moyun.ledger.domain.entity.LedgerStockFlow;
import com.moyun.ledger.domain.entity.LedgerWarehouse;
import com.moyun.ledger.mapper.LedgerStockFlowMapper;
import com.moyun.ledger.mapper.LedgerStockMapper;
import com.moyun.ledger.service.ILedgerInventoryService;
import com.moyun.ledger.service.ILedgerShelfService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 仓库库存链路 —— 真库集成测试。
 *
 * <h3>锁住的行为</h3>
 * <ol>
 *   <li><b>库存按 (商品, 货架) 拆分</b>：同一商品可分布在多个货架，
 *       商品总库存 = 各货架之和。唯一键是 {@code uk_product_shelf}。</li>
 *   <li><b>校准=盘点</b>：把某货架某商品直接设为实盘数，差异写 adjust 流水
 *       （正=盘盈 负=盘亏），不新建多余库存行。</li>
 *   <li><b>移库</b>：源减目标加，写一条 transfer 流水且带源/目标货架。</li>
 *   <li><b>销售成交单价</b>：出库 reason=sale 时可带单价，
 *       报表据此算销售额与毛利（而不只是成本）。</li>
 *   <li><b>最后动销时间</b>：任一库存变动都刷新 product.lastMovedAt。</li>
 * </ol>
 *
 * <p>类上 {@code @Transactional} → 默认回滚，不留测试数据。</p>
 *
 * @author moyun
 */
@SpringBootTest
@Transactional
class LedgerInventoryShelfFlowDbTest {

    private static final long UID = 999_980_101L;
    private static final BigDecimal COST = new BigDecimal("10.00");
    private static final BigDecimal SALE_PRICE = new BigDecimal("25.00");

    @Autowired
    private ILedgerInventoryService inventoryService;

    @Autowired
    private ILedgerShelfService shelfService;

    @Autowired
    private LedgerStockMapper stockMapper;

    @Autowired
    private LedgerStockFlowMapper flowMapper;

    @Test
    @DisplayName("入库到货架 → 销售出库(带单价) → 校准 → 移库：拆分/合计/流水/报表全部正确")
    void shelfFlowEndToEnd() {
        // ---------- 准备：仓库 + 两个货架 + 一个商品 ----------
        LedgerWarehouse wh = new LedgerWarehouse();
        wh.setName("集成测试仓");
        wh.setWarehouseType(LedgerWarehouse.TYPE_PERSONAL);
        Long whId = inventoryService.createWarehouse(UID, wh);
        assertNotNull(whId);

        Long shelfA = createShelf(whId, "A-01", "A架");
        Long shelfB = createShelf(whId, "B-01", "B架");

        LedgerProduct product = new LedgerProduct();
        product.setWarehouseId(whId);
        product.setName("测试商品");
        product.setCostPrice(COST);
        Long pid = inventoryService.createProduct(UID, product);
        assertNotNull(pid);

        // ---------- ① 入库 10 到 A 架 ----------
        inventoryService.stockIn(UID, whId, pid, shelfA, 10,
                LedgerStockFlow.REASON_PURCHASE, "首批进货");
        assertEquals(10, qtyOf(pid, shelfA), "A 架应有 10 件");

        // ---------- ② 销售出库 3 件，单价 25 ----------
        inventoryService.stockOut(UID, whId, pid, shelfA, 3,
                LedgerStockFlow.REASON_SALE, SALE_PRICE, null, "卖给客户");
        assertEquals(7, qtyOf(pid, shelfA), "出库 3 件后 A 架应剩 7 件");

        // ---------- ③ 校准到 5 件（盘亏 2）----------
        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        item.put("productId", pid);
        item.put("actualQuantity", 5);
        items.add(item);
        Map<String, Object> st = inventoryService.stocktake(UID, whId, shelfA, items, "月末盘点");
        assertEquals(1, ((Number) st.get("changed")).intValue(), "有 1 条商品产生差异");
        assertEquals(2, ((Number) st.get("loss")).intValue(), "盘亏 2 件");
        assertEquals(0, ((Number) st.get("gain")).intValue(), "无盘盈");
        assertEquals(5, qtyOf(pid, shelfA), "校准后 A 架应为 5 件");

        // ---------- ④ 移库 2 件 A → B ----------
        inventoryService.transferStock(UID, whId, pid, shelfA, shelfB, 2, "挪到 B 架");
        assertEquals(3, qtyOf(pid, shelfA), "移走 2 件后 A 架应为 3 件");
        assertEquals(2, qtyOf(pid, shelfB), "B 架应为 2 件");

        // ---------- ⑤ 商品总库存 = 各货架之和 = 5 ----------
        Map<String, Object> qtyMap = inventoryService.listProducts(UID, whId, null, null, null, 1, 10);
        @SuppressWarnings("unchecked")
        List<LedgerProduct> records = (List<LedgerProduct>) qtyMap.get("records");
        LedgerProduct listed = records.stream().filter(p -> pid.equals(p.getId())).findFirst().orElse(null);
        assertNotNull(listed, "商品应出现在列表里");
        assertEquals(5, listed.getStockQuantity().intValue(), "商品总库存 = 3 + 2 = 5");
        assertNotNull(listed.getLastMovedAt(), "库存变动后应刷新最后动销时间");

        // ---------- ⑥ 流水：in / out / adjust / transfer 各一条 ----------
        List<LedgerStockFlow> flows = flowMapper.selectList(new LambdaQueryWrapper<LedgerStockFlow>()
                .eq(LedgerStockFlow::getWarehouseId, whId)
                .eq(LedgerStockFlow::getProductId, pid)
                .orderByAsc(LedgerStockFlow::getId));
        assertEquals(4, flows.size(), "应有 4 条流水");
        assertEquals(LedgerStockFlow.FLOW_IN, flows.get(0).getFlowType());
        assertEquals(LedgerStockFlow.REASON_PURCHASE, flows.get(0).getReason());
        assertEquals(LedgerStockFlow.FLOW_OUT, flows.get(1).getFlowType());
        assertEquals(LedgerStockFlow.REASON_SALE, flows.get(1).getReason());
        assertEquals(0, SALE_PRICE.compareTo(flows.get(1).getSalePrice()), "销售流水应记录成交单价");
        assertEquals(LedgerStockFlow.FLOW_ADJUST, flows.get(2).getFlowType());
        assertEquals(-2, flows.get(2).getQuantityChange().intValue(), "盘亏量应为 -2");
        assertEquals(LedgerStockFlow.FLOW_TRANSFER, flows.get(3).getFlowType());
        assertEquals(shelfA, flows.get(3).getShelfId(), "移库流水记源货架");
        assertEquals(shelfB, flows.get(3).getToShelfId(), "移库流水记目标货架");

        // ---------- ⑦ 概览：价值/件数/销售额/毛利 ----------
        Map<String, Object> ov = inventoryService.warehouseOverview(UID, whId);
        // 库存价值 = 5 件 × 成本 10 = 50
        assertEquals(0, new BigDecimal("50.00").compareTo((BigDecimal) ov.get("stockValue")),
                "库存总价值应为 50");
        assertEquals(5, ((Number) ov.get("totalQuantity")).intValue(), "待售件数 5");
        assertEquals(2, ((Number) ov.get("shelfCount")).intValue(), "货架数 2");
        assertEquals(3, ((Number) ov.get("monthSaleQuantity")).intValue(), "本月销售 3 件");
        // 销售额 = 25 × 3 = 75；销售成本 = 10 × 3 = 30；毛利 = 45
        assertEquals(0, new BigDecimal("75.00").compareTo((BigDecimal) ov.get("monthSaleAmount")),
                "销售额应为 75");
        assertEquals(0, new BigDecimal("30.00").compareTo((BigDecimal) ov.get("monthSaleCost")),
                "销售成本应为 30");
        assertEquals(0, new BigDecimal("45.00").compareTo((BigDecimal) ov.get("monthGrossProfit")),
                "毛利应为 45");
    }

    @Test
    @DisplayName("货架列表聚合：商品种类数 / 件数 / 成本价值 / 封面字段")
    void shelfListAggregates() {
        LedgerWarehouse wh = new LedgerWarehouse();
        wh.setName("聚合测试仓");
        Long whId = inventoryService.createWarehouse(UID, wh);
        Long shelfA = createShelf(whId, "C-01", "C架");

        LedgerProduct p = new LedgerProduct();
        p.setWarehouseId(whId);
        p.setName("聚合商品");
        p.setCostPrice(new BigDecimal("7.50"));
        Long pid = inventoryService.createProduct(UID, p);

        inventoryService.stockIn(UID, whId, pid, shelfA, 4,
                LedgerStockFlow.REASON_PURCHASE, null);

        List<LedgerShelf> shelves = shelfService.listShelves(UID, whId);
        LedgerShelf sa = shelves.stream().filter(s -> shelfA.equals(s.getId())).findFirst().orElse(null);
        assertNotNull(sa);
        assertEquals(1, sa.getProductCount().intValue(), "1 种商品");
        assertEquals(4, sa.getStockQuantity().intValue(), "4 件");
        assertEquals(0, new BigDecimal("30.00").compareTo(sa.getCostValue()), "成本价值 7.5×4=30");
        assertEquals("聚合测试仓", sa.getWarehouseName(), "应带回仓库名");
    }

    @Test
    @DisplayName("有库存的货架禁止删除，清空后可删")
    void shelfWithStockCannotBeDeleted() {
        LedgerWarehouse wh = new LedgerWarehouse();
        wh.setName("删除测试仓");
        Long whId = inventoryService.createWarehouse(UID, wh);
        Long shelfA = createShelf(whId, "D-01", "D架");

        LedgerProduct p = new LedgerProduct();
        p.setWarehouseId(whId);
        p.setName("删除测试商品");
        p.setCostPrice(COST);
        Long pid = inventoryService.createProduct(UID, p);
        inventoryService.stockIn(UID, whId, pid, shelfA, 2, LedgerStockFlow.REASON_PURCHASE, null);

        // 有库存 → 拒绝
        boolean rejected = false;
        try {
            shelfService.deleteShelf(UID, shelfA);
        } catch (RuntimeException e) {
            rejected = true;
            assertTrue(String.valueOf(e.getMessage()).contains("库存"), "提示应先移库");
        }
        assertTrue(rejected, "有库存的货架不应被删除");

        // 出清后可删
        inventoryService.stockOut(UID, whId, pid, shelfA, 2,
                LedgerStockFlow.REASON_LOSS, null, null, "损耗清空");
        shelfService.deleteShelf(UID, shelfA);
        List<LedgerShelf> left = shelfService.listShelves(UID, whId);
        assertTrue(left.stream().noneMatch(s -> shelfA.equals(s.getId())), "删除后不应出现在列表");
    }

    // ---------------- 内部 ----------------

    private Long createShelf(Long warehouseId, String code, String name) {
        LedgerShelf s = new LedgerShelf();
        s.setWarehouseId(warehouseId);
        s.setCode(code);
        s.setName(name);
        return shelfService.createShelf(UID, s);
    }

    private int qtyOf(Long productId, Long shelfId) {
        LedgerStock s = stockMapper.selectOne(new LambdaQueryWrapper<LedgerStock>()
                .eq(LedgerStock::getProductId, productId)
                .eq(LedgerStock::getShelfId, shelfId)
                .eq(LedgerStock::getStatus, LedgerStock.STATUS_NORMAL)
                .last("LIMIT 1"));
        return s == null || s.getQuantity() == null ? 0 : s.getQuantity();
    }
}
