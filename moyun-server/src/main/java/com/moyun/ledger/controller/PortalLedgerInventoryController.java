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
    @PostMapping("/warehouses")
    public AjaxResult createWarehouse(@RequestBody LedgerWarehouse bo) {
        return AjaxResult.success(inventoryService.createWarehouse(PortalSecurityUtils.getUserId(), bo));
    }

    @Operation(summary = "修改仓库")
    @PutMapping("/warehouses/{id}")
    public AjaxResult updateWarehouse(@PathVariable("id") Long id, @RequestBody LedgerWarehouse bo) {
        inventoryService.updateWarehouse(PortalSecurityUtils.getUserId(), id, bo);
        return AjaxResult.success();
    }

    @Operation(summary = "删除仓库")
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
                                   @RequestParam(value = "pageNum", required = false) Integer pageNum,
                                   @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return AjaxResult.success(inventoryService.listProducts(
                PortalSecurityUtils.getUserId(), warehouseId, keyword, pageNum, pageSize));
    }

    @Operation(summary = "新增商品")
    @PostMapping("/products")
    public AjaxResult createProduct(@RequestBody LedgerProduct bo) {
        return AjaxResult.success(inventoryService.createProduct(PortalSecurityUtils.getUserId(), bo));
    }

    @Operation(summary = "修改商品")
    @PutMapping("/products/{id}")
    public AjaxResult updateProduct(@PathVariable("id") Long id, @RequestBody LedgerProduct bo) {
        inventoryService.updateProduct(PortalSecurityUtils.getUserId(), id, bo);
        return AjaxResult.success();
    }

    @Operation(summary = "删除商品")
    @DeleteMapping("/products/{id}")
    public AjaxResult deleteProduct(@PathVariable("id") Long id) {
        inventoryService.deleteProduct(PortalSecurityUtils.getUserId(), id);
        return AjaxResult.success();
    }

    // ---------------- 出入库 ----------------

    @Operation(summary = "入库（可选货架，缺省=未上架）")
    @PostMapping("/stock/in")
    public AjaxResult stockIn(@RequestBody Map<String, Object> bo) {
        return AjaxResult.success(inventoryService.stockIn(
                PortalSecurityUtils.getUserId(),
                longVal(bo, "warehouseId"), longVal(bo, "productId"), longVal(bo, "shelfId"),
                intVal(bo, "quantity"), strVal(bo, "remark")));
    }

    @Operation(summary = "出库（可选货架，缺省=未上架）")
    @PostMapping("/stock/out")
    public AjaxResult stockOut(@RequestBody Map<String, Object> bo) {
        return AjaxResult.success(inventoryService.stockOut(
                PortalSecurityUtils.getUserId(),
                longVal(bo, "warehouseId"), longVal(bo, "productId"), longVal(bo, "shelfId"),
                intVal(bo, "quantity"), strVal(bo, "remark")));
    }

    @Operation(summary = "移库（货架之间搬运，写 transfer 流水）")
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

    private String strVal(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? null : String.valueOf(v);
    }
}
