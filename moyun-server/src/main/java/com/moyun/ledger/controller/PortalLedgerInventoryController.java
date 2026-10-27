package com.moyun.ledger.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.ledger.domain.entity.LedgerProduct;
import com.moyun.ledger.domain.entity.LedgerWarehouse;
import com.moyun.ledger.service.ILedgerInventoryService;
import com.moyun.portal.util.PortalSecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 门户-库存（仓库/商品/出入库/流水）。
 *
 * <p>所有端点以当前登录用户为作用域，userId 一律取自会话，
 * 仓库/商品归属在服务层强制校验，杜绝横向越权。</p>
 *
 * @author moyun
 */
@Tag(name = "门户-库存")
@RestController
@RequestMapping("/portal/ledger/inventory")
public class PortalLedgerInventoryController {

    @Autowired
    private ILedgerInventoryService inventoryService;

    // ---------------- 仓库 ----------------

    @Operation(summary = "仓库列表")
    @GetMapping("/warehouses")
    public AjaxResult listWarehouses() {
        return AjaxResult.success(inventoryService.listWarehouses(PortalSecurityUtils.getUserId()));
    }

    @Operation(summary = "新增仓库")
    @com.moyun.common.annotation.RepeatSubmit(interval = 2000)
    @com.moyun.common.annotation.RateLimiter(key = "portalledgerinventory:", time = 60, count = 120)
    @PostMapping("/warehouses")
    public AjaxResult createWarehouse(@RequestBody LedgerWarehouse bo) {
        return AjaxResult.success(inventoryService.createWarehouse(PortalSecurityUtils.getUserId(), bo));
    }

    @Operation(summary = "修改仓库")
    @com.moyun.common.annotation.RepeatSubmit(interval = 2000)
    @com.moyun.common.annotation.RateLimiter(key = "portalledgerinventory:", time = 60, count = 120)
    @PutMapping("/warehouses/{id}")
    public AjaxResult updateWarehouse(@PathVariable("id") Long id, @RequestBody LedgerWarehouse bo) {
        inventoryService.updateWarehouse(PortalSecurityUtils.getUserId(), id, bo);
        return AjaxResult.success();
    }

    @Operation(summary = "删除仓库")
    @com.moyun.common.annotation.RepeatSubmit(interval = 2000)
    @com.moyun.common.annotation.RateLimiter(key = "portalledgerinventory:", time = 60, count = 120)
    @DeleteMapping("/warehouses/{id}")
    public AjaxResult deleteWarehouse(@PathVariable("id") Long id) {
        inventoryService.deleteWarehouse(PortalSecurityUtils.getUserId(), id);
        return AjaxResult.success();
    }

    // ---------------- 商品 ----------------

    @Operation(summary = "商品列表（带库存数量）")
    @GetMapping("/warehouses/{warehouseId}/products")
    public AjaxResult listProducts(@PathVariable("warehouseId") Long warehouseId,
                                   @RequestParam(value = "keyword", required = false) String keyword,
                                   @RequestParam(value = "categoryId", required = false) Long categoryId,
                                   @RequestParam(value = "shelfId", required = false) Long shelfId,
                                   @RequestParam(value = "pageNum", required = false) Integer pageNum,
                                   @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return AjaxResult.success(inventoryService.listProducts(
                PortalSecurityUtils.getUserId(), warehouseId, keyword, categoryId, shelfId, pageNum, pageSize));
    }

    @Operation(summary = "相似商品推荐（录入前查重，避免重复建档）")
    @GetMapping("/warehouses/{warehouseId}/products/similar")
    public AjaxResult similarProducts(@PathVariable("warehouseId") Long warehouseId,
                                      @RequestParam(value = "name", required = false) String name,
                                      @RequestParam(value = "excludeId", required = false) Long excludeId) {
        return AjaxResult.success(inventoryService.findSimilarProducts(
                PortalSecurityUtils.getUserId(), warehouseId, name, excludeId));
    }

    @Operation(summary = "新增商品")
    @com.moyun.common.annotation.RepeatSubmit(interval = 2000)
    @com.moyun.common.annotation.RateLimiter(key = "portalledgerinventory:", time = 60, count = 120)
    @PostMapping("/products")
    public AjaxResult createProduct(@RequestBody LedgerProduct bo) {
        return AjaxResult.success(inventoryService.createProduct(PortalSecurityUtils.getUserId(), bo));
    }

    @Operation(summary = "修改商品")
    @com.moyun.common.annotation.RepeatSubmit(interval = 2000)
    @com.moyun.common.annotation.RateLimiter(key = "portalledgerinventory:", time = 60, count = 120)
    @PutMapping("/products/{id}")
    public AjaxResult updateProduct(@PathVariable("id") Long id, @RequestBody LedgerProduct bo) {
        inventoryService.updateProduct(PortalSecurityUtils.getUserId(), id, bo);
        return AjaxResult.success();
    }

    @Operation(summary = "删除商品")
    @com.moyun.common.annotation.RepeatSubmit(interval = 2000)
    @com.moyun.common.annotation.RateLimiter(key = "portalledgerinventory:", time = 60, count = 120)
    @DeleteMapping("/products/{id}")
    public AjaxResult deleteProduct(@PathVariable("id") Long id) {
        inventoryService.deleteProduct(PortalSecurityUtils.getUserId(), id);
        return AjaxResult.success();
    }

    // ---------------- 出入库 ----------------

    @Operation(summary = "入库（可选货架，缺省=未上架）")
    @com.moyun.common.annotation.RepeatSubmit(interval = 2000)
    @com.moyun.common.annotation.RateLimiter(key = "portalledgerinventory:", time = 60, count = 120)
    @PostMapping("/stock/in")
    public AjaxResult stockIn(@RequestBody Map<String, Object> bo) {
        return AjaxResult.success(inventoryService.stockIn(
                PortalSecurityUtils.getUserId(),
                longVal(bo, "warehouseId"), longVal(bo, "productId"), longVal(bo, "shelfId"),
                intVal(bo, "quantity"), strVal(bo, "reason"), strVal(bo, "remark")));
    }

    @Operation(summary = "出库（可选货架，缺省=未上架）")
    @com.moyun.common.annotation.RepeatSubmit(interval = 2000)
    @com.moyun.common.annotation.RateLimiter(key = "portalledgerinventory:", time = 60, count = 120)
    @PostMapping("/stock/out")
    public AjaxResult stockOut(@RequestBody Map<String, Object> bo) {
        return AjaxResult.success(inventoryService.stockOut(
                PortalSecurityUtils.getUserId(),
                longVal(bo, "warehouseId"), longVal(bo, "productId"), longVal(bo, "shelfId"),
                intVal(bo, "quantity"), strVal(bo, "reason"), decVal(bo, "salePrice"), longVal(bo, "channelId"), strVal(bo, "remark"), decVal(bo, "extraCost")));
    }

    @Operation(summary = "库存盘点（实盘 vs 账面，差异写 adjust 流水）")
    @com.moyun.common.annotation.RepeatSubmit(interval = 2000)
    @com.moyun.common.annotation.RateLimiter(key = "portalledgerinventory:", time = 60, count = 120)
    @PostMapping("/stock/stocktake")
    @SuppressWarnings("unchecked")
    public AjaxResult stocktake(@RequestBody Map<String, Object> bo) {
        Object items = bo.get("items");
        java.util.List<Map<String, Object>> list = items instanceof java.util.List
                ? (java.util.List<Map<String, Object>>) items : new java.util.ArrayList<>();
        return AjaxResult.success(inventoryService.stocktake(
                PortalSecurityUtils.getUserId(),
                longVal(bo, "warehouseId"), longVal(bo, "shelfId"), list, strVal(bo, "remark")));
    }

    @Operation(summary = "仓库概览（库存总价值/待售件数/本月出入库/零库存商品数）")
    @GetMapping("/warehouses/{warehouseId}/overview")
    public AjaxResult overview(@PathVariable("warehouseId") Long warehouseId) {
        return AjaxResult.success(inventoryService.warehouseOverview(
                PortalSecurityUtils.getUserId(), warehouseId));
    }

    @Operation(summary = "移库（货架之间搬运，写 transfer 流水）")
    @com.moyun.common.annotation.RepeatSubmit(interval = 2000)
    @com.moyun.common.annotation.RateLimiter(key = "portalledgerinventory:", time = 60, count = 120)
    @PostMapping("/stock/transfer")
    public AjaxResult transferStock(@RequestBody Map<String, Object> bo) {
        return AjaxResult.success(inventoryService.transferStock(
                PortalSecurityUtils.getUserId(),
                longVal(bo, "warehouseId"), longVal(bo, "productId"),
                longVal(bo, "fromShelfId"), longVal(bo, "toShelfId"),
                intVal(bo, "quantity"), strVal(bo, "remark")));
    }

    @Operation(summary = "库存流水列表")
    @GetMapping("/warehouses/{warehouseId}/flows")
    public AjaxResult listStockFlows(@PathVariable("warehouseId") Long warehouseId,
                                     @RequestParam(value = "pageNum", required = false) Integer pageNum,
                                     @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return AjaxResult.success(inventoryService.listStockFlows(
                PortalSecurityUtils.getUserId(), warehouseId, pageNum, pageSize));
    }

    // ---------------- 内部：从 JSON body 取值（容错） ----------------

    private Long longVal(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? null : Long.valueOf(String.valueOf(v));
    }

    private Integer intVal(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? null : Integer.valueOf(String.valueOf(v));
    }

    private java.math.BigDecimal decVal(Map<String, Object> m, String k) {
        Object v = m.get(k);
        if (v == null) {
            return null;
        }
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return new java.math.BigDecimal(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String strVal(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? null : String.valueOf(v);
    }
}
