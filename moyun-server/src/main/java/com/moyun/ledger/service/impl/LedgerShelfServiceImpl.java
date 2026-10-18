package com.moyun.ledger.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.ext.file.domain.entity.SysFile;
import com.moyun.ext.file.mapper.SysFileMapper;
import com.moyun.ledger.domain.entity.LedgerProduct;
import com.moyun.ledger.domain.entity.LedgerShelf;
import com.moyun.ledger.domain.entity.LedgerStock;
import com.moyun.ledger.domain.entity.LedgerWarehouse;
import com.moyun.ledger.mapper.LedgerProductMapper;
import com.moyun.ledger.mapper.LedgerShelfMapper;
import com.moyun.ledger.mapper.LedgerStockMapper;
import com.moyun.ledger.mapper.LedgerWarehouseMapper;
import com.moyun.ledger.service.ILedgerShelfService;
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
 * 货架服务实现。
 *
 * <p>货架是仓库里的第二层归属：仓库 → 货架 → 商品 → 库存行。
 * 库存按 (product_id, shelf_id) 唯一，故货架上还有货时不允许删除货架。</p>
 *
 * @author moyun
 */
@Service
public class LedgerShelfServiceImpl implements ILedgerShelfService {

    @Autowired
    private LedgerShelfMapper shelfMapper;

    @Autowired
    private LedgerWarehouseMapper warehouseMapper;

    @Autowired
    private LedgerStockMapper stockMapper;

    @Autowired
    private LedgerProductMapper productMapper;

    @Autowired
    private SysFileMapper sysFileMapper;

    // ---------------- 查询 ----------------

    @Override
    public List<LedgerShelf> listShelves(Long userId, Long warehouseId) {
        LedgerWarehouse w = getOwnedWarehouse(userId, warehouseId);
        List<LedgerShelf> list = shelfMapper.selectList(new LambdaQueryWrapper<LedgerShelf>()
                .eq(LedgerShelf::getWarehouseId, warehouseId)
                .eq(LedgerShelf::getStatus, LedgerShelf.STATUS_ENABLED)
                .orderByAsc(LedgerShelf::getSortOrder)
                .orderByAsc(LedgerShelf::getCode));
        if (list.isEmpty()) {
            return list;
        }
        // 一次 IN 查询聚合每个货架的商品种类/件数/成本价值，避免 N+1
        List<Long> shelfIds = list.stream().map(LedgerShelf::getId).collect(Collectors.toList());
        List<LedgerStock> stocks = stockMapper.selectList(new LambdaQueryWrapper<LedgerStock>()
                .in(LedgerStock::getShelfId, shelfIds)
                .eq(LedgerStock::getStatus, LedgerStock.STATUS_NORMAL));
        Map<Long, List<LedgerStock>> byShelf = stocks.stream()
                .collect(Collectors.groupingBy(LedgerStock::getShelfId));
        // 封面图：batch 查 sys_file，每货架取 id 最小的一张
        Map<String, List<SysFile>> filesByBiz = sysFileMapper.selectList(
                        new LambdaQueryWrapper<SysFile>()
                                .eq(SysFile::getBusinessType, LedgerShelf.FILE_BIZ_TYPE)
                                .in(SysFile::getBusinessId, shelfIds.stream().map(String::valueOf).collect(Collectors.toList()))
                                .orderByAsc(SysFile::getId))
                .stream().collect(Collectors.groupingBy(SysFile::getBusinessId));
        for (LedgerShelf s : list) {
            List<SysFile> imgs = filesByBiz.getOrDefault(String.valueOf(s.getId()), new ArrayList<>());
            s.setCoverUrl(imgs.isEmpty() ? null : imgs.get(0).getFileUrl());
            List<LedgerStock> rows = byShelf.getOrDefault(s.getId(), new ArrayList<>());
            s.setProductCount((int) rows.stream()
                    .filter(r -> r.getQuantity() != null && r.getQuantity() > 0).count());
            s.setStockQuantity(rows.stream()
                    .mapToInt(r -> r.getQuantity() == null ? 0 : r.getQuantity()).sum());
            s.setCostValue(rows.stream()
                    .map(r -> r.getCostValue() == null ? BigDecimal.ZERO : r.getCostValue())
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            s.setWarehouseName(w.getName());
        }
        return list;
    }

    @Override
    public Map<String, Object> shelfDetail(Long userId, Long id) {
        LedgerShelf shelf = getOwnedShelf(userId, id);
        LedgerWarehouse w = warehouseMapper.selectById(shelf.getWarehouseId());
        shelf.setWarehouseName(w == null ? null : w.getName());

        // 该货架上的库存行 + 商品档案
        List<LedgerStock> stocks = stockMapper.selectList(new LambdaQueryWrapper<LedgerStock>()
                .eq(LedgerStock::getShelfId, id)
                .eq(LedgerStock::getStatus, LedgerStock.STATUS_NORMAL)
                .gt(LedgerStock::getQuantity, 0)
                .orderByDesc(LedgerStock::getQuantity));
        List<Map<String, Object>> items = new ArrayList<>();
        int totalQty = 0;
        BigDecimal totalCost = BigDecimal.ZERO;
        if (!stocks.isEmpty()) {
            List<Long> productIds = stocks.stream().map(LedgerStock::getProductId).collect(Collectors.toList());
            Map<Long, LedgerProduct> products = productMapper.selectBatchIds(productIds).stream()
                    .collect(Collectors.toMap(LedgerProduct::getId, p -> p, (a, b) -> a));
            for (LedgerStock st : stocks) {
                LedgerProduct p = products.get(st.getProductId());
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("productId", st.getProductId());
                row.put("productName", p == null ? "（已删除商品）" : p.getName());
                row.put("unit", p == null ? null : p.getUnit());
                row.put("costPrice", p == null ? null : p.getCostPrice());
                row.put("sellPrice", p == null ? null : p.getSellPrice());
                row.put("quantity", st.getQuantity());
                row.put("costValue", st.getCostValue());
                items.add(row);
                totalQty += st.getQuantity() == null ? 0 : st.getQuantity();
                totalCost = totalCost.add(st.getCostValue() == null ? BigDecimal.ZERO : st.getCostValue());
            }
        }

        Map<String, Object> m = new HashMap<>();
        m.put("shelf", shelf);
        m.put("items", items);
        m.put("totalQuantity", totalQty);
        m.put("totalCostValue", totalCost);
        m.put("overCapacity", shelf.getCapacity() != null && shelf.getCapacity() > 0 && totalQty > shelf.getCapacity());
        return m;
    }

    // ---------------- 增删改 ----------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createShelf(Long userId, LedgerShelf bo) {
        if (bo.getWarehouseId() == null) {
            throw new ServiceException("请指定所属仓库");
        }
        getOwnedWarehouse(userId, bo.getWarehouseId());
        String code = trim(bo.getCode());
        if (code == null) {
            throw new ServiceException("请填写货架编号");
        }
        String name = trim(bo.getName());
        if (name == null) {
            throw new ServiceException("请填写货架名称");
        }
        assertCodeUnique(bo.getWarehouseId(), code, null);

        LedgerShelf s = new LedgerShelf();
        s.setWarehouseId(bo.getWarehouseId());
        s.setUserId(userId);
        s.setCode(code);
        s.setName(name);
        applyPosition(s, bo);
        s.setVersion(0);
        s.setStatus(LedgerShelf.STATUS_ENABLED);
        s.setCreateBy(String.valueOf(userId));
        s.setLocationText(buildLocation(s));
        shelfMapper.insert(s);
        return s.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateShelf(Long userId, Long id, LedgerShelf bo) {
        LedgerShelf s = getOwnedShelf(userId, id);
        if (bo.getCode() != null) {
            String code = trim(bo.getCode());
            if (code == null) {
                throw new ServiceException("货架编号不能为空");
            }
            assertCodeUnique(s.getWarehouseId(), code, id);
            s.setCode(code);
        }
        if (bo.getName() != null) {
            String name = trim(bo.getName());
            if (name == null) {
                throw new ServiceException("货架名称不能为空");
            }
            s.setName(name);
        }
        applyPosition(s, bo);
        s.setLocationText(buildLocation(s));
        shelfMapper.updateById(s);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteShelf(Long userId, Long id) {
        LedgerShelf s = getOwnedShelf(userId, id);
        // 货架上有货时禁止删除，否则库存行会指向一个不存在的货架
        Long occupied = stockMapper.selectCount(new LambdaQueryWrapper<LedgerStock>()
                .eq(LedgerStock::getShelfId, id)
                .eq(LedgerStock::getStatus, LedgerStock.STATUS_NORMAL)
                .gt(LedgerStock::getQuantity, 0));
        if (occupied != null && occupied > 0) {
            throw new ServiceException("该货架还有库存，请先移库后再删除");
        }
        s.setStatus(LedgerShelf.STATUS_DISABLED);
        shelfMapper.updateById(s);
    }

    // ---------------- 内部 ----------------

    /** 位置类字段（区域/排/层/位/类型/容量/排序/备注）统一写入 */
    private void applyPosition(LedgerShelf s, LedgerShelf bo) {
        s.setZone(trim(bo.getZone()));
        s.setRowNo(bo.getRowNo());
        s.setLayerNo(bo.getLayerNo());
        s.setPositionNo(bo.getPositionNo());
        s.setShelfType(bo.getShelfType() == null || bo.getShelfType().isBlank()
                ? LedgerShelf.TYPE_NORMAL : bo.getShelfType());
        s.setCapacity(bo.getCapacity());
        s.setSortOrder(bo.getSortOrder() == null ? 0 : bo.getSortOrder());
        s.setRemark(bo.getRemark());
    }

    /** 位置描述：A区 3排 2层 5位（缺省项自动跳过） */
    private String buildLocation(LedgerShelf s) {
        StringBuilder sb = new StringBuilder();
        if (s.getZone() != null && !s.getZone().isBlank()) {
            sb.append(s.getZone());
        }
        if (s.getRowNo() != null) {
            sb.append(sb.length() > 0 ? " " : "").append(s.getRowNo()).append("排");
        }
        if (s.getLayerNo() != null) {
            sb.append(sb.length() > 0 ? " " : "").append(s.getLayerNo()).append("层");
        }
        if (s.getPositionNo() != null) {
            sb.append(sb.length() > 0 ? " " : "").append(s.getPositionNo()).append("位");
        }
        String text = sb.toString().trim();
        // 全空时至少回落到名称，避免列表出现空白位置
        return text.isEmpty() ? s.getName() : text;
    }

    /** 同仓内货架编号唯一（excludeId 用于修改时排除自身） */
    private void assertCodeUnique(Long warehouseId, String code, Long excludeId) {
        LambdaQueryWrapper<LedgerShelf> qw = new LambdaQueryWrapper<LedgerShelf>()
                .eq(LedgerShelf::getWarehouseId, warehouseId)
                .eq(LedgerShelf::getCode, code)
                .eq(LedgerShelf::getStatus, LedgerShelf.STATUS_ENABLED);
        if (excludeId != null) {
            qw.ne(LedgerShelf::getId, excludeId);
        }
        Long cnt = shelfMapper.selectCount(qw);
        if (cnt != null && cnt > 0) {
            throw new ServiceException("货架编号「" + code + "」在该仓库已存在");
        }
    }

    private LedgerWarehouse getOwnedWarehouse(Long userId, Long warehouseId) {
        if (warehouseId == null) {
            throw new ServiceException("请指定仓库");
        }
        LedgerWarehouse w = warehouseMapper.selectById(warehouseId);
        if (w == null || !userId.equals(w.getUserId())
                || w.getStatus() == null || w.getStatus() != LedgerWarehouse.STATUS_NORMAL) {
            throw new ServiceException("仓库不存在或无权操作");
        }
        return w;
    }

    private LedgerShelf getOwnedShelf(Long userId, Long id) {
        LedgerShelf s = shelfMapper.selectById(id);
        if (s == null || s.getStatus() == null || s.getStatus() != LedgerShelf.STATUS_ENABLED) {
            throw new ServiceException("货架不存在或已停用");
        }
        getOwnedWarehouse(userId, s.getWarehouseId());
        return s;
    }

    private String trim(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
