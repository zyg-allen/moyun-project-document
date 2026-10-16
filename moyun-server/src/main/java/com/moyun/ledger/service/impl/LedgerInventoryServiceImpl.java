package com.moyun.ledger.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.ledger.domain.entity.LedgerProduct;
import com.moyun.ledger.domain.entity.LedgerStock;
import com.moyun.ledger.domain.entity.LedgerStockFlow;
import com.moyun.ledger.domain.entity.LedgerWarehouse;
import com.moyun.ledger.mapper.LedgerProductMapper;
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
            Map<Long, Integer> qty = stockMapper.selectList(new LambdaQueryWrapper<LedgerStock>()
                            .eq(LedgerStock::getWarehouseId, warehouseId)
                            .in(LedgerStock::getProductId, ids))
                    .stream().collect(Collectors.toMap(LedgerStock::getProductId, LedgerStock::getQuantity, (a, b) -> a));
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

        // 同步建库存行（数量 0），后续出入库在此行上增减
        LedgerStock s = new LedgerStock();
        s.setWarehouseId(bo.getWarehouseId());
        s.setProductId(p.getId());
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
    public Map<String, Object> stockIn(Long userId, Long warehouseId, Long productId, Integer quantity, String remark) {
        return applyStockChange(userId, warehouseId, productId, quantity, LedgerStockFlow.FLOW_IN, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> stockOut(Long userId, Long warehouseId, Long productId, Integer quantity, String remark) {
        return applyStockChange(userId, warehouseId, productId, -Math.abs(quantity), LedgerStockFlow.FLOW_OUT, remark);
    }

    /** 出入库原子操作：数量 + 流水同写 */
    private Map<String, Object> applyStockChange(Long userId, Long warehouseId, Long productId,
                                                 int change, String flowType, String remark) {
        getOwnedWarehouse(userId, warehouseId);
        getOwnedProduct(userId, productId);
        if (change == 0) {
            throw new ServiceException("数量不能为0");
        }
        LedgerStock s = stockMapper.selectOne(new LambdaQueryWrapper<LedgerStock>()
                .eq(LedgerStock::getWarehouseId, warehouseId)
                .eq(LedgerStock::getProductId, productId));
        if (s == null) {
            throw new ServiceException("库存记录不存在");
        }
        int before = s.getQuantity() == null ? 0 : s.getQuantity();
        int after = before + change;
        if (after < 0) {
            throw new ServiceException("库存不足：当前 " + before + "，本次 " + Math.abs(change));
        }
        s.setQuantity(after);
        stockMapper.updateById(s);

        LedgerStockFlow f = new LedgerStockFlow();
        f.setWarehouseId(warehouseId);
        f.setProductId(productId);
        f.setFlowType(flowType);
        f.setQuantityBefore(before);
        f.setQuantityChange(change);
        f.setQuantityAfter(after);
        f.setOperatorId(userId);
        f.setRemark(remark);
        f.setStatus(LedgerStockFlow.STATUS_NORMAL);
        stockFlowMapper.insert(f);

        Map<String, Object> m = new HashMap<>();
        m.put("quantityAfter", after);
        m.put("flowId", f.getId());
        return m;
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
