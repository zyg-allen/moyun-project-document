package com.moyun.ledger.service;

import com.moyun.ledger.domain.entity.LedgerProduct;
import com.moyun.ledger.domain.entity.LedgerWarehouse;

import java.util.List;
import java.util.Map;

/**
 * 库存服务：仓库 / 商品 / 库存 / 库存流水的增删改查 + 出入库。
 *
 * <p>设计要点：入库/出库 = 库存数量变动 + 写一条 stock_flow 留痕，
 * 二者在同一事务内原子完成，避免"数量改了、流水没记"对不上账。</p>
 *
 * @author moyun
 */
public interface ILedgerInventoryService {

    // ---------------- 仓库 ----------------

    List<LedgerWarehouse> listWarehouses(Long userId);

    Long createWarehouse(Long userId, LedgerWarehouse bo);

    void updateWarehouse(Long userId, Long id, LedgerWarehouse bo);

    void deleteWarehouse(Long userId, Long id);

    // ---------------- 商品 ----------------

    /** 商品列表（带当前库存数量），分页 + 关键字 */
    Map<String, Object> listProducts(Long userId, Long warehouseId, String keyword,
                                     Integer pageNum, Integer pageSize);

    /** 新增商品（同时建一条 quantity=0 的库存行） */
    Long createProduct(Long userId, LedgerProduct bo);

    void updateProduct(Long userId, Long id, LedgerProduct bo);

    void deleteProduct(Long userId, Long id);

    // ---------------- 出入库 ----------------

    /** 入库：数量 +，写 in 流水 */
    Map<String, Object> stockIn(Long userId, Long warehouseId, Long productId, Integer quantity, String remark);

    /** 出库：数量 -（校验不超现有库存），写 out 流水 */
    Map<String, Object> stockOut(Long userId, Long warehouseId, Long productId, Integer quantity, String remark);

    // ---------------- 流水 ----------------

    Map<String, Object> listStockFlows(Long userId, Long warehouseId, Integer pageNum, Integer pageSize);
}
