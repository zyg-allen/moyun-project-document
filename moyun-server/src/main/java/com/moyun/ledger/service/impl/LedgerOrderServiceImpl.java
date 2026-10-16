package com.moyun.ledger.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.ledger.domain.entity.LedgerObligation;
import com.moyun.ledger.domain.entity.LedgerProduct;
import com.moyun.ledger.domain.entity.LedgerPurchaseOrder;
import com.moyun.ledger.domain.entity.LedgerPurchaseOrderItem;
import com.moyun.ledger.domain.entity.LedgerSaleOrder;
import com.moyun.ledger.domain.entity.LedgerSaleOrderItem;
import com.moyun.ledger.domain.entity.LedgerStock;
import com.moyun.ledger.domain.entity.LedgerStockFlow;
import com.moyun.ledger.domain.entity.LedgerWarehouse;
import com.moyun.ledger.mapper.LedgerObligationMapper;
import com.moyun.ledger.mapper.LedgerProductMapper;
import com.moyun.ledger.mapper.LedgerPurchaseOrderItemMapper;
import com.moyun.ledger.mapper.LedgerPurchaseOrderMapper;
import com.moyun.ledger.mapper.LedgerSaleOrderItemMapper;
import com.moyun.ledger.mapper.LedgerSaleOrderMapper;
import com.moyun.ledger.mapper.LedgerStockFlowMapper;
import com.moyun.ledger.mapper.LedgerStockMapper;
import com.moyun.ledger.mapper.LedgerWarehouseMapper;
import com.moyun.ledger.service.ILedgerOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单服务实现：赊账下单 = 建单 + 建债权债务 + 变动库存，三件事同一事务原子完成。
 *
 * @author moyun
 */
@Service
public class LedgerOrderServiceImpl implements ILedgerOrderService {

    @Autowired
    private LedgerWarehouseMapper warehouseMapper;
    @Autowired
    private LedgerProductMapper productMapper;
    @Autowired
    private LedgerStockMapper stockMapper;
    @Autowired
    private LedgerStockFlowMapper stockFlowMapper;
    @Autowired
    private LedgerObligationMapper obligationMapper;
    @Autowired
    private LedgerSaleOrderMapper saleOrderMapper;
    @Autowired
    private LedgerSaleOrderItemMapper saleItemMapper;
    @Autowired
    private LedgerPurchaseOrderMapper purchaseOrderMapper;
    @Autowired
    private LedgerPurchaseOrderItemMapper purchaseItemMapper;

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private static BigDecimal nz(BigDecimal v) { return v == null ? ZERO : v; }

    // ---------------- 销售 ----------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSaleOrder(Long userId, LedgerSaleOrder order, List<LedgerSaleOrderItem> items) {
        LedgerWarehouse wh = getOwnedWarehouse(userId, order.getWarehouseId());
        if (items == null || items.isEmpty()) {
            throw new ServiceException("请添加商品明细");
        }
        if (!LedgerSaleOrder.PAY_CREDIT.equals(order.getPaymentType())) {
            throw new ServiceException("暂只支持赊账下单（现结后续实现）");
        }

        // 算金额（售价/成本/利润）
        BigDecimal total = ZERO, cost = ZERO;
        for (LedgerSaleOrderItem it : items) {
            LedgerProduct p = getOwnedProduct(userId, it.getProductId());
            BigDecimal qty = BigDecimal.valueOf(it.getQuantity() == null ? 0 : it.getQuantity());
            if (qty.compareTo(ZERO) <= 0) {
                throw new ServiceException("数量必须大于0");
            }
            BigDecimal unit = nz(it.getUnitPrice());
            BigDecimal costUnit = nz(p.getCostPrice());
            it.setUnitPrice(unit);
            it.setCostPrice(costUnit);
            it.setSubtotal(unit.multiply(qty));
            it.setCostSubtotal(costUnit.multiply(qty));
            it.setProfit(unit.subtract(costUnit).multiply(qty));
            total = total.add(it.getSubtotal());
            cost = cost.add(it.getCostSubtotal());
        }

        order.setUserId(userId);
        order.setOperatorId(userId);
        order.setTotalAmount(total);
        order.setCostAmount(cost);
        order.setProfitAmount(total.subtract(cost));
        order.setStatus(LedgerSaleOrder.STATUS_NORMAL);
        saleOrderMapper.insert(order);

        for (LedgerSaleOrderItem it : items) {
            it.setSaleId(order.getId());
            it.setStatus(LedgerSaleOrderItem.STATUS_NORMAL);
            saleItemMapper.insert(it);
            // 出库：减库存 + 写流水
            changeStock(userId, wh.getId(), it.getProductId(), -it.getQuantity(), LedgerStockFlow.FLOW_OUT,
                    "销售出库-" + order.getId());
        }

        // 赊账：自动建应收
        LedgerObligation ob = new LedgerObligation();
        ob.setUserId(userId);
        ob.setType(LedgerObligation.TYPE_RECEIVABLE);
        ob.setCounterparty(order.getCustomerName());
        ob.setCounterpartyId(order.getCustomerId());
        ob.setWarehouseId(wh.getId());
        ob.setSourceType(LedgerObligation.SOURCE_SALE_CREDIT);
        ob.setSourceId(order.getId());
        ob.setTotalAmount(total);
        ob.setPaidAmount(ZERO);
        ob.setSettleStatus(LedgerObligation.SETTLE_PENDING);
        ob.setStatus(LedgerObligation.STATUS_NORMAL);
        obligationMapper.insert(ob);

        order.setObligationId(ob.getId());
        saleOrderMapper.updateById(order);
        return order.getId();
    }

    // ---------------- 采购 ----------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPurchaseOrder(Long userId, LedgerPurchaseOrder order, List<LedgerPurchaseOrderItem> items) {
        LedgerWarehouse wh = getOwnedWarehouse(userId, order.getWarehouseId());
        if (items == null || items.isEmpty()) {
            throw new ServiceException("请添加商品明细");
        }
        if (!LedgerPurchaseOrder.PAY_CREDIT.equals(order.getPaymentType())) {
            throw new ServiceException("暂只支持赊账下单（现结后续实现）");
        }

        BigDecimal total = ZERO;
        for (LedgerPurchaseOrderItem it : items) {
            getOwnedProduct(userId, it.getProductId());
            BigDecimal qty = BigDecimal.valueOf(it.getQuantity() == null ? 0 : it.getQuantity());
            if (qty.compareTo(ZERO) <= 0) {
                throw new ServiceException("数量必须大于0");
            }
            it.setUnitPrice(nz(it.getUnitPrice()));
            it.setSubtotal(it.getUnitPrice().multiply(qty));
            total = total.add(it.getSubtotal());
        }

        order.setUserId(userId);
        order.setOperatorId(userId);
        order.setTotalAmount(total);
        order.setStatus(LedgerPurchaseOrder.STATUS_NORMAL);
        purchaseOrderMapper.insert(order);

        for (LedgerPurchaseOrderItem it : items) {
            it.setPurchaseId(order.getId());
            it.setStatus(LedgerPurchaseOrderItem.STATUS_NORMAL);
            purchaseItemMapper.insert(it);
            changeStock(userId, wh.getId(), it.getProductId(), it.getQuantity(), LedgerStockFlow.FLOW_IN,
                    "采购入库-" + order.getId());
        }

        LedgerObligation ob = new LedgerObligation();
        ob.setUserId(userId);
        ob.setType(LedgerObligation.TYPE_PAYABLE);
        ob.setCounterparty(order.getSupplierName());
        ob.setWarehouseId(wh.getId());
        ob.setSourceType(LedgerObligation.SOURCE_PURCHASE_CREDIT);
        ob.setSourceId(order.getId());
        ob.setTotalAmount(total);
        ob.setPaidAmount(ZERO);
        ob.setSettleStatus(LedgerObligation.SETTLE_PENDING);
        ob.setStatus(LedgerObligation.STATUS_NORMAL);
        obligationMapper.insert(ob);

        order.setObligationId(ob.getId());
        purchaseOrderMapper.updateById(order);
        return order.getId();
    }

    // ---------------- 内部 ----------------

    /** 库存变动 + 流水同写（与库存服务同一套口径：成本价×数量） */
    private void changeStock(Long userId, Long warehouseId, Long productId, int change, String flowType, String remark) {
        LedgerProduct p = getOwnedProduct(userId, productId);
        LedgerStock s = stockMapper.selectOne(new LambdaQueryWrapper<LedgerStock>()
                .eq(LedgerStock::getWarehouseId, warehouseId)
                .eq(LedgerStock::getProductId, productId));
        if (s == null) {
            throw new ServiceException("商品「" + p.getName() + "」库存记录不存在");
        }
        int before = s.getQuantity() == null ? 0 : s.getQuantity();
        int after = before + change;
        if (after < 0) {
            throw new ServiceException("商品「" + p.getName() + "」库存不足：当前 " + before + "，本次 " + Math.abs(change));
        }
        BigDecimal costPrice = nz(p.getCostPrice());
        s.setQuantity(after);
        s.setCostValue(costPrice.multiply(BigDecimal.valueOf(after)));
        stockMapper.updateById(s);

        LedgerStockFlow f = new LedgerStockFlow();
        f.setWarehouseId(warehouseId);
        f.setProductId(productId);
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
    }

    private LedgerWarehouse getOwnedWarehouse(Long userId, Long id) {
        LedgerWarehouse w = warehouseMapper.selectById(id);
        if (w == null || !userId.equals(w.getUserId()) || LedgerWarehouse.STATUS_DELETED == w.getStatus()) {
            throw new ServiceException("仓库不存在");
        }
        return w;
    }

    private LedgerProduct getOwnedProduct(Long userId, Long id) {
        LedgerProduct p = productMapper.selectById(id);
        if (p == null || !userId.equals(p.getUserId()) || LedgerProduct.STATUS_DELETED == p.getStatus()) {
            throw new ServiceException("商品不存在");
        }
        return p;
    }
}
