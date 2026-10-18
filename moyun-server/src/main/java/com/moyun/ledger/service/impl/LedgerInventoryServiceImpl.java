package com.moyun.ledger.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.ledger.domain.entity.LedgerProduct;
import com.moyun.ledger.domain.entity.LedgerProductCategory;
import com.moyun.ledger.domain.entity.LedgerShelf;
import com.moyun.ledger.domain.entity.LedgerStock;
import com.moyun.ledger.domain.entity.LedgerStockFlow;
import com.moyun.ledger.domain.entity.LedgerWarehouse;
import com.moyun.ledger.mapper.LedgerProductCategoryMapper;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

    @Autowired
    private LedgerProductCategoryMapper productCategoryMapper;

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
    public Map<String, Object> listProducts(Long userId, Long warehouseId, String keyword, Long categoryId,
                                            Integer pageNum, Integer pageSize) {
        // 仓库归属校验：不存在的仓库/别人的仓库直接拒绝
        getOwnedWarehouse(userId, warehouseId);

        LambdaQueryWrapper<LedgerProduct> q = new LambdaQueryWrapper<>();
        q.eq(LedgerProduct::getWarehouseId, warehouseId)
                .eq(LedgerProduct::getStatus, LedgerProduct.STATUS_NORMAL);
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            // 名称或条码模糊匹配，找货时两者都可能被拿来搜
            q.and(w -> w.like(LedgerProduct::getName, kw).or().like(LedgerProduct::getBarcode, kw));
        }
        if (categoryId != null) {
            // 选一级类目时把其二级类目的商品也带出来
            List<Long> scope = new ArrayList<>();
            scope.add(categoryId);
            productCategoryMapper.selectList(new LambdaQueryWrapper<LedgerProductCategory>()
                            .eq(LedgerProductCategory::getParentId, categoryId))
                    .forEach(c -> scope.add(c.getId()));
            q.in(LedgerProduct::getCategoryId, scope);
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

            // 回填类目名称（列表直接展示，省一次请求）
            List<Long> catIds = records.stream().map(LedgerProduct::getCategoryId)
                    .filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
            if (!catIds.isEmpty()) {
                Map<Long, String> catNames = productCategoryMapper.selectBatchIds(catIds).stream()
                        .collect(Collectors.toMap(LedgerProductCategory::getId,
                                LedgerProductCategory::getName, (a, b) -> a));
                records.forEach(p -> p.setCategoryName(catNames.get(p.getCategoryId())));
            }
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
        p.setCategoryId(bo.getCategoryId());
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
        if (bo.getCategoryId() != null) {
            db.setCategoryId(bo.getCategoryId());
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
    public Map<String, Object> productDetail(Long userId, Long productId) {
        LedgerProduct p = getOwnedProduct(userId, productId);
        // 各货架分布：quantity>0 的行，shelfId=0 归为「未上架」
        List<LedgerStock> rows = stockMapper.selectList(new LambdaQueryWrapper<LedgerStock>()
                .eq(LedgerStock::getProductId, productId)
                .eq(LedgerStock::getStatus, LedgerStock.STATUS_NORMAL)
                .gt(LedgerStock::getQuantity, 0)
                .orderByDesc(LedgerStock::getQuantity));
        List<Map<String, Object>> shelves = new ArrayList<>();
        int totalQty = 0;
        BigDecimal totalCost = BigDecimal.ZERO;
        if (!rows.isEmpty()) {
            List<Long> shelfIds = rows.stream()
                    .map(LedgerStock::getShelfId)
                    .filter(x -> x != null && x > 0)
                    .collect(Collectors.toList());
            Map<Long, LedgerShelf> shelfMap = shelfIds.isEmpty() ? new HashMap<>()
                    : shelfMapper.selectBatchIds(shelfIds).stream()
                            .collect(Collectors.toMap(LedgerShelf::getId, x -> x, (a, b) -> a));
            for (LedgerStock st : rows) {
                LedgerShelf sh = (st.getShelfId() == null || st.getShelfId() == 0)
                        ? null : shelfMap.get(st.getShelfId());
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("shelfId", st.getShelfId());
                r.put("shelfName", sh == null ? "未上架" : sh.getName());
                r.put("shelfCode", sh == null ? null : sh.getCode());
                r.put("locationText", sh == null ? null : sh.getLocationText());
                r.put("quantity", st.getQuantity());
                r.put("costValue", st.getCostValue());
                shelves.add(r);
                totalQty += st.getQuantity() == null ? 0 : st.getQuantity();
                totalCost = totalCost.add(st.getCostValue() == null ? BigDecimal.ZERO : st.getCostValue());
            }
        }
        Map<String, Object> m = new HashMap<>();
        m.put("product", p);
        m.put("shelves", shelves);
        m.put("totalQuantity", totalQty);
        m.put("totalCostValue", totalCost);
        return m;
    }

    @Override
    public List<Map<String, Object>> findSimilarProducts(Long userId, Long warehouseId, String name, Long excludeId) {
        getOwnedWarehouse(userId, warehouseId);
        String q = normalizeName(name);
        if (q.isEmpty()) {
            return new ArrayList<>();
        }
        List<LedgerProduct> all = productMapper.selectList(new LambdaQueryWrapper<LedgerProduct>()
                .eq(LedgerProduct::getWarehouseId, warehouseId)
                .eq(LedgerProduct::getStatus, LedgerProduct.STATUS_NORMAL));
        if (all.isEmpty()) {
            return new ArrayList<>();
        }
        // 打分：条码完全一致 > 归一化同名 > 互相包含
        List<Map<String, Object>> hits = new ArrayList<>();
        for (LedgerProduct p : all) {
            if (excludeId != null && excludeId.equals(p.getId())) {
                continue;
            }
            int score = similarity(q, name, p);
            if (score <= 0) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("productId", p.getId());
            row.put("name", p.getName());
            row.put("unit", p.getUnit());
            row.put("barcode", p.getBarcode());
            row.put("costPrice", p.getCostPrice());
            row.put("sellPrice", p.getSellPrice());
            row.put("score", score);
            row.put("stockQuantity", 0);
            row.put("shelves", new ArrayList<Map<String, Object>>());
            hits.add(row);
        }
        if (hits.isEmpty()) {
            return hits;
        }
        hits.sort((a, b) -> Integer.compare((Integer) b.get("score"), (Integer) a.get("score")));
        if (hits.size() > 10) {
            hits = new ArrayList<>(hits.subList(0, 10));
        }

        // 给命中商品补库存分布（哪个货架还有多少）
        List<Long> ids = hits.stream().map(r -> (Long) r.get("productId")).collect(Collectors.toList());
        List<LedgerStock> stocks = stockMapper.selectList(new LambdaQueryWrapper<LedgerStock>()
                .in(LedgerStock::getProductId, ids)
                .eq(LedgerStock::getStatus, LedgerStock.STATUS_NORMAL)
                .gt(LedgerStock::getQuantity, 0));
        if (stocks.isEmpty()) {
            return hits;
        }
        List<Long> shelfIds = stocks.stream().map(LedgerStock::getShelfId)
                .filter(x -> x != null && x > 0).distinct().collect(Collectors.toList());
        Map<Long, LedgerShelf> shelfMap = shelfIds.isEmpty() ? new HashMap<>()
                : shelfMapper.selectBatchIds(shelfIds).stream()
                        .collect(Collectors.toMap(LedgerShelf::getId, x -> x, (a, b) -> a));
        Map<Long, List<LedgerStock>> byProduct = stocks.stream()
                .collect(Collectors.groupingBy(LedgerStock::getProductId));
        for (Map<String, Object> row : hits) {
            Long pid = (Long) row.get("productId");
            List<LedgerStock> rows = byProduct.getOrDefault(pid, new ArrayList<>());
            int total = 0;
            List<Map<String, Object>> shelves = new ArrayList<>();
            for (LedgerStock st : rows) {
                LedgerShelf sh = (st.getShelfId() == null || st.getShelfId() == 0)
                        ? null : shelfMap.get(st.getShelfId());
                Map<String, Object> s = new LinkedHashMap<>();
                s.put("shelfId", st.getShelfId());
                s.put("shelfName", sh == null ? "未上架" : sh.getName());
                s.put("quantity", st.getQuantity());
                shelves.add(s);
                total += st.getQuantity() == null ? 0 : st.getQuantity();
            }
            row.put("stockQuantity", total);
            row.put("shelves", shelves);
        }
        return hits;
    }

    /** 相似度打分：条码一致 100，归一化同名 95，互相包含 80，否则 0 */
    private int similarity(String normalizedQuery, String rawQuery, LedgerProduct p) {
        if (rawQuery != null && p.getBarcode() != null && !p.getBarcode().isBlank()
                && p.getBarcode().trim().equalsIgnoreCase(rawQuery.trim())) {
            return 100;
        }
        String pn = normalizeName(p.getName());
        if (pn.isEmpty()) {
            return 0;
        }
        if (pn.equals(normalizedQuery)) {
            return 95;
        }
        if (pn.contains(normalizedQuery) || normalizedQuery.contains(pn)) {
            return 80;
        }
        return 0;
    }

    /**
     * 归一化：转小写 + 去掉空白与常见中英文标点。
     * 避免「可口可乐 500ml」与「可口可乐500ML」被当成两个商品各建一次档。
     */
    private String normalizeName(String s) {
        if (s == null) {
            return "";
        }
        return s.toLowerCase()
                .replaceAll("[\\s\\p{Punct}，。、；：！？“”‘’（）《》【】·…—～]+", "");
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
                                       Integer quantity, String reason, String remark) {
        return applyStockChange(userId, warehouseId, productId, shelfId, quantity,
                LedgerStockFlow.FLOW_IN, defaultReason(reason, LedgerStockFlow.REASON_PURCHASE), null, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> stockOut(Long userId, Long warehouseId, Long productId, Long shelfId,
                                        Integer quantity, String reason, String remark) {
        return applyStockChange(userId, warehouseId, productId, shelfId, -Math.abs(quantity),
                LedgerStockFlow.FLOW_OUT, defaultReason(reason, LedgerStockFlow.REASON_SALE), null, remark);
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
        f.setReason(LedgerStockFlow.REASON_TRANSFER);
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
                                                 String reason, Long toShelfId, String remark) {
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
        f.setReason(reason);
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

    /** 原因缺省兜底 */
    private String defaultReason(String reason, String fallback) {
        return (reason == null || reason.isBlank()) ? fallback : reason.trim();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> stocktake(Long userId, Long warehouseId, Long shelfId,
                                          List<Map<String, Object>> items, String remark) {
        getOwnedWarehouse(userId, warehouseId);
        long sid = shelfId == null ? LedgerStock.NO_SHELF : shelfId;
        assertShelfBelongs(warehouseId, sid);
        if (items == null || items.isEmpty()) {
            throw new ServiceException("请至少提交一项盘点结果");
        }
        int gain = 0, loss = 0, changed = 0;
        List<Map<String, Object>> details = new ArrayList<>();
        for (Map<String, Object> it : items) {
            Long productId = toLong(it.get("productId"));
            Integer actual = toInt(it.get("actualQuantity"));
            if (productId == null || actual == null || actual < 0) {
                continue;
            }
            LedgerProduct p = getOwnedProduct(userId, productId);
            LedgerStock s = findStock(productId, sid);
            int before = s == null || s.getQuantity() == null ? 0 : s.getQuantity();
            int delta = actual - before;
            if (delta == 0) {
                continue;
            }
            changed++;
            BigDecimal costPrice = p.getCostPrice() == null ? BigDecimal.ZERO : p.getCostPrice();
            if (s == null) {
                s = newStockRow(warehouseId, productId, sid);
                stockMapper.insert(s);
            }
            s.setQuantity(actual);
            s.setCostValue(costPrice.multiply(BigDecimal.valueOf(actual)));
            stockMapper.updateById(s);

            // 盘盈/盘亏：一条 adjust 流水，quantity_change 即差异
            LedgerStockFlow f = new LedgerStockFlow();
            f.setWarehouseId(warehouseId);
            f.setProductId(productId);
            f.setShelfId(sid);
            f.setToShelfId(0L);
            f.setFlowType(LedgerStockFlow.FLOW_ADJUST);
            f.setReason(LedgerStockFlow.REASON_STOCKTAKE);
            f.setQuantityBefore(before);
            f.setQuantityChange(delta);
            f.setQuantityAfter(actual);
            f.setCostBefore(costPrice.multiply(BigDecimal.valueOf(before)));
            f.setCostAfter(costPrice.multiply(BigDecimal.valueOf(actual)));
            f.setOperatorId(userId);
            f.setRemark(remark == null || remark.isBlank()
                    ? (delta > 0 ? "盘盈" : "盘亏") : remark);
            f.setStatus(LedgerStockFlow.STATUS_NORMAL);
            stockFlowMapper.insert(f);

            Map<String, Object> d = new LinkedHashMap<>();
            d.put("productId", productId);
            d.put("productName", p.getName());
            d.put("before", before);
            d.put("actual", actual);
            d.put("delta", delta);
            details.add(d);
            if (delta > 0) {
                gain += delta;
            } else {
                loss += -delta;
            }
        }
        Map<String, Object> m = new HashMap<>();
        m.put("changed", changed);
        m.put("gain", gain);
        m.put("loss", loss);
        m.put("details", details);
        return m;
    }

    @Override
    public Map<String, Object> warehouseOverview(Long userId, Long warehouseId) {
        getOwnedWarehouse(userId, warehouseId);
        List<LedgerStock> stocks = stockMapper.selectList(new LambdaQueryWrapper<LedgerStock>()
                .eq(LedgerStock::getWarehouseId, warehouseId)
                .eq(LedgerStock::getStatus, LedgerStock.STATUS_NORMAL));
        BigDecimal stockValue = BigDecimal.ZERO;
        int totalQty = 0;
        java.util.Set<Long> withStock = new java.util.HashSet<>();
        for (LedgerStock s : stocks) {
            int q = s.getQuantity() == null ? 0 : s.getQuantity();
            totalQty += q;
            if (s.getCostValue() != null) {
                stockValue = stockValue.add(s.getCostValue());
            }
            if (q > 0) {
                withStock.add(s.getProductId());
            }
        }
        Long productCount = productMapper.selectCount(new LambdaQueryWrapper<LedgerProduct>()
                .eq(LedgerProduct::getWarehouseId, warehouseId)
                .eq(LedgerProduct::getStatus, LedgerProduct.STATUS_NORMAL));
        Long shelfCount = shelfMapper.selectCount(new LambdaQueryWrapper<LedgerShelf>()
                .eq(LedgerShelf::getWarehouseId, warehouseId)
                .eq(LedgerShelf::getStatus, LedgerShelf.STATUS_ENABLED));
        int pc = productCount == null ? 0 : productCount.intValue();

        // 本月出入库统计（销售数量按 reason=sale 统计，销售成本用成本值差额）
        java.time.LocalDateTime monthStart = java.time.LocalDate.now().withDayOfMonth(1).atStartOfDay();
        List<LedgerStockFlow> monthFlows = stockFlowMapper.selectList(new LambdaQueryWrapper<LedgerStockFlow>()
                .eq(LedgerStockFlow::getWarehouseId, warehouseId)
                .eq(LedgerStockFlow::getStatus, LedgerStockFlow.STATUS_NORMAL)
                .ge(LedgerStockFlow::getCreateTime, monthStart));
        int monthIn = 0, monthOut = 0, monthSaleQty = 0;
        BigDecimal monthSaleCost = BigDecimal.ZERO;
        for (LedgerStockFlow f : monthFlows) {
            int c = f.getQuantityChange() == null ? 0 : f.getQuantityChange();
            if (c > 0) {
                monthIn += c;
            } else {
                monthOut += -c;
                if (LedgerStockFlow.REASON_SALE.equals(f.getReason())) {
                    monthSaleQty += -c;
                    BigDecimal cb = f.getCostBefore() == null ? BigDecimal.ZERO : f.getCostBefore();
                    BigDecimal ca = f.getCostAfter() == null ? BigDecimal.ZERO : f.getCostAfter();
                    monthSaleCost = monthSaleCost.add(cb.subtract(ca));
                }
            }
        }

        Map<String, Object> m = new HashMap<>();
        m.put("stockValue", stockValue);
        m.put("totalQuantity", totalQty);
        m.put("productCount", pc);
        m.put("shelfCount", shelfCount == null ? 0 : shelfCount);
        m.put("zeroStockCount", Math.max(pc - withStock.size(), 0));
        m.put("monthInQuantity", monthIn);
        m.put("monthOutQuantity", monthOut);
        m.put("monthSaleQuantity", monthSaleQty);
        m.put("monthSaleCost", monthSaleCost);
        return m;
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        try { return Long.valueOf(String.valueOf(v)); } catch (Exception e) { return null; }
    }

    private Integer toInt(Object v) {
        if (v == null) return null;
        try { return Integer.valueOf(String.valueOf(v)); } catch (Exception e) { return null; }
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
