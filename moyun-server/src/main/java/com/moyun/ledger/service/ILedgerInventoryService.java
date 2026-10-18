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

    /** 商品列表（带当前库存数量 + 类目名），分页 + 关键字 + 类目筛选 */
    Map<String, Object> listProducts(Long userId, Long warehouseId, String keyword, Long categoryId,
                                     Long shelfId, Integer pageNum, Integer pageSize);

    /**
     * 相似商品推荐：录入商品时先看看是不是已经有了，避免重复建档。
     *
     * <p>按「归一化名称（去空格/标点、忽略大小写）+ 条码」与同仓已有商品比对，
     * 返回带库存分布的前 10 条，前端可直接「入库到这个商品」而不是新建。</p>
     */
    List<Map<String, Object>> findSimilarProducts(Long userId, Long warehouseId, String name, Long excludeId);

    /** 新增商品（同时建一条 quantity=0 的库存行） */
    Long createProduct(Long userId, LedgerProduct bo);

    void updateProduct(Long userId, Long id, LedgerProduct bo);

    void deleteProduct(Long userId, Long id);

    /**
     * 商品详情：商品档案 + 各货架库存分布（含未上架那一行）。
     * 图片不在返回体里，由 Controller 从 sys_file 补。
     */
    Map<String, Object> productDetail(Long userId, Long productId);

    // ---------------- 出入库 ----------------

    /** 入库：数量 +，写 in 流水（reason 记原因，如 purchase/gift/other） */
    Map<String, Object> stockIn(Long userId, Long warehouseId, Long productId, Long shelfId,
                                Integer quantity, String reason, String remark);

    /** 出库：数量 -（校验不超该货架现有库存），reason 记原因（sale/self_use/loss/transfer/gift/other） */
    Map<String, Object> stockOut(Long userId, Long warehouseId, Long productId, Long shelfId,
                                 Integer quantity, String reason, java.math.BigDecimal salePrice, Long channelId, String remark);

    /**
     * 库存盘点：按实盘数量与账面比对，差异写一条 adjust 流水（正=盘盈，负=盘亏）。
     * items 每项 {productId, actualQuantity}。
     */
    Map<String, Object> stocktake(Long userId, Long warehouseId, Long shelfId,
                                  java.util.List<Map<String, Object>> items, String remark);

    /** 仓库概览：库存总价值 / 待售件数 / 商品数 / 货架数 / 本月出入库 / 零库存商品数 */
    Map<String, Object> warehouseOverview(Long userId, Long warehouseId);

    /**
     * 移库：从 fromShelfId 搬到 toShelfId，同一事务内一减一加，写一条 transfer 流水。
     * 源货架库存不足时拒绝。
     */
    Map<String, Object> transferStock(Long userId, Long warehouseId, Long productId,
                                      Long fromShelfId, Long toShelfId, Integer quantity, String remark);

    // ---------------- 流水 ----------------

    Map<String, Object> listStockFlows(Long userId, Long warehouseId, Integer pageNum, Integer pageSize);
}
