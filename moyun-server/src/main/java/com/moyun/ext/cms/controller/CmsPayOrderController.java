package com.moyun.ext.cms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.pay.domain.entity.LedgerEntry;
import com.moyun.pay.domain.entity.PayOrder;
import com.moyun.pay.gateway.IPayGateway;
import com.moyun.pay.mapper.LedgerEntryMapper;
import com.moyun.pay.mapper.PayOrderMapper;
import com.moyun.util.bean.PageUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CMS 支付订单后台管理 Controller（V11.0）
 *
 * <p>统一支付单列表/详情（含分账明细）/手动关单。
 *
 * @author moyun
 */
@Tag(name = "CMS支付订单管理", description = "公共支付通道订单后台管理")
@RestController
@RequestMapping("/cms/pay/order")
public class CmsPayOrderController extends BaseController {

    @Autowired
    private PayOrderMapper payOrderMapper;

    @Autowired
    private LedgerEntryMapper ledgerEntryMapper;

    @Autowired
    private IPayGateway payGateway;

    @Operation(summary = "支付订单列表", description = "分页查询统一支付单，支持状态/业务类型/支付单号筛选")
    @PreAuthorize("@ss.hasPermi('cms:payOrder:list')")
    @GetMapping("/list")
    public AjaxResult list(@RequestParam(required = false) String status,
                           @RequestParam(required = false) String bizType,
                           @RequestParam(required = false) String payNo) {
        Page<PayOrder> page = PageUtils.startPage();
        LambdaQueryWrapper<PayOrder> wrapper = new LambdaQueryWrapper<PayOrder>()
                .eq(status != null && !status.isBlank(), PayOrder::getStatus, status)
                .eq(bizType != null && !bizType.isBlank(), PayOrder::getBizType, bizType)
                .like(payNo != null && !payNo.isBlank(), PayOrder::getPayNo, payNo)
                .orderByDesc(PayOrder::getId);
        payOrderMapper.selectPage(page, wrapper);
        page.getRecords().forEach(this::fillYuan);
        return success(page);
    }

    @Operation(summary = "支付订单详情（含分账明细）", description = "支付单详情 + 该单全部分账流水")
    @PreAuthorize("@ss.hasPermi('cms:payOrder:query')")
    @GetMapping("/{payNo}")
    public AjaxResult detail(@PathVariable String payNo) {
        PayOrder order = payGateway.getByPayNo(payNo);
        if (order == null) {
            return error("支付单不存在");
        }
        fillYuan(order);
        List<LedgerEntry> entries = ledgerEntryMapper.selectList(new LambdaQueryWrapper<LedgerEntry>()
                .eq(LedgerEntry::getPayNo, payNo)
                .orderByAsc(LedgerEntry::getId));
        entries.forEach(e -> {
            if (e.getAmount() != null) {
                e.setAmountYuan(BigDecimal.valueOf(e.getAmount(), 2));
            }
            if (e.getBalanceAfter() != null) {
                e.setBalanceAfterYuan(BigDecimal.valueOf(e.getBalanceAfter(), 2));
            }
        });
        Map<String, Object> data = new HashMap<>();
        data.put("order", order);
        data.put("ledgerEntries", entries);
        return success(data);
    }

    @Operation(summary = "手动关单", description = "对待支付订单手动关单（原因 ADMIN_MANUAL_CLOSE），已支付/已分账单不可关")
    @PreAuthorize("@ss.hasPermi('cms:payOrder:close')")
    @PostMapping("/{payNo}/close")
    public AjaxResult close(@PathVariable String payNo) {
        PayOrder before = payGateway.getByPayNo(payNo);
        if (before == null) {
            return error("支付单不存在");
        }
        if (!PayOrder.STATUS_CREATED.equals(before.getStatus())) {
            return error("仅待支付订单可关单，当前状态：" + before.getStatus());
        }
        PayOrder after = payGateway.closeOrder(payNo, "ADMIN_MANUAL_CLOSE");
        return success(after);
    }

    private void fillYuan(PayOrder order) {
        if (order.getAmount() != null) {
            order.setAmountYuan(BigDecimal.valueOf(order.getAmount(), 2));
        }
    }
}
