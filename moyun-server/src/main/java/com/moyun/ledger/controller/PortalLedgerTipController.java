package com.moyun.ledger.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.ledger.service.ILedgerTipService;
import com.moyun.portal.util.PortalSecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 门户记账-打赏控制器（演示：模拟支付成功即落库）
 *
 * @author moyun
 */
@RestController
@RequestMapping("/portal/ledger/tips")
public class PortalLedgerTipController {

    @Autowired
    private ILedgerTipService tipService;

    /** 累计打赏金额 */
    @GetMapping("/total")
    public AjaxResult total() {
        Long userId = PortalSecurityUtils.getUserId();
        BigDecimal total = tipService.totalAmount(userId);
        return AjaxResult.success(Map.of("totalAmount", total));
    }

    /** 发起打赏（模拟支付成功） */
    @PostMapping
    public AjaxResult create(@RequestBody Map<String, Object> body) {
        Long userId = PortalSecurityUtils.getUserId();
        BigDecimal amount = new BigDecimal(String.valueOf(body.get("amount")));
        String payWay = body.get("payWay") == null ? null : String.valueOf(body.get("payWay"));
        String target = body.get("target") == null ? null : String.valueOf(body.get("target"));
        String reason = body.get("reason") == null ? null : String.valueOf(body.get("reason"));
        Long id = tipService.createTip(userId, amount, payWay, target, reason);
        return AjaxResult.success("赞赏成功，感谢支持！", Map.of("id", id));
    }
}