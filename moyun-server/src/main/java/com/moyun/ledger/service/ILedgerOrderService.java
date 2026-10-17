package com.moyun.ledger.service;

import com.moyun.ledger.domain.entity.LedgerPurchaseOrder;
import com.moyun.ledger.domain.entity.LedgerPurchaseOrderItem;
import com.moyun.ledger.domain.entity.LedgerSaleOrder;
import com.moyun.ledger.domain.entity.LedgerSaleOrderItem;

import java.util.List;

/**
 * 订单服务：销售/采购下单，赊账时自动联动账户与库存。
 *
 * <p>「库存赊账自动生成应收/应付」的核心：
 * 赊销 → 减库存 + 自动建应收账户（ledger_asset_account, account_type=receivable）；
 * 赊购 → 加库存 + 自动建应付账户（ledger_liability_account, account_type=credit）。</p>
 *
 * @author moyun
 */
public interface ILedgerOrderService {

    /** 销售下单。credit=赊销（自动建应收 + 出库）；cash=现结（暂不支持，后续接入资金流水） */
    Long createSaleOrder(Long userId, LedgerSaleOrder order, List<LedgerSaleOrderItem> items);

    /** 采购下单。credit=赊购（自动建应付 + 入库） */
    Long createPurchaseOrder(Long userId, LedgerPurchaseOrder order, List<LedgerPurchaseOrderItem> items);
}
