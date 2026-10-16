package com.moyun.ledger.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.ledger.domain.entity.LedgerPurchaseOrder;
import com.moyun.ledger.domain.entity.LedgerPurchaseOrderItem;
import com.moyun.ledger.domain.entity.LedgerSaleOrder;
import com.moyun.ledger.domain.entity.LedgerSaleOrderItem;
import com.moyun.ledger.service.ILedgerOrderService;
import com.moyun.portal.util.PortalSecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 门户-订单（销售/采购下单，赊账自动联动债权债务与库存）。
 *
 * @author moyun
 */
@Tag(name = "门户-订单")
@RestController
@RequestMapping("/portal/ledger/orders")
public class PortalLedgerOrderController {

    @Autowired
    private ILedgerOrderService orderService;

    @Operation(summary = "销售下单（赊销自动建应收 + 出库）")
    @PostMapping("/sale")
    public AjaxResult createSaleOrder(@RequestBody Map<String, Object> body) {
        Long userId = PortalSecurityUtils.getUserId();
        LedgerSaleOrder order = new LedgerSaleOrder();
        order.setWarehouseId(longVal(body, "warehouseId"));
        order.setCustomerName(strVal(body, "customerName"));
        order.setCustomerId(longVal(body, "customerId"));
        order.setPaymentType(strVal(body, "paymentType"));
        order.setRemark(strVal(body, "remark"));

        List<LedgerSaleOrderItem> items = new ArrayList<>();
        for (Object o : listVal(body, "items")) {
            Map<String, Object> it = (Map<String, Object>) o;
            LedgerSaleOrderItem i = new LedgerSaleOrderItem();
            i.setProductId(longVal(it, "productId"));
            i.setQuantity(intVal(it, "quantity"));
            i.setUnitPrice(new BigDecimal(String.valueOf(it.getOrDefault("unitPrice", "0"))));
            items.add(i);
        }
        return AjaxResult.success(orderService.createSaleOrder(userId, order, items));
    }

    @Operation(summary = "采购下单（赊购自动建应付 + 入库）")
    @PostMapping("/purchase")
    public AjaxResult createPurchaseOrder(@RequestBody Map<String, Object> body) {
        Long userId = PortalSecurityUtils.getUserId();
        LedgerPurchaseOrder order = new LedgerPurchaseOrder();
        order.setWarehouseId(longVal(body, "warehouseId"));
        order.setSupplierName(strVal(body, "supplierName"));
        order.setSupplierId(longVal(body, "supplierId"));
        order.setPaymentType(strVal(body, "paymentType"));
        order.setRemark(strVal(body, "remark"));

        List<LedgerPurchaseOrderItem> items = new ArrayList<>();
        for (Object o : listVal(body, "items")) {
            Map<String, Object> it = (Map<String, Object>) o;
            LedgerPurchaseOrderItem i = new LedgerPurchaseOrderItem();
            i.setProductId(longVal(it, "productId"));
            i.setQuantity(intVal(it, "quantity"));
            i.setUnitPrice(new BigDecimal(String.valueOf(it.getOrDefault("unitPrice", "0"))));
            items.add(i);
        }
        return AjaxResult.success(orderService.createPurchaseOrder(userId, order, items));
    }

    // ---------------- 内部：容错取值 ----------------

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

    private List<Object> listVal(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v instanceof List ? (List<Object>) v : new ArrayList<>();
    }
}
