package com.moyun.ledger.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.ledger.domain.entity.LedgerProduct;
import com.moyun.ledger.domain.entity.LedgerShelf;
import com.moyun.ledger.domain.entity.LedgerStock;
import com.moyun.ledger.domain.entity.LedgerStockFlow;
import com.moyun.ledger.domain.entity.LedgerWarehouse;
import com.moyun.ledger.mapper.LedgerProductMapper;
import com.moyun.ledger.mapper.LedgerShelfMapper;
import com.moyun.ledger.mapper.LedgerStockFlowMapper;
import com.moyun.ledger.mapper.LedgerStockMapper;
import com.moyun.ledger.mapper.LedgerWarehouseMapper;
import com.moyun.ledger.service.ILedgerInventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 库存服务实现。
 *
 * @author moyun
 */
@Service
public class LedgerInventoryServiceImpl implements ILedgerInventoryService {

    @Autowired
    private LedgerWarehouseMapper warehouseMapper;
    @Autowired
    private LedgerProductMapper productMapper;
    @Autowired
    private LedgerStockMapper stockMapper;
    @Autowired
    private LedgerStockFlowMapper stockFlowMapper;

    @Autowired
    private LedgerShelfMapper shelfMapper;

    // ---------------- 仓库 ----------------

    @Override
    public List<LedgerWarehouse> listWarehouses(Long userId) {
        return warehouseMapper.selectList(new LambdaQueryWrapper<LedgerWarehouse>()
                .eq(LedgerWarehouse::getUserId, userId)
                .eq(LedgerWarehouse::getStatus, LedgerWarehouse.STATUS_NORMAL)
                .orderByAsc(LedgerWarehouse::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createWarehouse(Long userId, LedgerWarehouse bo) {
        if (bo.getName() == null || bo.getName().isBlank()) {
            throw new ServiceException("请填写仓库名称");
        }
        LedgerWarehouse w = new LedgerWarehouse();
        w.setUserId(userId);
        w.setName(bo.getName().trim());
        w.setWarehouseType(bo.getWarehouseType() == null ? LedgerWarehouse.TYPE_PERSONAL : bo.getWarehouseType());
        w.setIsActive(bo.getIsActive() == null ? 1 : bo.getIsActive());
        w.setStatus(LedgerWarehouse.STATUS_NORMAL);
        w.setCreateBy(bo.getCreateBy());
        warehouseMapper.insert(w);
        return w.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWarehouse(Long userId, Long id, LedgerWarehouse bo) {
        LedgerWarehouse db = getOwnedWarehouse(userId, id);
        if (bo.getName() != null && !bo.getName().isBlank()) {
            db.setName(bo.getName().trim());
        }
        if (bo.getWarehouseType() != null) {
            db.setWarehouseType(bo.getWarehouseType());
        }
        if (bo.getIsActive() != null) {
            db.setIsActive(bo.getIsActive());
        }
        warehouseMapper.updateById(db);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWarehouse(Long userId, Long id) {
        LedgerWarehouse db = getOwnedWarehouse(userId, id);
        db.setStatus(LedgerWarehouse.STATUS_DELETED);
        warehouseMapper.updateById(db);
    }

    // ---------------- 商品 ----------------

    @Override
    public Map<String, Object> listProducts(Long userId, Long warehouseId, String keyword,
                                            Integer pageNum, Integer pageSize) {
        // 仓库归属校验：不存在的仓库/别人的仓库直接拒绝
        getOwnedWarehouse(userId, warehouseId);

        LambdaQueryWrapper<LedgerProduct> q = new LambdaQueryWrapper<>();
        q.eq(LedgerProduct::getWarehouseId, warehouseId)
                .eq(LedgerProduct::getStatus, LedgerProduct.STATUS_NORMAL);
        if (keyword != null && !keyword.isBlank()) {
            q.like(LedgerProduct::getName, keyword.trim());
        }
        q.orderByDesc(LedgerProduct::getId);

        int pn = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int ps = (pageSize == null || pageSize < 1) ? 20 : Math.min(pageSize, 100);
        Page<LedgerProduct> page = productMapper.selectPage(new Page<>(pn, ps), q);
        List<LedgerProduct> records = page.getRecords();

        // 回填库存数量：一次 IN 查询，避免 N+1
        if (!records.isEmpty()) {
            List<Long> ids = records.stream().map(LedgerProduct::getId).collect(Collectors.toList());
            // 跨货架求和：同一商品可能分布在多个货架（唯一键 product_id+shelf_id）
            Map<Long, Integer> qty = stockMapper.selectList(new LambdaQueryWrapper<LedgerStock>()
                            .eq(LedgerStock::getWarehouseId, warehouseId)
                            .eq(LedgerStock::getStatus, LedgerStock.STATUS_NORMAL)
                            .in(LedgerStock::getProductId, ids))
                    .stream().collect(Collectors.toMap(LedgerStock::getProductId, LedgerStock::getQuantity, Integer::sum));
            records.forEach(p -> p.setStockQuantity(qty.getOrDefault(p.getId(), 0)));
        }

        Map<String, Object> m = new HashMap<>();
        m.put("records", records);
        m.put("total", page.getTotal());
        m.put("pageNum", pn);
        m.put("pageSize", ps);
        m.put("pages", page.getPages());
        return m;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createProduct(Long userId, LedgerProduct bo) {
        getOwnedWarehouse(userId, bo.getWarehouseId());
        if (bo.getName() == null || bo.getName().isBlank()) {
            throw new ServiceException("请填写商品名称");
        }
        LedgerProduct p = new LedgerProduct();
        p.setWarehouseId(bo.getWarehouseId());
        p.setUserId(userId);
        p.setName(bo.getName().trim());
        p.setBarcode(bo.getBarcode());
        p.setCategory(bo.getCategory());
        p.setUnit(bo.getUnit() == null ? "个" : bo.getUnit());
        p.setCostPrice(bo.getCostPrice());
        p.setSellPrice(bo.getSellPrice());
        p.setImageUrls(bo.getImageUrls());
        p.setIsSecondhand(bo.getIsSecondhand() == null ? 0 : bo.getIsSecondhand());
        p.setConditionGrade(bo.getConditionGrade());
        p.setSourceChannel(bo.getSourceChannel());
        p.setRemark(bo.getRemark());
        p.setStatus(LedgerProduct.STATUS_NORMAL);
        productMapper.insert(p);

        // 同步建库存行（数量 0、未上架），后续入库按货架再建/累加
        LedgerStock s = new LedgerStock();
        s.setWarehouseId(bo.getWarehouseId());
        s.setProductId(p.getId());
        s.setShelfId(LedgerStock.NO_SHELF);
        s.setQuantity(0);
        s.setCostValue(BigDecimal.ZERO);
        s.setStatus(LedgerStock.STATUS_NORMAL);
        stockMapper.insert(s);
        return p.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProduct(Long userId, Long id, LedgerProduct bo) {
        LedgerProduct db = getOwnedProduct(userId, id);
        if (bo.getName() != null && !bo.getName().isBlank()) {
            db.setName(bo.getName().trim());
        }
        if (bo.getCostPrice() != null) {
            db.setCostPrice(bo.getCostPrice());
        }
        if (bo.getSellPrice() != null) {
            db.setSellPrice(bo.getSellPrice());
        }
        if (bo.getCategory() != null) {
            db.setCategory(bo.getCategory());
        }
        if (bo.getRemark() != null) {
            db.setRemark(bo.getRemark());
        }
        productMapper.updateById(db);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long userId, Long id) {
        LedgerProduct db = getOwnedProduct(userId, id);
        db.setStatus(LedgerProduct.STATUS_DELETED);
        productMapper.updateById(db);
    }

    // ---------------- 出入库 ----------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> stockIn(Long userId, Long warehouseId, Long productId, Long shelfId,
                                       Integer quantity, String remark) {
        return applyStockChange(userId, warehouseId, productId, shelfId, quantity,
                LedgerStockFlow.FLOW_IN, null, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> stockOut(Long userId, Long warehouseId, Long productId, Long shelfId,
                                        Integer quantity, String remark) {
        return applyStockChange(userId, warehouseId, productId, shelfId, -Math.abs(quantity),
                LedgerStockFlow.FLOW_OUT, null, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> transferStock(Long userId, Long warehouseId, Long productId,
                                              Long fromShelfId, Long toShelfId, Integer quantity, String remark) {
        long from = fromShelfId == null ? LedgerStock.NO_SHELF : fromShelfId;
        long to = toShelfId == null ? LedgerStock.NO_SHELF : toShelfId;
        if (from == to) {
            throw new ServiceException("源货架与目标货架不能相同");
        }
        if (quantity == null || quantity <= 0) {
            throw new ServiceException("移库数量必须大于 0");
        }
        getOwnedWarehouse(userId, warehouseId);
        LedgerProduct p = getOwnedProduct(userId, productId);
        assertShelfBelongs(warehouseId, from);
        assertShelfBelongs(warehouseId, to);

        LedgerStock src = findStock(productId, from);
        int before = src == null || src.getQuantity() == null ? 0 : src.getQuantity();
        if (before < quantity) {
            throw new ServiceException("源货架库存不足：当前 " + before + "，本次移动 " + quantity);
        }
        BigDecimal costPrice = p.getCostPrice() == null ? BigDecimal.ZERO : p.getCostPrice();

        // 源减
        src.setQuantity(before - quantity);
        src.setCostValue(costPrice.multiply(BigDecimal.valueOf(src.getQuantity())));
        stockMapper.updateById(src);

        // 目标加（该货架还没这个商品时自动建行）
        LedgerStock dst = findStock(productId, to);
        if (dst == null) {
            dst = newStockRow(warehouseId, productId, to);
            stockMapper.insert(dst);
        }
        int dstBefore = dst.getQuantity() == null ? 0 : dst.getQuantity();
        dst.setQuantity(dstBefore + quantity);
        dst.setCostValue(costPrice.multiply(BigDecimal.valueOf(dst.getQuantity())));
        stockMapper.updateById(dst);

        // 一条 transfer 流水，同时带源货架与目标货架，便于回溯
        LedgerStockFlow f = new LedgerStockFlow();
        f.setWarehouseId(warehouseId);
        f.setProductId(productId);
        f.setShelfId(from);
        f.setToShelfId(to);
        f.setFlowType(LedgerStockFlow.FLOW_TRANSFER);
        f.setQuantityBefore(before);
        f.setQuantityChange(quantity);
        f.setQuantityAfter(before - quantity);
        f.setCostBefore(costPrice.multiply(BigDecimal.valueOf(before)));
        f.setCostAfter(costPrice.multiply(BigDecimal.valueOf(before - quantity)));
        f.setOperatorId(userId);
        f.setRemark(remark);
        f.setStatus(LedgerStockFlow.STATUS_NORMAL);
        stockFlowMapper.insert(f);

        Map<String, Object> m = new HashMap<>();
        m.put("fromQuantity", before - quantity);
        m.put("toQuantity", dstBefore + quantity);
        m.put("flowId", f.getId());
        return m;
    }

    /** 出入库原子操作：数量 + 流水同写（按 商品×货架 定位库存行） */
    private Map<String, Object> applyStockChange(Long userId, Long warehouseId, Long productId,
                                                 Long shelfId, int change, String flowType,
                                                 Long toShelfId, String remark) {
        getOwnedWarehouse(userId, warehouseId);
        LedgerProduct p = getOwnedProduct(userId, productId);
        if (change == 0) {
            throw new ServiceException("数量不能为0");
        }
        long sid = shelfId == null ? LedgerStock.NO_SHELF : shelfId;
        assertShelfBelongs(warehouseId, sid);

        LedgerStock s = findStock(productId, sid);
        if (s == null) {
            if (change < 0) {
                throw new ServiceException("该货架暂无此商品库存，无法出库");
            }
            // 首次入到某货架：自动建行，避免"库存记录不存在"
            s = newStockRow(warehouseId, productId, sid);
            stockMapper.insert(s);
        }
        int before = s.getQuantity() == null ? 0 : s.getQuantity();
        int after = before + change;
        if (after < 0) {
            throw new ServiceException("库存不足：当前 " + before + "，本次 " + Math.abs(change));
        }
        // 库存按成本价计（文档 2.1）：cost_value = 成本价 × 数量，供总资产公式用
        BigDecimal costPrice = p.getCostPrice() == null ? BigDecimal.ZERO : p.getCostPrice();
        s.setQuantity(after);
        s.setCostValue(costPrice.multiply(BigDecimal.valueOf(after)));
        stockMapper.updateById(s);

        LedgerStockFlow f = new LedgerStockFlow();
        f.setWarehouseId(warehouseId);
        f.setProductId(productId);
        f.setShelfId(sid);
        f.setToShelfId(toShelfId == null ? 0L : toShelfId);
        f.setFlowType(flowType);
        f.setQuantityBefore(before);
        f.setQuantityChange(change);
        f.setQuantityAfter(after);
        f.setCostBefore(costPrice.multiply(BigDecimal.valueOf(before)));
        f.setCostAfter(costPrice.multiply(BigDecimal.valueOf(after)));
        f.setOperatorId(userId);
        f.setRemark(remark);
        f.setStatus(LedgerStockFlow.STATUS_NORMAL);
        stockFlowMapper.insert(f);

        Map<String, Object> m = new HashMap<>();
        m.put("quantityAfter", after);
        m.put("flowId", f.getId());
        return m;
    }

    /** 建一条空库存行 */
    private LedgerStock newStockRow(Long warehouseId, Long productId, long shelfId) {
        LedgerStock s = new LedgerStock();
        s.setWarehouseId(warehouseId);
        s.setProductId(productId);
        s.setShelfId(shelfId);
        s.setQuantity(0);
        s.setCostValue(BigDecimal.ZERO);
        s.setStatus(LedgerStock.STATUS_NORMAL);
        return s;
    }

    /** 定位某商品在某货架上的库存行 */
    private LedgerStock findStock(Long productId, long shelfId) {
        return stockMapper.selectOne(new LambdaQueryWrapper<LedgerStock>()
                .eq(LedgerStock::getProductId, productId)
                .eq(LedgerStock::getShelfId, shelfId)
                .eq(LedgerStock::getStatus, LedgerStock.STATUS_NORMAL)
                .last("LIMIT 1"));
    }

    /** 货架必须存在、启用且属于当前仓库（0=未上架直接放行，兼容旧调用） */
    private void assertShelfBelongs(Long warehouseId, long shelfId) {
        if (shelfId == LedgerStock.NO_SHELF) {
            return;
        }
        LedgerShelf shelf = shelfMapper.selectById(shelfId);
        if (shelf == null || shelf.getStatus() == null || shelf.getStatus() != LedgerShelf.STATUS_ENABLED) {
            throw new ServiceException("货架不存在或已停用");
        }
        if (!warehouseId.equals(shelf.getWarehouseId())) {
            throw new ServiceException("货架不属于该仓库");
        }
    }

    // ---------------- 流水 ----------------

    @Override
    public Map<String, Object> listStockFlows(Long userId, Long warehouseId, Integer pageNum, Integer pageSize) {
        getOwnedWarehouse(userId, warehouseId);
        LambdaQueryWrapper<LedgerStockFlow> q = new LambdaQueryWrapper<>();
        q.eq(LedgerStockFlow::getWarehouseId, warehouseId)
                .eq(LedgerStockFlow::getStatus, LedgerStockFlow.STATUS_NORMAL)
                .orderByDesc(LedgerStockFlow::getId);
        int pn = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int ps = (pageSize == null || pageSize < 1) ? 20 : Math.min(pageSize, 100);
        Page<LedgerStockFlow> page = stockFlowMapper.selectPage(new Page<>(pn, ps), q);
        Map<String, Object> m = new HashMap<>();
        m.put("records", page.getRecords());
        m.put("total", page.getTotal());
        m.put("pageNum", pn);
        m.put("pageSize", ps);
        m.put("pages", page.getPages());
        return m;
    }

    // ---------------- 内部 ----------------

    private LedgerWarehouse getOwnedWarehouse(Long userId, Long id) {
        LedgerWarehouse w = warehouseMapper.selectById(id);
        if (w == null || !userId.equals(w.getUserId())
                || LedgerWarehouse.STATUS_DELETED == w.getStatus()) {
            throw new ServiceException("仓库不存在");
        }
        return w;
    }

    private LedgerProduct getOwnedProduct(Long userId, Long id) {
        LedgerProduct p = productMapper.selectById(id);
        if (p == null || !userId.equals(p.getUserId())
                || LedgerProduct.STATUS_DELETED == p.getStatus()) {
            throw new ServiceException("商品不存在");
        }
        return p;
    }
}
